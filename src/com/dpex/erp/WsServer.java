package com.dpex.erp;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * websocket 服务端
 */
public class WsServer {
    public static void main(String[] args) throws IOException{
        new WsServer().service();
    }

    //服务端口
    private int port = 9898;
    private ServerSocket serverSocket;

    //构造方法，开启ServerSocket
    public WsServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("服务器启动");
    }

    //用Handler线程处理socket连接
    private void service() {
        Socket socket = null;
        while (true) {
            try {
                //监听客户端连接
                socket = serverSocket.accept();
                System.out.println("获取socket连接");
                Thread workThread = new Thread(new WsServer.Handler(socket));
                workThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //WsServer内部类 处理单个socket
    class Handler implements Runnable {
        private Socket socket;
        //记录socket是否已握手
        private boolean hasHandshake = false;
        Charset charset = Charset.forName("UTF-8");

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private PrintWriter getWriter(Socket socket) throws IOException {
            //获取socket输出流
            OutputStream socketOut = socket.getOutputStream();
            return new PrintWriter(socketOut, true);
        }


        public String echo(String msg) {
            return "echo:" + msg;
        }

        public void run() {

            try {
                System.out.println("New connection accepted"
                        + socket.getInetAddress() + ":" + socket.getPort());
                //获取socket输入流
                InputStream in = socket.getInputStream();
                //获取socket输出流
                PrintWriter pw = getWriter(socket);
                //读入缓存
                byte[] buf = new byte[1024];
                //读到字节
                int len = in.read(buf, 0, 1024);
                //读到字节数组
                byte[] res = new byte[len];
                System.arraycopy(buf, 0, res, 0, len);
                String key = new String(res);
                if(!hasHandshake && key.indexOf("Key") > 0){
                    //握手
                    key = key.substring(0, key.indexOf("==") + 2);
                    //截取Sec-WebSocket-Key值
                    key = key.substring(key.indexOf("Key") + 4, key.length()).trim();
                    key+= "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                    //拼接key值+UUID 并且对拼接完以后的字符串进行sha1加密，然后base64编码
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(key.getBytes("utf-8"), 0, key.length());
                    byte[] sha1Hash = md.digest();
                    sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
                    key = encoder.encode(sha1Hash);

                    pw.println("HTTP/1.1 101 Switching Protocols");
                    pw.println("Upgrade: websocket");
                    pw.println("Connection: Upgrade");
                    pw.println("Sec-WebSocket-Accept: " + key);
                    pw.println();
                    pw.flush();
                    //发送回应信息，握手成功开始通信
                    hasHandshake = true;

                    //接收数据
                    byte[] first = new byte[1];
                    //这里会阻塞
                    int read = in.read(first, 0, 1);
                    while(read > 0){
                        int b = first[0] & 0xFF;
                        //1为字符数据，8为关闭socket
                        byte opCode = (byte) (b & 0x0F);

                        if(opCode == 8){
                            socket.getOutputStream().close();
                            break;
                        }
                        b = in.read();
                        int payloadLength = b & 0x7F;
                        if (payloadLength == 126) {
                            byte[] extended = new byte[2];
                            in.read(extended, 0, 2);
                            int shift = 0;
                            payloadLength = 0;
                            for (int i = extended.length - 1; i >= 0; i--) {
                                payloadLength = payloadLength + ((extended[i] & 0xFF) << shift);
                                shift += 8;
                            }

                        } else if (payloadLength == 127) {
                            byte[] extended = new byte[8];
                            in.read(extended, 0, 8);
                            int shift = 0;
                            payloadLength = 0;
                            for (int i = extended.length - 1; i >= 0; i--) {
                                payloadLength = payloadLength + ((extended[i] & 0xFF) << shift);
                                shift += 8;
                            }
                        }

                        //掩码
                        byte[] mask = new byte[4];
                        in.read(mask, 0, 4);
                        int readThisFragment = 1;
                        ByteBuffer byteBuf = ByteBuffer.allocate(payloadLength + 10);
                        ByteBuffer byteBuf2 = ByteBuffer.allocate(payloadLength + 1024);
                        while(payloadLength > 0){
                            int masked = in.read();
                            masked = masked ^ (mask[(int) ((readThisFragment - 1) % 4)] & 0xFF);
                            byteBuf.put((byte) masked);
                            payloadLength--;
                            readThisFragment++;
                        }
                        byteBuf.flip();
//                        String clientMsg = printRes(byteBuf.array()); //在控制台打印发送给客户端的信息
                        responseClient(byteBuf, true);
//                        String printResult = QuietPrint.handle(clientMsg);  //处理打印，返回结果
//                        byteBuf2.put(printResult.getBytes("UTF-8"));
//                        responseClient(byteBuf2, true); //将打印结果发送给客户端

                        in.read(first, 0, 1);
                    }

                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //发送信息给客户端
        private void responseClient(ByteBuffer byteBuf, boolean finalFragment) throws IOException {
            OutputStream out = socket.getOutputStream();
            int first = 0x00;
            //是否是输出最后的WebSocket响应片段
            if (finalFragment) {
                first = first + 0x80;
                first = first + 0x1;
            }
            out.write(first);


            if (byteBuf.limit() < 126) {
                out.write(byteBuf.limit());
            } else if (byteBuf.limit() < 65536) {
                out.write(126);
                out.write(byteBuf.limit() >>> 8);
                out.write(byteBuf.limit() & 0xFF);
            } else {
                // Will never be more than 2^31-1
                out.write(127);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(byteBuf.limit() >>> 24);
                out.write(byteBuf.limit() >>> 16);
                out.write(byteBuf.limit() >>> 8);
                out.write(byteBuf.limit() & 0xFF);

            }

            // Write the content
            out.write(byteBuf.array(), 0, byteBuf.limit());
            out.flush();
        }

        //在控制台打印发送给客户端的响应信息
        private String printRes(byte[] array) {
            ByteArrayInputStream  byteIn = new ByteArrayInputStream(array);
            InputStreamReader reader = new InputStreamReader(byteIn, charset.newDecoder());
            int b = 0;
            String res = "";
            try {
                while((b = reader.read()) > 0){
                    res += (char)b;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "";
            }
//            System.out.println("客户端发来："+res);
            return res;
        }
    }


}

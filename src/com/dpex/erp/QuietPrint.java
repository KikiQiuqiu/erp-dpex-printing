package com.dpex.erp;

import javax.print.*;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QuietPrint {

    public static String handle(String msg) throws JSONException, IOException {
        JSONArray arr = new JSONArray(msg);
        ArrayList<String> responseArr = new ArrayList<String>(); //响应的数组，包含文件名，错误信息
        String responseStr = ""; //响应数据json格式
        JSONArray responseJSON = new JSONArray(msg);
        String urlpath; //远程文件完整路径
        String filename; //文件名称

        //循环要打印的文件的内容
        for (int i = 0; i < arr.length(); i++) {
            JSONObject temp = (JSONObject) arr.get(i);
            urlpath = temp.getString("urlpath");
            filename = temp.getString("filename");
            HashMap<String,String> fileResult = ErpFile.handle(urlpath,filename);
            if(fileResult.get("error") == null){
                //读取文件未发生错误，开始打印
                HashMap<String,String> printResult = print(fileResult.get("localpath"));

                if( "PrinterNotFoundError".equals(printResult.get("status")) ){
                    //如果发生打印机未找到错误，则直接跳出循环，返回not found错误
                    responseStr = "{\"error\":\""+printResult.get("error")+"\"}";
                    break;
                }
                //普通错误
                else if("error".equals(printResult.get("status"))){
                    fileResult.put("error",printResult.get("error"));
                }
            }
            responseArr.add(arrayToJSON(fileResult));
        }
        //未发生打印机未找到错误
        if(responseStr.length() < 1){
            responseStr = "[" + String.join(",",responseArr) + "]";
        }

        return responseStr;
    }

    public static HashMap print(String filepath){
        HashMap result = new HashMap();
        FileInputStream psStream = null;
        try {
            psStream = new FileInputStream(filepath);
//            psStream = new FileInputStream(new File("\\\\www.t.com\\123.txt"));
        } catch (FileNotFoundException ffne) {
            ffne.printStackTrace();
        }
        if (psStream == null) {
            result.put("status","error");
            result.put("error","file not found");
            return result;
        }
        //设置打印数据的格式
        DocFlavor psInFormat = DocFlavor.INPUT_STREAM.AUTOSENSE;
        //创建打印数据
//		DocAttributeSet docAttr = new HashDocAttributeSet();//设置文档属性
//		Doc myDoc = new SimpleDoc(psStream, psInFormat, docAttr);
        Doc myDoc = new SimpleDoc(psStream, psInFormat, null);

        //设置打印属性
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(new Copies(1));//打印份数
        //查找所有打印服务
        PrintService[] services = PrintServiceLookup.lookupPrintServices(psInFormat, aset);
        //定位默认的打印服务
        PrintService myPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (myPrinter != null) {
            DocPrintJob job = myPrinter.createPrintJob();//创建文档打印作业
            try {
                job.print(myDoc, aset);//打印文档
                result.put("status","ok");

            } catch (Exception pe) {
                pe.printStackTrace();
                result.replace("status","error");
                result.put("error",pe.getMessage());
                return result;
            }
        } else {
            result.put("status","PrinterNotFoundError");
            result.put("error","no printer services found");
        }
        return result;
    }


    public static void main(String args[]) throws JSONException {


    }


    public static String arrayToJSON(HashMap<String,String> map){
        //{"k":"v","key":"value"}
        StringBuffer sBuffer = new StringBuffer("");
        String start = "{";
        String end = "}";
        String separator = ":";
        sBuffer.append(start);
        for(String key :map.keySet()){
            sBuffer.append("\"");
            sBuffer.append(key);
            sBuffer.append("\"");
            sBuffer.append(separator);
            sBuffer.append("\"");
            sBuffer.append(map.get(key));
            sBuffer.append("\"");
            sBuffer.append(",");
        }
        sBuffer.substring(0,sBuffer.length()-1);
        sBuffer.append(end);
        return sBuffer.toString();
    }




    public static void main1(String[] args) {
        System.out.println("Hello World!");
        JFileChooser fileChooser = new JFileChooser(); //创建打印作业
        int state = fileChooser.showOpenDialog(null);
        if(state == fileChooser.APPROVE_OPTION){
            // File file = new File("D:/haha.txt"); //获取选择的文件
            File file = fileChooser.getSelectedFile();//获取选择的文件
            //构建打印请求属性集
            HashPrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
            //设置打印格式，因为未确定类型，所以选择autosense
            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            //查找所有的可用的打印服务
            PrintService printService[] = PrintServiceLookup.lookupPrintServices(flavor, pras);
            //定位默认的打印服务
            PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
            //显示打印对话框
            PrintService service = ServiceUI.printDialog(null, 200, 200, printService,
                    defaultService, flavor, pras);
            if(service != null){
                try {
                    DocPrintJob job = service.createPrintJob(); //创建打印作业
                    FileInputStream fis = new FileInputStream(file); //构造待打印的文件流
                    DocAttributeSet das = new HashDocAttributeSet();
                    Doc doc = new SimpleDoc(fis, flavor, das);
                    job.print(doc, pras);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

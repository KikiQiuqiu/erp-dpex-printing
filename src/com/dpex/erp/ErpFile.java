package com.dpex.erp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class ErpFile {

    public static void main(String[] args) {

    }

    public static String mkdir() throws IOException{
        String dir = System.getProperty("user.dir");
        File directory = new File(dir+"/../erpfile");
        if(!directory.exists()){
            directory.mkdir();
        }
        return directory.getCanonicalPath();
    }

    /**
     * 处理文件下载，返回文件远程路径、本地路径
     * @param urlpath
     * @param filename
     * @return
     * @throws IOException
     */
    public static HashMap handle(String urlpath,String filename) throws IOException{
        HashMap<String,String> result = new HashMap<String,String>();
        String localFilePath = mkdir()+"\\"+filename;
        result = downloadFile(urlpath,localFilePath);
        return result;
    }

    /**
     * 下载远程文件并保存到本地
     *
     * @param remoteFilePath-远程文件路径
     * @param localFilePath-本地文件路径（带文件名）
     */
    public static HashMap<String,String> downloadFile(String remoteFilePath, String localFilePath) {
        HashMap<String,String> res = new HashMap<String,String>();
        URL urlfile = null;
        HttpURLConnection httpUrl = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        File f = new File(localFilePath);
        try {
            urlfile = new URL(remoteFilePath);
            httpUrl = (HttpURLConnection) urlfile.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(f));
            int len = 2048;
            byte[] b = new byte[len];
            while ((len = bis.read(b)) != -1) {
                bos.write(b, 0, len);
            }
            bos.flush();
            bis.close();
            httpUrl.disconnect();
            res.put("urlpath",remoteFilePath);
            res.put("localpath",localFilePath);
            res.put("status","ok");

        } catch (Exception e) {
            e.printStackTrace();
            res.replace("status","error");
            res.put("error",e.getMessage());

        } finally {
            try {
                bis.close();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
                res.replace("error",e.getMessage());
                return res;
            }
            return res;
        }

    }
}

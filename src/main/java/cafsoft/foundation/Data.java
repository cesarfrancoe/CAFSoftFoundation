package cafsoft.foundation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//
//  Data.java
//
//  Created by Cesar Franco on 10/07/16.
//  Copyright © 2015 Cesar Franco. All rights reserved.
//
public class Data {

    private static final int BUFFER_SIZE = 100 * 1024; // ~130K.
    private ByteBuffer byteData = null;

    /*public Data(byte[] bytes, int length) {
        byteData = ByteBuffer.wrap(bytes, 0, length);
    }*/
    public Data(int count) {
        byteData = ByteBuffer.allocate(count);
    }

    public Data(String text, Charset charset){
       byteData = ByteBuffer.wrap(text.getBytes(charset));
    }
    
    public Data(String text){
       this(text, Charset.forName("UTF-8"));
    }

    public Data(URL url)
            throws IOException, URISyntaxException {

        read(url);
    }
    

    /*
    Read bytes from a InputStream
     */
    private static ByteBuffer getBytes(InputStream inStream)
            throws IOException {

        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream outStream = null;
        ByteBuffer byteData = null;

        outStream = new ByteArrayOutputStream(buffer.length);
        int bytesRead = 0;
        while (bytesRead != -1) {
            bytesRead = inStream.read(buffer);
            if (bytesRead > 0) {
                outStream.write(buffer, 0, bytesRead);
            }
        }
        byteData = ByteBuffer.wrap(outStream.toByteArray());

        return byteData;
    }

    public void read(String path)
            throws IOException {

        File inFile = new File(path);
        try (InputStream inStream = new FileInputStream(inFile)) {
            byteData = getBytes(inStream);
        }
    }

    public void read(URL url)
            throws IOException, URISyntaxException {

        final String protocol = url.getProtocol();

        if (protocol.equals("file")) {
            read(url.toURI().getPath());
        }
        else if (protocol.equals("http")) {
            InputStream inStream = null;
            int response = -1;
            URLConnection conn = null;

            conn = url.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                httpConn.setReadTimeout(10000);
                httpConn.setConnectTimeout(15000);
                httpConn.setAllowUserInteraction(false);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod("GET");
                httpConn.setDoInput(true);
                httpConn.connect();
                response = httpConn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {
                    inStream = httpConn.getInputStream();
                    byteData = getBytes(inStream);
                    inStream.close();
                }
                httpConn.disconnect();
            }
        }
    }

    /* 
    Writes the data object's bytes to the file specified by a given path.
    @path The location to which to write the receiver's bytes.
    @useAuxiliaryFile If true, the data is written to a backup file, and 
    then—assuming no errors occur—the backup file is renamed to the name 
    specified by path; otherwise, the data is written directly to path.
     */
    private boolean write(String path, boolean useAuxiliaryFile) {
        boolean wasWritten = false;
        File inFile = null;
        FileOutputStream fis = null;

        try {
            if (useAuxiliaryFile) {
                inFile = File.createTempFile("tmp", ".tmp");
            }
            else {
                inFile = new File(path);
            }

            fis = new FileOutputStream(inFile);
            fis.write(byteData.array());

            inFile.setLastModified(new Date().getTime());
            fis.close();

            if (useAuxiliaryFile) {
                if (inFile.renameTo(new File(path))) {
                    wasWritten = true;
                }
            }
            else {
                wasWritten = true;
            }

        }
        catch (IOException e) {
        }

        return wasWritten;
    }

    public void write(URL url, boolean useAuxiliaryFile) 
            throws IOException, URISyntaxException {
        
        final String protocol = url.getProtocol();
        
        if (protocol.equals("file")) {
            write(url.toURI().getPath(), useAuxiliaryFile);
        }
    }
    public String toText(Charset charset) {
        return new String(byteData.array(), charset);
    }

    public String toText() {
        return toText(Charset.forName("UTF-8"));
    }

    public int length() {
        int length = 0;

        if (byteData != null) {
            length = byteData.array().length;
        }

        return length;
    }

    public boolean isEqual(Data other) {
        return byteData.equals(other.byteData);
    }

    public static Data dataWithContentsOfURL(URL url)
            throws IOException, URISyntaxException {

        Data data = new Data(url);
        if (data.length() == 0) {
            data = null;
        }

        return data;
    }

    public static Data dataWithContentsOfFile(String path)
            throws IOException {

        Data data = new Data(path);
        if (data.length() == 0) {
            data = null;
        }

        return data;
    }

}

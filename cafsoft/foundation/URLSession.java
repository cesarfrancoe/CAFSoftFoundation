/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cafsoft.foundation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ceaufres
 */
public class URLSession {

    //private int nextTaskIdentifier = 1;
    private OperationQueue workQueue = null;

    private static URLSession shared = null;

    private URLSessionConfiguration configuration = null;

    private URLSessionDelegate delegate = null;

    private URLSession(URLSessionConfiguration configuration,
            URLSessionDelegate delegate, OperationQueue queue) {

        this.configuration = configuration;
        this.delegate = delegate;
        //... = queue;

        workQueue = new OperationQueue();
    }

    public URLSession(URLSessionConfiguration configuration) {

        this(configuration, null, null);
    }

    public URLSession() {

        this(new URLSessionConfiguration());
    }

    public static URLSession getShared() {

        if (shared == null) {
            shared = new URLSession();
        }

        return shared;
    }

    private static void transfer(InputStream inStream,
            OutputStream outStream, long contentSize)
            throws IOException {

        final int BUFFER_SIZE = 100 * 1024;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        int total = 0;

        do {
            bytesRead = inStream.read(buffer);
            if (bytesRead > 0) {
                outStream.write(buffer, 0, bytesRead);
                total += bytesRead;
                //System.out.println("Write bytes: " + total + "/" + contentSize);
                //if (reading != null){
                //    reading.onReading(bytesRead);
                //}
            }
        } while (bytesRead != -1);
    }

    // Read bytes from connection and save its in memory block
    private static Data downloadStreamInData(InputStream inStream)
            throws IOException {

        ByteArrayOutputStream outStream = null;
        ByteBuffer byteData = null;

        outStream = new ByteArrayOutputStream(); // (buffer.length)
        transfer(inStream, outStream, -1);
        byteData = ByteBuffer.wrap(outStream.toByteArray());
        outStream.close();

        return new Data(byteData);
    }

    private static File downloadStreamInFile(InputStream inStream,
            long contentLength)
            throws IOException {

        FileOutputStream outStream = null;
        File tempFile = null;

        tempFile = File.createTempFile("tmp", ".tmp");
        outStream = new FileOutputStream(tempFile);
        transfer(inStream, outStream, contentLength);
        outStream.close();

        return tempFile;
    }

    private static void uploadStream(OutputStream outStream, byte[] bytesBuffer)
            throws IOException {

        outStream.write(bytesBuffer);
    }

    /*
    private static ByteBuffer getBytes(InputStream inStream)
            throws IOException {

        final int BUFFER_SIZE = 100 * 1024;

        byte[] buffer = new byte[BUFFER_SIZE];
        ByteArrayOutputStream outStream = null;
        ByteBuffer byteData = null;

        outStream = new ByteArrayOutputStream(buffer.length);
        int bytesRead = 0;
        while (bytesRead != -1) {
            bytesRead = inStream.read(buffer);
            if (bytesRead > 0) {
                outStream.write(buffer, 0, bytesRead);
                //if (reading != null){
                //    reading.onReading(bytesRead);
                //}
            }
        }
        byteData = ByteBuffer.wrap(outStream.toByteArray());

        return byteData;
    }
     */
 /*
    private int createNextTaskIdentifier() {

        int i = nextTaskIdentifier;

        nextTaskIdentifier = nextTaskIdentifier + 1;

        return i;
    }
     */
    private void sendGETRequest(URLRequest req,
            DataTaskCompletion completionHandler) {

        HttpURLConnection httpURLConnection = null;
        int respCode = -1;
        InputStream inStream = null;
        Data data = null;
        Error error = null;

        try {
            httpURLConnection = (HttpURLConnection) req.getUrl().openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod(req.getHttpMethod());

            addRequestProperties(httpURLConnection, req);

            httpURLConnection.setConnectTimeout(configuration.getConnectTimeout());
            httpURLConnection.setReadTimeout(configuration.getReadTimeout());
            //System.out.println(httpURLConnection.getConnectTimeout());
            //System.out.println(httpURLConnection.getReadTimeout());

            if (req.getHttpMethod().equals("POST")) {
                if (!req.getHttpBody().isEmpty()) {
                    OutputStream os = httpURLConnection.getOutputStream();
                    os.write(req.getHttpBody().getBytes());
                    os.flush();
                }
            }

            respCode = httpURLConnection.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                inStream = httpURLConnection.getInputStream();
                data = new Data(inStream);
                inStream.close();
            } else {
                error = new Error();
            }

            httpURLConnection.disconnect();

        } catch (IOException e) {
        }

        if (completionHandler != null) {
            HTTPURLResponse resp = null;
            resp = new HTTPURLResponse(req.getUrl(), respCode);

            completionHandler.run(data, resp, error);
        }
    }

    private void sendHttpPUTRequest(URLRequest request, Data bodyData,
            DataTaskCompletion completionHandler) {

        HttpURLConnection conn = null;
        int respCode = -1;
        InputStream inStream = null;
        Data data = null;
        Error error = null;
        HTTPURLResponse resp = null;

        try {
            conn = (HttpURLConnection) request.getUrl().openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod(request.getHttpMethod());

            addRequestProperties(conn, request);

            conn.setConnectTimeout(configuration.getConnectTimeout());
            conn.setReadTimeout(configuration.getReadTimeout());

            if (bodyData != null) {
                OutputStream os = conn.getOutputStream();
                /*InputStream stream = new ByteArrayInputStream(bodyData.toBytes());
                
                int i;
                // read byte by byte until end of stream
                while ((i = stream.read()) > 0) {
                    os.write(i);
                }
                 */
                //while (stream.re)
                os.write(bodyData.toBytes());
                os.close();
                //os.flush();
            }

            respCode = conn.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                inStream = conn.getInputStream();
                data = new Data(inStream);
                inStream.close();
            }

            resp = new HTTPURLResponse(request.getUrl(), respCode);

        } catch (IOException e) {
            error = new Error();
        }

        if (conn != null) {
            conn.disconnect();
        }

        if (completionHandler != null) {
            completionHandler.run(data, resp, error);
        }
    }

    private void addRequestProperties(HttpURLConnection connection,
            URLRequest request) {

        // Java 7 compatible code
        Set<Map.Entry<String, String>> allHeaders;
        allHeaders = request.getAllHttpHeaderFields().entrySet();
        for (Map.Entry<String, String> header : allHeaders) {
            String key = header.getKey();
            String value = header.getValue();
            connection.addRequestProperty(key, value);
        }

        ////Java 8 compatible code (Lambda expressions
        //request.getAllHttpHeaderFields().forEach((value, field) -> {
        //    connection.addRequestProperty(field, value);
        //});
    }

    /* Old Old 
    private void sendHttpRequest(URLRequest request, Data bodyData,
            DataTaskCompletionHandler completionHandler, 
            OutputStream outStream) {

        HttpURLConnection conn = null;
        int respCode = -1;
        InputStream inStream = null;
        Data data = null;
        Error error = null;
        HTTPURLResponse resp = null;

        try {
            conn = (HttpURLConnection) request.getUrl().openConnection();
            if (request.getHttpMethod().equals("POST") || request.getHttpMethod().equals("PUT")) {
                conn.setDoOutput(true);
                conn.setRequestMethod(request.getHttpMethod());

                addRequestProperties(conn, request);

                conn.setConnectTimeout(configuration.getConnectTimeout());
                conn.setReadTimeout(configuration.getReadTimeout());

                if (!request.getHttpBody().isEmpty()) {
                    OutputStream os = conn.getOutputStream();
                    os.write(request.getHttpBody().getBytes());
                    os.flush();
                } else if (bodyData != null) {
                    OutputStream os = conn.getOutputStream();
                    os.write(bodyData.toBytes());
                    os.flush();
                    //os.close();
                }

            } else { // Method GET
                conn.setRequestMethod(request.getHttpMethod());
                conn.setConnectTimeout(configuration.getConnectTimeout());
                conn.setReadTimeout(configuration.getReadTimeout());
            }

            respCode = conn.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                inStream = conn.getInputStream();
                data = new Data(inStream);
                inStream.close();
            }

            resp = new HTTPURLResponse(request.getUrl(), respCode);

        } catch (IOException e) {
            error = new Error();
        }

        if (conn != null) {
            conn.disconnect();
        }

        if (completionHandler != null) {
            completionHandler.run(data, resp, error);
        }
    }
     */
 /* Old
    private void sendHttpRequest(URLRequest request, Data bodyData,
            DataTaskCompletion completionHandler) {

        HttpURLConnection conn = null;
        Data data = null;
        Error error = null;
        HTTPURLResponse resp = null;
        int respCode = -1;

        try {
            conn = (HttpURLConnection) request.getUrl().openConnection();
            if (request.getHttpMethod().equals("POST") || request.getHttpMethod().equals("PUT")) {
                conn.setDoOutput(true);
                conn.setRequestMethod(request.getHttpMethod());

                addRequestProperties(conn, request);

                conn.setConnectTimeout(configuration.getConnectTimeout());
                conn.setReadTimeout(configuration.getReadTimeout());

                if (!request.getHttpBody().isEmpty()) {
                    OutputStream os = conn.getOutputStream();
                    os.write(request.getHttpBody().getBytes());
                    os.flush();
                } else if (bodyData != null) {
                    OutputStream os = conn.getOutputStream();
                    os.write(bodyData.toBytes());
                    os.flush();
                    //os.close();
                }

            } else { // Method GET
                conn.setRequestMethod(request.getHttpMethod());
                conn.setConnectTimeout(configuration.getConnectTimeout());
                conn.setReadTimeout(configuration.getReadTimeout());
            }

            respCode = conn.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                //inStream = conn.getInputStream();
                //outStream = new ByteArrayOutputStream(buffer.length);
                //data = new Data(inStream);
                data = downloadStreamInData(conn);
            }

            resp = new HTTPURLResponse(request.getUrl(), respCode);

        } catch (IOException e) {
            error = new Error();
        }

        if (conn != null) {
            conn.disconnect();
        }

        if (completionHandler != null) {
            completionHandler.run(data, resp, error);
        }
    }
     */
    public URLSessionDataTask dataTask(URLRequest request,
            DataTaskCompletion completionHandler) {

        Operation operation = null;

        operation = new BlockOperation(new Runnable() {
            @Override
            public void run() {
                String method = request.getHttpMethod();

                if (method.equals("GET") || method.equals("POST")) {
                    sendHttpRequest(request, null, completionHandler);
                }
            }
        });

        return new URLSessionDataTask(workQueue, operation);
    }

    public URLSessionUploadTask uploadTask(URLRequest request, Data bodyData,
            DataTaskCompletion completionHandler) {

        Operation operation = new BlockOperation(new Runnable() {
            @Override
            public void run() {
                String method = request.getHttpMethod();

                if (method.equals("PUT")) {
                    sendHttpRequest(request, bodyData, completionHandler);
                }
            }
        });

        return new URLSessionUploadTask(workQueue, operation);
    }

    public URLSessionDownloadTask downloadTask(URLRequest request,
            DownloadTaskCompletion completionHandler) {

        String protocol = request.getUrl().getProtocol();
        Operation operation = null;

        if (protocol.equals("http") || protocol.equals("https")) {
            operation = new BlockOperation(new Runnable() {
                @Override
                public void run() {

                    sendHttpRequest(request, null, completionHandler);
                }
            });
        } else {
            return null;
        }

        return new URLSessionDownloadTask(workQueue, operation);
    }

    public URLSessionDownloadTask downloadTask(URL url,
            DownloadTaskCompletion completionHandler) {

        return downloadTask(new URLRequest(url), completionHandler);
    }

    private void sendHttpRequest(URLRequest request, Data bodyData,
            DownloadTaskCompletion completionHandler) {

        HttpURLConnection urlConnection = null;
        File tempFile = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        URL url = null;
        URL newURL = null;
        int respCode = -1;
        Error error = null;
        URLResponse resp = null;
        long contentLength = -1;

        try {
            url = request.getUrl();
            urlConnection = (HttpURLConnection) url.openConnection();

            // Set configuration
            //urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod(request.getHttpMethod());
            addRequestProperties(urlConnection, request);
            urlConnection.setConnectTimeout(configuration.getConnectTimeout());
            urlConnection.setReadTimeout(configuration.getReadTimeout());

            if (!request.getHttpBody().isEmpty()) {
                urlConnection.setDoOutput(true);
                outStream = urlConnection.getOutputStream();
                uploadStream(outStream, request.getHttpBody().getBytes());

            } else if (bodyData != null) {
                urlConnection.setDoOutput(true);
                outStream = urlConnection.getOutputStream();
                uploadStream(outStream, bodyData.toBytes());
            }

            respCode = urlConnection.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                inStream = urlConnection.getInputStream();
                contentLength = urlConnection.getContentLengthLong();
                tempFile = downloadStreamInFile(inStream, contentLength);
                inStream.close();
            }

            resp = new HTTPURLResponse(request.getUrl(), respCode);

        } catch (IOException ex) {
            error = new Error();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        try {
            newURL = tempFile.toURI().toURL();
        } catch (MalformedURLException ex) {
            //Logger.getLogger(URLSession.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (completionHandler != null) {
            completionHandler.run(newURL, resp, error);
        }

        if (tempFile != null) {
            tempFile.delete();
        }
    }

    private void sendHttpRequest(URLRequest request, Data bodyData,
            DataTaskCompletion completionHandler) {

        HttpURLConnection urlConnection = null;
        Data data = null;
        InputStream inStream = null;
        OutputStream outStream = null;
        URL url = null;
        URL newURL = null;
        int respCode = -1;
        Error error = null;
        URLResponse resp = null;
        long contentLength = -1;

        try {
            url = request.getUrl();
            urlConnection = (HttpURLConnection) url.openConnection();

            // Set configuration
            urlConnection.setRequestMethod(request.getHttpMethod());
            addRequestProperties(urlConnection, request);
            urlConnection.setConnectTimeout(configuration.getConnectTimeout());
            urlConnection.setReadTimeout(configuration.getReadTimeout());

            if (!request.getHttpBody().isEmpty()) {
                urlConnection.setDoOutput(true);
                outStream = urlConnection.getOutputStream();
                uploadStream(outStream, request.getHttpBody().getBytes());

            } else if (bodyData != null) {
                urlConnection.setDoOutput(true);
                outStream = urlConnection.getOutputStream();
                uploadStream(outStream, bodyData.toBytes());
            }

            respCode = urlConnection.getResponseCode();
            if (respCode == HttpURLConnection.HTTP_OK) {
                inStream = urlConnection.getInputStream();
                contentLength = urlConnection.getContentLengthLong();
                data = downloadStreamInData(inStream);
                inStream.close();
            }

            resp = new HTTPURLResponse(request.getUrl(), respCode);

        } catch (IOException ex) {
            error = new Error();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (completionHandler != null) {
            completionHandler.run(data, resp, error);
        }
    }

    /* Old Old
    public URLSessionDataTask dataTask(URLRequest req,
            DataTaskCompletionHandler completionHandler) {

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                Data data = null;
                URLConnection conn = null;
                InputStream inStream = null;
                int respCode = -1;
                Error error = null;

                try {
                    conn = req.getUrl().openConnection();
                    if (conn instanceof HttpURLConnection) {
                        HttpURLConnection httpConn = (HttpURLConnection) conn;

                        respCode = httpConn.getResponseCode();

                        httpConn.setRequestMethod(req.getHttpMethod());

                        addRequestProperties(conn, request);

                        
                        httpConn.setConnectTimeout(configuration.getConnectTimeout());
                        httpConn.setReadTimeout(configuration.getReadTimeout());
                        //httpConn.setAllowUserInteraction(false);
                        //httpConn.setInstanceFollowRedirects(true);
                        //httpConn.setDoInput(true);
                        //httpConn.setDoOutput(true);

                        if (req.getHttpMethod().equals("POST")) {
                            if (!req.getHttpBody().isEmpty()) {
                                httpConn.setDoOutput(true);
                                OutputStream os = httpConn.getOutputStream();
                                os.write(req.getHttpBody().getBytes());
                                os.flush();
                            }
                        }

                        if (respCode == HttpURLConnection.HTTP_OK) {
                            inStream = httpConn.getInputStream();
                            data = new Data(inStream);
                            inStream.close();
                        } else {
                            error = new Error();
                        }
                        httpConn.disconnect();
                    }

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    //e.printStackTrace();
                }

                if (completionHandler != null) {
                    HTTPURLResponse resp = null;
                    resp = new HTTPURLResponse(req.getUrl(), respCode);

                    completionHandler.run(data, resp, error);
                }
            }
        };

        return new URLSessionDataTask(workQueue, runnable);
    }
     */
    public URLSessionDataTask dataTask(URLRequest request) {

        return dataTask(request, null);
    }

    public URLSessionDataTask dataTask(URL url) {

        return dataTask(new URLRequest(url), null);
    }

    public URLSessionDataTask dataTask(URL url,
            DataTaskCompletion completionHandler) {

        return dataTask(new URLRequest(url), completionHandler);
    }

    public interface TaskCompletion {
    }

    public interface DataTaskCompletion extends TaskCompletion {

        public abstract void run(Data data, URLResponse response, Error error);
    }

    public interface DownloadTaskCompletion extends TaskCompletion {

        public abstract void run(URL url, URLResponse response, Error error);
    }

    public interface UploadTaskCompletion extends TaskCompletion {

        public abstract void run(URL url, URLResponse response, Error error);
    }

}

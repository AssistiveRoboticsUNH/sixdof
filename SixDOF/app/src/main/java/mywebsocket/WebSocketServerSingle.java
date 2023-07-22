/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mywebsocket;

import android.os.Build;
import android.util.Log;
//import android.support.annotation.RequiresApi;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;



/**
 *
 * @author FridayLab
 */
public class WebSocketServerSingle extends Thread {

    public static final String TAG=WebSocketServerSingle.class.getSimpleName();
    int port;
    ServerSocket server;
    Socket client;
    InputStream is;
    OutputStream os;
    boolean islive = true;
    boolean has_client = false;
    WebsocketInterface listener;

    public WebSocketServerSingle(WebsocketInterface listener, int port) {
        this.listener = listener;
        this.port = port;
        try {
            server = new ServerSocket(port);
//            System.out.println("WebSocketServerSingle: Server started on port=" + port);
        } catch (IOException exception) {
//            System.out.println("WebSocketServerSingle: Can't create server");
            return;
        }
        islive = true;
    }
    public void close(){
        try{
            client.close();
        }catch (Exception ex){

        }

        try{
            server.close();
        }catch (Exception ex){

        }
    }

    public boolean isConnected() {
        return this.has_client;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void run() {
        start_serving_one_allday();
    }

    public boolean send_msg(String msg) {
//        Log.d(TAG, "sending="+msg);
        if (!has_client) {
//            Log.d(TAG, "failed: no client");
            return false;
        }
        try {
            os.write(encode(msg));
            os.flush();
            Log.d(TAG, "sent success");
        } catch (Exception ex) {
//            Log.d(TAG, "failed: msg="+ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void wait_and_connect_one_client() throws Exception {
        client = server.accept();
        is = client.getInputStream();
        os = client.getOutputStream();
        doHandShakeToInitializeWebSocketConnection(is, os);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start_serving_one_allday() {
        while (islive) {
            if (has_client == false) {
//                System.out.println("WebSocketServerSingle: waiting for client");
                try {
                    wait_and_connect_one_client();
                    has_client = true;
                    listener.onopen(client.getRemoteSocketAddress().toString());
                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    System.out.println("WebSocketServerSingle: Can't accept client");
                }
            } else {
                try {
//                    Thread.sleep(100);

                    //read_msg
                    String msg = read_websocket_msg(is);
                    listener.onmessage(msg);
                } catch (Exception ex) {
//                    System.out.println("WebSocketServerSingle: client_lost");
                    has_client = false;
                    listener.onclose();
                }

            }
        }
    }

    /*
    * Source for encoding and decoding:
    * https://stackoverflow.com/questions/8125507/how-can-i-send-and-receive-websocket-messages-on-the-server-side
     */
    public String read_websocket_msg(InputStream inputStream) throws Exception {

        byte[] b = new byte[1024];
        int len = inputStream.read(b);
        String msg = null;
        if (len != -1) {
            byte rLength = 0;
            int rMaskIndex = 2;
            int rDataStart = 0;
            //b[0] is always text in my case so no need to check;
            byte data = b[1];
            byte op = (byte) 127;
            rLength = (byte) (data & op);

            if (rLength == (byte) 126) {
                rMaskIndex = 4;
            }
            if (rLength == (byte) 127) {
                rMaskIndex = 10;
            }

            byte[] masks = new byte[4];

            int j = 0;
            int i = 0;
            for (i = rMaskIndex; i < (rMaskIndex + 4); i++) {
                masks[j] = b[i];
                j++;
            }

            rDataStart = rMaskIndex + 4;

            int messLen = len - rDataStart;

            byte[] message = new byte[messLen];

            for (i = rDataStart, j = 0; i < len; i++, j++) {
                message[j] = (byte) (b[i] ^ masks[j % 4]);
            }

            String line = new String(message);
//            System.out.println("line=" + line);
            msg = line;
            if (line.equals("�")) {
//                System.out.println("same found.");
                throw new Exception("_connection_closed_ns_");
            }
        } else {
            throw new Exception("_connection_closed_ns_");
        }
        return msg;
    }

    public byte[] encode(String mess) throws IOException {
        byte[] rawData = mess.getBytes();

        int frameCount = 0;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if (rawData.length <= 125) {
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        } else if (rawData.length >= 126 && rawData.length <= 65535) {
            frame[1] = (byte) 126;
            int len = rawData.length;
            frame[2] = (byte) ((len >> 8) & (byte) 255);
            frame[3] = (byte) (len & (byte) 255);
            frameCount = 4;
        } else {
            frame[1] = (byte) 127;
            int len = rawData.length;
            frame[2] = (byte) ((len >> 56) & (byte) 255);
            frame[3] = (byte) ((len >> 48) & (byte) 255);
            frame[4] = (byte) ((len >> 40) & (byte) 255);
            frame[5] = (byte) ((len >> 32) & (byte) 255);
            frame[6] = (byte) ((len >> 24) & (byte) 255);
            frame[7] = (byte) ((len >> 16) & (byte) 255);
            frame[8] = (byte) ((len >> 8) & (byte) 255);
            frame[9] = (byte) (len & (byte) 255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] reply = new byte[bLength];

        int bLim = 0;
        for (int i = 0; i < frameCount; i++) {
            reply[bLim] = frame[i];
            bLim++;
        }
        for (int i = 0; i < rawData.length; i++) {
            reply[bLim] = rawData[i];
            bLim++;
        }

        return reply;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void doHandShakeToInitializeWebSocketConnection(InputStream inputStream, OutputStream outputStream) throws UnsupportedEncodingException {
        String data = new Scanner(inputStream, "UTF-8").useDelimiter("\\r\\n\\r\\n").next();

        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();

            byte[] response = null;
            try {
                response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + Base64.getEncoder().encodeToString(
                                MessageDigest
                                        .getInstance("SHA-1")
                                        .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes("UTF-8")))
                        + "\r\n\r\n")
                        .getBytes("UTF-8");
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                outputStream.write(response, 0, response.length);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {

        }
    }

}

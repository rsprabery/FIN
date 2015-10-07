package com.example.androidfunctionmitm;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import de.robv.android.xposed.XposedBridge;

public class SocketHandler {
    private static SocketHandler ourInstance = new SocketHandler();

    public static SocketHandler getInstance() {
        return ourInstance;
    }

    private Socket clientSocket = null;
    private DataOutputStream outputStream = null;
    private BufferedReader inputBufferReader = null;
    private Lock bufferLock = new ReentrantLock();
    private MitmConfig mitmConfig = null;
    private InputStream inputStream = null;

    private SocketHandler() {
        mitmConfig = new MitmConfig();
        waitForConnect();
    }

    private void cleanUpConnections() {

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputStream = null;
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream = null;
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientSocket = null;
        }

    }

    private void waitForConnect() {
        XposedBridge.log("Trying to reconnect...");
        while (!setupConnection()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        XposedBridge.log("Reconnected!");
    }

    private boolean setupConnection() {

        if (clientSocket == null) {
            try {
                clientSocket = new Socket(mitmConfig.getServerIp(), mitmConfig.getServerPort());
            } catch (IOException e) {
                XposedBridge.log("Failed to create client socket....");
                XposedBridge.log(e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            XposedBridge.log("Client socket is not none...");
        }

        if (outputStream == null && clientSocket != null) {
            try {
                outputStream = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (inputBufferReader == null && clientSocket != null) {
            try {
                inputStream = clientSocket.getInputStream();
//                inputBufferReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    public List<String> sendReceiveUntilSuccess(String functionToHook, List<String> args) {
        bufferLock.lock();
        XposedBridge.log("got lock on socket");
        List<String> newArgs = sendReceive(functionToHook, args);
        while (newArgs == null) {
            cleanUpConnections();
            waitForConnect();
            newArgs = sendReceive(functionToHook, args);
        }
        bufferLock.unlock();
        return newArgs;
    }

    private List<String> sendReceive(String functionToHook, List<String> args) {
        List<String> newArgs = new ArrayList<>();
        try {
            StringBuilder messageBuffer = new StringBuilder();

            XposedBridge.log("writing out the function name");
            messageBuffer.append(Base64.encodeToString(functionToHook.getBytes(), Base64.DEFAULT)).append(",");

            XposedBridge.log("writing out each arg");
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                messageBuffer.append(encodingOutgoingString(arg));
                if (i != args.size() - 1) {
                    messageBuffer.append(",");
                }
            }

            XposedBridge.log("sending size of message to server");
            String finalMessage = messageBuffer.toString();
            ByteBuffer numberOfBytesBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(messageBuffer.length());
            outputStream.write(numberOfBytesBuffer.array());
            outputStream.flush();

            XposedBridge.log("Wrote out length of" + String.valueOf(messageBuffer.length()));
            XposedBridge.log(String.valueOf(numberOfBytesBuffer.array()[3]));
            XposedBridge.log("writing out the full message");
            outputStream.writeBytes(finalMessage);
            outputStream.flush();

            XposedBridge.log("asking for input from the server.");
            String encodedMessage = receive();
            if (encodedMessage == null) {
                return null;
            }
            newArgs = decodeReceivedMessage(encodedMessage);
        } catch (IOException e) {
            return null;
        } catch (BufferUnderflowException e) {
            return null;
        }

        return newArgs;
    }

    private String encodingOutgoingString(String outgoingPart) {
        String dataToSend = null;
        if (outgoingPart != null) {
            dataToSend = "\"" + outgoingPart + "\"";
        } else {
            dataToSend = "null";
        }
        String encodedPart = Base64.encodeToString(dataToSend.getBytes(), Base64.DEFAULT);
        return encodedPart;
    }

    private List<String> decodeReceivedMessage(String encodedMessage) {
        List<String> newArgs = new ArrayList<>();
        String[] messageParts = encodedMessage.split(",");
        for (String part : messageParts) {
            XposedBridge.log("Base64 String: " + part);
            String newArg = new String(Base64.decode(part.getBytes(), Base64.DEFAULT));
            XposedBridge.log("Non base64 part: " + newArg);
            if (newArg.equals("null")) {
                newArg = null;
            } else { // Remove the leading and trailing quotes
                newArg = newArg.substring(1, newArg.length() - 1);
                XposedBridge.log("The string after removing quotes:");
                XposedBridge.log(newArg);
            }
            newArgs.add(newArg);
        }
        return newArgs;
    }

    private void readUntilFull(byte[] outputArray) throws IOException {
        int size = outputArray.length;
        XposedBridge.log("Reading in This number of bytes: " + size);

        int sizeRead = inputStream.read(outputArray, 0, size);
        while (sizeRead != size) {
            sizeRead += inputStream.read(outputArray, sizeRead, size - sizeRead);
        }
    }

    private String receive() throws IOException {
        byte[] sizeBuffer = new byte[4];
        XposedBridge.log("reading in the size of the new message");
        try {
            readUntilFull(sizeBuffer);
            int byteCount = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN).put(sizeBuffer).getInt(0);
            XposedBridge.log("incoming size in bytes: " + String.valueOf(byteCount));

            byte[] messageBuffer = new byte[byteCount];
            XposedBridge.log("reading in the full message");
            readUntilFull(messageBuffer);
            String message = new String(messageBuffer);
            XposedBridge.log("got the full message");
            return message;
        } catch (ArrayIndexOutOfBoundsException e) {
            // This is thrown if the connection is reset in the middle of the protocol
            return null;
        }
    }

}

package com.example.Android_Group5;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    private static final String appName = "Android Group5";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "BluetoothConnectionService";
    public static BluetoothDevice mBTDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Intent connectionStatus;
    public static boolean BluetoothConnectionStatus = false;
    private static ConnectedThread mConnectedThread;

    // constructor
    public BluetoothConnectionService(Context context) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        startAcceptThread();
    }

    // Reconnection Thread when bluetooth is disconnected. Attempts to reconnect to device.
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
            } catch (IOException ignored) {
            }
            ServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = ServerSocket.accept();
            } catch (IOException ignored) {
            }

            if (socket != null) {
                connected(socket, socket.getRemoteDevice());
            }
        }

        //Close the serverSocket
        public void cancel() {
            try {
                ServerSocket.close();
            } catch (IOException ignored) {
            }
        }

    }

    // Connect thread when the device is attempting to connect to the other device.
    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID u) {
            mDevice = device;
            deviceUUID = u;
        }

        //Listens to the bluetooth device in proximity.
        public void run() {
            BluetoothSocket tmp = null;

            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException ignored) {
            }
            mSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
                connected(mSocket, mDevice);
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    bluetoothConnectionActivity mBluetoothPageActivity = (bluetoothConnectionActivity) mContext;
                    mBluetoothPageActivity.runOnUiThread(() -> Toast.makeText(mContext, "Failed to connect to the Device.", Toast.LENGTH_LONG).show());
                } catch (Exception z) {
                    z.printStackTrace();
                }

            }
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        //Close the bluetooth socket
        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException ignored) {
            }
        }
    }


    //Synchronises the thread when bluetooth is attempting to reconnect to the device
    public synchronized void startAcceptThread() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }


    public void startClientThread(BluetoothDevice device, UUID uuid) {
        try {
            mBTDevice = device;
            mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception ignored) {
        }

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    @SuppressLint("LongLogTag")
    public void fastConnect() {
        Log.d(TAG, mBTDevice.getName());
        mConnectThread = new ConnectThread(mBTDevice, myUUID);
        mConnectThread.start();
    }

    //Thread for connected and paired device
    // This is for sending/receiving messages between rpi and local device
    private class ConnectedThread extends Thread {
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {

            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        //Looks for incoming messages from the raspberry pi
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    // Function to handle different types of incoming strings
                    handleIncomingBTMessage(incomingMessage);
                } catch (IOException e) {

                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", mDevice);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;
                    break;
                }
            }
        }

        //Sends out messages to the raspberrypi
        @SuppressLint("LongLogTag")
        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
                Log.d(TAG, "I'm sending out messages");
            } catch (IOException ignored) {
            }
        }
    }


    // Thread for connected and disconnected device
    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        mDevice = device;
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    //Write method for connected device.
    public static void write(byte[] out) {
        mConnectedThread.write(out);
    }


    //Method to send out message by accepting String from user.
    public static boolean sendMessage(String message) {
        if (BluetoothConnectionStatus) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
            return true;
        }
        return false;
    }

    //Method to handle different types of incoming strings based on KEYWORDS
    // TODO: Define data format from different teams
    @SuppressLint("LongLogTag")
    private void handleIncomingBTMessage(String msg)  {
        Log.d(TAG, "handleIncomingBTMessage: New incoming message: "+msg);
        try{
            if(msg.toUpperCase().contains("ROBOT")) {
                // Received robot car location update as string, KEYWORD: ROBOT
                // ROBOT,<x>,<y>,<direction>
                Log.d(TAG, "New incoming message: ROBOT CAR LOCATION UPDATE --> " + msg);
                sendIntent("updateRoboCarLocation", msg);
        }
            else if(msg.toUpperCase().contains("STATUS")) {
                // TODO: Define an if-else condition that separates input from AMD and actual rpi
                // Received robot status update as string, KEYWORD: ROBOT
                // {"status":"value"} from AMD
//                JSONObject msgJSON = new JSONObject(msg);
//                String status = msgJSON.getString("status");
//                Log.d(TAG, "New incoming message: ROBOT STATUS UPDATE --> " + status);
//                sendIntent("updateRoboCarStatus", status);
                  Log.d(TAG, "New incoming message: ROBOT STATUS UPDATE --> " + msg);
                  sendIntent("updateRoboCarStatus", msg);
            }
            else if(msg.toUpperCase().contains("TARGET")) {
                // Received target ID obstacle update as string, KEYWORD: TARGET
                // TARGET,<Obstacle Number>,<Target ID>
                Log.d(TAG, "New incoming message: TARGET ID OBSTACLE UPDATE --> " + msg);
                sendIntent("imageResult", msg);
            }
            else {
                // Received plaintext
                Log.d(TAG, "New incoming message: PLAINTEXT FOR CHAT --> " + msg);
                sendIntent("incomingMessage", msg);
            }
        }catch (Exception e){
//            //NOT a JSON Obj
//            JSONObject msgJSON = new JSONObject();
//            msgJSON.put("msg",msg);
//            sendIntent("incomingBTMessage", msgJSON.toString());
        }
    }
    private void sendIntent(String intentAction, String msg){
        // Pack the string into Intent with intent action "intentAction"
        /* If we want to receive message from Intent with intent action "intentAction",
        registerReceiver with (broadcastReceiver, new IntentFilter("intentAction") */
        Intent incomingMessageIntent = new Intent(intentAction);
        incomingMessageIntent.putExtra("receivedMessage", msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
    }
}


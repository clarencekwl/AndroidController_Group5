package com.example.Android_Group5;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class bluetoothCommunicationActivity extends AppCompatActivity {

    private static final String TAG = "commsDEBUG";
    TextView showReceived;
    EditText inputMessage;
    Button sendButton;
    public static ProgressDialog myDialog;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_communication);

        showReceived = findViewById(R.id.showReceived);
        inputMessage = findViewById(R.id.inputMessage);
        sendButton = findViewById((R.id.sendButton));
        showReceived.setMovementMethod(new ScrollingMovementMethod());

        // Calls messageReceiver to get message from rpi
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
        //LocalBroadcastManager.getInstance(this).registerReceiver(robotLocationUpdateReceiver, new IntentFilter("updateRobocarLocation"));

        // Handle sending of message to rpi
        sendButton.setOnClickListener(v -> {
            inputMessage =  findViewById(R.id.inputMessage);
            // write message to rpi
            String message = inputMessage.getText().toString();
            boolean test = BluetoothConnectionService.sendMessage(message);
            Log.d(TAG, "Message sent: " + message);

            // Alternative way to send text to rpi
//                if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
//                    byte[] bytes = message.getBytes(Charset.defaultCharset());
//                    BluetoothConnectionService.sendMessage(message);
//                    Log.d(TAG, "Message sent: " + message);
//                }
        });

        myDialog = new ProgressDialog(bluetoothCommunicationActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(true);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (dialog, which) -> dialog.dismiss());
    }

    // handles the receiving of messages from rpi
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showReceived.setText(message);
//            showReceived.append("\n"+message);
            Log.d(TAG, "Message received: " + message);
        }
    };

}
package com.example.Android_Group5.arena;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.Android_Group5.BluetoothConnectionService;
import com.example.Android_Group5.R;
import com.example.Android_Group5.mapView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class controller extends AppCompatActivity {
    public static String TAG = "controllerDEBUG";

    private boolean initializedIntentListeners = false;
    private TextView txtRoboStatus;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch manualModeSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch outdoorArenaSwitch;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch turningModeSwitch;

    //For Arena
    boolean placingRobot, settingObstacle, settingDir;

    private final Handler handler = new Handler();

    //GridMap
    private static mapView mapView;

    //For robot
    private boolean isManual = false;

    //For Obstacle listview
    private ObstaclesListViewAdapter obstaclesListViewAdapter;
    private List<ObstacleListItem> obstacleListItemList;

    //Auxiliary
    private long timeStarted;
    private long timeEnded;
    private long timeTakenInNanoSeconds;

    //Android widgets for UI
    //ROBOT RELATED
    Button btnSendArenaInfo;
    Button btnSendStartImageRec;
    Button btnSendStartFastestCar;

    //ARENA RELATED
    Button btnResetArena;
    Button btnSetObstacle;
    Button btnSetFacing;
    Button btnPlaceRobot;

    //Adding obstacles using buttons
    Button btnAddObsManual;
    EditText addObs_x;
    EditText addObs_y;

    //Bot Status
    TextView txtTimeTaken;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_controller);

        Objects.requireNonNull(getSupportActionBar()).hide();

        obstacleListItemList = new ArrayList<>();

        if(!initializedIntentListeners){
            LocalBroadcastManager.getInstance(this).registerReceiver(roboStatusUpdateReceiver, new IntentFilter("updateRoboCarStatus"));
            LocalBroadcastManager.getInstance(this).registerReceiver(roboStateReceiver, new IntentFilter("updateRoboCarState"));
            LocalBroadcastManager.getInstance(this).registerReceiver(roboModeUpdateReceiver, new IntentFilter("updateRoboCarMode"));
            LocalBroadcastManager.getInstance(this).registerReceiver(updateObstacleListReceiver, new IntentFilter("newObstacleList"));
            LocalBroadcastManager.getInstance(this).registerReceiver(imageRecResultReceiver, new IntentFilter("imageResult"));
            LocalBroadcastManager.getInstance(this).registerReceiver(robotLocationUpdateReceiver, new IntentFilter("updateRoboCarLocation"));

            initializedIntentListeners = true;
        }

        if(mapView == null){
            mapView = new mapView(this);
            mapView = findViewById(R.id.mapView);
        }

        //For obstacle list view
        ListView obstacleListView = findViewById(R.id.home_obstacles_listview);
        obstaclesListViewAdapter = new ObstaclesListViewAdapter(this, R.layout.home_obstacle_list_layout, obstacleListItemList);
        obstacleListView.setAdapter(obstaclesListViewAdapter);

        //Switches
        manualModeSwitch = findViewById(R.id.switch_manualMode);
        outdoorArenaSwitch = findViewById(R.id.switch_outdoor);
        turningModeSwitch = findViewById(R.id.switch_turnmode);

        manualModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                sendModeCmdIntent("manual");
            }else{
                sendModeCmdIntent("path");
            }
        });

        outdoorArenaSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mapView.setIsOutdoorArena(isChecked));

        //Initialize Flags
        placingRobot = false;

        // For updating of robot status
        this.txtRoboStatus = findViewById(R.id.robotStatusText);

        //CONTROL BUTTON DECLARATIONS
        ImageButton controlBtnUp = findViewById(R.id.upArrowBtn);
        ImageButton controlBtnDown = findViewById(R.id.downArrowBtn);
        ImageButton controlBtnLeft = findViewById(R.id.leftArrowBtn);
        ImageButton controlBtnRight = findViewById(R.id.rightArrowBtn);


        // CONTROL BUTTON: Up
        controlBtnUp.setOnClickListener(view -> {
            Log.d(TAG, "control button UP clicked");
            BluetoothConnectionService.sendMessage("f");
        });

        //CONTROL BUTTON: Reverse
        controlBtnDown.setOnClickListener(view -> {
            Log.d(TAG, "control button DOWN clicked");
            BluetoothConnectionService.sendMessage("r");
        });

        //CONTROL BUTTON: Rotate left
        controlBtnLeft.setOnClickListener(view -> {
            Log.d(TAG, "control button ROTATE LEFT clicked");
            BluetoothConnectionService.sendMessage("tl");
        });

        //CONTROL BUTTON: Rotate right
        controlBtnRight.setOnClickListener(view -> {
            Log.d(TAG, "control button ROTATE RIGHT clicked");
            BluetoothConnectionService.sendMessage("tr");
        });

        //TIME TAKEN TEXTVIEW
        txtTimeTaken = findViewById(R.id.txt_timeTaken);

        //ROBOT RELATED
        btnSendArenaInfo = findViewById(R.id.btnSendInfo);
        btnSendStartImageRec = findViewById(R.id.btnStartImageRec);
        btnSendStartFastestCar = findViewById(R.id.btnStartFastestCar);

        //ARENA RELATED
        btnResetArena = findViewById(R.id.btnResetArena);
        btnSetObstacle = findViewById(R.id.btnSetObstacle);
        btnSetFacing = findViewById(R.id.btnDirectionFacing);
        btnPlaceRobot = findViewById(R.id.btnPlaceRobot);

        //Adding obstacles using buttons
        btnAddObsManual = findViewById(R.id.add_obs_btn);
        addObs_x = findViewById(R.id.add_obs_x_value);
        addObs_y = findViewById(R.id.add_obs_y_value);

        // OnClickListeners for sending arena info (obstacles) to RPI
        btnSendArenaInfo.setOnClickListener(v-> mapView.sendUpdatedObstacleInformation());

        // TODO: Understand this part
        btnSendStartImageRec.setOnClickListener(v->{
            mapView.removeAllTargetIDs();
            txtTimeTaken.setVisibility(View.INVISIBLE);
            sendControlCmdIntent("start");
            timeStarted = System.nanoTime();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    sendControlCmdIntent("stop");
                }
            }, 360000);
        });

        // TODO: Understand this part
        btnSendStartFastestCar.setOnClickListener(v->{
            txtTimeTaken.setVisibility(View.INVISIBLE);
            timeStarted = System.nanoTime();

            boolean isBigTurn = turningModeSwitch.isChecked();
            boolean isOutdoor = outdoorArenaSwitch.isChecked();

            if(isBigTurn){
                if(isOutdoor){
                    sendTurningModeCmdIntent("WN04");
                }else{
                    sendTurningModeCmdIntent("WN02");
                }
            }else{
                if(isOutdoor){
                    sendTurningModeCmdIntent("WN03");
                }else{
                    sendTurningModeCmdIntent("WN01");
                }
            }
        });

        btnResetArena.setOnClickListener(v->{
            try{
                Log.d(TAG, "btnResetArena clicked: Resetting map");
                mapView.resetMap();
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while resetting map");
                e.printStackTrace();
            }
        });

        // OnClickListeners for the arena related buttons
        btnPlaceRobot.setOnClickListener(v -> {
            try{
                //New status
                placingRobot = !placingRobot;
                if(placingRobot){
                    mapView.setStartingCoordinateStatus(true);
                    btnPlaceRobot.setText("Stop Set Robot");

                    //Disable other buttons
                    btnSetObstacle.setEnabled(false);
                    btnSetFacing.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    mapView.setStartingCoordinateStatus(false);
                    btnSetObstacle.setEnabled(true);
                    btnSetFacing.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
                    btnPlaceRobot.setText("Place Robot");
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while placing robot");
                e.printStackTrace();
            }
        });

        btnSetObstacle.setOnClickListener(v->{
            try{
                settingObstacle = !settingObstacle;
                if(settingObstacle){
                    mapView.setSetObstacleStatus(true);
                    btnSetObstacle.setText("Stop Set Obstacle");

                    //Disable other buttons
                    btnSetFacing.setEnabled(false);
                    btnPlaceRobot.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    mapView.setSetObstacleStatus(false);
                    btnSetObstacle.setText("Set Obstacle");

                    //Re-enable other buttons
                    btnSetFacing.setEnabled(true);
                    btnPlaceRobot.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while setting obstacle");
                e.printStackTrace();
            }
        });

        btnSetFacing.setOnClickListener(v -> {
            try{
                settingDir = !settingDir;
                if(settingDir){
                    mapView.setSetObstacleDirection(true);
                    btnSetFacing.setText("Stop Set Facing");

                    //Disable Other Buttons
                    btnSetObstacle.setEnabled(false);
                    btnPlaceRobot.setEnabled(false);
                    btnResetArena.setEnabled(false);
                    btnSendStartFastestCar.setEnabled(false);
                    btnSendStartImageRec.setEnabled(false);
                }else{
                    mapView.setSetObstacleDirection(false);
                    btnSetFacing.setText("Set Facing");

                    //Re-enable other buttons
                    btnSetObstacle.setEnabled(true);
                    btnPlaceRobot.setEnabled(true);
                    btnResetArena.setEnabled(true);
                    btnSendStartFastestCar.setEnabled(true);
                    btnSendStartImageRec.setEnabled(true);
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while setting obstacle direction");
                e.printStackTrace();
            }
        });

        btnAddObsManual.setOnClickListener(v -> {
            try{
                String x_value = addObs_x.getText().toString();
                String y_value = addObs_y.getText().toString();
                try
                {
                    int x_value_int = Integer.parseInt(x_value);
                    int y_value_int = Integer.parseInt(y_value);

                    if( x_value_int < 20 && x_value_int >=0 && y_value_int < 20 && y_value_int >=0){
                        mapView.setObstacleCoordinates(x_value_int, y_value_int);
                        showShortToast("Added obstacle");
                        addObs_x.setText("");
                        addObs_y.setText("");
                    }else{
                        showShortToast("Invalid Coordinates");
                    }
                }catch (Exception e){
                    showShortToast("Incorrect values!");
                }
            }catch (Exception e){
                Log.e(TAG, "onCreateView: An error occurred while adding obstacle manually");
                e.printStackTrace();
            }
        });

//        // DEBUGGING BUTTONS
//        Button btnFW10 = findViewById(R.id.temp_btnFW10);
//        btnFW10.setOnClickListener(v -> sendDirectionCmdIntent("FW10"));
//        Button btnBT10 = findViewById(R.id.temp_btnBW10);
//        btnBT10.setOnClickListener(v -> sendDirectionCmdIntent("BW10"));
//        Button btnFL00 = findViewById(R.id.temp_btnFL00);
//        btnFL00.setOnClickListener(v -> sendDirectionCmdIntent("FL00"));
//        Button btnFR00 = findViewById(R.id.temp_btnFR00);
//        btnFR00.setOnClickListener(v -> sendDirectionCmdIntent("FR00"));
//        Button btnBL00 = findViewById(R.id.temp_btnBL00);
//        btnBL00.setOnClickListener(v -> sendDirectionCmdIntent("BL00"));
//        Button btnBR00 = findViewById(R.id.temp_btnBR00);
//        btnBR00.setOnClickListener(v-> sendDirectionCmdIntent("BR00"));
    }

    // Handles incoming strings with keyword (STATUS) to update the robot status textview
    private final BroadcastReceiver roboStatusUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String msgInfo = intent.getStringExtra("receivedMessage");
                txtRoboStatus.setText(msgInfo);
            }catch (Exception e){
                txtRoboStatus.setText("UNKNOWN");
                showShortToast("Error updating roboCar status");
                Log.e(TAG, "onReceive: An error occurred while updating the roboCar status");
                e.printStackTrace();
            }
        }
    };

    // TODO: Understand this part
    private final BroadcastReceiver roboStateReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String state = intent.getStringExtra("msg");
                switch(state.toUpperCase()){
                    case "FINISHED":
                        timeEnded = System.nanoTime();
                        timeTakenInNanoSeconds = timeEnded - timeStarted;

                        double timeTakenInSeconds = (double) timeTakenInNanoSeconds/1000000000;
                        int timeTakenMin = (int) timeTakenInSeconds/60;
                        double timeTakenSec = timeTakenInSeconds %60;
                        DecimalFormat df = new DecimalFormat("0.00");

                        txtTimeTaken.setText("Run completed in: "+ timeTakenMin +"min "+df.format(timeTakenSec)+"secs");
                        txtTimeTaken.setVisibility(View.VISIBLE);

                        btnSetObstacle.setEnabled(true);
                        btnPlaceRobot.setEnabled(true);
                        btnResetArena.setEnabled(true);
                        btnSetFacing.setEnabled(true);
                        btnSendStartFastestCar.setEnabled(true);
                        btnSendStartImageRec.setEnabled(true);
                        btnSendArenaInfo.setEnabled(true);
                        btnAddObsManual.setEnabled(true);
                        break;
                    case "RUNNING":
                        btnSetObstacle.setEnabled(false);
                        btnPlaceRobot.setEnabled(false);
                        btnResetArena.setEnabled(false);
                        btnSetFacing.setEnabled(false);
                        btnSendStartFastestCar.setEnabled(false);
                        btnSendStartImageRec.setEnabled(false);
                        btnSendArenaInfo.setEnabled(false);
                        btnAddObsManual.setEnabled(false);
                        break;
                }
            }catch (Exception ex){
                Log.e(TAG, "onReceive: Error receiving robot completion status");
            }
        }
    };

    // TODO: Understand this part
    private final BroadcastReceiver roboModeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                String mode = intent.getStringExtra("msg");
                switch (mode.toUpperCase()){
                    case "PATH":
                        manualModeSwitch.setChecked(false);
                        break;
                    case "MANUAL":
                        manualModeSwitch.setChecked(true);
                        break;
                }
            }catch (Exception ex){
                Log.e(TAG, "onReceive: An error occurred on receiving roboCar mode");
                ex.printStackTrace();
            }
        }
    };

    // Handles updating of listview for obstacles on the controller page
    private final BroadcastReceiver updateObstacleListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            obstacleListItemList.clear();
            try{
                JSONArray msgInfo = new JSONArray(intent.getStringExtra("msg"));
                for(int i=0; i<msgInfo.length();i++){
                    JSONObject obj = msgInfo.getJSONObject(i);
                    obstacleListItemList.add(new ObstacleListItem(obj.getInt("no"), obj.getInt("x"), obj.getInt("y"), obj.getString("facing")));
                }
                obstaclesListViewAdapter.updateList(obstacleListItemList);
            }catch (Exception ex){
                Log.e(TAG, "onReceive: An error occurred while updating obstacle list view");
                ex.printStackTrace();
            }
        }
    };

    // Handles incoming strings with keyword (ROBOT) to update robot location manually
    private final BroadcastReceiver robotLocationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                // Received robot car location update string
                // ROBOT, <x>, <y>, <direction>
                String message = intent.getStringExtra("receivedMessage");
                Log.d(TAG, "Received robotCarLocationUpdate text: " + message);
                String[] commandComponents = message.split(",");
                if(commandComponents.length < 4){
                    Log.e(TAG, "handleIncomingBTMessage: The ROBOT plain text command has insufficient parts, command: "+ message);
                    return;
                }
                int xCoordinate = Integer.parseInt(commandComponents[1]);
                xCoordinate++;
                Log.d(TAG, "Received XCoordinate: " + xCoordinate);
                int yCoordinate = Integer.parseInt(commandComponents[2]);
                yCoordinate++;
                Log.d(TAG, "Received YCoordinate: " + yCoordinate);
                int dir = -1;
                switch(commandComponents[3].trim().toUpperCase()){
                    case "N":
                        dir = 0;
                        break;
                    case"E":
                        dir=2;
                        break;
                    case "S":
                        dir=4;
                        break;
                    case"W":
                        dir=6;
                        break;
                }
                int directionInt = dir;
                Log.d(TAG, "Received direction: " + directionInt);
                mapView.Direction direction = com.example.Android_Group5.mapView.Direction.UP;
                switch(directionInt){
                    case 0: //NORTH
                        direction = com.example.Android_Group5.mapView.Direction.UP;
                        break;
                    case 2: //EAST
                        direction = com.example.Android_Group5.mapView.Direction.RIGHT;
                        break;
                    case 4: //SOUTH
                        direction = com.example.Android_Group5.mapView.Direction.DOWN;
                        break;
                    case 6: //WEST
                        direction = com.example.Android_Group5.mapView.Direction.LEFT;
                        break;
                }
                Log.d(TAG,"Setting direction: " + direction);

                if(xCoordinate < 0 || yCoordinate < 0 || xCoordinate > 20 || yCoordinate > 20){
                    showShortToast("Error: Robot move out of area (x: "+xCoordinate+", y: "+yCoordinate+")");
                    Log.e(TAG, "onReceive: Robot is out of the arena area");
                    return;
                }

                mapView.updateCurrentCoordinate(xCoordinate, yCoordinate, direction);
            }catch (Exception e){
                showShortToast("Error updating robot location");
                Log.e(TAG, "onReceive: An error occurred while updating robot location");
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver imageRecResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                // Received target ID obstacle update
                // TARGET,<Obstacle Number>,<Target ID>
                String message = intent.getStringExtra("receivedMessage");
                Log.d(TAG, "Received targetID Obstacle update text: " + message);
                String[] commandComponents = message.split(",");
                if(commandComponents.length < 3){
                    Log.e(TAG, "handleIncomingBTMessage: The TARGET plain text command has insufficient parts, command: "+ message);
                    return;
                }
                int obstacleID = Integer.parseInt(commandComponents[1]);
                Log.d(TAG, "Received obstacleID: " + obstacleID);
                String targetID = commandComponents[2];
                Log.d(TAG,"Received targetID: " + targetID);
                mapView.updateImageNumberCell(obstacleID, targetID);
            }catch (Exception e){
                showShortToast("Error updating image rec result");
                Log.e(TAG, "onReceive: An error occurred while updating the image rec result");
                e.printStackTrace();
            }
        }
    };

    private void showShortToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // Sends bluetooth direction command to rpi
    private void sendDirectionCmdIntent(String direction){

        try{
            JSONObject directionJSONObj = new JSONObject();
            directionJSONObj.put("cat","manual");
            directionJSONObj.put("value",direction);
            Log.d(TAG, direction + "debug");
            broadcastSendBTIntent(directionJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendDirectionCmdIntent: An error occurred while sending direction command intent");
            e.printStackTrace();
        }
    }

    private void sendModeCmdIntent(String mode){
        try{
            if(!mode.equals("path") && !mode.equals("manual")){
                Log.i(TAG, "sendModeIntent: Invalid mode to send: "+mode);
                return;
            }
            JSONObject modeJSONObj = new JSONObject();
            modeJSONObj.put("cat","mode");
            modeJSONObj.put("value",mode);

            broadcastSendBTIntent(modeJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendModeIntent: An error occurred while sending mode command intent");
            e.printStackTrace();
        }
    }

    private void sendTurningModeCmdIntent(String mode){
        try{
            JSONObject modeJSONObj = new JSONObject();
            modeJSONObj.put("cat","manual");
            modeJSONObj.put("value",mode);

            broadcastSendBTIntent(modeJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendModeIntent: An error occurred while sending mode command intent");
            e.printStackTrace();
        }
    }

    private void sendControlCmdIntent(String control){
        try{
            JSONObject ctrlJSONObj = new JSONObject();
            ctrlJSONObj.put("cat","control");
            ctrlJSONObj.put("value",control);

            broadcastSendBTIntent(ctrlJSONObj.toString());
        }catch (Exception e){
            Log.e(TAG, "sendControlCmdIntent: An error occurred while sending control command intent");
            e.printStackTrace();
        }
    }

    private void broadcastSendBTIntent(String msg){
        Intent sendBTIntent = new Intent("sendBTMessage");
        sendBTIntent.putExtra("msg",msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(sendBTIntent);
    }

    public static class ObstaclesListViewAdapter extends ArrayAdapter<ObstacleListItem>{
        protected List<ObstacleListItem> items;

        public ObstaclesListViewAdapter(@NonNull Context context, int resource, @NonNull List<ObstacleListItem> objects) {
            super(context, resource, objects);
            this.items = objects;
        }

        public void updateList(List<ObstacleListItem> list) {
            this.items = list;
            this.notifyDataSetChanged();
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.home_obstacle_list_layout, parent, false);
            }
            ObstacleListItem item = items.get(position);
            TextView obsNoTxt = convertView.findViewById(R.id.txtObsListItem_obsNo);
            TextView xPosTxt = convertView.findViewById(R.id.txtObsListItem_x);
            TextView yPosTxt = convertView.findViewById(R.id.txtObsListItem_y);
            TextView facingTxt = convertView.findViewById(R.id.txtObsListItem_dir);

            obsNoTxt.setText("#"+item.obsNo);
            xPosTxt.setText(Integer.toString(item.x));
            yPosTxt.setText(Integer.toString(item.y));
            facingTxt.setText(item.facing);

            return convertView;
        }
    }

    public static class ObstacleListItem {
        public int obsNo;
        public int x;
        public int y;
        public String facing;

        public ObstacleListItem(int obsNo,int x, int y, String facing){
            this.obsNo = obsNo;
            this.x=x;
            this.y=y;
            this.facing=facing;
        }
    }
}
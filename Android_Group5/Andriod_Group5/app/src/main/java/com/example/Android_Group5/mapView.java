package com.example.Android_Group5;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class mapView extends View{

    private final Paint blackPaint = new Paint();
    private final Paint whitePaint = new Paint();
    private final Paint gridWithoutTextPaint =  new Paint();
    private final Paint obstaclePaint = new Paint();
    private final Paint robotPaint = new Paint();
    private final Paint endPaint = new Paint();
    private final Paint startPaint = new Paint();
    private final Paint unexploredPaint = new Paint();
    private final Paint imageLinePaint = new Paint();
    private final Paint imageLineConfirmPaint = new Paint();

    private static Direction robotDirection = Direction.NONE;
    private static int[] currentCoordinate = new int[]{-1, -1};
    private static ArrayList<int[]> obstacleCoordinates = new ArrayList<>();
    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean startingCoordinateStatus = false;
    private static boolean obstacleSetStatus = false;
    private static boolean obstacleSetDirection = false;

    private static final String TAG = "mapViewDEBUG";
    private static final int COLUMN_SIZE = 20;
    private static final int ROW_SIZE = 20;
    private static float cellSize;
    private static Cell[][] cells;

    private static boolean isOutdoorArena = false;

    private boolean isMapDrawn = false;

    private static int[] selectedObstacleCoordinate = new int[3];
    private static boolean isObstacleSelected = false;

    int switchDirection = -1; // 0:None 1: Up, 2: Down, 3: Left, 4:Right
    String[] directionList = new String[]{"NONE", "UP", "DOWN", "LEFT", "RIGHT"};
    private static int[] obstacleNoArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    public mapView(Context context) {
        super(context);
    }

    public mapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initMap();

        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        whitePaint.setColor(Color.WHITE);

        obstaclePaint.setColor(Color.BLACK);

        robotPaint.setColor(Color.CYAN);

        endPaint.setColor(Color.GRAY);

        startPaint.setColor(Color.CYAN);

        unexploredPaint.setColor(Color.LTGRAY);

        gridWithoutTextPaint.setColor(Color.WHITE);
        gridWithoutTextPaint.setTextSize(15);
        gridWithoutTextPaint.setFakeBoldText(true);

        imageLinePaint.setStyle(Paint.Style.STROKE);
        imageLinePaint.setColor(Color.YELLOW);
        imageLinePaint.setStrokeWidth(2);

        imageLineConfirmPaint.setStyle(Paint.Style.STROKE);
        imageLineConfirmPaint.setColor(Color.YELLOW);
        imageLineConfirmPaint.setStrokeWidth(5);
    }

    private void initMap() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG ,"Entering onDraw");
        super.onDraw(canvas);
        Log.d(TAG, "Redrawing map");

        //CREATE CELL COORDINATES
        Log.d(TAG, "Creating Cell");

        if (!isMapDrawn) {
            createCell();
            resetMap();
            isMapDrawn = true;
        }

        drawIndividualCell(canvas);
        drawGridLines(canvas);
        drawGridNumber(canvas);

        if (canDrawRobot)
            drawRobot(canvas);

        Log.d(TAG,"Exiting onDraw");
    }

    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");

        for (int x = 0; x < COLUMN_SIZE + 2; x++) {
            for (int y = 0; y < ROW_SIZE + 2; y++) {
                Cell cell = cells[x][y];

                //DRAWING: CELL ITSELF
                canvas.drawRect(cell.startX, cell.startY, cell.endX, cell.endY, cell.paint);

                //DRAWING: OBSTACLE NUMBER & DIRECTION
                if (cell.type == CellType.OBSTACLE) {
                    //Draw the number for the obstacle
                    if (cell.targetID == null) {
                        canvas.drawText(Integer.toString(cell.obstacleNo), cell.startX + (cellSize / 3.2f), cell.startY + (cellSize / 1.5f), whitePaint);
                    } else {
                        Paint targetPaint = new Paint();
                        targetPaint.setTextSize(20);
                        targetPaint.setColor(Color.GREEN);
                        targetPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText(cell.targetID, (cell.startX + cell.endX) / 2, cell.endY + (cell.startY - cell.endY) / 4, targetPaint);
                    }

                    //Draw the obstacle facing
                    if (cell.obstacleDirection != null) {
                        switch (cell.obstacleDirection) {
                            case UP:
                                canvas.drawRect(cell.startX + 2, cell.startY + 1, cell.endX, cell.endY - (cellSize / 1.1f), imageLinePaint);
                                break;
                            case DOWN:
                                canvas.drawRect(cell.startX + 2, cell.startY + cellSize - 2, cell.endX, cell.endY - 1, imageLinePaint);
                                break;
                            case LEFT:
                                canvas.drawRect(cell.startX + 1, cell.startY + 2, cell.endX - (cellSize / 1.1f), cell.endY, imageLinePaint);
                                break;
                            case RIGHT:
                                canvas.drawRect(cell.startX + cellSize - 2, cell.startY, cell.endX - 1, cell.endY, imageLinePaint);
                                break;
                        }
                    }
                }
            }
        }
        showLog("Exiting drawIndividualCell");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        showLog("Entering onTouchEvent");
        int mapX = (int) (event.getX() / cellSize) - 1;
        int mapY = ROW_SIZE - ((int) (event.getY() / cellSize));

        Cell selectedCell = null;
        if ((mapX >= 0 && mapY >= 0 && mapX <= COLUMN_SIZE - 1 && mapY <= ROW_SIZE - 1)) {
            selectedCell = getCellFromCoordinateRow(mapX, mapY);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (startingCoordinateStatus && selectedCell != null) {
                //Move to a new function setRobotPosition(int x, int y)
                //Current code base violates DRY
                updateCurrentCoordinate(mapX,mapY,Direction.UP);
                canDrawRobot = true;
                invalidate();
                turnOffRobotPlacementButton();
                return true;
            }
            if (obstacleSetStatus && selectedCell != null) {
                Log.i(TAG, "onTouchEvent: Adding Obstacle at X: " + mapX + " Y: " + mapY);
                this.setObstacleCoordinates(mapX, mapY);
                return true;
            }
            if (obstacleSetDirection && selectedCell != null) {
                selectStartingDirection(selectedCell);
            }
            if (!isObstacleSelected && selectedCell != null) {
                for (int i = 0; i < obstacleCoordinates.size(); i++)
                    if (obstacleCoordinates.get(i)[0] == mapX && obstacleCoordinates.get(i)[1] == mapY) {
                        selectedObstacleCoordinate[0] = mapX;
                        selectedObstacleCoordinate[1] = mapY;
                        isObstacleSelected = true;
                        return true;
                    }
            }
        } else if(event.getAction() == MotionEvent.ACTION_UP){
            //Reset obs selected when finger is lifted from map
            if (isObstacleSelected) {
                isObstacleSelected = false;
                return true;
            }
        }else if(event.getAction() == MotionEvent.ACTION_MOVE){
            if (isObstacleSelected) {
                boolean occupied = false;
                for (int i = 0; i < obstacleCoordinates.size(); i++) {
                    if (obstacleCoordinates.get(i)[0] == mapX && obstacleCoordinates.get(i)[1] == mapY) {
                        occupied = true;
                    }
                }
                if (!occupied) {
                    Cell oldObstacleCell = getCellFromCoordinateRow(selectedObstacleCoordinate[0], selectedObstacleCoordinate[1]);
                    //Cache old obstacle direction
                    Direction oldObstacleDir = oldObstacleCell.obstacleDirection;
                    String oldTargetID = oldObstacleCell.targetID;
                    removeObstacleCoordinate(selectedObstacleCoordinate[0], selectedObstacleCoordinate[1]);

                    //If selection is within the grid; move to new position
                    if (mapX < 20 && mapY < 20 && mapX > 0 && mapY > 0) {
                        //Update selectedObstacleCoordinate;
                        selectedObstacleCoordinate[0] = mapX;
                        selectedObstacleCoordinate[1] = mapY;

                        setObstacleCoordinates(mapX,mapY);
                        selectedCell.obstacleDirection = oldObstacleDir;
                        selectedCell.targetID = oldTargetID;
                    }
                    this.invalidate();
                    return true;
                }

            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    private void selectStartingDirection(Cell selectedCell) {
        boolean isSetRobot = (selectedCell.type == CellType.ROBOT);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
        mBuilder.setTitle("Select Direction");
        mBuilder.setSingleChoiceItems(directionList, switchDirection, (dialogInterface, i) -> switchDirection = i);
        mBuilder.setNeutralButton("OK", (dialogInterface, i) -> {
            Direction selectedDirection;
            switch (switchDirection) {
                case 1:
                    selectedDirection = Direction.UP;
                    break;
                case 2:
                    selectedDirection = Direction.DOWN;
                    break;
                case 3:
                    selectedDirection = Direction.LEFT;
                    break;
                case 4:
                    selectedDirection = Direction.RIGHT;
                    break;
                default:
                    selectedDirection = Direction.NONE;
                    break;
            }

            if(isSetRobot && selectedDirection == Direction.NONE){
                setRobotDirection(Direction.UP);
            }else if(isSetRobot){
                setRobotDirection(selectedDirection);
            }else{
                selectedCell.setObstacleDirection(selectedDirection);
                updateHomeObstacleListView();
            }
            // UNCOMMENT BELOW FOR C6/7
//            if(!isSetRobot){
//                sendUpdatedObstacleInformation();
//            }
            invalidate();
            dialogInterface.dismiss();
        });

        if(selectedCell.type == CellType.OBSTACLE || selectedCell.type == CellType.ROBOT){
            AlertDialog dialog = mBuilder.create();
            dialog.show();
        }
    }

    @SuppressLint("SetTextI18n")
    public void turnOffRobotPlacementButton() {
        if(!startingCoordinateStatus){
            return;
        }
        setStartingCoordinateStatus(false);

        //Re-enable other buttons
        Button placeRobotBtn = ((Activity) this.getContext()).findViewById(R.id.btnPlaceRobot);
        Button btnSetObstacle  = ((Activity) this.getContext()).findViewById(R.id.btnSetObstacle);
        Button btnSetFacing = ((Activity) this.getContext()).findViewById(R.id.btnDirectionFacing);
        Button btnResetArena = ((Activity) this.getContext()).findViewById(R.id.btnResetArena);
        Button btnSendStartFastestCar = ((Activity) this.getContext()).findViewById(R.id.btnStartFastestCar);
        Button btnSendStartImageRec = ((Activity) this.getContext()).findViewById(R.id.btnStartImageRec);

        btnSetObstacle.setEnabled(true);
        btnSetFacing.setEnabled(true);
        btnResetArena.setEnabled(true);
        btnSendStartFastestCar.setEnabled(true);
        btnSendStartImageRec.setEnabled(true);
        placeRobotBtn.setText("Place Robot");
    }

    @SuppressLint("SetTextI18n")
    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusText);
        updateRobotStatusTextView(1, 1, Direction.NONE);
        robotStatusTextView.setText("Not Available");

        currentCoordinate = new int[]{-1, -1};
        robotDirection = Direction.NONE;
        obstacleCoordinates = new ArrayList<>();
        isMapDrawn = false;
        canDrawRobot = false;

        // newly added
        obstacleNoArray = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        updateHomeObstacleListView();
        showLog("Exiting resetMap");
        this.invalidate();
    }

    @Contract(pure = true)
    private String directionEnumToString(@NonNull Direction direction) {
        switch (direction) {
            case UP:
                return "N";
            case RIGHT:
                return "E";
            case DOWN:
                return "S";
            case LEFT:
                return "W";
            default:
                return "-1";
        }
    }

    // TODO: Define data format for receiving obstacle info with RPI
    // Send updated obstacle information in one COMBINED string: <id>,<x>,<y>,<facing>:
    public void sendUpdatedObstacleInformation() {
        try {
            String combined = "";
            ArrayList<String> obstacleList = new ArrayList<>(); // List of obstacles
            for (int i = 0; i < obstacleCoordinates.size(); i++) {
                int obstacleX = obstacleCoordinates.get(i)[0];
                int obstacleY = obstacleCoordinates.get(i)[1];
                Cell obstacleCell = getCellFromCoordinateRow(obstacleX, obstacleY); // id for obstacle
                // <id>,<x>,<y>,<facing>
                String obstacle = obstacleCell.obstacleNo + "," + obstacleX + "," + (19 - obstacleY) + "," + directionEnumToString(obstacleCell.obstacleDirection) + ",";
                Log.d(TAG, "Found obstacle: " + obstacle);
                combined += obstacle;
                Log.d(TAG, "Combined string: " + combined);
                obstacleList.add(obstacle); // Put obstacle into obstacleList
                Log.d(TAG, "Adding obstacle into list: " + obstacle);
            }
            combined = "ALG|" + combined;
            Log.d(TAG, "sending via bt " + combined);
            BluetoothConnectionService.sendMessage(combined);
            Log.d(TAG, "Obstacle list size: " + obstacleList.size());

//            for (int j=0; j<obstacleList.size(); j++) {
//                Log.d(TAG, "Sending obstacle via BT: " + obstacleList.get(j));
//                BluetoothConnectionService.sendMessage(obstacleList.get(j));
//            }
        } catch (Exception ex) {
            Log.e(TAG, "sendUpdatedObstacleInformation: An error occurred while sending obstacle information to device");
            ex.printStackTrace();
        }
    }

    // Sending obstacle information in JSON
//    public void sendUpdatedObstacleInformation() {
//        try {
//            JSONArray obstaclesList = new JSONArray(); // List of obstacles
//
//            for (int i = 0; i < obstacleCoords.size(); i++) {
//                JSONObject obstacle = new JSONObject(); // new JSON object for each obstacle
//                int obstacleX = obstacleCoords.get(i)[0];
//                int obstacleY = obstacleCoords.get(i)[1];
//                Cell obstacleCell = getCellAtMapCoord(obstacleX, obstacleY); // id for obstacle
//                obstacle.put("x", obstacleX);
//                obstacle.put("y", obstacleY);
//                obstacle.put("id", obstacleCell.obstacleNo);
//                obstacle.put("d", directionEnumToInt(obstacleCell.obstacleFacing));
//
//                obstaclesList.put(obstacle);
//            }
//            JSONObject valueObj = new JSONObject();
//            valueObj.put("obstacles", obstaclesList);
//            if(isOutdoorArena){
//                valueObj.put("mode","1");
//            }else{
//                valueObj.put("mode","0");
//            }
//
//            JSONObject msgJSON = new JSONObject();
//            msgJSON.put("cat", "obstacles");
//            msgJSON.put("value", valueObj);
//
//            Intent upDirectionIntent = new Intent("sendBTMessage");
//            upDirectionIntent.putExtra("msg", msgJSON.toString());
//            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(upDirectionIntent);
//        } catch (Exception ex) {
//            Log.e(TAG, "sendUpdatedObstacleInformation: An error occurred while sending obstacle information to device");
//            ex.printStackTrace();
//        }
//    }



    private int[] convertMapCoordinateToCellIndices(int mapX, int mapY){
        return new int[]{mapX+1, ROW_SIZE -mapY};
    }

    private void updateHomeObstacleListView(){
        try{
            JSONArray obstacleInfo = new JSONArray();
            for(int[] obstacleCoordinate : obstacleCoordinates){
                JSONObject obstacleObjects = new JSONObject();
                Cell cell = getCellFromCoordinatesColumn(obstacleCoordinate[0],obstacleCoordinate[1]);
                obstacleObjects.put("no",cell.obstacleNo);
                obstacleObjects.put("x",obstacleCoordinate[0]);
                obstacleObjects.put("y",obstacleCoordinate[1]);
                obstacleObjects.put("facing",cell.obstacleDirection.toString());

                obstacleInfo.put(obstacleObjects);
            }

            Intent obstacleListIntent = new Intent("newObstacleList");
            obstacleListIntent.putExtra("msg", obstacleInfo.toString());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(obstacleListIntent);
        }catch (Exception e){
            Log.e(TAG, "updateFrontEndListView: Error adding obstacle to JSON");
        }
    }

    public void removeAllTargetIDs(){
        try{
            for (int i = 0; i < obstacleCoordinates.size(); i++) {
                int obstacleX = obstacleCoordinates.get(i)[0];
                int obstacleY = obstacleCoordinates.get(i)[1];
                Cell obstacleCell = getCellFromCoordinateRow(obstacleX, obstacleY);
                obstacleCell.targetID = null;
            }
            invalidate();
        }catch (Exception ex){
            Log.e(TAG, "removeAllObstacleIDs: An error occurred while removing confirmed target IDs");
        }
    }

    private Cell getCellFromCoordinatesColumn(int x, int y) {
        return cells[x + 1][COLUMN_SIZE - y];
    }

    private Cell getCellFromCoordinateRow(int x, int y) {
        return cells[x + 1][ROW_SIZE - y];
    }

    public void updateImageNumberCell(int obstacleNo, String targetID) {
        // find the obstacle no which has the same id
        for (int x = 1; x <= COLUMN_SIZE; x++)
            for (int y = 1; y <= ROW_SIZE; y++)
                if (cells[x][y].obstacleNo == obstacleNo) {
                    cells[x][y].targetID = targetID;
                }
        this.invalidate();
    }

    private void drawGridLines(Canvas canvas) {
        //HORIZONTAL LINES
        for (int y = 0; y <= COLUMN_SIZE; y++) {
            Cell start = cells[1][y];
            Cell end = cells[COLUMN_SIZE][y];
            canvas.drawLine(start.startX, start.endY, end.endX, end.endY, blackPaint);
        }

        //VERTICAL LINES
        for (int x = 1; x <= COLUMN_SIZE + 1; x++) {
            Cell start = cells[x][1];
            Cell end = cells[x][ROW_SIZE];
            canvas.drawLine(start.startX, start.startY, end.startX, end.endY, blackPaint);
        }
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        //X-AXIS numbers
        for (int x = 1; x <= COLUMN_SIZE; x++) {
            Cell cell = cells[x][COLUMN_SIZE + 1];
            String num = Integer.toString(x - 1);
            if (x > 9)
                canvas.drawText(
                        num,
                        cell.startX + (cellSize / 5),
                        cell.startY + (cellSize / 2),
                        gridWithoutTextPaint
                );
            else
                canvas.drawText(
                        num,
                        cell.startX + (cellSize / 3),
                        cell.startY + (cellSize / 2),
                        gridWithoutTextPaint
                );
        }
        //Y-AXIS numbers
        for (int y = 1; y <= ROW_SIZE; y++) {
            Cell cell = cells[0][y];
            int adjustedY = ROW_SIZE - y;
            String num = Integer.toString(adjustedY);
            if (adjustedY > 9)
                canvas.drawText(
                        num,
                        cell.startX + (cellSize / 3),
                        cell.startY + (cellSize / 1.5f),
                        gridWithoutTextPaint
                );
            else
                canvas.drawText(
                        num,
                        cell.startX + (cellSize / 2),
                        cell.startY + (cellSize / 1.5f),
                        gridWithoutTextPaint
                );
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas) {
        showLog("Entering drawRobot");
        if(currentCoordinate[0] == -1 || currentCoordinate[1] == -1){
            //No robot to draw
            return;
        }

        int[] cellIndexes = convertMapCoordinateToCellIndices(currentCoordinate[0], currentCoordinate[1]);
        int xIndex = cellIndexes[0];
        int yIndex = cellIndexes[1];

        switch (robotDirection) {
            case UP:
                //left drawn line
                canvas.drawLine(
                        cells[xIndex - 1][yIndex + 1].startX,
                        cells[xIndex - 1][yIndex + 1].endY,
                        (cells[xIndex][yIndex - 1].startX + cells[xIndex][yIndex - 1].endX) / 2,
                        cells[xIndex][yIndex - 1].startY,
                        blackPaint
                );
                //right drawn line
                canvas.drawLine(
                        (cells[xIndex][yIndex - 1].startX + cells[xIndex][yIndex - 1].endX) / 2,
                        cells[xIndex][yIndex - 1].startY,
                        cells[xIndex + 1][yIndex + 1].endX,
                        cells[xIndex + 1][yIndex + 1].endY,
                        blackPaint
                );
                break;
            case DOWN:
                canvas.drawLine(
                        cells[xIndex - 1][yIndex - 1].startX,
                        cells[xIndex - 1][yIndex - 1].startY,
                        (cells[xIndex][yIndex + 1].startX + cells[xIndex][yIndex + 1].endX) / 2,
                        cells[xIndex][yIndex + 1].endY,
                        blackPaint
                );
                canvas.drawLine(
                        (cells[xIndex][yIndex + 1].startX + cells[xIndex][yIndex + 1].endX) / 2,
                        cells[xIndex][yIndex + 1].endY,
                        cells[xIndex + 1][yIndex - 1].endX,
                        cells[xIndex + 1][yIndex - 1].startY,
                        blackPaint
                );
                break;
            case RIGHT:
                canvas.drawLine(
                        cells[xIndex - 1][yIndex - 1].startX,
                        cells[xIndex - 1][yIndex - 1].startY,
                        cells[xIndex + 1][yIndex].endX,
                        cells[xIndex + 1][yIndex - 1].endY + (cells[xIndex + 1][yIndex].endY - cells[xIndex + 1][yIndex - 1].endY) / 2,
                        blackPaint
                );
                canvas.drawLine(
                        cells[xIndex + 1][yIndex].endX,
                        cells[xIndex + 1][yIndex - 1].endY + (cells[xIndex + 1][yIndex].endY - cells[xIndex + 1][yIndex - 1].endY) / 2,
                        cells[xIndex - 1][yIndex + 1].startX,
                        cells[xIndex - 1][yIndex + 1].endY,
                        blackPaint
                );
                break;
            case LEFT:
                canvas.drawLine(
                        cells[xIndex + 1][yIndex - 1].endX,
                        cells[xIndex + 1][yIndex - 1].startY,
                        cells[xIndex - 1][yIndex].startX,
                        cells[xIndex - 1][yIndex - 1].endY + (cells[xIndex - 1][yIndex].endY - cells[xIndex - 1][yIndex - 1].endY) / 2,
                        blackPaint
                );
                canvas.drawLine(
                        cells[xIndex - 1][yIndex].startX,
                        cells[xIndex - 1][yIndex - 1].endY + (cells[xIndex - 1][yIndex].endY - cells[xIndex - 1][yIndex - 1].endY) / 2,
                        cells[xIndex + 1][yIndex + 1].endX,
                        cells[xIndex + 1][yIndex + 1].endY,
                        blackPaint
                );
                break;
            default:
                Toast.makeText(
                        this.getContext(),
                        "Error with drawing robot (unknown direction)",
                        Toast.LENGTH_LONG
                ).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    public Direction getRobotDirection() {
        return robotDirection;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    public boolean getIsOutdoorArena(){
        return isOutdoorArena;
    }

    public void setIsOutdoorArena(boolean isOutdoor){
        isOutdoorArena=isOutdoor;
    }

    public void setSetObstacleDirection(boolean status) {
        obstacleSetDirection = status;
    }

    public void setSetObstacleStatus(boolean status) {
        obstacleSetStatus = status;
    }

    public void setStartingCoordinateStatus(boolean status) {
        startingCoordinateStatus = status;
    }

    private void createCell() {
        showLog("Entering createCell");
        cells = new Cell[COLUMN_SIZE + 2][ROW_SIZE + 2];
        cellSize = getWidth() / (float)(COLUMN_SIZE + 2);

        for (int x = 0; x < COLUMN_SIZE + 2; x++) {
            for (int y = 0; y < ROW_SIZE + 2; y++) {
                float startX = x * cellSize;
                float startY = y * cellSize;
                cells[x][y] = new Cell(startX, startY, startX + cellSize, startY + cellSize, CellType.UNEXPLORED);
            }
        }

        //Set the borders
        for (int x = 0; x < COLUMN_SIZE + 2; x++) {
            cells[x][0].setType(CellType.BORDER);
            cells[x][ROW_SIZE + 1].setType(CellType.BORDER);
        }
        for (int y = 0; y < ROW_SIZE + 2; y++) {
            cells[0][y].setType(CellType.BORDER);
            cells[COLUMN_SIZE + 1][y].setType(CellType.BORDER);
        }

        showLog("Exiting createCell");
    }

    public void updateCurrentCoordinate(int mapX, int mapY, Direction direction){
        Log.d(TAG, "Updating map coordinates");
        Log.i(TAG, "updateCurrentCoordinate: CURRENT COORDINATES:"+ currentCoordinate[0]+","+ currentCoordinate[1]);
        if(currentCoordinate[0] != -1 && currentCoordinate[1] != -1){
            Log.i(TAG, "updateCurrentCoordinate: UNSETTING ROBOT");
            //Unset old robot position
            int[] oldCoordinateIndices = convertMapCoordinateToCellIndices(currentCoordinate[0], currentCoordinate[1]);
            int oldCoordinateXIndex = oldCoordinateIndices[0];
            int oldCoordinateYIndex = oldCoordinateIndices[1];

            for (int x = oldCoordinateXIndex - 1; x <= oldCoordinateXIndex + 1; x++) {
                for (int y = oldCoordinateYIndex - 1; y <= oldCoordinateYIndex + 1; y++) {
                    if (cells[x][y].type == CellType.ROBOT) {
                        Log.i(TAG, "updateCurrentCoordinate: set ["+x+"]"+"["+y+"] to unexplored");
                        cells[x][y].setType(CellType.UNEXPLORED);
                    }
                }
            }
        }

        //Update new location
        robotDirection = direction;
        setRobotDirection(direction);
        currentCoordinate[0] = mapX;
        currentCoordinate[1] = mapY;
        int[] newCoordinatesIndices = convertMapCoordinateToCellIndices(mapX,mapY);
        int newXCoordinateIndex = newCoordinatesIndices[0];
        int newYCoordinateIndex = newCoordinatesIndices[1];
        for (int x = newXCoordinateIndex - 1; x <= newXCoordinateIndex + 1; x++) {
            for (int y = newYCoordinateIndex - 1; y <= newYCoordinateIndex + 1; y++) {
                if (cells[x][y].type != CellType.OBSTACLE && cells[x][y].type != CellType.BORDER) {
                    cells[x][y].setType(CellType.ROBOT);
                }
            }
        }
        invalidate();
        Log.d(TAG,"Done updating map coordinates");
    }

    public void setRobotDirection(Direction direction) {
        robotDirection = direction;
        this.invalidate();
    }

    private void updateRobotStatusTextView(int col, int row, Direction direction) {
        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robot_x_value);
        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robot_y_value);
        TextView directionAxisTextView = ((Activity) this.getContext()).findViewById(R.id.robotDirText);

        String newDirText_x = "X: " + (col - 1);

        String newDirText_y = "Y: " + (row - 1);

        xAxisTextView.setText(newDirText_x);
        yAxisTextView.setText(newDirText_y);

        directionAxisTextView.setText(direction.toString());
    }

    public void setObstacleCoordinates(int mapX, int mapY) {
        showLog("Entering setObstacleCoordinates");
        //Check if obstacle has been previously set there
        if (getCellFromCoordinatesColumn(mapX, mapY).type == CellType.OBSTACLE) {
            return;
        }
        int[] obstacleCoordinates = new int[]{mapX, mapY};
        mapView.obstacleCoordinates.add(obstacleCoordinates);

        Cell newObsCell = getCellFromCoordinatesColumn(mapX, mapY);
        newObsCell.setType(CellType.OBSTACLE);
        // Assign obstacle no
        for (int i = 0; i < obstacleNoArray.length; i++) {
            if (obstacleNoArray[i] != -1) {
                if (newObsCell.obstacleNo == -1) {
                    newObsCell.obstacleNo = obstacleNoArray[i]; // assign obstacle no
                    obstacleNoArray[i] = -1; // set index to marked as used
                    break;
                }
            }
        }
        this.invalidate();
        showLog("Exiting setObstacleCoordinates");
        updateHomeObstacleListView();
//         UNCOMMENT LINE BELOW FOR C6/7
//        sendUpdatedObstacleInformation();
    }

    protected void removeObstacleCoordinate(int mapX, int mapY){
        showLog("Entering removeObstacleCoordinate");
        Cell removeObstacleCell = getCellFromCoordinateRow(mapX, mapY);
        if(removeObstacleCell.type != CellType.OBSTACLE){
            Log.i(TAG, "removeObstacleCoordinate: Tried to remove obstacle that is not an obstacle at X:"+mapX+"Y: "+mapY);
            return;
        }
        //Return available obstacle no.
        int oldObstacleNo = removeObstacleCell.obstacleNo;
        obstacleNoArray[oldObstacleNo-1] = oldObstacleNo;
        //Reset the obstacle cell
        removeObstacleCell.obstacleNo=-1;
        removeObstacleCell.targetID=null;
        removeObstacleCell.obstacleDirection = Direction.NONE;
        removeObstacleCell.setType(CellType.UNEXPLORED);
        //Remove from arraylist
        for(int i = 0; i< obstacleCoordinates.size(); i++){
            int[] coordinates = obstacleCoordinates.get(i);
            if(coordinates[0] == mapX && coordinates[1]==mapY){
                obstacleCoordinates.remove(i);
                break;
            }
        }
        this.invalidate();
        updateHomeObstacleListView();
        showLog("Exiting removeObstacleCoordinate");
    }

    private ArrayList<int[]> getObstacleCoordinates() {
        return obstacleCoordinates;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private String getObstacleDirectionText(int inDirection) {
        String direction = "";
        switch (inDirection) {
            case 0:
                direction = "NONE";
                break;
            case 1:
                direction = "UP";
                break;
            case 2:
                direction = "DOWN";
                break;
            case 3:
                direction = "LEFT";
                break;
            case 4:
                direction = "RIGHT";
                break;
        }

        return direction;
    }

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        CellType type;
        int id = -1;

        // Obstacle Face @ GridMap.Java -> class Cell
        Direction obstacleDirection = Direction.NONE;

        String targetID = null;
        int obstacleNo = -1;

        private Cell(float startX, float startY, float endX, float endY, CellType type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            setType(type);
        }

        public void setType(CellType type) {
            this.type = type;
            switch (type) {
                case OBSTACLE:
                    this.paint = obstaclePaint;
                    break;
                case ROBOT:
                    this.paint = robotPaint;
                    break;
                case BORDER:
                    this.paint = endPaint;
                    break;
                case UNEXPLORED:
                    this.paint = unexploredPaint;
                    break;
                default:
                    Log.d(TAG, "setType default: " + type);
                    break;
            }
        }

        // Obstacle Face @ GridMap.Java -> class Cell
        public void setObstacleDirection(Direction obstacleDirection) {
            this.obstacleDirection = obstacleDirection;
        }

        public Direction getObstacleDirection() {
            return this.obstacleDirection;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    private enum CellType {
        UNEXPLORED,
        OBSTACLE,
        ROBOT,
        BORDER
    }

    public enum Direction{
        NONE,
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

}

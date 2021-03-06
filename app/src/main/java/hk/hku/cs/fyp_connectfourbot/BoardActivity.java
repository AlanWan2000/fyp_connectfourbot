package hk.hku.cs.fyp_connectfourbot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.cppexample.CppActivity;
import com.makerlab.bt.BluetoothConnect;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class BoardActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    static private String LOG_TAG = BoardActivity.class.getSimpleName();

    // Receiver
    private static final String FINISH_ACTIVITY_BROADCAST = BuildConfig.APPLICATION_ID + ".FINISH_ACTIVITY_BROADCAST";
    private LocalBroadcastManager localBroadcastManager;
    JavaCameraView javaCameraView;
//    TextView textView;
    TextView scoreView;
    TextView messageView;
    TextView turnView;
    Button hintsButton;
    Mat mRGBA, mRGBAT;
    public int count = 10;
    public int turnCount = 1;
    public int playerScore = 0;
    public int boardScore = 1;
    public int height = 5;
    public int width = 6;
    public String realStr = "";

    public ArrayList<ArrayList<ArrayList<Integer>>> trueBoard = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> preState = new ArrayList<>();
    public ArrayList<ArrayList<Integer>> currentState = new ArrayList<>();
    public ArrayList<Integer> validPos = new ArrayList<>();
    public ArrayList<Integer> bestMove = new ArrayList<>();

    public boolean initialReading = false;

    private CppActivity mCppActivity = new CppActivity();
//    private TextView HintsView;
    public String sequence = "";
    public int playerCode;
    public String firstPlayer;
    public String secondPlayer;

    private BluetoothConnect mBluetoothConnect;
    private Timer mDataSendTimer = null;
    private static RobotArmGcode mRobotArmGcode = new RobotArmGcode();
    private static Queue<byte[]> mQueue = new LinkedList<>();

    private final int[][] discID = {
            {R.id.disc00, R.id.disc01, R.id.disc02, R.id.disc03, R.id.disc04, R.id.disc05, R.id.disc06},
            {R.id.disc10, R.id.disc11, R.id.disc12, R.id.disc13, R.id.disc14, R.id.disc15, R.id.disc16},
            {R.id.disc20, R.id.disc21, R.id.disc22, R.id.disc23, R.id.disc24, R.id.disc25, R.id.disc26},
            {R.id.disc30, R.id.disc31, R.id.disc32, R.id.disc33, R.id.disc34, R.id.disc35, R.id.disc36},
            {R.id.disc40, R.id.disc41, R.id.disc42, R.id.disc43, R.id.disc44, R.id.disc45, R.id.disc46},
            {R.id.disc50, R.id.disc51, R.id.disc52, R.id.disc53, R.id.disc54, R.id.disc55, R.id.disc56}
    };

    private final int[] hintID = {R.id.hint0, R.id.hint1, R.id.hint2, R.id.hint3, R.id.hint4, R.id.hint5, R.id.hint6};

    private ImageView[][] discImgView = new ImageView[6][7];
    private ImageView[] hintImgView = new ImageView[7];
    private ImageView turnImage;
    private int humanColor;
    private boolean useHint=false;



    BroadcastReceiver FinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            Log.e(LOG_TAG, "onReceiveFinishReceiver()");
            Log.e(LOG_TAG, action);
            if (action.equals(FINISH_ACTIVITY_BROADCAST)) {
                Log.e(LOG_TAG, "it is FINISH_ACTIVITY_BROADCAST");
//                Toast.makeText(getApplicationContext(), "finish me", Toast.LENGTH_SHORT).show();
                Intent backIntent = new Intent(BoardActivity.this, MainActivity.class);
                backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(backIntent);
                finish();
            }
        }
    };

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                }
            }

        }

        @Override
        public void onPackageInstall(int operation, InstallCallbackInterface callback) {
            super.onPackageInstall(operation, callback);
        }
    };

    private static final String TAG = "BoardActivity";

    static {
        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV Config successful 1");
        }
        else {
            Log.d(TAG, "OpenCV Config failed");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        setContentView(R.layout.activity_board);
        mCppActivity.setNativeAssetManager(getAssets());
//        HintsView = findViewById(R.id.hintsView);
//        textView = findViewById(R.id.textView2);
        hintsButton = findViewById(R.id.hintsButton);
        scoreView = findViewById(R.id.scoreView);
        messageView = findViewById(R.id.messageView);
        turnView = findViewById(R.id.turnView);
        turnImage = findViewById(R.id.turnImage);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        javaCameraView = (JavaCameraView) findViewById(R.id.cameraview1);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(BoardActivity.this);
        preState = initBoardState();
        currentState = initBoardState();
        Payload serializable = (Payload) getIntent().getSerializableExtra("serializable");
        if(serializable != null){
            trueBoard = serializable.coor;

            playerCode = serializable.player;
            Log.d(TAG, String.valueOf(firstPlayer));
            if ( playerCode == 1){
                firstPlayer = "Human";
                secondPlayer = "Robot";
            }
            else if( playerCode == 0){
                firstPlayer = "Robot";
                secondPlayer = "Human";
            }
            Log.d(TAG, "Data retrieved");
        }
        for(int i = 0; i < 7; i++){
            if(i == 3){
                bestMove.add(1);
            }
            else{
                bestMove.add(0);
            }

        }

        for ( int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                discImgView[r][c] = findViewById(discID[r][c]);
            }
        }
        for (int c = 0; c < 7; c++) {
            hintImgView[c] = findViewById(hintID[c]);
        }

        if (firstPlayer=="Robot"){
            hintsButton.setEnabled(false);
            turnImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_robot_120));
            turnView.setText(firstPlayer+" takes the turn");
        }


/*        for ( int r = 0; r < 6; r++) {
            String tempRow = "";
            for (int c = 0; c < 7; c++) {
                tempRow += "X ";
            }
            realStr += tempRow + "\n";
        }
        updateText();*/

        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                updateText(); //Update board State on Screen
                                updateSequence(); //Update the new sequence if there are any changes
                                updateTurn(); //Update the turn display if turn changed

                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat input = inputFrame.rgba();
        Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2HSV);


        if (count != 0){
            count--;

        }
        else{
            count = 10;

            String boardStr = "Board State: \n";
            for (int x=0; x < trueBoard.size() ; x++ ) {
                String rowStr = "";
                for (int y = 0; y < trueBoard.get(x).size(); y++){
                    Point p = new Point((int) trueBoard.get(x).get(y).get(0), (int) trueBoard.get(x).get(y).get(1));
                    double[] pixel = input.get(trueBoard.get(x).get(y).get(1), trueBoard.get(x).get(y).get(0));
                    rowStr = getColor(pixel) + " " +rowStr;
                    if(!initialReading){
                        constructBoardState(getColor(pixel), x, y, 0);
                    }
                    constructBoardState(getColor(pixel), x, y, 1);


                }
                boardStr += rowStr + "\n";
            }
            if(!initialReading){
                getInitValidPos();
                if (firstPlayer == "Robot" && turnCount == 1){ //
                    //ask robot to pick and place col 4
                    robotMove(4);
                }

            }
//            realStr = boardStr;

            initialReading = true;


        }
        return inputFrame.rgba();
    }

    public String getColor(double[] rgbValue){
        int Hue = (int) rgbValue[0];
        if ( Hue > 100 ){
            return "P";
        }
        else if (Hue < 100 && Hue >30){
            return "G";
        }
        else{
            return "X";
        }

    }

    public void displayBoard(){
        //display the board state in the virtual board
    }

    public void updateTurn(){
        //display who takes the next turn

        int order = turnCount%2;

        if (order == 1){
//            Log.i(TAG, "player:"+firstPlayer);
            String tempStr = firstPlayer+" takes the turn";
            turnView.setText(tempStr);
            if (firstPlayer=="Robot"){
                turnImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_robot_120));
            } else {
                turnImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_face_120));
            }
        }
        else if(order == 0){
//            Log.i(TAG, "player:"+secondPlayer);
            String tempStr = secondPlayer+" takes the turn";
            turnView.setText(tempStr);
            if (secondPlayer=="Robot"){
                turnImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_robot_120));
            } else {
                turnImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_face_120));
            }
        }


    }

    public void robotMove(int col){
        Log.d(TAG, "Start robotMove");
        synchronized (mQueue) {
            mQueue.add(mRobotArmGcode.goHome()); //start at home
            mQueue.add(mRobotArmGcode.place()); //open gripper
            mQueue.add(mRobotArmGcode.goLeft()); //move left to be above stacker
            mQueue.add(mRobotArmGcode.goDiscPos()); //move down to pick
            mQueue.add(mRobotArmGcode.pick()); //pick disc
            mQueue.add(mRobotArmGcode.goLeft()); //move up
            mQueue.add(mRobotArmGcode.goHome()); //back to home
            mQueue.add(mRobotArmGcode.toCol(col)); //move down into column
            mQueue.add(mRobotArmGcode.place()); //place the disc
        }
    }


    public void showHints(View view){
//        Log.d(TAG, "Sequence: "+sequence);
        String displayString = "";
        useHint = true;
        for(int i = 0; i < bestMove.size(); i++){
            if(bestMove.get(i) == 1){
                displayString += "O" + ",";
                if (humanColor == 2)
                    hintImgView[i].setImageDrawable(getResources().getDrawable(R.drawable.pink_disc));
                else
                    hintImgView[i].setImageDrawable(getResources().getDrawable(R.drawable.teal_disc));
            }
            else{
                hintImgView[i].setImageDrawable(getResources().getDrawable(R.drawable.blank_disc));
                displayString += "X" + ",";
            }
        }
//        HintsView.setText(displayString);
        ArrayList<Integer> scores = new ArrayList<>();
    }

    public void getBestMove(){
        Log.d(TAG, "Sequence: "+sequence);
        ArrayList<Integer> scores = new ArrayList<>();
        ArrayList<Integer> tempMoves = new ArrayList<>();
        int max;
        if (sequence != ""){
            String scoreResults = mCppActivity.mystringFromJNI(sequence);
            if (scoreResults != "0123456"){
                String[] splitText = scoreResults.split(",");
                for(int i = 0; i < splitText.length; i++){
                    scores.add(Integer.valueOf(splitText[i]));
                }
                max = Collections.max(scores);
                boardScore = Collections.max(scores);
                for(int i = 0; i < scores.size(); i++){
                    if(scores.get(i) == max){
                        tempMoves.add(1);
                    }
                    else {
                        tempMoves.add(0);
                    }
                }
                bestMove = tempMoves;
                Log.i(TAG, scoreResults);
            }

        }
    }

//    public void updateText(){
//        textView.setText(realStr);
//    }

    public void updateScore(int col, int row){
        int order = turnCount%2;
        boolean goodMove = false;
        Log.i(TAG, String.valueOf("order:"+order+" playerCode:"+playerCode));
        if (order == playerCode){
            for (int i = 0; i < bestMove.size(); i++){
                Log.i(TAG, String.valueOf("get:"+bestMove.get(i)+" col:"+col+"i:"+i));
                if (bestMove.get(i) == 1 && col == 6 - i){
                    if (!useHint){
                        playerScore += 5;
                    }
                    goodMove = true;
                }
            }
            if (goodMove){
                messageView.setText("Good Move!");
            }
            else{
                messageView.setText("Bad Move!");
            }
        }
        scoreView.setText(String.valueOf(playerScore));
    }

    public void updateSequence(){
        //compare the current board state and previous board state to output the sequence
        //validate if the board is valid first
        if(isValidBoard()){
            ArrayList<Integer> tempCol = new ArrayList<>();
            for ( int col = 0; col < 7; col++){
                for (int row = 0; row < 6; row++){
                    if (currentState.get(col).get(row) - preState.get(col).get(row) > 0 && validateChange(col, row)){
                        //update the board state if all validation is completed, change is made here
                        tempCol = preState.get(col);    //Set new state to preState
                        tempCol.set(row, currentState.get(col).get(row));
                        preState.set(col, tempCol);
                        int colorCode = preState.get(col).get(row);
                        Log.i(TAG, "updateSequence"+ String.valueOf("Col:"+col+" Row:"+row));
                        Log.i(TAG, "updateSequence" + String.valueOf("ColorCode:"+colorCode));
                        sequence += String.valueOf(6-col+1);    //Update sequence

                        if (turnCount%2 != playerCode){ // robot turn
                            humanColor = 3 - colorCode; // opposite of robot color
                        }

                        if (turnCount%2 == playerCode){//if human player's turn
                            humanColor = colorCode;
                        }

                        Log.i(TAG, "discImgView "+ String.valueOf(row+" "+col));
                        if (colorCode==2){
                            discImgView[row][6 - col].setImageDrawable(getResources().getDrawable(R.drawable.pink_disc));
                        } else {
                            discImgView[row][6 - col].setImageDrawable(getResources().getDrawable(R.drawable.teal_disc));
                        }


                        updateScore(col, row);  //if human player's turn
                        checkWinning(6 - col, row, colorCode); //Flip vertically and horizontally
                        getBestMove();  //Get new best move
                        realStr = "";
                        /*for ( int r = 5; r >= 0; r--) {
                            String tempRow = "";
                            for (int c = 6; c >= 0; c--) {
                                if (preState.get(c).get(r) == 1){
                                    tempRow += "G ";
                                }
                                else if (preState.get(c).get(r) == 2){
                                    tempRow += "P ";
                                }
                                else if (preState.get(c).get(r) == 0){
                                    tempRow += "X ";
                                }
                            }
                            realStr += tempRow + "\n";
                        }
                        updateText();*/
                        turnCount++;



                        //Turn ends

                        if (turnCount%2 == playerCode){//if human player's turn
                            calAndDisplayMessage();
                            useHint = false;
                            hintsButton.setEnabled(true);
                        }

                        if (turnCount%2 != playerCode){ //if robot player's turn
                            hintsButton.setEnabled(false);
                            for(int i = 0; i < 7; i++){
                                hintImgView[i].setImageDrawable(getResources().getDrawable(R.drawable.blank_disc));
                            }

                            ArrayList<Integer> tempBestMove = new ArrayList<>();
                            if(bestMove.size() > 0){
                                for(int i = 0; i < bestMove.size(); i++){
                                    if(bestMove.get(i) == 1){
                                        tempBestMove.add(i+1);
                                    }
                                }
                                Collections.shuffle(tempBestMove);
                                //ask robot to pick and place col tempBestMove.get(0);
                                robotMove(tempBestMove.get(0));
                            }
                        }

                    }
                }
            }
        }

    }

    public void checkWinning(int col, int row, int colorCode){
        boolean win = false;
        boolean draw = false;
        int finishStatus = 0; //default robot win
        String prevPLayer = "";
        Log.i(TAG, String.valueOf("Col:"+col+" Row:"+row+"colorCode:"+colorCode));

        int pos = 0;
        int neg = 0;

        if (turnCount%2 == playerCode){
            prevPLayer = "Human";
        }
        else {
            prevPLayer = "Robot";
        }

        //check if the game has ended and which player won
        if (row >= 3){
            Log.i(TAG, String.valueOf(preState.get(6 - col).get(row - 1)));
            Log.i(TAG, String.valueOf(preState.get(6 - col).get(row - 2)));
            Log.i(TAG, String.valueOf(preState.get(6 - col).get(row - 3)));
            //check vertical
            if (preState.get(6 - col).get(row - 1) == colorCode && preState.get(6 - col).get(row - 2) == colorCode && preState.get(6 - col).get(row - 3) == colorCode){
                //check 3 discs below current disc
                win = true;
                if (prevPLayer == "Human"){
                    finishStatus = 1;
                }
            }
        }

        for (int i = 0; i <= 3; i++){
            //check horizontal
            if (preState.get(i).get(row) == colorCode && preState.get(i+1).get(row) == colorCode && preState.get(i+2).get(row) == colorCode && preState.get(i+3).get(row) == colorCode){
                win = true;
                if (prevPLayer == "Human"){
                    finishStatus = 1;
                }
            }
        }

        //check diagonal
        for (int dy = -1; dy <= 1 ; dy++){
            for (int dx = -1; dx <= 1; dx++){
                int step = 0;
                if (dx == -1 && dy == -1){
                    step = Math.min(col, row);
                }
                if (dx == 1 && dy == 1){
                    step = Math.min(6 - col, 5 - row);
                }
                if (dx == -1 && dy == 1){
                    step = Math.min(col, 5 - row);
                }
                if (dx == 1 && dy == -1) {
                    step = Math.min(6 - col, row);
                }
                for (int i = 1; i <= step; i++){
                    if (preState.get(6- (col + dx*i)).get(row + dy*i) == colorCode){
                        if (dx/dy == 1){
                            pos += 1;
                        }
                        else{
                            neg += 1;
                        }
                    }
                    else{
                        break;
                    }
                }
            }
        }

        if (pos >= 3 || neg >= 3){
            win = true;
            if (prevPLayer == "Human"){
                finishStatus = 1;
            }
        }

        //check if the game has ended and it is a draw
        if (turnCount == 42 && !win){
            draw = true;
            finishStatus = 2;
        }

        //If the game is finished, proceed to GameEndActivity
        if (win || draw){
            Intent gameEndAct = new Intent(this, GameEndActivity.class);
            gameEndAct.putExtra("score", playerScore);
            gameEndAct.putExtra("finishStatus", finishStatus);
            startActivity(gameEndAct);
            Log.i(TAG, String.valueOf("score:"+playerScore+" finishStatus:"+finishStatus));
        }

    }


    public boolean isValidBoard(){
        Integer changedPos = 0;
        for ( int col = 0; col < 7; col++){
            for (int row = 0; row < 6; row++){
                if (currentState.get(col).get(row) - preState.get(col).get(row) > 0){
                    changedPos += 1;
                }
            }
        }
        if (changedPos == 1){
            return true;
        }
        return false;
    }

    public void getInitValidPos(){
        //check the board to know the valid positions
        for ( int col = 0; col < 7; col++){
            boolean posConfirm = false;
            for (int row = 0; row < 6; row++){

                if(!posConfirm && preState.get(col).get(row)==0){
                    Log.i(TAG, String.valueOf("pos:"+col+","+row));
                    validPos.add(row);
                    posConfirm = true;
                }
            }
        }
    }

    public boolean validateChange(int col, int row){
        Log.d(TAG, "row: "+row+"real row:"+String.valueOf(currentState.get(col).get(row)));
        if(validPos.get(col) == row){
            Log.i(TAG, String.valueOf("pos:"+col+","+row));
            validPos.set(col, row+1);
            return true;
        }
        return false;
    }

    //BoardState related

    public ArrayList<ArrayList<Integer>> initBoardState(){
        ArrayList<ArrayList<Integer>> tempBoard = new ArrayList<>();
        for ( int col = 0; col < 7; col++){
            ArrayList<Integer> tempCol = new ArrayList<>();
            for (int row = 0; row < 6; row++){
                tempCol.add(0);
            }
            tempBoard.add(tempCol);
        }
        Log.i(TAG, String.valueOf("boardState.size():"+tempBoard.size()));
        Log.i(TAG, String.valueOf("boardState.get(0).size():"+tempBoard.get(0).size()));
        return tempBoard;
    }

    public void constructBoardState(String color, int x, int y, int board){
        //column first
        int code = 0;
        int posX = 5 - x;
        int posY = y;
        ArrayList<Integer> tempCol = new ArrayList<>();
        if(color == "G"){
            code = 1;
        }
        else if(color == "P"){
            code = 2;
        }
        if (board == 0){
//            Log.i(TAG, String.valueOf("X:"+posX+", Y:"+posY+", code:"+code));
            tempCol = preState.get(posY);
            tempCol.set(posX, code);
            preState.set(posY, tempCol);
        }
        else if(board == 1){
            tempCol = currentState.get(posY);
            tempCol.set(posX, code);
            currentState.set(posY, tempCol);
//            Log.i(TAG, String.valueOf("X:"+posX+", Y:"+posY+", code:"+currentState.get(posY).get(posX)));
        }


    }

    public void calAndDisplayMessage(){
        //Calculate and Display User's Message
        int tempBoardScore = boardScore;
        if (tempBoardScore == 0 ){
            messageView.setText("You can draw");
        }
        if (firstPlayer == "Human"){
            if (tempBoardScore > 0){
                //if human first and +ve
                int movePlayed = turnCount / 2;
                int winMove = 22 - tempBoardScore - movePlayed;
                messageView.setText("You can win in " + String.valueOf(winMove) + " Move(s)!");
            }
            else if (tempBoardScore < 0){
                //if human first and -ve
                int movePlayedByOpponent = turnCount / 2;
                int loseMove = tempBoardScore + 22 - movePlayedByOpponent;
                messageView.setText("You lose in " + String.valueOf(loseMove) + " Move(s)!");
            }
        }
        else if (firstPlayer == "Robot"){
            if (tempBoardScore > 0){
                //if human first and +ve
                int movePlayed = turnCount / 2 - 1;
                int winMove = 22 - tempBoardScore - movePlayed;
                messageView.setText("You can win in " + String.valueOf(winMove) + " Move(s)!");
            }
            else if (tempBoardScore < 0){
                //if human first and -ve
                int movePlayedByOpponent = turnCount / 2;
                int loseMove = tempBoardScore + 22 - movePlayedByOpponent;
                messageView.setText("You lose in " + String.valueOf(loseMove) + " Move(s)!");
            }
        }

    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        //Get the width and height to be displayed on the screen
        Log.d(TAG, "Width: "+Integer.toString(width));
        Log.d(TAG, "Height: "+Integer.toString(height));
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        //Actions when the camera stop
        mRGBA.release();
    }


    @Override
    protected void onStart() {
        super.onStart();
        localBroadcastManager.registerReceiver(FinishReceiver, new IntentFilter(FINISH_ACTIVITY_BROADCAST));
        MainActivity activity = MainActivity.getInstance();
        mBluetoothConnect = activity.getBluetoothConnect();
        mDataSendTimer = new Timer();
        mDataSendTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
        mQueue.clear();

    }

    class DataSendTimerTask extends TimerTask {
        private String LOG_TAG = ControllerFragment.DataSendTimerTask.class.getSimpleName();

        @Override
        public void run() {
            if (mBluetoothConnect == null) {
                return;
            }
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    mBluetoothConnect.send(mQueue.remove());
                    Log.e(LOG_TAG, "DataSendTimerTask.run() - send");
                }
            }
        }
    }



    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(FinishReceiver);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();


        if (OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV Config successful");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
        else {
            Log.d(TAG, "OpenCV Config failed");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }

    @Override
    public void onBackPressed(){
        //Back to MainActivity instead of prevActivity
        Intent intent = new Intent(BoardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
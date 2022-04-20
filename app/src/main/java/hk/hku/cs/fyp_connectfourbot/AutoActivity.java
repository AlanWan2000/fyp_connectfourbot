package hk.hku.cs.fyp_connectfourbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import androidx.camera.core.ImageProxy;
//
//import org.pytorch.IValue;
//import org.pytorch.LiteModuleLoader;
//import org.pytorch.Module;
//import org.pytorch.Tensor;
//import org.pytorch.torchvision.TensorImageUtils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class AutoActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRGBA, mRGBAT;
    public int count = 100;
    Mat cannyImg = new Mat();
    Mat circles = new Mat();
    public ArrayList<ArrayList<Integer>> stablePoints = new ArrayList<>();
    int precision = 30; //control the precision to determine new points
    public ArrayList<ArrayList<Integer>> locatedPoints = new ArrayList<>();
    public boolean boardInit = false;
    public ArrayList<ArrayList<ArrayList<Integer>>> board = new ArrayList<>();
    public ArrayList<ArrayList<ArrayList<Integer>>> trueBoard = new ArrayList<>();
    public int nextStage = 0;
    public int player;
    Button refresh;

    static private String LOG_TAG = AutoActivity.class.getSimpleName();

    // Receiver
    private static final String FINISH_ACTIVITY_BROADCAST = BuildConfig.APPLICATION_ID + ".FINISH_ACTIVITY_BROADCAST";
    private LocalBroadcastManager localBroadcastManager;

    BroadcastReceiver FinishReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            Log.e(LOG_TAG, "onReceiveFinishReceiver()");
            Log.e(LOG_TAG, action);
            if (action.equals(FINISH_ACTIVITY_BROADCAST)) {
                Log.e(LOG_TAG, "it is FINISH_ACTIVITY_BROADCAST");
//                Toast.makeText(getApplicationContext(), "finish me", Toast.LENGTH_SHORT).show();
//                Intent backIntent = new Intent(AutoActivity.this, MainActivity.class);
//                backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(backIntent);
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
        setContentView(R.layout.activity_auto);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        player = getIntent().getIntExtra("player", 0);
//        Log.d(TAG, player);

        javaCameraView = (JavaCameraView) findViewById(R.id.cameraview);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(AutoActivity.this);
        refresh = (Button) findViewById(R.id.refresh);



    }



    @Override
    public void onCameraViewStarted(int width, int height) {
        //Get the width and height to be displayed on the screen
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        //Actions when the camera stop
        mRGBA.release();
    }



    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        int x_c = 0, y_c = 0, width = 100, height = 100;

        Mat input = inputFrame.rgba();
        Imgproc.rectangle(input, new Point(190, 480), new Point(0, 0), new Scalar(255, 255, 255), -1);
        Imgproc.rectangle(input, new Point(720, 480), new Point(500, 0), new Scalar(255, 255, 255), -1);
        Imgproc.rectangle(input, new Point(190, 0), new Point(500, 20), new Scalar(225, 225, 225), -1);
        Imgproc.rectangle(input, new Point(190, 460), new Point(500, 480), new Scalar(225, 225, 225), -1);
        checkBoardInit();
        if (!boardInit){

            Imgproc.blur(input, input, new Size(3, 3), new Point(2, 2));
            Imgproc.cvtColor(input, cannyImg, Imgproc.COLOR_BGR2GRAY);


            Imgproc.HoughCircles(cannyImg, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, 50, 100, 50, 1, 100);
            if (locatedPoints.size() >= 2) {
                board = getInlines();

            }
            if (circles.cols() > 0) {
                for (int x=0; x < 100; x++ ) {
                    double circleVec[] = circles.get(0, x);


                    if (circleVec == null) {
                        break;
                    }

                    Point center = new Point((int) circleVec[0], (int) circleVec[1]);
                    ArrayList<Integer> point = new ArrayList<>();
                    point.add((int) circleVec[0]);
                    point.add((int) circleVec[1]);
                    point.add(1);
                    addStablePoints(point);


                    Imgproc.circle(cannyImg, center, 0, new Scalar(255, 255, 255), 20);
                }
            }


            if (board.size() >0){
                for (int x=0; x < board.size() ; x++ ) {
                    Log.i(TAG, String.valueOf("Number of Lines "+ board.get(x).size()));
                    Point p1 = new Point((int) board.get(x).get(0).get(0), (int) board.get(x).get(0).get(1));
                    Point p2 = new Point((int) board.get(x).get(board.get(x).size()-1).get(0), (int) board.get(x).get(board.get(x).size()-1).get(1));
                    Imgproc.line(cannyImg, p1, p2, new Scalar(255, 255, 255), 5);
                }
            }
            Imgproc.line(cannyImg, new Point(190, 0), new Point(190, 480), new Scalar(255, 255, 255), 5);
            Imgproc.line(cannyImg, new Point(500, 0), new Point(500, 480), new Scalar(255, 255, 255), 5);
            Imgproc.line(cannyImg, new Point(190, 20), new Point(500, 20), new Scalar(255, 255, 255), 5);
            Imgproc.line(cannyImg, new Point(190, 460), new Point(500, 460), new Scalar(255, 255, 255), 5);
            circles.release();
            input.release();
            return cannyImg;
        }
        else{
            nextStage++;
            if(nextStage == 1){
                Intent boardAct = new Intent(this, BoardActivity.class);
                boardAct.putExtra("serializable", new Payload(trueBoard, player));
                startActivity(boardAct);
            }

            return input;
        }

    }

    public void refreshRecog(View view){
        stablePoints = new ArrayList<>();
        locatedPoints = new ArrayList<>();
        board = new ArrayList<>();
        trueBoard = new ArrayList<>();
    }

    public void checkBoardInit(){
        if (board.size() > 0){
            ArrayList<ArrayList<ArrayList<Integer>>> tempBoard = new ArrayList<>();
            for (int i = 0; i < board.size(); i++) {
                if (board.get(i).size() == 7){
                    //This is a row confirmed
                    tempBoard.add(board.get(i));
                }
            }
            if (tempBoard.size() == 6){
                //Got the board
                trueBoard = tempBoard;
                boardInit = true;
            }
        }
    }

    public void addStablePoints(ArrayList circleVec){
        int i = isStablePoints(circleVec);
        if(i == -1){
            //add new
            stablePoints.add(circleVec);

        }
        else if(i == -2){
            if(!locatedPoints.contains(circleVec)){
                boolean state = true;
                for(int k = 0;k<locatedPoints.size();k++){
                    if(Math.abs((int)circleVec.get(0) - locatedPoints.get(k).get(0)) <= precision && Math.abs((int)circleVec.get(1) - locatedPoints.get(k).get(1)) <= precision){
                        state = false;
                    }
                }
                if(state){
                    locatedPoints.add(circleVec);
                }

            }
        }
        else{
            //add count
            ArrayList<Integer> tempPoint = stablePoints.get(i);
            int tempCount = tempPoint.get(2) + 1;
            tempPoint.set(2, tempCount);
            stablePoints.set(i, tempPoint);
        }
    }

    public int isStablePoints(ArrayList circleVec){
        for (int i = 0; i < stablePoints.size(); i++) {
            if (Math.abs((int)circleVec.get(0)-stablePoints.get(i).get(0)) <= precision && Math.abs((int)circleVec.get(1)-stablePoints.get(i).get(1)) <= precision){
                if (stablePoints.get(i).get(2) == 5){
                    return -2;
                }
                return i;
            }
        }
        return -1;
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> getInlines() {
        ArrayList<ArrayList<Integer>> sortedArray = sortTheArray((ArrayList<ArrayList<Integer>>) locatedPoints.clone(), 0);
        ArrayList<ArrayList<ArrayList<Integer>>> tempBoard = new ArrayList<>();
        ArrayList<ArrayList<Integer>> lineArray = new ArrayList<>();
        lineArray.add(sortedArray.get(0));
        for (int i = 1; i < sortedArray.size(); i++) {
            ArrayList<Integer> coor = sortedArray.get(i);
            if (sortedArray.get(i).get(0) - sortedArray.get(i-1).get(0) > 10){
                //They are not on the same line
                tempBoard.add(sortTheArray(lineArray, 1));
                lineArray = new ArrayList<>();
                lineArray.add(sortedArray.get(i));
            }
            else if (i == sortedArray.size() - 1) {
                lineArray.add(sortedArray.get(i));
                tempBoard.add(sortTheArray(lineArray, 1));
            }
            else {
                //They are on the same line
                lineArray.add(sortedArray.get(i));
            }
        }
        return tempBoard;
    }

    public ArrayList<ArrayList<Integer>> sortTheArray(ArrayList<ArrayList<Integer>> arrayToBeSorted, int pos){
        ArrayList<ArrayList<Integer>> arrayCopy = arrayToBeSorted;
        int size = arrayCopy.size();
        for (int i = 0; i < size; i++){
            int minIndex = i;
            for (int j = i + 1; j < size ; j++){
                if (arrayCopy.get(minIndex).get(pos) > arrayCopy.get(j).get(pos)){
                    minIndex = j;
                }
            }
            ArrayList<Integer> tempArray = arrayCopy.get(i);
            arrayCopy.set(i, arrayCopy.get(minIndex));
            arrayCopy.set(minIndex, tempArray);
        }
        return arrayCopy;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        localBroadcastManager.registerReceiver(FinishReceiver, new IntentFilter(FINISH_ACTIVITY_BROADCAST));
    }

    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(FinishReceiver);
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

//    @Override
//    public void onBackPressed(){
//        //Back to MainActivity instead of prevActivity
//        Intent intent = new Intent(AutoActivity.this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//        finish();
//    }


}
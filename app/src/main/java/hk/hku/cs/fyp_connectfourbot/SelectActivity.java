package hk.hku.cs.fyp_connectfourbot;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.makerlab.bt.BluetoothConnect;

public class SelectActivity extends AppCompatActivity {
    private static String TAG = "SelectActivity";

    private BluetoothConnect mBluetoothConnect;
    private static RobotArmGcode mRobotArmGcode = new RobotArmGcode();
    AlertDialog.Builder builder;

    static private String LOG_TAG = SelectActivity.class.getSimpleName();

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
                finish();
            }
        }
    };

//    Button humanPlayer;
//    Button robotPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        humanPlayer = findViewById(R.id.youButton);
//        robotPlayer = findViewById(R.id.robotButton);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        builder = new AlertDialog.Builder(SelectActivity.this);
    }


    public void clickedYouButton(View view){
        launchReminderActivity(1);
    }

    public void clickedRobotButton(View view){
        launchReminderActivity(0);
    }

    public void launchReminderActivity(int player) {
//        Log.d(TAG, "Launch Auto Activity");
        Intent readyModeAct = new Intent(this, ReminderActivity.class);
        readyModeAct.putExtra("player", player);
        startActivity(readyModeAct);
    }

    //do autohome while player is selecting play order
    @Override
    protected void onStart() {
        super.onStart();
        localBroadcastManager.registerReceiver(FinishReceiver, new IntentFilter(FINISH_ACTIVITY_BROADCAST));
        MainActivity activity = MainActivity.getInstance();
        mBluetoothConnect = activity.getBluetoothConnect();
        if (mBluetoothConnect != null) {
            mBluetoothConnect.send(mRobotArmGcode.autoHome());
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        localBroadcastManager.unregisterReceiver(FinishReceiver);
    }

    @Override
    public void onBackPressed(){
        //Back to MainActivity instead of prevActivity

        Intent intent = new Intent(SelectActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

}
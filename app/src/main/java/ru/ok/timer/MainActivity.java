package ru.ok.timer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import java.util.Timer;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private static final String FORMAT = "%02d:%02d:%02d";
    ActivityRecord activityRecord;
    TimerService timerService;
    EditText editText;
    Button startOrStop;
    IncomingHandler handler;
    boolean isScheduled;
    boolean isStop;
    Button resetButton;
    Timer timer;
    final Messenger messenger = new Messenger(new IncomingHandler());
    Messenger toServiceMessenger;
    TestServiceConnection testServConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timerService = new TimerService();
        editText = findViewById(R.id.timer);
        startOrStop = findViewById(R.id.btn_start_stop);
        resetButton = findViewById(R.id.btn_reset);
        handler = new IncomingHandler();
        final String[] timeString = new String[1];
        final long[] timerTime = new long[1];
        Intent intent = new Intent(MainActivity.this, TimerService.class);
        boolean res = bindService(intent, (testServConn = new TestServiceConnection()), 0);
        Log.d(LOG_TAG ,"res is " + res);
        if(res){
            Log.d(LOG_TAG, "ready to start service");
            startService(intent);
            //bindService(intent, testServConn, 0);
        } else {
            Log.d(LOG_TAG, "not binned");
        }
        timer = new Timer();
        //TODO
        isScheduled = false;
        isStop = false;
        startOrStop.setOnClickListener(v -> {
            if (isScheduled) {
                isScheduled = false;
                Message msg = Message.obtain(null, TimerService.PAUSE);
                msg.replyTo = messenger;
                try {
                    toServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                startOrStop.setText(getString(R.string.stop_button));

            } else {
                startOrStop.setText(getString(R.string.start_button));
                isScheduled = true;
                timeString[0] = editText.getText().toString();
                try {
                    timerTime[0] = Long.parseLong(timeString[0]);
                } catch (NumberFormatException e) {
                    timerTime[0] = 0;
                }

                Message msg = Message.obtain(null, TimerService.TIME_FROM_ACTIVITY, timerTime[0]);
                msg.replyTo = messenger;
                Log.d(LOG_TAG, msg.obj.toString());
                //msg.obj = timerTime[0]; //наш счетчик
                try {
                    toServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        resetButton.setOnClickListener(v -> {

            isScheduled = false;
            Message msg = Message.obtain(null, TimerService.RESET);
            msg.replyTo = messenger;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(LOG_TAG, "is looper has msg " + Looper.getMainLooper().getQueue().isIdle());
                }
                toServiceMessenger.send(msg);

            } catch (RemoteException e) {
                e.printStackTrace();
            }

        });


    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "main unbind");
        unbindService(testServConn);

    }

    @SuppressLint("DefaultLocale")
    private String makeDate(long milliseconds) {
        return String.format(FORMAT,
                TimeUnit.MILLISECONDS.toHours(milliseconds),
                TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(milliseconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(milliseconds)));
    }

    @SuppressLint("HandlerLeak")
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TimerService.TIME_TO_ACTIVITY:
                    //Log.d(LOG_TAG, "(activity)...get count");
                    editText.setText(makeDate((long) msg.obj));

                    break;
                case TimerService.SCHEDULE:
                    Log.d(LOG_TAG, "scheduled OK");

                    Toast.makeText(getApplicationContext(), "Schedule", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private class TestServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            toServiceMessenger = new Messenger(service);
            Log.d(LOG_TAG, "Connected");
            Message msg = Message.obtain(null, TimerService.CHECK_CONNECT);
            msg.replyTo = messenger;
            //msg.obj = 0L; //наш счетчик
            try {
                toServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

}

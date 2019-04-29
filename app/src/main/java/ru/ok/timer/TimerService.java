package ru.ok.timer;

import android.app.Service;

import android.content.Intent;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Process;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;

public class TimerService extends Service {

    private static final String LOG_TAG = "TimerService";
    private Timer timer = null;
    long timeMillis;
    IncomingHandler inHandler;
    static final int TIME_FROM_ACTIVITY = 0;
    static final int TIME_TO_ACTIVITY = 1;
    static final int PAUSE = 2;
    static final int RESET = 3;
    static final int SCHEDULE = 4;
    static final int CONTINUE = 5;
    static final int CHECK_CONNECT = 6;
    static final int NOT_EXIST = 7;

    Messenger messenger;
    Messenger toActivityMessenger;
    private boolean isScheduled = false;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "service start");
        startTask();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "service onCreate");

        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        inHandler = new IncomingHandler(thread.getLooper());
        messenger = new Messenger(inHandler);
        timer = new Timer();


    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "Destroyed");
        resetTimer();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "Bind");
        if (!isScheduled) {
            Message outMsg = Message.obtain(inHandler, NOT_EXIST);
            outMsg.replyTo = messenger;
            //Log.d(LOG_TAG, "ready message " + toActivityMessenger.toString());
            try {
                if (toActivityMessenger != null) {
                    Log.d(LOG_TAG, "Send not_exist message");
                    toActivityMessenger.send(outMsg);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //timeMillis = intent.getLongExtra("time", 10);
        return messenger.getBinder();
    }


    private void resetTimer() {
        timer.cancel();
        timer.purge();
        timer = new Timer();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "Unbind");
        return super.onUnbind(intent);

    }

    private void startTask() {
        if (isScheduled) {
            isScheduled = false;

        } else {
            isScheduled = true;
            long i = System.currentTimeMillis();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Message outMsg = Message.obtain(inHandler, TIME_TO_ACTIVITY);

                    outMsg.obj = timeMillis;
                    outMsg.replyTo = messenger;
                    try {
                        if (toActivityMessenger != null) {
                            Log.d(LOG_TAG, "send time to activity " + timeMillis);
                            toActivityMessenger.send(outMsg);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    timeMillis -= 1000;

                }
            }, 0, 1000);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    resetTimer();
                    isScheduled = false;
                    timeMillis = 0;
                    Message outMsg = Message.obtain(inHandler, SCHEDULE);
                    outMsg.replyTo = messenger;

                    try {
                        if (toActivityMessenger != null){
                            Log.d(LOG_TAG, "Send schedule");
                            toActivityMessenger.send(outMsg);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }, timeMillis+10);
            Log.d(LOG_TAG, "SecondStep");

        }
    }

    private class IncomingHandler extends Handler {
        IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            toActivityMessenger = msg.replyTo;

            switch (msg.what) {
                case TIME_FROM_ACTIVITY:
                    if ((long) msg.obj != 0) {
                        timeMillis = (long) msg.obj;
                    }
                    startTask();
                    Log.d(LOG_TAG, "OK time: " + timeMillis);
                    break;

                case PAUSE:
                    resetTimer();
                    Log.d(LOG_TAG, "OK paused");
                    break;

                case RESET:
                    isScheduled = false;
                    timeMillis = 0;
                    resetTimer();
                    Message outMsg = Message.obtain(inHandler, TIME_TO_ACTIVITY);
                    outMsg.obj = timeMillis;
                    outMsg.replyTo = messenger;

                    try {
                        if (toActivityMessenger != null)
                            toActivityMessenger.send(outMsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    Log.d(LOG_TAG, "OK rested");
                    break;
                case CONTINUE:
                    isScheduled = true;
                    startTask();
                    Log.d(LOG_TAG, "OK resumed");

                case CHECK_CONNECT:
                    Message outMsgCheck = Message.obtain(inHandler, TIME_TO_ACTIVITY);
                    outMsgCheck.obj = timeMillis;
                    outMsgCheck.replyTo = messenger;

                    try {
                        if (toActivityMessenger != null)
                            toActivityMessenger.send(outMsgCheck);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    Log.d(LOG_TAG, "check connected");

            }
        }
    }
}

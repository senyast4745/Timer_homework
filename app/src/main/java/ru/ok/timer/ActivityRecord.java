package ru.ok.timer;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.util.Collection;
import java.util.LinkedList;

public class ActivityRecord {
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private Activity visibleActivity;

    private final Collection<Runnable> pendingVisibleActivityCallbacks = new LinkedList<>();

    public void executeOnVisible(final Runnable callback) {
        UI_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (visibleActivity == null) {
                    pendingVisibleActivityCallbacks.add(callback);
                } else {
                    callback.run();
                }
            }
        });
    }

    void setVisibleActivity(Activity visibleActivity) {
        this.visibleActivity = visibleActivity;

        if (visibleActivity != null) {
            for (Runnable callback : pendingVisibleActivityCallbacks) {
                callback.run();
            }
            pendingVisibleActivityCallbacks.clear();
        }
    }

    public Activity getVisibleActivity() {
        return visibleActivity;
    }
}

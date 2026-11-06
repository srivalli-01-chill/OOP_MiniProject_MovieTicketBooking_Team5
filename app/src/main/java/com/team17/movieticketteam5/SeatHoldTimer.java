package com.team17.movieticketteam5;

import android.os.Handler;
import android.os.Looper;

/**
 * Refreshes the seat display every 15 seconds.
 * Firestore transactions enforce actual seat ownership.
 */
public class SeatHoldTimer {

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Runnable active;

    public void start(Runnable refresh) {
        stop();

        active = new Runnable() {
            @Override
            public void run() {
                refresh.run();

                if (active == this) {
                    handler.postDelayed(this, 15000);
                }
            }
        };

        handler.postDelayed(active, 15000);
    }

    public void stop() {
        if (active != null) {
            handler.removeCallbacks(active);
        }
        active = null;
    }
}

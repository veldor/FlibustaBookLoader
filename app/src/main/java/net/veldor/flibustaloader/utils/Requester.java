package net.veldor.flibustaloader.utils;

import android.os.Handler;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

public class Requester {

    private AndroidOnionProxyManager mTor;
    private Handler mHandler;

    public void startRequestTor(AndroidOnionProxyManager tor) {
        mTor = tor;
        mHandler = new Handler();
        mStatusChecker.run();
    }

    public void stopRequestTor() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateStatus(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                long mInterval = 100;
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private void updateStatus() {
        Log.d("surprise", "Requester updateStatus " + mTor.getLastLog());
    }
}

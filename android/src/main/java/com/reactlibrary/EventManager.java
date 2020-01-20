package com.reactlibrary;

import androidx.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class EventManager {

    private ReactApplicationContext mContext;

    private static final String TAG = "RNMagtek";
    public static final String EVENT_CONNECTED = "connected";
    public static final String EVENT_DATA_RECEIVED = "onDataReceived";
    public static final String EVENT_CONNECTION_CHANGED = "onDeviceConnectionDidChange";

    public static final String EVENT_DEV = "devMessage";

    public EventManager(ReactApplicationContext context) {
        mContext = context;
    }

    public void sendEvent(String eventName, @Nullable WritableMap params) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sendEvent "+eventName+" params "+params);
        }
        if (mContext.hasActiveCatalystInstance()) {
            mContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "failed Catalyst instance not active");
            }
        }
    }
}

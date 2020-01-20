
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import android.util.Log;

//import android.widget.Toast;

import com.magtek.mobile.android.mtlib.MTDeviceFeatures;
import com.magtek.mobile.android.mtlib.MTEMVDeviceConstants;
import com.magtek.mobile.android.mtlib.MTSCRA;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTSCRAEvent;
import com.magtek.mobile.android.mtlib.MTEMVEvent;
import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTCardDataState;
import com.magtek.mobile.android.mtlib.MTDeviceConstants;
import com.magtek.mobile.android.mtlib.IMTCardData;
import com.magtek.mobile.android.mtlib.MTServiceState;
import com.magtek.mobile.android.mtlib.config.MTSCRAConfig;
import com.magtek.mobile.android.mtlib.config.ProcessMessageResponse;
import com.magtek.mobile.android.mtlib.config.SCRAConfigurationDeviceInfo;

import static com.reactlibrary.EventManager.EVENT_DEV;

public class RNMagtekModule extends ReactContextBaseJavaModule {
    private RNMagtek mag;
    public static final String TAG = "RNMagtek";
    private final ReactApplicationContext reactContext;
    private EventManager eventManager;

  public RNMagtekModule(ReactApplicationContext reactContext) {
      super(reactContext);
      Log.i(TAG, "*** INITIALIZING ");
      eventManager = new EventManager(reactContext);
      this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNMagtek";
  }

  @ReactMethod
  public void connect(Promise promise) {
      try {
          mag = new RNMagtek(reactContext, eventManager);

          WritableMap map = Arguments.createMap();

          map.putBoolean("isDeviceConnected", true);
          map.putBoolean("isDeviceOpened", true);

          promise.resolve(map);
      } catch (Exception e) {
          promise.reject("Failed to connect", e);
      }
  }

    @ReactMethod
    public void disconnect(Promise promise) {
      if (mag != null) {
          try {
              mag.close();
              mag = null;

              WritableMap map = Arguments.createMap();

              map.putBoolean("isDeviceConnected", false);

              promise.resolve(map);
          } catch (Exception e) {
              promise.reject("Failed to connect", e);
          }
      }
    }
}

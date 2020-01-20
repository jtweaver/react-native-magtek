package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

import static com.reactlibrary.EventManager.EVENT_CONNECTION_CHANGED;
import static com.reactlibrary.EventManager.EVENT_DATA_RECEIVED;
import static com.reactlibrary.EventManager.EVENT_DEV;

public class RNMagtek extends Activity
{

    private final ReactApplicationContext reactContext;
    private EventManager eventManager;
    private static final String TAG = RNMagtek.class.getSimpleName();

    public static final String EXTRAS_CONNECTION_TYPE_VALUE_AUDIO = "Audio";

    public static final String AUDIO_CONFIG_FILE = "MTSCRAAudioConfig.cfg";

    public static final String CONFIGWS_URL = "https://deviceconfig.magensa.net/service.asmx";//Production URL
    private static final String CONFIGWS_USERNAME = "magtek";
    private static final String CONFIGWS_PASSWORD = "p@ssword";
    private static final int CONFIGWS_READERTYPE = 0;
    private static final int CONFIGWS_TIMEOUT = 10000;

    private static String SCRA_CONFIG_VERSION = "102.02";

    private static final int HEADSET_PLUGGED_IN = 1;
    private static final int HEADSET_UNPLUGGED = 0;

    private final HeadSetBroadCastReceiver m_headsetReceiver = new HeadSetBroadCastReceiver();
    private final NoisyAudioStreamReceiver m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    private MTConnectionState m_connectionState = MTConnectionState.Disconnected;
    private MTConnectionType m_connectionType = MTConnectionType.Audio;
    private String m_deviceName = "Audio";
    private String m_deviceAddress = "";
    private String m_audioConfigType = "3"; // Server

    private MTSCRA m_scra;
    private AudioManager m_audioManager;
    private Handler m_scraHandler = new Handler(new SCRAHandlerCallback());

    private int m_audioVolume;
    private int headSetState = HEADSET_UNPLUGGED;
    private boolean configSavedToFile = false;

    public RNMagtek(ReactApplicationContext reactContext, EventManager eventManager) {
        this.reactContext = reactContext;
        this.eventManager = eventManager;

        m_scra = new MTSCRA(this, m_scraHandler);
        m_audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);
        configSavedToFile = false;

        this.reactContext.registerReceiver(m_headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        this.reactContext.registerReceiver(m_noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        long opened = openDevice();

        if (opened == 0) {
            WritableMap map = Arguments.createMap();

            map.putBoolean("deviceOpened", true);

            eventManager.sendEvent(EVENT_DEV, map);
        }
    }

    public void close() {
        closeDevice();
        this.reactContext.unregisterReceiver(m_headsetReceiver);
        this.reactContext.unregisterReceiver(m_noisyAudioStreamReceiver);
//        m_scra = null;
    }

    private class SCRAHandlerCallback implements Callback  {
        public boolean handleMessage(Message msg) {
            try {
                Log.i(TAG, "*** Callback " + msg.what);
                switch (msg.what) {
                    case MTSCRAEvent.OnDeviceConnectionStateChanged:
                        OnDeviceStateChanged((MTConnectionState) msg.obj);
                        break;
                    case MTSCRAEvent.OnDataReceived:
                        OnCardDataReceived((IMTCardData) msg.obj);
                        break;
                }
            } catch (Exception ex) {

            }

            return true;
        }
    }

    protected void OnDeviceStateChanged(MTConnectionState deviceState) {
        setState(deviceState);

        Log.i(TAG, "*** Device connection state changed: " + deviceState.toString());

        WritableMap params = Arguments.createMap();
        boolean connected = false;

        switch (deviceState)
        {
            case Disconnected:
                if (m_connectionType == MTConnectionType.Audio)
                {
                    restoreVolume();
                }
                break;
            case Connected:
                displayDeviceFeatures();
                if (m_connectionType == MTConnectionType.Audio)
                {
                    setVolumeToMax();
                }

                connected = true;
                break;
            case Error:
                break;
            case Connecting:
                break;
            case Disconnecting:
                break;
        }

        params.putBoolean("isDeviceConnected", connected);

        eventManager.sendEvent(EVENT_CONNECTION_CHANGED, params);
    }

    protected void OnCardDataReceived(IMTCardData cardData) {
        Log.i(TAG, "*** Data received");
        //clearDisplay();

//        sendToDisplay("[Raw Data]");
//        sendToDisplay(m_scra.getResponseData());
//
//        sendToDisplay("[Card Data]");
//        sendToDisplay(getCardInfo());
//
//        sendToDisplay("[TLV Payload]");
//        sendToDisplay(cardData.getTLVPayload());

//        WritableMap params = Arguments.createMap();
//        params.putString("rawData", m_scra.getResponseData());
//        params.putString("cardData", getCardInfo());
//        params.putString("tlvPayload", cardData.getTLVPayload());

        WritableMap params = getCardInfo();

        Log.i(TAG, "*** Map created");

        eventManager.sendEvent(EVENT_DATA_RECEIVED, params);
    }

    public WritableMap getCardInfo() {
        WritableMap params = Arguments.createMap();

        params.putString("sessionID", m_scra.getSessionID());

        WritableMap track1 = Arguments.createMap();
        track1.putString("masked", m_scra.getTrack1Masked());
        track1.putString("encrypted", m_scra.getTrack1());
        params.putMap("track1", track1);

        WritableMap track2 = Arguments.createMap();
        track2.putString("masked", m_scra.getTrack2Masked());
        track2.putString("encrypted", m_scra.getTrack2());
        params.putMap("track2", track2);

        WritableMap track3 = Arguments.createMap();
        track3.putString("masked", m_scra.getTrack3Masked());
        track3.putString("encrypted", m_scra.getTrack3());
        params.putMap("track3", track3);

        WritableMap card = Arguments.createMap();
        card.putString("pan", m_scra.getCardPAN());
        card.putString("name", m_scra.getCardName());
        card.putString("last4", m_scra.getCardLast4());
        card.putString("expDate", m_scra.getCardExpDate());
        params.putMap("card", card);
//
        WritableMap device = Arguments.createMap();
        device.putString("SN", m_scra.getMagTekDeviceSerial());
        device.putString("KSN", m_scra.getKSN());
        device.putString("magnePrint", m_scra.getMagnePrint());
        device.putString("magnePrintStatus", m_scra.getMagnePrintStatus());
        params.putMap("device", device);

        Log.i(TAG, "*** Sending Map");

        return params;
    }

    public String getCardInfo2() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("Tracks.Masked=%s \n", m_scra.getMaskedTracks()));

        stringBuilder.append(String.format("Track1.Encrypted=%s \n", m_scra.getTrack1()));
        stringBuilder.append(String.format("Track2.Encrypted=%s \n", m_scra.getTrack2()));
        stringBuilder.append(String.format("Track3.Encrypted=%s \n", m_scra.getTrack3()));

        stringBuilder.append(String.format("Track1.Masked=%s \n", m_scra.getTrack1Masked()));
        stringBuilder.append(String.format("Track2.Masked=%s \n", m_scra.getTrack2Masked()));
        stringBuilder.append(String.format("Track3.Masked=%s \n", m_scra.getTrack3Masked()));

        stringBuilder.append(String.format("MagnePrint.Encrypted=%s \n", m_scra.getMagnePrint()));
        stringBuilder.append(String.format("MagnePrint.Status=%s \n", m_scra.getMagnePrintStatus()));
        stringBuilder.append(String.format("Device.Serial=%s \n", m_scra.getDeviceSerial()));
        stringBuilder.append(String.format("Session.ID=%s \n", m_scra.getSessionID()));
        stringBuilder.append(String.format("KSN=%s \n", m_scra.getKSN()));

        //stringBuilder.append(formatStringIfNotEmpty("Device.Name=%s \n", m_scra.getDeviceName()));
        //stringBuilder.append(String.format("Swipe.Count=%d \n", m_scra.getSwipeCount()));

        stringBuilder.append(formatStringIfNotEmpty("Cap.MagnePrint=%s \n", m_scra.getCapMagnePrint()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagnePrintEncryption=%s \n", m_scra.getCapMagnePrintEncryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagneSafe20Encryption=%s \n", m_scra.getCapMagneSafe20Encryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MagStripeEncryption=%s \n", m_scra.getCapMagStripeEncryption()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.MSR=%s \n", m_scra.getCapMSR()));
        stringBuilder.append(formatStringIfNotEmpty("Cap.Tracks=%s \n", m_scra.getCapTracks()));

        stringBuilder.append(String.format("Card.Data.CRC=%d \n", m_scra.getCardDataCRC()));
        stringBuilder.append(String.format("Card.Exp.Date=%s \n", m_scra.getCardExpDate()));
        stringBuilder.append(String.format("Card.IIN=%s \n", m_scra.getCardIIN()));
        stringBuilder.append(String.format("Card.Last4=%s \n", m_scra.getCardLast4()));
        stringBuilder.append(String.format("Card.Name=%s \n", m_scra.getCardName()));
        stringBuilder.append(String.format("Card.PAN=%s \n", m_scra.getCardPAN()));
        stringBuilder.append(String.format("Card.PAN.Length=%d \n", m_scra.getCardPANLength()));
        stringBuilder.append(String.format("Card.Service.Code=%s \n", m_scra.getCardServiceCode()));
        stringBuilder.append(String.format("Card.Status=%s \n", m_scra.getCardStatus()));

        stringBuilder.append(formatStringIfNotEmpty("HashCode=%s \n", m_scra.getHashCode()));
        stringBuilder.append(formatStringIfNotValueZero("Data.Field.Count=%s \n", m_scra.getDataFieldCount()));

        stringBuilder.append(String.format("Encryption.Status=%s \n", m_scra.getEncryptionStatus()));

        //stringBuilder.append(formatStringIfNotEmpty("Firmware=%s \n", m_scra.getFirmware()));

        stringBuilder.append(formatStringIfNotEmpty("MagTek.Device.Serial=%s \n", m_scra.getMagTekDeviceSerial()));

        stringBuilder.append(formatStringIfNotEmpty("Response.Type=%s \n", m_scra.getResponseType()));
        stringBuilder.append(formatStringIfNotEmpty("TLV.Version=%s \n", m_scra.getTLVVersion()));

        stringBuilder.append(String.format("Track.Decode.Status=%s \n", m_scra.getTrackDecodeStatus()));

        String tkStatus = m_scra.getTrackDecodeStatus();

        String tk1Status = "01";
        String tk2Status = "01";
        String tk3Status = "01";

        if (tkStatus.length() >= 6)
        {
            tk1Status = tkStatus.substring(0, 2);
            tk2Status = tkStatus.substring(2, 4);
            tk3Status = tkStatus.substring(4, 6);

            stringBuilder.append(String.format("Track1.Status=%s \n", tk1Status));
            stringBuilder.append(String.format("Track2.Status=%s \n", tk2Status));
            stringBuilder.append(String.format("Track3.Status=%s \n", tk3Status));
        }

        stringBuilder.append(String.format("SDK.Version=%s \n", m_scra.getSDKVersion()));

        stringBuilder.append(String.format("Battery.Level=%d \n", m_scra.getBatteryLevel()));

        return stringBuilder.toString();
    }

    private void displayDeviceFeatures()
    {
        if (m_scra != null)
        {
            MTDeviceFeatures features = m_scra.getDeviceFeatures();

            if (features != null)
            {
                StringBuilder infoSB = new StringBuilder();

                infoSB.append("[Device Features]\n");

                infoSB.append("Supported Types: " + (features.MSR ? "(MSR) ":"") + (features.Contact ? "(Contact) ":"") + (features.Contactless ? "(Contactless) ":"") + "\n");
                infoSB.append("MSR Power Saver: " + (features.MSRPowerSaver ? "Yes":"No") + "\n");
                infoSB.append("Battery Backed Clock: " + (features.BatteryBackedClock ? "Yes":"No"));

                Log.i(TAG, "Features:\n" + infoSB.toString());
//                sendToDisplay(infoSB.toString());
            }
        }
    }

    public String formatStringIfNotEmpty(String format, String data)
    {
        String result = "";

        if (!data.isEmpty())
        {
            result = String.format(format, data);
        }

        return result;
    }

    public String formatStringIfNotValueZero(String format, int data)
    {
        String result = "";

        if (data != 0)
        {
            result = String.format(format, data);
        }

        return result;
    }

    private void setVolume(int volume)
    {
        m_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
    }

    private void saveVolume()
    {
        m_audioVolume = m_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void restoreVolume()
    {
        setVolume(m_audioVolume);
    }

    private void setVolumeToMax()
    {
        saveVolume();

        int volume = m_audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        setVolume(volume);
    }

    public long openDevice()
    {
        Log.i(TAG, "SCRADevice openDevice");

        WritableMap map = Arguments.createMap();

        map.putBoolean("openingDevice", true);

        eventManager.sendEvent(EVENT_DEV, map);

        long result = -1;

        if (m_scra != null)
        {
            m_scra.setConnectionType(m_connectionType);

            m_scra.setConnectionRetry(true);

            if (m_connectionType == MTConnectionType.Audio)
            {
//                if (m_audioConfigType.equalsIgnoreCase("1"))
//                {
//                    // Manual Configuration
//                    Log.i(TAG, "*** Manual Audio Config");
//                    m_scra.setDeviceConfiguration(getManualAudioConfig());
//                }
//                else if (m_audioConfigType.equalsIgnoreCase("2"))
//                {
//                    // Configuration File
//                    Log.i(TAG, "*** Audio Config From File");
//                    startAudioConfigFromFile();
//                    return 0;
//                }
                if (m_audioConfigType.equalsIgnoreCase("3"))
                {
                    if (configSavedToFile) {
                        Log.i(TAG, "*** Audio Config From File");
                        startAudioConfigFromFile();
                    } else {
                        // Configuration From Server
                        Log.i(TAG, "*** Audio Config From Server");
                        startAudioConfigFromServer();
                        return 0;
                    }
                }
            }

            result = 0;
        }

        this.registerReceiver(m_headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        this.registerReceiver(m_noisyAudioStreamReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        return result;
    }

    public long closeDevice()
    {
        Log.i(TAG, "SCRADevice closeDevice");

        long result = -1;

        if (m_scra != null && isDeviceOpened())
        {
            m_scra.closeDevice();

            result = 0;
        }

        return result;
    }

    public boolean isDeviceOpened() {
        Log.i(TAG, "SCRADevice isDeviceOpened");

        return (m_connectionState == MTConnectionState.Connected);
    }

    private void sendEvent(String eventName,
                           @Nullable WritableMap params) {
        Log.i(TAG, "Sending connected event");
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void startAudioConfigFromFile()
    {
        try
        {
            new LoadAudioConfigFromFileTask().execute("");
        }
        catch (Exception ex)
        {
            Log.i(TAG, "*** Audio config from file Exception");
        }
    }

    private void startAudioConfigFromServer()
    {
        try
        {
            new LoadAudioConfigFromServerTask().execute("");
        }
        catch (Exception ex)
        {
            Log.i(TAG, "*** Exception");
        }
    }

    private void onAudioConfigReceived(String xmlConfig)
    {
        try
        {
            if (m_scra !=null)
            {
                String config = "";

                try
                {
                    if ((xmlConfig != null) && !xmlConfig.isEmpty())
                    {
                        String model = android.os.Build.MODEL.toUpperCase();

                        Log.i(TAG, "*** Model=" + model);

                        MTSCRAConfig scraConfig = new MTSCRAConfig(SCRA_CONFIG_VERSION);

                        ProcessMessageResponse configurationResponse =scraConfig.getConfigurationResponse(xmlConfig);

                        Log.i(TAG, "*** ProcessMessageResponse Count =" + configurationResponse.getPropertyCount());

                        config = scraConfig.getConfigurationParams(model, configurationResponse);
                    }

                    Log.i(TAG, "*** Config=" + config);

                    m_scra.setDeviceConfiguration(config);
                }
                catch (Exception ex)
                {
                    Log.i(TAG, "*** Exception " + ex.getMessage());
                }

                if (headSetState == HEADSET_PLUGGED_IN) {
                    Log.i(TAG, "Headset plugged in. Opening device.");
                    m_scra.openDevice();
                } else {
                    Log.i(TAG, "Headset not plugged in. Not opening device");
                }
            }
        }
        catch (Exception ex)
        {
            Log.i(TAG, "*** Exception");
        }
    }

    private class LoadAudioConfigFromFileTask extends AsyncTask<String, Void, String>
    {
        protected String doInBackground(String... params)
        {
            String xmlConfig = "";

            try
            {
                xmlConfig = getAudioConfigFromFile();
            }
            catch (Exception ex)
            {
                Log.i(TAG, "*** Get file audio config Exception");
            }

            Log.i(TAG, "*** XML Config=" + xmlConfig);

            return xmlConfig;
        }

        @Override
        protected void onPostExecute(String result)
        {
            onAudioConfigReceived(result);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private class LoadAudioConfigFromServerTask extends AsyncTask<String, Void, String>
    {
        protected String doInBackground(String... params)
        {
            String xmlConfig = "";

            try
            {
                String model = android.os.Build.MODEL.toUpperCase();

                Log.i(TAG, "*** Model=" + model);

                MTSCRAConfig scraConfig = new MTSCRAConfig(SCRA_CONFIG_VERSION);

                SCRAConfigurationDeviceInfo deviceInfo = new SCRAConfigurationDeviceInfo();
                deviceInfo.setProperty(SCRAConfigurationDeviceInfo.PROP_PLATFORM, "Android");
                deviceInfo.setProperty(SCRAConfigurationDeviceInfo.PROP_MODEL, model);

                xmlConfig = scraConfig.getConfigurationXML(CONFIGWS_USERNAME, CONFIGWS_PASSWORD, CONFIGWS_READERTYPE, deviceInfo, CONFIGWS_URL, CONFIGWS_TIMEOUT);


                saveAudioConfigToFile(xmlConfig);
            }
            catch (Exception ex)
            {
                Log.i(TAG, "*** Exception");
            }

            Log.i(TAG, "*** XML Config=" + xmlConfig);

            return xmlConfig;
        }

        @Override
        protected void onPostExecute(String result)
        {
            onAudioConfigReceived(result);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    public String getAudioConfigFromFile()
    {
        String config = "";

        try
        {
            config = ReadSettings(getApplicationContext(), AUDIO_CONFIG_FILE);

            if (config==null)
            {
                config = "";
            }
        }
        catch (Exception ex)
        {
        }

        return config;
    }

    public void saveAudioConfigToFile(String xmlConfig)
    {
        try
        {
            WriteSettings(reactContext, xmlConfig, AUDIO_CONFIG_FILE);
            configSavedToFile = true;
        }
        catch (Exception ex)
        {
            Log.i(TAG, "*** Failed to write XML Config=" + xmlConfig);
        }
    }

    public static String ReadSettings(Context context, String file) throws IOException
    {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        String data = null;
        fis = context.openFileInput(file);
        isr = new InputStreamReader(fis);
        char[] inputBuffer = new char[fis.available()];
        isr.read(inputBuffer);
        data = new String(inputBuffer);
        isr.close();
        fis.close();
        return data;
    }

    public static void WriteSettings(Context context, String data, String file) throws IOException
    {
        FileOutputStream fos= null;
        OutputStreamWriter osw = null;
        fos= context.openFileOutput(file,Context.MODE_PRIVATE);
        osw = new OutputStreamWriter(fos);
        osw.write(data);
        osw.close();
        fos.close();
    }

    private void setState(MTConnectionState deviceState)
    {
        m_connectionState = deviceState;
    }

    public void startTransaction()
    {
        byte type = (byte) 0x01;

        startTransactionWithOptions(type);
    }

    public void startTransactionWithOptions(byte cardType)
    {
        if (m_scra != null)
        {
//            byte timeLimit = 0x3C;
            byte timeLimit = 0x09;
            //byte cardType = 0x02;  // Chip Only
            //byte cardType = 0x03;  // MSR + Chip
            byte option = (byte) 00;
            byte[] amount = new byte[] {0x00, 0x00, 0x00, 0x00, 0x15, 0x00};
            byte transactionType = 0x00; // Purchase
            byte[] cashBack = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
            byte[] currencyCode = new byte[] { 0x08, 0x40};
            byte reportingOption = 0x02;  // All Status Changes

            int result = m_scra.startTransaction(timeLimit, cardType, option, amount, transactionType, cashBack, currencyCode, reportingOption);

            Log.i(TAG, "*** Starting transaction");
        }
    }

    public class NoisyAudioStreamReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(TAG, "*** NOISY STREAMER" + intent.getAction());
            /* If the device is unplugged, this will immediately detect that action,
             * and close the device.
             */
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                if (m_connectionType == MTConnectionType.Audio)
                {
                    if(m_scra.isDeviceConnected())
                    {
                        closeDevice();
                    }
                }
            }
        }
    }

    public class HeadSetBroadCastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {

            try
            {
                Log.i(TAG, "*** HEADSET BROADCAST");
                String action = intent.getAction();

                if( (action.compareTo(Intent.ACTION_HEADSET_PLUG))  == 0)   //if the action match a headset one
                {
                    headSetState = intent.getIntExtra("state", 0);      //get the headset state property
                    int hasMicrophone = intent.getIntExtra("microphone", 0);//get the headset microphone property

                    if (headSetState == HEADSET_PLUGGED_IN) {
                        Log.i(TAG, "Headset Plugged In");
                        openDevice();
                    } else {
                        Log.i(TAG, "Headset unplugged");
                    }

                    if( (headSetState == 1) && (hasMicrophone == 1))        //headset was unplugged & has no microphone
                    {
                    }
                    else
                    {
                        if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
                        {
                            if (m_connectionType == MTConnectionType.Audio)
                            {
                                if(m_scra.isDeviceConnected())
                                {
                                    closeDevice();
                                }
                            }
                        }
                    }

                }

            }
            catch(Exception ex)
            {

            }
        }
    }
}

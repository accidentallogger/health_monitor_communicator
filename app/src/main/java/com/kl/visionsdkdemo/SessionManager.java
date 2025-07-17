package com.kl.visionsdkdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.mintti.visionsdk.ble.BleManager;
import java.io.File;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_PHONE = "phone";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String phone) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_PHONE, phone);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getLoggedInPhone() {
        return pref.getString(KEY_PHONE, null);
    }

    public void logoutUser(Context context) {
        // 1. Disconnect Bluetooth first
        disconnectBluetooth();

        // 2. Clear all shared preferences
        editor.clear();
        editor.commit();

        // 3. Clear any cached data
        clearAppCache(context);

        // 4. Redirect to login with flags to prevent back navigation
        Intent intent = new Intent(context, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);

        context.startActivity(intent);

        // 5. Add transition and finish if needed
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);

            if (!(activity instanceof LoginActivity)) {
                activity.finish();
            }
        }

        // 6. Broadcast logout event
        sendLogoutBroadcast(context);
    }

    private void disconnectBluetooth() {
        try {
            // Check if Bluetooth is connected via BleManager
            if (BleManager.getInstance() != null) {
                if (BleManager.getInstance().isConnected()) {
                    Log.d("SessionManager", "Disconnecting Bluetooth device");
                    BleManager.getInstance().disconnect();
                }

            }
        } catch (Exception e) {
            Log.e("SessionManager", "Error during Bluetooth disconnection", e);
        }
    }

    private void clearAppCache(Context context) {
        try {
            // Clear WebView cache
            context.deleteDatabase("webview.db");
            context.deleteDatabase("webviewCache.db");

            // Clear app cache
            File cacheDir = context.getCacheDir();
            if (cacheDir != null && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }

            // Clear external cache
            File externalCacheDir = context.getExternalCacheDir();
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                File[] externalFiles = externalCacheDir.listFiles();
                if (externalFiles != null) {
                    for (File file : externalFiles) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SessionManager", "Error clearing cache", e);
        }
    }

    private void sendLogoutBroadcast(Context context) {
        try {
            Intent broadcastIntent = new Intent("com.kl.visionsdkdemo.ACTION_LOGOUT");
            context.sendBroadcast(broadcastIntent);
        } catch (Exception e) {
            Log.e("SessionManager", "Error sending logout broadcast", e);
        }
    }
}
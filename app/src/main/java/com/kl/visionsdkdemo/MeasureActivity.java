package com.kl.visionsdkdemo;

import static com.kongzue.dialogx.interfaces.BaseDialog.getContext;

import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.BaseVBindingActivity;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleConnectionListener;
import com.kl.visionsdkdemo.databinding.ActivityMeasureBinding;
import com.kl.visionsdkdemo.fragment.MeasurementFragment;
import com.kl.visionsdkdemo.fragment.RecordFragment;
import com.kl.visionsdkdemo.fragment.SettingsFragment;

import java.io.File;

public class MeasureActivity extends BaseVBindingActivity<ActivityMeasureBinding> implements IBleConnectionListener {
    private ActionBar actionBar;
    private boolean isUpdate = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.measure);
        }
        initView();
        setupBottomNavigation();

    }

    @Override
    public ActivityMeasureBinding getViewBinding() {
        return ActivityMeasureBinding.inflate(getLayoutInflater());
    }

    private void initView() {
        // Initialize your main content with the measurement fragment by default
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new MeasurementFragment())
                .commit();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = getBinding().bottomNav;
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_measurement) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new MeasurementFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_record) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new RecordFragment())
                        .commit();
                return true;
            } else if (itemId == R.id.nav_settings) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new SettingsFragment())
                        .commit();
                return true;
            }
            return false;
        });
    }

    public void setIsUpdate(boolean update) {
        isUpdate = update;
        Log.e("MeasureActivity", "setIsUpdate: " + update);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BleManager.getInstance().addConnectionListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BleManager.getInstance().removeConnectionListener(this);
    }

    @Override
    public void onBackPressed() {
        if (BleManager.getInstance().isConnected()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hint)
                    .setMessage(R.string.want_disconnect)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        BleManager.getInstance().disconnect();
                        dialog.cancel();
                        finish();
                    })
                    .setNegativeButton(R.string.cancle, (dialog, which) -> dialog.cancel())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnectSuccess(String mac) {
        isUpdate = false;
    }

    @Override
    public void onConnectFailed(String mac) {
        // Handle connection failure
    }

    @Override
    public void onDisconnected(String mac, boolean isActiveDisconnect) {
        Log.e("MeasureActivity", "onDisconnected: " + mac + "  " + isActiveDisconnect);
        if (!isActiveDisconnect && !isUpdate) {
            showToast(getString(R.string.bluetooth_disconnected));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_measure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_item_share) {
            File dataFileDir = getExternalFilesDir("data");
            File[] files = dataFileDir.listFiles();
            showShareFileDialog(files);
            return true;
        } else if (itemId == R.id.menu_item_delete) {
            File dataFileDir = getExternalFilesDir("data");
            File[] files = dataFileDir.listFiles();
            for (File file : files) {
                file.delete();
            }
            showToast(getString(R.string.cleared_successfully));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
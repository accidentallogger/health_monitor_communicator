package com.kl.visionsdkdemo;

import static com.kongzue.dialogx.interfaces.BaseDialog.getContext;

import com.kl.visionsdkdemo.SessionManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.DialogLifecycleCallback;
import com.mintti.visionsdk.ble.BleDevice;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleConnectionListener;
import com.mintti.visionsdk.ble.callback.IBleScanCallback;
import com.kl.visionsdkdemo.adapter.BleDeviceAdapter;
import com.kl.visionsdkdemo.base.BaseVBindingActivity;
import com.kl.visionsdkdemo.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseVBindingActivity<ActivityMainBinding> implements IBleConnectionListener, Handler.Callback {
    private static final int MSG_SCAN_DELAY = 0x01;
    private final List<BleDevice> deviceList = new ArrayList<>();
    private BleDeviceAdapter deviceAdapter;
    private Handler mHandler;
    private boolean isScanning = false;

    public String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
    public List<String> permissionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.scan);
        }
        checkPermission();
        mHandler = new Handler(getMainLooper(), this);
        initView();
    }

    @Override
    public ActivityMainBinding getViewBinding() {
        return ActivityMainBinding.inflate(getLayoutInflater());
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(permission);
                }
            }

            if (!permissionList.isEmpty()) {
                String[] requestPermission = permissionList.toArray(new String[0]);
                ActivityCompat.requestPermissions(MainActivity.this, requestPermission, 1);
            }
        }
    }

    private void initView() {
        getBinding().recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getBinding().recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        deviceAdapter = new BleDeviceAdapter(deviceList);
        getBinding().recyclerView.setAdapter(deviceAdapter);
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            BleManager.getInstance().stopScan();
            getBinding().swipeRefreshLayout.setRefreshing(false);
            if (deviceList.get(position).getName().contains("Mintti-Vision") || deviceList.get(position).getName().contains("HC")) {
                WaitDialog.show(getString(R.string.connecting)).setCancelable(false);
                BleManager.getInstance().connect(deviceList.get(position));

                SharedPreferences sharedPreferences = getContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("savedDeviceName", deviceList.get(position).getName());
                editor.apply();
            } else {
                PopTip.show(getString(R.string.pleace_connect_device));
            }
        });

        updateRecyclerVisibility();

        getBinding().btScan.setOnClickListener(v -> {
            if (!BleManager.getInstance().isBluetoothEnable()) {
                Toast.makeText(this, getString(R.string.please_turn_on_bluetooth), Toast.LENGTH_SHORT).show();
                return;
            }

            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            getBinding().swipeRefreshLayout.setRefreshing(true);
            startScan();
        });

        getBinding().logout.setOnClickListener(v->{
            SessionManager sm = new SessionManager(getContext());
            sm.logoutUser(this);
        });

        getBinding().swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isScanning) {
                startScan();
            }
        });
    }

    private void updateRecyclerVisibility() {
        if (deviceList.isEmpty()) {
            getBinding().emptyView.setVisibility(View.VISIBLE);
            getBinding().recyclerView.setVisibility(View.GONE);
        } else {
            getBinding().emptyView.setVisibility(View.GONE);
            getBinding().recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void startScan() {
        mHandler.sendEmptyMessageDelayed(MSG_SCAN_DELAY, 10 * 1000);
        isScanning = true;
        BleManager.getInstance().startScan(new IBleScanCallback() {
            @Override
            public void onScanResult(BleDevice bleDevice) {
                if (TextUtils.isEmpty(bleDevice.getName()) || TextUtils.isEmpty(bleDevice.getMac())) {
                    return;
                }
                boolean hasDevice = false;
                for (BleDevice device : deviceList) {
                    if (device.getMac().equals(bleDevice.getMac())) {
                        hasDevice = true;
                        device.setRssi(bleDevice.getRssi());
                        break;
                    }
                }
                if (!hasDevice) {
                    deviceList.add(bleDevice);
                }
                deviceAdapter.notifyDataSetChanged();
                updateRecyclerVisibility();
            }

            @Override
            public void onScanFailed(int errorCode) {
                isScanning = false;
                mHandler.removeMessages(MSG_SCAN_DELAY);
            }
        });
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
    public void onConnectSuccess(String mac) {
        TipDialog.show(getString(R.string.connection_succeeded), TipDialog.TYPE.SUCCESS)
                .setDialogLifecycleCallback(new DialogLifecycleCallback<WaitDialog>() {
                    @Override
                    public void onDismiss(WaitDialog dialog) {
                        super.onDismiss(dialog);
                        startActivity(new Intent(MainActivity.this, MeasureActivity.class));
                    }
                });

    }

    @Override
    public void onConnectFailed(String mac) {
        TipDialog.show(getString(R.string.connection_failed), TipDialog.TYPE.ERROR);
    }

    @Override
    public void onDisconnected(String mac, boolean isActiveDisconnect) {
        runOnUiThread(() -> {
            if (isActiveDisconnect) {
                // User-initiated disconnect
                Toast.makeText(this,
                        R.string.device_disconnected,
                        Toast.LENGTH_SHORT).show();

                // Navigate back
                if (!isFinishing()) {
                    onBackPressed();
                }
            } else {
                // Automatic disconnect
                TipDialog.show(getString(R.string.device_disconnected),
                        TipDialog.TYPE.WARNING);
            }
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == MSG_SCAN_DELAY) {
            getBinding().swipeRefreshLayout.setRefreshing(false);
            BleManager.getInstance().stopScan();
            isScanning = false;
            updateRecyclerVisibility();
            return true;
        }
        return false;
    }
}

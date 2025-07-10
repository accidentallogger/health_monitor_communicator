package com.kl.visionsdkdemo;

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

import com.google.android.material.tabs.TabLayout;
import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.BaseVBindingActivity;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleConnectionListener;
import com.kl.visionsdkdemo.adapter.MeasureFragmentAdapter;
import com.kl.visionsdkdemo.databinding.ActivityMeasureBinding;
import com.mintti.visionsdk.ble.callback.IDeviceVersionCallback;


import java.io.File;
import java.util.List;

public class MeasureActivity extends BaseVBindingActivity<ActivityMeasureBinding> implements IBleConnectionListener {
    private MeasureFragmentAdapter fragmentAdapter;
    private ActionBar actionBar;

    private boolean isUpdate = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_measure);
        actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.measure);
        }
        initView();
    }

    @Override
    public ActivityMeasureBinding getViewBinding() {
        return ActivityMeasureBinding.inflate(getLayoutInflater());
    }

    private void initView() {

        fragmentAdapter = new MeasureFragmentAdapter(getSupportFragmentManager());
        getBinding().viewPager.setAdapter(fragmentAdapter);
        getBinding().viewPager.setOffscreenPageLimit(5);
        getBinding().tabLayout.setupWithViewPager(getBinding().viewPager);
        getBinding().tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
//                if (actionBar != null){
//                    actionBar.setTitle(tab.getText());
//                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    public void setIsUpdate(boolean update) {
        isUpdate = update;
        Log.e("MeasureActivity", "setIsUpdate: "+update );
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

            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.hint)
                    .setMessage(R.string.want_disconnect)
                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BleManager.getInstance().disconnect();
                            dialog.cancel();
                            finish();
                        }
                    }).setNegativeButton(R.string.cancle, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
            alertDialog.show();


        }else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConnectSuccess(String mac) {
        isUpdate = false;
    }

    @Override
    public void onConnectFailed(String mac) {

    }

    @Override
    public void onDisconnected(String mac, boolean isActiveDisconnect) {
        Log.e("MeasureActivity", "onDisconnected: "+mac + "  "+isActiveDisconnect);
        if (!isActiveDisconnect && !isUpdate){
            showToast(getString(R.string.bluetooth_disconnected));
            finish();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_measure,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_item_share){
            File dataFileDir = getExternalFilesDir("data");
            File[] files = dataFileDir.listFiles();
            showShareFileDialog(files);
            return true;
        }else if (item.getItemId() == R.id.menu_item_delete){
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

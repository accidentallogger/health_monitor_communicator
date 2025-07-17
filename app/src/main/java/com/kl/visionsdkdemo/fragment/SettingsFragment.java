package com.kl.visionsdkdemo.fragment;

import static android.app.Activity.RESULT_OK;

import static com.kongzue.dialogx.interfaces.BaseDialog.getContext;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;


import com.kl.visionsdkdemo.CodeScanActivity;
import com.kl.visionsdkdemo.MeasureActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.kl.visionsdkdemo.base.utils.PackageUtil;
import com.kl.visionsdkdemo.base.utils.SizeUtils;
import com.kl.visionsdkdemo.view.CommonSelectDialog;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.callback.IDeviceBatteryCallback;
import com.mintti.visionsdk.ble.callback.IDeviceVersionCallback;
import com.mintti.visionsdk.ble.callback.IManufacturerInfoCallback;
import com.kl.visionsdkdemo.databinding.FragmentDeviceInfoBinding;
import com.kl.visionsdkdemo.dfu.DfuService;
import com.mintti.visionsdk.ble.callback.ISerialNumberCallback;
import com.mintti.visionsdk.common.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * Created by leopold on 2021/4/13
 * Description:
 */
public class SettingsFragment extends BaseVBindingFragment<FragmentDeviceInfoBinding> implements DfuProgressListener {

    private File zipDir;
    private ProgressDialog progressDialog;
    public static final int REQUEST_CODE_128 = 128;
    public static final String KEY_CODE_128 = "code_128";
    private boolean isDfu = false;

    public static DeviceInfoFragment newInstance() {
        Bundle args = new Bundle();
        DeviceInfoFragment fragment = new DeviceInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentDeviceInfoBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentDeviceInfoBinding.inflate(inflater,container,false);
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        getBinding().btUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startUpdate();
                copyZip();
                File[] files = zipDir.listFiles();
                if ( files != null && files.length > 0){
                    showFirmwareListDialog(files);
                }else {
                    Toast.makeText(requireContext(), R.string.no_firmware_package, Toast.LENGTH_LONG).show();
                }
            }
        });

        getBinding().logout.setOnClickListener(v->{

            SessionManager sm = new SessionManager(getContext());
            sm.logoutUser(getContext());
        });
// Add this in your initView() method of SettingsFragment
        getBinding().btDisconnect.setOnClickListener(v -> {
            if (BleManager.getInstance().isConnected()) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.confirm_disconnect)
                        .setMessage(R.string.confirm_disconnect_message)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            // Disconnect Bluetooth
                            BleManager.getInstance().disconnect();

                            // Navigate back to previous screen
                            if (isAdded() && !requireActivity().isFinishing()) {
                                requireActivity().onBackPressed();
                            }

                            Toast.makeText(requireContext(),
                                    R.string.device_disconnected,
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            } else {
                Toast.makeText(requireContext(),
                        R.string.no_device_connected,
                        Toast.LENGTH_SHORT).show();
            }
        });

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle(getString(R.string.firmware_upgrade));
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setMax(100);
        getBinding().btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btScan.getText().equals(getString(R.string.scan))){
                    startActivityForResult(new Intent(requireActivity(), CodeScanActivity.class), REQUEST_CODE_128);
                }else {
                    writeSerialNumber(getBinding().etSerialNumber.getText().toString().trim());
                }

            }
        });
        getBinding().etSerialNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0){
                    getBinding().btScan.setText(getString(R.string.confirm));
                }else {
                    getBinding().btScan.setText(getString(R.string.scan));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        getBinding().tvAppVision.setText("App number："+PackageUtil.getVersionName(requireContext()));


    }

    @Override
    public void onResume() {
        super.onResume();
        if (BleManager.getInstance().isHaveSerialNumber()){
            BleManager.getInstance().getSerialNumber(new ISerialNumberCallback() {
                @Override
                public void onSerialNumber(String number) {
                    requireActivity().runOnUiThread(()->{
                        getBinding().tvSerialNumber.setText(number);
                        isHaveSerialNumber(true);
                    });
                }
            });
        }else {
            isHaveSerialNumber(false);
        }
        BleManager.getInstance().getDeviceManufacturer(new IManufacturerInfoCallback() {
            @Override
            public void onManufacturerInfo(String manufacturer) {
                requireActivity().runOnUiThread(()->{
                    getBinding().tvManufacture.setText(manufacturer);
                });
            }
        });

        BleManager.getInstance().getDeviceVersionInfo(new IDeviceVersionCallback() {
            @Override
            public void onDeviceVersionInfo(String versionInfo) {
                requireActivity().runOnUiThread(()->{
                    getBinding().tvVersion.setText(versionInfo);
                });
            }
        });
        BleManager.getInstance().setBatteryListener(new IDeviceBatteryCallback() {
            @Override
            public void onDeviceBattery(int battery) {
                requireActivity().runOnUiThread(()->{
                    if (getBinding().tvBattery != null) {
                        getBinding().tvBattery.setText(battery+" %");
                    }
                });
            }
        });
        BleManager.getInstance().getDeviceBattery(new IDeviceBatteryCallback() {
            @Override
            public void onDeviceBattery(int battery) {
                requireActivity().runOnUiThread(()->{
                    getBinding().tvBattery.setText(battery+" %");
                });
            }
        });
    }

    private void isHaveSerialNumber(boolean isHave) {
        if (isHave) {
            getBinding().btScan.setVisibility(View.INVISIBLE);
            getBinding().etSerialNumber.setVisibility(View.INVISIBLE);
            getBinding().tvSerialNumber.setVisibility(View.VISIBLE);
        }else {
            getBinding().btScan.setVisibility(View.VISIBLE);
            getBinding().etSerialNumber.setVisibility(View.VISIBLE);
            getBinding().tvSerialNumber.setVisibility(View.INVISIBLE);
        }
    }
    private void writeSerialNumber(String serialNumber){
        if (serialNumber.length() != 12){
            Toast.makeText(requireContext(), R.string.serial_number_must_be_12_digits, Toast.LENGTH_SHORT).show();
            return;
        }
        BleManager.getInstance().writeSerialNumber(serialNumber, new IBleWriteResponse() {
            @Override
            public void onWriteSuccess() {
                requireActivity().runOnUiThread(()->{
                    Toast.makeText(requireContext(), R.string.serial_number_written_successfully, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onWriteFailed() {
                requireActivity().runOnUiThread(()->{
                    Toast.makeText(requireContext(), R.string.failed_to_write_serial_number, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

//    private void startUpdate(){
//        MeasureActivity measureActivity = (MeasureActivity) requireActivity();
//        measureActivity.setIsUpdate(true);
//        Log.e("DeviceInfoFragment", "startUpdate: " );
//        String address = BleManager.getInstance().getBluetoothDevice().getAddress();
//        String bleName = BleManager.getInstance().getBluetoothDevice().getName();
//        final DfuServiceInitiator starter = new DfuServiceInitiator(address);
//        starter.setDeviceName(bleName)
//                .setDisableNotification(true)
//                .setKeepBond(true)
//                .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
//        starter.setForeground(true);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            DfuServiceInitiator.createDfuNotificationChannel(requireContext());
//        }
    ////        starter.setZip(path);
//        starter.setZip(R.raw.vision02);
//        DfuServiceController dfuServiceController = starter.start(requireContext(), DfuService.class);
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        DfuServiceListenerHelper.registerProgressListener(requireContext(),this);


    }

    @Override
    public void onPause() {
        super.onPause();
        BleManager.getInstance().setBatteryListener(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DfuServiceListenerHelper.unregisterProgressListener(requireContext(),this);
        if (progressDialog != null) {
            progressDialog.cancel();
        }
    }

    @Override
    public void onDeviceConnecting(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDeviceConnecting: "+deviceAddress );
//        requireActivity().runOnUiThread(()->{
//            progressDialog = new ProgressDialog(requireContext());
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            progressDialog.setMax(100);
//            progressDialog.setTitle("固件更新中 请稍后");
//            progressDialog.setCanceledOnTouchOutside(false);
//            progressDialog.setCancelable(false);
//            progressDialog.show();
//        });
    }

    @Override
    public void onDeviceConnected(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDeviceConnected: "+deviceAddress );
        if (isAdded()){
            requireActivity().runOnUiThread(()->{
                if (progressDialog == null){
                    progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMax(100);
                    progressDialog.setTitle(getString(R.string.firmware_updated));
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }

            });
        }

    }

    @Override
    public void onDfuProcessStarting(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDfuProcessStarting: "+deviceAddress );
//        requireActivity().runOnUiThread(() -> {
//            if (progressDialog != null) {
//                progressDialog.setTitle("DfuProcessStarting");
//            }
//
//        });
    }

    @Override
    public void onDfuProcessStarted(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDfuProcessStarted: "+deviceAddress );
//        requireActivity().runOnUiThread(() -> {
//            if (progressDialog != null) {
//                progressDialog.setTitle("DfuProcessStarted");
//            }
//
//        });
    }

    @Override
    public void onEnablingDfuMode(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onEnablingDfuMode: "+deviceAddress);
    }

    @Override
    public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
        Log.e("DeviceInfoFragment", "onProgressChanged:" +
                " "+" percent " +percent +" speed "+speed +" avgSpeed "+avgSpeed +" currentPart "+currentPart +" partsTotal "+partsTotal);
        if (progressDialog != null) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setProgress(percent);
                }
            });
        }

    }

    @Override
    public void onFirmwareValidating(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onFirmwareValidating: "+deviceAddress );
    }

    @Override
    public void onDeviceDisconnecting(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDeviceDisconnecting: "+deviceAddress );
    }

    @Override
    public void onDeviceDisconnected(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDeviceDisconnected: "+deviceAddress );
    }

    @Override
    public void onDfuCompleted(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDfuCompleted: "+deviceAddress );
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setTitle(getString(R.string.firmware_upgrade_complete));
                showToast(getString(R.string.firmware_upgrade_complete_restart));
                progressDialog.cancel();
                requireActivity().finish();
            }
        });
    }

    @Override
    public void onDfuAborted(String deviceAddress) {
        Log.e("DeviceInfoFragment", "onDfuAborted: "+deviceAddress );
        if (progressDialog != null) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                }
            });

        }
    }

    @Override
    public void onError(String deviceAddress, int error, int errorType, String message) {
        Log.e("DeviceInfoFragment", "onError: "+error +" errorType " +errorType+"  message "+message);
        if (progressDialog != null) {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.cancel();
                    showToast(getString(R.string.update_fail));
                }
            });

        }
    }


    /**
     * 将固件包复制到zip文件目录下
     */
    private void copyZip(){
        zipDir = new File(requireContext().getExternalFilesDir(""),"zip");
        if ( !zipDir.exists() ){
            zipDir.mkdirs();
        }
        try {
            AssetManager assetManager = requireContext().getAssets();
            String[] firmwareFiles = assetManager.list("zip");
            for (String firmwareFile : firmwareFiles) {
                File file = new File(zipDir.getAbsolutePath()+"/"+firmwareFile);
                if (file.exists()){
                    continue;
                }
                InputStream is = assetManager.open("zip/"+firmwareFile);
                FileOutputStream fos = new FileOutputStream(file);
                int len = -1;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void showFirmwareListDialog(File[] files){
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_firmware_upgrade_package);
        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = files[which];
                startUpdateServer(file.getAbsolutePath());
            }
        }).create().show();


    }

    /**
     * 开启固件升级服务
     */
    private void startUpdateServer(String path) {
        MeasureActivity measureActivity = (MeasureActivity) requireActivity();
        measureActivity.setIsUpdate(true);
        final DfuServiceInitiator starter = new DfuServiceInitiator(BleManager.getInstance().getBluetoothDevice().getAddress());
//        starter.setDeviceName(BleManager.getInstance().getBluetoothDevice().getName())
        starter.setDeviceName("DfuTarg")
                .setDisableNotification(true)
                .setKeepBond(true)
                .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        starter.setForeground(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            starter.createDfuNotificationChannel(requireContext());
        }
        starter.setZip(path);
        starter.start(requireContext(), DfuService.class);
        LogUtils.d("MeasureActivity", "startUpdateServer: ");
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setMessage("Start update progress");
        progressDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_128 && resultCode == RESULT_OK) {
            String code = data.getStringExtra(KEY_CODE_128);
//            tvCode128.setText(code);
            Log.d("DeviceInfoFragment", "onActivityResult: "+code);
            CommonSelectDialog mConfirmDialog = new CommonSelectDialog.Builder(getActivity())
                    .setWidth(SizeUtils.dp2px(300))
                    .setHeight(SizeUtils.dp2px(121))
                    .setTitle(code)
                    .setOnClickListener(new CommonSelectDialog.OnClickListener() {
                        @Override
                        public void onClick(CommonSelectDialog dialog, boolean isOk) {
                            if (isOk) {
                                writeSerialNumber(code);
                            }
                            dialog.dismiss();
                        }
                    })
                    .build();


            mConfirmDialog.show();

        }
    }
}

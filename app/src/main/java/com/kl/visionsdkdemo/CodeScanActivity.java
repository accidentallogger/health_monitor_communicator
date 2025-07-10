package com.kl.visionsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.kl.visionsdkdemo.fragment.DeviceInfoFragment;

//import cn.bingoogolapple.qrcode.core.QRCodeView;
//import cn.bingoogolapple.qrcode.zbar.ZBarView;

/**
 * Created by gaoyingjie on 2020/6/10
 * Description:
 */
public class CodeScanActivity extends AppCompatActivity { //implements QRCodeView.Delegate

   /* private static final int CAMERA_REQUEST_CODE = 0;
    private ZBarView zBarView;*/

    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_scan);
//        initPermission();
//        zBarView = findViewById(R.id.zbarview);
//        zBarView.setDelegate(this);
//        zBarView.changeToScanBarcodeStyle(); // 切换成扫描条码样式


        Button scanButton = findViewById(R.id.btn_scan);
        scanButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            }
        });
    }

    private void startScanning() {
        new IntentIntegrator(this).initiateScan(); // Starts the scanner
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                // Handle the scanned result
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan", Toast.LENGTH_LONG).show();
            }
        }
    }
}


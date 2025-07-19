package com.kl.visionsdkdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kl.visionsdkdemo.databinding.ActivityEcgDetailBinding;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.EcgRecord;
import com.kl.visionsdkdemo.view.ecg.ChartView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class EcgDetailActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private int userId;
    private int recordId;
    private ChartView ecgView;
    private ActivityEcgDetailBinding binding;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEcgDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        saveEcgGraphToGallery();
                    } else {
                        Toast.makeText(this, "Permission required to save images", Toast.LENGTH_SHORT).show();
                    }
                });

        dbHelper = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        String phone = session.getLoggedInPhone();
        userId = dbHelper.getUserId(phone);
        recordId = getIntent().getIntExtra("record_id", -1);

        binding.btnPrintGraph.setOnClickListener(v -> saveEcgGraphToGallery());

        if (userId == -1 || recordId == -1) {
            Toast.makeText(this, "Invalid record", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.ecgViewDetail.setSampleRate(512);
        binding.ecgViewDetail.setGain(1.0f);
        binding.ecgViewDetail.setShowGrid(true);
        loadEcgRecord();

        binding.btnEditNotesEcg.setOnClickListener(v -> showEditNotesDialog());
    }

    private void loadEcgRecord() {
        EcgRecord record = dbHelper.getEcgRecordById(userId, recordId);

        if (record != null) {
            binding.tvAvgHrValue.setText(record.getAvgHr() + " bpm");
           // binding.tvRespValue.setText(record.getRespRate() != null ? record.getRespRate() + " bpm" : "--");
            binding.tvRrMaxValue.setText(record.getRrMax() + " ms");
            binding.tvRrMinValue.setText(record.getRrMin() + " ms");
           // binding.tvHrvValue.setText(record.getHrv() + " ms");
            binding.tvEcgDuration.setText(record.getDuration());
            binding.tvTime.setText(record.getTime());
            binding.tvDate.setText(record.getDate());

            try {
                List<Integer> ecgData = new Gson().fromJson(
                        record.getEcgData(),
                        new TypeToken<List<Integer>>(){}.getType()
                );
                if (ecgData != null && !ecgData.isEmpty()) {
                    binding.ecgViewDetail.post(() -> {
                        binding.ecgViewDetail.clearDatas();
                        binding.ecgViewDetail.setDataPoints(ecgData);
                        binding.ecgViewDetail.fitGraphToView();
                        binding.ecgViewDetail.drawView();
                    });
                }
            } catch (Exception e) {
                Log.e("EcgDetailActivity", "Error loading ECG data", e);
            }
        }
    }

    private void saveEcgGraphToGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                saveGraphToMediaStore();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            saveGraphToMediaStore();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                saveGraphToMediaStore();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void saveGraphToMediaStore() {
        try {
            Bitmap graphBitmap = binding.ecgViewDetail.createFullGraphBitmap();
            saveBitmapToGallery(graphBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Error saving ECG: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            Log.e("EcgDetailActivity", "Save error", e);
        }
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        String fileName = "ECG_Record_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ECG_Records");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri uri = null;

        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Failed to create new MediaStore record");
            }

            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null) {
                    throw new IOException("Failed to open output stream");
                }
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw new IOException("Failed to compress bitmap");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }

            // Notify gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(uri);
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "ECG graph saved to Gallery/ECG_Records", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            Toast.makeText(this, "Failed to save ECG graph: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            Log.e("EcgDetailActivity", "Save error", e);
        }
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }


    private void showEditNotesDialog() {
        EcgRecord record = dbHelper.getEcgRecordById(userId, recordId);
        if (record == null) {
            Toast.makeText(this, "Record not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_notes);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            layoutParams.width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        EditText etNotes = dialog.findViewById(R.id.et_notes);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnOk = dialog.findViewById(R.id.btn_ok);
        if (!TextUtils.isEmpty(record.getNotes())) {
            etNotes.setText(record.getNotes());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String newNotes = etNotes.getText().toString().trim();
            if (dbHelper.updateEcgRecordNotes(recordId, newNotes)) {
                record.setNotes(newNotes);
                Toast.makeText(this, "Notes updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update notes", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

}
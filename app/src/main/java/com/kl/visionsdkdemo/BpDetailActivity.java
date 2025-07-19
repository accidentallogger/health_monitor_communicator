package com.kl.visionsdkdemo;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.model.BpRecord;

public class BpDetailActivity extends AppCompatActivity {
    private BpRecord record;
    private DatabaseHelper dbHelper;
    private int userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bp_detail);

        dbHelper = new DatabaseHelper(this);

        SessionManager session = new SessionManager(this);
        String phone = session.getLoggedInPhone();
        userId = dbHelper.getUserId(phone);

        record = (BpRecord) getIntent().getSerializableExtra("record");

        if (record == null) {
            Toast.makeText(this, "No BP record data available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvSystolic = findViewById(R.id.tv_systolic_value);
        TextView tvDiastolic = findViewById(R.id.tv_diastolic_value);
        TextView tvHr = findViewById(R.id.tv_hr_value);
        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTime = findViewById(R.id.tvTime);

        ImageView indicatorSys = findViewById(R.id.indicatorPointerSys);
        ImageView indicatorDias = findViewById(R.id.indicatorPointerDias);
        ImageView indicatorHr = findViewById(R.id.indicatorPointerHr);

        tvSystolic.setText(record.getSystolic() + " / mmHg");
        tvDiastolic.setText(record.getDiastolic() + " / mmHg");
        tvHr.setText(record.getHeartRate() + " / BPM");
        tvDate.setText(record.getDate());
        tvTime.setText(record.getTime());

        updatePointerPosition(indicatorSys, record.getSystolic(), 0, 300);
        updatePointerPosition(indicatorDias, record.getDiastolic(), 0, 300);
        updatePointerPosition(indicatorHr, record.getHeartRate(), 0, 300);

        findViewById(R.id.btn_edit_notes).setOnClickListener(v -> {
            if (record != null) {
                showEditNotesDialog();
            } else {
                Toast.makeText(this, "No record data available", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updatePointerPosition(View pointer, int value, int min, int max) {
        if (pointer == null) return;

        View parent = (View) pointer.getParent();
        parent.post(() -> {
            int parentWidth = parent.getWidth();
            int pointerWidth = pointer.getWidth();
            Log.d("IndicatorDebug", "Actual width: " + parentWidth + "px");

            float percentage = ((float)(value - min) / (float)(max - min));
            percentage = Math.max(0, Math.min(1, percentage));

            if (pointer.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) pointer.getLayoutParams();
                params.horizontalBias = percentage;
                pointer.setLayoutParams(params);
            } else if (pointer.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pointer.getLayoutParams();
                params.leftMargin = (int) (percentage * (parentWidth - pointerWidth));
                pointer.setLayoutParams(params);
            }
        });
    }

    private void showEditNotesDialog() {
        if (record == null) {
            Toast.makeText(this, "No record available", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_notes);

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


        String currentNotes = record.getNotes() != null ? record.getNotes() : "";
        etNotes.setText(currentNotes);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String newNotes = etNotes.getText().toString().trim();
            if (dbHelper.updateBpRecordNotes(record.getRecordId(), newNotes)) {
                record.setNotes(newNotes);
                Toast.makeText(this, "Notes updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update notes", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }

}
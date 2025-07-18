package com.kl.visionsdkdemo.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IRawBpDataCallback;
import com.mintti.visionsdk.ble.callback.IBpResultListener;
import com.kl.visionsdkdemo.databinding.FragmentBpBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BpFragment extends BaseMeasureFragment<FragmentBpBinding> implements IBleWriteResponse,
        IBpResultListener, Handler.Callback, IRawBpDataCallback {

    private SessionManager sessionManager;
    private static final int MSG_BP_RESULT = 1;
    private static final int MSG_BP_LEAK = 2;
    private static final int MSG_BP_ERROR = 3;
    private static final int MSG_BP_ADD_DATA = 4;
    private static final int MSG_BP_DE_DATA = 5;
    private static final int MSG_DURATION = 6;
    private static final String SYSTOLIC = "systolic";
    private static final String DIASTOLIC = "diastolic";
    private static final String HR = "hr";

    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    private File bpDeTxtFile;
    private BufferedWriter deFileWriter;
    private File bpAddTxtFile;
    private BufferedWriter addFileWriter;
    private int measureDuration = 0;
    private boolean isTest = false;
    private MessageDialog messageDialog;

    // Variables to store last measurement
    private int lastSystolic = 0;
    private int lastDiastolic = 0;
    private int lastHeartRate = 0;
    private final ArrayList<Short> bpDeDataList = new ArrayList<>();
    private final ArrayList<Short> bpAddDataList = new ArrayList<>();

    public static BpFragment newInstance() {
        Bundle args = new Bundle();
        BpFragment fragment = new BpFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentBpBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBpBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView(View rootView) {
        BleManager.getInstance().setBpResultListener(this);
        BleManager.getInstance().setRawBpDataCallback(this);
        sessionManager = new SessionManager(requireContext());

        BleManager.getInstance().setBpResultListener(this);
        BleManager.getInstance().setRawBpDataCallback(this);


        getBinding().btMeasureBp.setOnClickListener(v -> {
            getBinding().btMeasureBp.setImageResource(!isTest ? R.drawable.ic_stop : R.drawable.ic_play);

            if (!isTest) {
                // Start measurement
                isTest = true;
                resetValue();
                bpDeTxtFile = createFile("bpDeTxt.txt");
                bpAddTxtFile = createFile("bpAddTxt.txt");
                deFileWriter = createBufferWriter(bpDeTxtFile);
                addFileWriter = createBufferWriter(bpAddTxtFile);
                BleManager.getInstance().startMeasure(MeasureType.TYPE_BP, BpFragment.this);
                handler.sendEmptyMessageDelayed(MSG_DURATION, 1000);
            } else {
                // Stop measurement
                isTest = false;
                closeFile();
                handler.removeMessages(MSG_DURATION);
                BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, BpFragment.this);
            }
        });

        // Save record button click listener
        getBinding().btnSaveRecord.setOnClickListener(v -> {
            if (lastSystolic > 0 && lastDiastolic > 0 && lastHeartRate > 0) {
                saveBpRecord(lastSystolic, lastDiastolic, lastHeartRate);
            } else {
                Toast.makeText(getContext(), "No measurement to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (getBinding() == null) return false;

        switch (msg.what) {
            case MSG_BP_RESULT:
                Bundle bundle = msg.getData();
                int hr = bundle.getInt(HR);
                int systolic = bundle.getInt(SYSTOLIC);
                int diastolic = bundle.getInt(DIASTOLIC);

                // Store last values
                lastSystolic = systolic;
                lastDiastolic = diastolic;
                lastHeartRate = hr;

                // Update UI
                getBinding().tvSystolicValue.setText(systolic + " / mmHg");
                getBinding().tvDiastolicValue.setText(diastolic + " / mmHg");
                getBinding().tvHrValue.setText(hr + " / BPM");

                // Post to server
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
                String savedDeviceName = sharedPreferences.getString("savedDeviceName", null);
                postBpData(systolic, diastolic, hr, savedDeviceName, "Blood Pressure");
                getBinding().btMeasureBp.setImageResource(R.drawable.ic_play);
                bpDeDataList.clear();
                isTest = false;
                return true;

            case MSG_BP_LEAK:
                showErrorDialog(getString(R.string.leak));
                getBinding().btMeasureBp.setImageResource(R.drawable.ic_play);
                isTest = false;
                return true;

            case MSG_BP_ERROR:
                showErrorDialog(getString(R.string.measure_err));
                getBinding().btMeasureBp.setImageResource(R.drawable.ic_play);
                isTest = false;
                return true;

            case MSG_BP_ADD_DATA:
                int addPressure = msg.arg1;
                getBinding().btMeasureBp.setImageResource(R.drawable.ic_play);
                getBinding().tvSystolicValue.setText(addPressure + " / mmHg");
                return true;

            case MSG_BP_DE_DATA:
                int dePressure = msg.arg1;
                getBinding().btMeasureBp.setImageResource(R.drawable.ic_play);
                getBinding().tvDiastolicValue.setText(dePressure + " / mmHg");
                return true;

            case MSG_DURATION:
                measureDuration++;
                getBinding().tvDuration.setText(measureDuration + " s");
                handler.sendEmptyMessageDelayed(MSG_DURATION, 1000);
                return true;
        }
        return false;
    }

    private void saveBpRecord(int systolic, int diastolic, int heartRate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());

        if (sessionManager.isLoggedIn()) {
            String phone = sessionManager.getLoggedInPhone();
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            int userId = dbHelper.getUserId(phone); // Make sure this method exists in DatabaseHelper

            if (userId != -1) {
                long recordId = dbHelper.addBpRecord(userId, systolic, diastolic, heartRate, date, time, "");

                if (recordId != -1) {
                    Toast.makeText(getContext(), "BP record saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Failed to save BP record", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "User not found in database", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Please login to save records", Toast.LENGTH_SHORT).show();
            // Optionally redirect to login
            // startActivity(new Intent(getContext(), LoginActivity.class));
        }
    }

    private void postBpData(int systolic, int diastolic, int hr, String device_name, String module_name) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("systolic", systolic);
            jsonObject.put("diastolic", diastolic);
            jsonObject.put("hr", hr);
            jsonObject.put("device_name", device_name);
            jsonObject.put("module_name", module_name);

            String url = "http://156.67.105.81:1880/TW/RHEMOS";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST, url, jsonObject,
                    response -> Log.d("API BP_Response", response.toString()),
                    error -> Log.e("API BP_Error", error.toString())
            );
            Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void resetValue() {
        getBinding().tvSystolicValue.setText("-- / mmHg");
        getBinding().tvDiastolicValue.setText("-- / mmHg");
        getBinding().tvHrValue.setText("-- / BPM");
        getBinding().tvDuration.setText("0 s");
        bpDeDataList.clear();
        bpAddDataList.clear();
        measureDuration = 0;
    }

    private void showErrorDialog(String msg) {
        if (messageDialog == null) {
            messageDialog = MessageDialog.build();
        }
        messageDialog.setTitle(getString(R.string.hint))
                .setMessage(msg)
                .setOkButton(R.string.ok)
                .show();
    }

    @Override
    public void onBpResult(int systolic, int diastolic, int heartRate) {
        closeFile();
        handler.removeMessages(MSG_DURATION);
        Message msg = handler.obtainMessage(MSG_BP_RESULT);
        Bundle bundle = new Bundle();
        bundle.putInt(SYSTOLIC, systolic);
        bundle.putInt(DIASTOLIC, diastolic);
        bundle.putInt(HR, heartRate);
        msg.setData(bundle);
        handler.sendMessage(msg);
        updateIndicators(systolic, diastolic, heartRate);
    }

    @Override
    public void onLeadError() {
        closeFile();
        handler.removeMessages(MSG_DURATION);
        handler.sendEmptyMessage(MSG_BP_LEAK);
    }

    @Override
    public void onBpError() {
        closeFile();
        handler.removeMessages(MSG_DURATION);
        handler.sendEmptyMessage(MSG_BP_ERROR);
    }

    @Override
    public void onPressurizationData(short pressurizationData) {
        bpAddDataList.add(pressurizationData);
        Message message = handler.obtainMessage(MSG_BP_ADD_DATA);
        message.arg1 = pressurizationData;
        handler.sendMessage(message);
        try {
            if (addFileWriter != null) {
                addFileWriter.write(String.valueOf(pressurizationData));
                addFileWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDecompressionData(short decompressionData) {
        bpDeDataList.add(decompressionData);
        Message message = handler.obtainMessage(MSG_BP_DE_DATA);
        message.arg1 = decompressionData;
        handler.sendMessage(message);
        try {
            if (deFileWriter != null) {
                deFileWriter.write(String.valueOf(decompressionData));
                deFileWriter.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPressure(short pressure) {
        Log.d("BP", "onPressure: " + pressure);
    }

    @Override
    protected void closeFile() {
        super.closeFile();
        try {
            if (deFileWriter != null) {
                deFileWriter.close();
                deFileWriter = null;
            }
            if (addFileWriter != null) {
                addFileWriter.close();
                addFileWriter = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isTest) {
            BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, this);
            isTest = false;
            handler.removeMessages(MSG_DURATION);
            closeFile();
            resetValue();
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setBpResultListener(null);
        BleManager.getInstance().setRawBpDataCallback(null);
    }

    private void updateIndicators(int systolic, int diastolic, int heartRate) {
        updatePointerPosition(getBinding().indicatorPointerSys, systolic, 0, 300);
        updatePointerPosition(getBinding().indicatorPointerDias, diastolic, 0, 300);
        updatePointerPosition(getBinding().indicatorPointerHr, heartRate, 0, 300);
    }

    private void updatePointerPosition(View pointer, int value, int min, int max) {
        if (getBinding() == null || pointer == null) return;

        View parent = (View) pointer.getParent();
        parent.post(() -> {
            int parentWidth = parent.getWidth();
            int pointerWidth = pointer.getWidth();

            float percentage = ((float)(value - min) / (float)(max - min));
            percentage = Math.max(0, Math.min(1, percentage));

            float position = percentage * (parentWidth - pointerWidth);

            if (pointer.getLayoutParams() instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) pointer.getLayoutParams();
                params.horizontalBias = percentage;
                pointer.setLayoutParams(params);
            } else if (pointer.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) pointer.getLayoutParams();
                params.leftMargin = (int) position;
                params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
                pointer.setLayoutParams(params);
            }
        });
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BpFragment", "onWriteSuccess: BP");
    }

    @Override
    public void onWriteFailed() {
        Log.e("BpFragment", "onWriteFailed: BP");
        handler.post(() -> {
            resetValue();
            isTest = false;
            handler.removeMessages(MSG_DURATION);
            closeFile();
        });
    }
}
package com.kl.visionsdkdemo.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kl.vision_ecg.ISmctAlgoCallback;
import com.kl.vision_ecg.SmctConstant;
import com.kl.visionsdkdemo.R;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IEcgResultListener;
import com.kl.visionsdkdemo.base.utils.TimeUtils;
import com.kl.visionsdkdemo.databinding.FragmentEcgBinding;
import com.mintti.visionsdk.common.ArrayUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class ECGFragment extends BaseMeasureFragment<FragmentEcgBinding>
        implements IBleWriteResponse, IEcgResultListener, ISmctAlgoCallback {

    private File ecgFile;
    private FileOutputStream fosEcg = null;
    private BufferedOutputStream dataOutputStream = null;
    private int heart_rate;
    private int respiratory_Rate_val;
    private String ECG_Duration;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isMeasuring = false;

    public static ECGFragment newInstance() {
        Bundle args = new Bundle();
        ECGFragment fragment = new ECGFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentEcgBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentEcgBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView(View rootView) {
        // Create the spinner adapter with custom styling
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.ecg_gain)
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.BLACK);  // Selected item text color
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.BLACK);  // Dropdown items text color
                textView.setBackgroundColor(Color.WHITE);  // Dropdown items background
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getBinding().gainSpinner.setAdapter(adapter);

        // Spinner selection listener
        getBinding().gainSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    ((TextView) view).setTextColor(Color.BLACK);
                }
                getBinding().ecgView.setGain(position == 0 ? 1 : (position == 1 ? 2 : 5));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set initial spinner selection
        float gain = getBinding().ecgView.getGain();
        getBinding().gainSpinner.setSelection(gain == 1f ? 0 : (gain == 2f ? 1 : 2));

        if (BleManager.getInstance().isConnected()) {
            getBinding().ecgView.setSampleRate(BleManager.getInstance().getSampleRate());
        }

        // Measure button click listener
        getBinding().btMeasureEcg.setOnClickListener(v -> {
            if (!isMeasuring) {
                startMeasure();
            } else {
                stopMeasure();
            }
        });
    }

    private void startMeasure() {
        isMeasuring = true;
        BleManager.getInstance().setEcgResultListener(null);
        BleManager.getInstance().setEcgResultListener(this);

        ecgFile = createFile("ecg_handle.ecg");
        fosEcg = createFos(ecgFile);
        dataOutputStream = new BufferedOutputStream(fosEcg);

        resetResult();
        BleManager.getInstance().startMeasure(MeasureType.TYPE_ECG, this);

        Log.d("ECGFragment", "ECG measurement started");
    }

    private void stopMeasure() {
        isMeasuring = false;
        BleManager.getInstance().stopMeasure(MeasureType.TYPE_ECG, this);
        getBinding().tvEcgDuration.setText("00:00");
        handler.postDelayed(() -> getBinding().ecgView.clearDatas(), 500);

        Log.d("ECGFragment", "ECG measurement stopped");
    }

    @Override
    public void onWriteSuccess() {
        Log.d("ECGFragment", "Write success");
    }

    @Override
    public void onWriteFailed() {
        Log.e("ECGFragment", "Write failed");
    }

    @Override
    public void algoData(int key, int value) {
        handler.post(() -> {
            switch (key) {
                case SmctConstant.KEY_ALGO_HEART_RATE:
                    getBinding().tvAvgHrValue.setText(value + " bpm");
                    break;
                case SmctConstant.KEY_ALGO_RESP_RATE:
                    getBinding().tvRespValue.setText(value + " bpm");
                    break;
                case SmctConstant.KEY_ALGO_ARRHYTHMIA:
                    Log.d("ECGFragment", "Arrhythmia detected: " + value);
                    break;
            }
        });
    }

    @Override
    public void algoData(int i, int i1, int i2) {
        // Unused
    }

    private void resetResult() {
        getBinding().tvRrMaxValue.setText("-- ms");
        getBinding().tvRrMinValue.setText("-- ms");
        getBinding().tvHrvValue.setText("-- ms");
        getBinding().tvAvgHrValue.setText("-- bpm");
        getBinding().tvRespValue.setText("-- bpm");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setEcgResultListener(null);
    }

    @Override
    public void onDrawWave(int wave) {
        if (getBinding() != null) {
            getBinding().ecgView.addPoint(wave);
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.write(ArrayUtils.int2ByteArray(wave));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onHeartRate(int heartRate) {
        handler.post(() -> {
            if (getBinding() != null) {
                getBinding().tvAvgHrValue.setText(heartRate + " BPM");
                heart_rate = heartRate;
            }
        });
    }

    @Override
    public void onRespiratoryRate(int respiratoryRate) {
        handler.post(() -> {
            if (getBinding() != null) {
                getBinding().tvRespValue.setText(respiratoryRate + " BPM");
                respiratory_Rate_val = respiratoryRate;
            }
        });
    }

    @Override
    public void onEcgResult(int rrMax, int rrMin, int hrv) {
        handler.post(() -> {
            if (getBinding() == null) return;

            getBinding().tvRrMaxValue.setText(rrMax + " ms");
            getBinding().tvRrMinValue.setText(rrMin + " ms");
            getBinding().tvHrvValue.setText(hrv + " ms");

            SharedPreferences prefs = requireContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
            String savedDeviceName = prefs.getString("savedDeviceName", "Unknown");

            if (respiratory_Rate_val == 0) {
                postEcgData(rrMax, rrMin, hrv, heart_rate, savedDeviceName, "ECG_Module", -1, ECG_Duration);
            } else {
                postEcgData(rrMax, rrMin, hrv, heart_rate, savedDeviceName, "ECG_Module", respiratory_Rate_val, ECG_Duration);
            }
        });
    }

    private void postEcgData(int rrMax, int rrMin, int hrv, int heartRate_, String deviceName, String moduleName, int respiratoryRate, String ecgDuration) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("rrMax", rrMax);
            jsonObject.put("rrMin", rrMin);
            jsonObject.put("hrv", hrv);
            jsonObject.put("heart_rate", heartRate_);
            jsonObject.put("device_name", deviceName);
            jsonObject.put("module_name", moduleName);
            jsonObject.put("respiratory_rate", respiratoryRate == -1 ? "NA" : respiratoryRate);
            jsonObject.put("ECG_duration", ecgDuration);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://156.67.105.81:1880/TW/RHEMOS";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    Log.d("API ECG_Response", response.toString());
                    Toast.makeText(getContext(), "ECG Response: " + response.toString(), Toast.LENGTH_SHORT).show();
                },
                error -> Log.e("API ECG_Error", error.toString()));

        Volley.newRequestQueue(getContext()).add(request);
    }

    @Override
    public void onEcgDuration(int duration, boolean isEnd) {
        handler.post(() -> {
            if (getBinding() != null) {
                String formatted = TimeUtils.formatDuration(duration);
                getBinding().tvEcgDuration.setText(formatted);
                ECG_Duration = formatted;
            }
        });
    }

    @Override
    protected void closeFile() {
        try {
            if (fosEcg != null) {
                fosEcg.close();
                fosEcg = null;
            }
            if (dataOutputStream != null) {
                dataOutputStream.close();
                dataOutputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isMeasuring) {
            stopMeasure();
        }
        getBinding().ecgView.clearDatas();
        handler.removeCallbacksAndMessages(null);
    }
}
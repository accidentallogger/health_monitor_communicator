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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.databinding.FragmentBoBinding;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.fragment.BaseMeasureFragment;
import com.kl.visionsdkdemo.view.PPGDrawWave;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.callback.IRawSpo2DataCallback;
import com.mintti.visionsdk.ble.callback.ISpo2ResultListener;
import com.mintti.visionsdk.common.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class Spo2Fragment extends BaseMeasureFragment<FragmentBoBinding>
        implements IBleWriteResponse, ISpo2ResultListener, Handler.Callback, IRawSpo2DataCallback {
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;
    private static final int MSG_SPO = 0;
    private static final int MSG_HR = 1;

    private PPGDrawWave oxWave;
    private Handler mHandler;
    private boolean isMeasureEnd;
    private boolean isMeasuring = false;

    private File boGlowFile;
    private File boInfraredFile;
    private FileOutputStream fosBoGlow;
    private FileOutputStream fosBoInfrared;
    private int mPrevHr = 0;

    public static Spo2Fragment newInstance() {
        Bundle args = new Bundle();
        Spo2Fragment fragment = new Spo2Fragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    protected FragmentBoBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBoBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView(View view) {
        BleManager.getInstance().setSpo2ResultListener(this);
        BleManager.getInstance().setRawSpo2DataCallback(this);

        oxWave = new PPGDrawWave();
        getBinding().boWaveView.setDrawWave(oxWave);
        mHandler = new Handler(Looper.getMainLooper(), this);

        getBinding().btMeasureBo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getBinding().btnSaveRecord.setOnClickListener(v2 -> {
                    String spo2Text = getBinding().tvSpo2.getText().toString();
                    String hrText = getBinding().tvHr.getText().toString();

                    if (!spo2Text.equals("-- %") && !hrText.equals("000 bpm")) {
                        try {
                            double spo2 = Double.parseDouble(spo2Text.replace(" %", ""));
                            int hr = Integer.parseInt(hrText.replace(" bpm", ""));

                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                            String currentDate = dateFormat.format(new Date());
                            String currentTime = timeFormat.format(new Date());

                            // Get user ID from session
                            String phone = sessionManager.getLoggedInPhone();
                            if (phone != null) {
                                int userId = databaseHelper.getUserId(phone);
                                if (userId != -1) {
                                    long id = databaseHelper.addSpo2Record(
                                            userId,
                                            spo2,
                                            hr,
                                            currentDate,
                                            currentTime,
                                            "SPO2 Measurement"
                                    );

                                    if (id != -1) {
                                        Toast.makeText(requireContext(), "Record saved successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to save record", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Invalid measurement values", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "No measurement to save", Toast.LENGTH_SHORT).show();
                    }
                });
                getBinding().btMeasureBo.setImageResource(!isMeasuring ? R.drawable.ic_stop : R.drawable.ic_play);

                if (!isMeasuring) {
                    // Start measurement
                    isMeasuring = true;
                    reset();
                    createSpo2File();
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_SPO2, Spo2Fragment.this);
                } else {
                    // Stop measurement
                    isMeasuring = false;
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_SPO2, Spo2Fragment.this);
                    closeFile();
                    isMeasureEnd = true;
                    mHandler.postDelayed(() -> oxWave.clear(), 500);
                }
            }
        });
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BOFragment", "onWriteSuccess: BO");
    }

    @Override
    public void onWriteFailed() {
        Log.e("BOFragment", "onWriteFailed: BO");
    }

    @Override
    public void onResume() {
        super.onResume();
        oxWave.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        oxWave.clear();
        mHandler.removeCallbacksAndMessages(null);
        if (isMeasuring) {
            getBinding().btMeasureBo.performClick(); // Trigger stop
        }
    }

    private void reset() {
        getBinding().tvSpo2.setText("-- %");
        getBinding().tvHr.setText("000 bpm");
        getBinding().boWaveView.reply();
        isMeasureEnd = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setSpo2ResultListener(null);
        BleManager.getInstance().setRawSpo2DataCallback(null);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_SPO:
                Bundle bundle = msg.getData();
                double spo = bundle.getDouble("spo");
                int heartRate = bundle.getInt("hr");

                if (spo > 0 && spo < 100) {
                    getBinding().tvSpo2.setText(spo + " %");
                    getBinding().circularProgressBar.setProgress((int) spo);
                }

                if (heartRate > 30 && heartRate < 200) {
                    getBinding().tvHr.setText(heartRate + " bpm");
                }

                mPrevHr = heartRate;

                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
                String savedDeviceName = sharedPreferences.getString("savedDeviceName", null);
                postSpo2Data(spo, heartRate, savedDeviceName, "Heart Rate Module");
                return true;

            case MSG_HR:
                return true;

            default:
                return false;
        }
    }

    private void postSpo2Data(double spo_, int heartRate_, String savedDeviceName, String module_name) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("spo", spo_);
            jsonObject.put("heart_rate", heartRate_);
            jsonObject.put("device_name", savedDeviceName);
            jsonObject.put("module_name", module_name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://156.67.105.81:1880/TW/RHEMOS";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                response -> {
                    Log.d("API SPO2_Response", response.toString());
                    Toast.makeText(getContext(), "SPO2_Response: " + response.toString(), Toast.LENGTH_SHORT).show();
                },
                error -> Log.e("API SPO2_Error", error.toString())
        );

        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }

    @Override
    public void onWaveData(int waveData) {
        if (!isMeasureEnd) {
            oxWave.addData(waveData);
        }
    }

    @Override
    public void onSpo2ResultData(int heartRate, double spo2) {
        Message message = mHandler.obtainMessage(MSG_SPO);
        Bundle bundle = new Bundle();
        bundle.putDouble("spo", spo2);
        bundle.putInt("hr", heartRate);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    @Override
    public void onSpo2End() {
        LogUtils.e("Spo2Fragment", "onSpo2End");
        closeFile();
        isMeasureEnd = true;

        mHandler.postDelayed(() -> {
            if (getBinding() != null) {
                getBinding().btMeasureBo.setImageResource(R.drawable.ic_play);
                isMeasuring = false; // reset flag
                oxWave.clear();
            }
        }, 500);
    }

    @Override
    public void onSpo2RawData(byte[] redDatum, byte[] irDatum) {
        try {
            if (fosBoGlow != null) {
                fosBoGlow.write(redDatum);
            }
            if (fosBoInfrared != null) {
                fosBoInfrared.write(irDatum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSpo2File() {
        try {
            boGlowFile = createFile("red.bin");
            fosBoGlow = new FileOutputStream(boGlowFile);
            boInfraredFile = createFile("ir.bin");
            fosBoInfrared = new FileOutputStream(boInfraredFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void closeFile() {
        try {
            if (fosBoGlow != null) {
                fosBoGlow.close();
                fosBoGlow = null;
            }
            if (fosBoInfrared != null) {
                fosBoInfrared.close();
                fosBoInfrared = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

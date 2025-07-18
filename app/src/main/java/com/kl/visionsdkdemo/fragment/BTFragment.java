package com.kl.visionsdkdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kl.visionsdkdemo.BTCheckActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.SessionManager;
import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.Event;
import com.kl.visionsdkdemo.base.TempValue;
import com.kl.visionsdkdemo.db.DatabaseHelper;
import com.kl.visionsdkdemo.guide.bt.BTGuideFragment;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.mintti.visionsdk.ble.callback.IBtResultListener;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;
import com.kl.visionsdkdemo.databinding.FragmentBtBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BTFragment extends BaseVBindingFragment<FragmentBtBinding>
        implements IBtResultListener, IBleWriteResponse, IRawBtDataCallback {

    private boolean isCheckModel = false;
    private View tempIndicatorPointer;
    private int minTemp = 3500; // 35.0°C * 100
    private int maxTemp = 3900; // 39.0°C * 100
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Override
    protected FragmentBtBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBtBinding.inflate(inflater, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tempIndicatorPointer = getBinding().getRoot().findViewById(R.id.temp_indicator_pointer);

        // Initialize UI components and listeners
        initView(view);
    }

    @Override
    protected void initView(View rootView) {
        getBinding().btMeasureBt.setOnClickListener(v -> {
            getBinding().btMeasureBt.setImageResource(!isCheckModel ? R.drawable.ic_stop : R.drawable.ic_play);

            if (!isCheckModel) {
                BleManager.getInstance().startMeasure(MeasureType.TYPE_BT, BTFragment.this);
                isCheckModel = true;
            } else {
                isCheckModel = false;
                BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
            }
        });

        getBinding().btnSaveRecord.setOnClickListener(v -> {
            String tempText = getBinding().tvBt.getText().toString();
            if (!tempText.equals("-- ℃")) {
                try {
                    double temperature = Double.parseDouble(tempText.replace(" ℃", ""));
                    saveRecord(temperature);
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid temperature value", Toast.LENGTH_SHORT).show();
                    Log.e("BTFragment", "Error parsing temperature", e);
                }
            } else {
                Toast.makeText(requireContext(), "No temperature measurement available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRecord(double temperature) {

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Session expired. Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = sessionManager.getLoggedInPhone();
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid session data", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d("BTFragment", "Attempting to save record for phone: " + phone);

        if (phone == null) {
            Log.e("BTFragment", "Save failed: User not logged in");
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = dbHelper.getUserId(phone);
        Log.d("BTFragment", "Retrieved user ID: " + userId);

        if (userId == -1) {
            Log.e("BTFragment", "Save failed: User not found in database");
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String currentDate = dateFormat.format(new Date());
            String currentTime = timeFormat.format(new Date());

            Log.d("BTFragment", "Saving record with values - " +
                    "UserID: " + userId +
                    ", Temp: " + temperature +
                    ", Date: " + currentDate +
                    ", Time: " + currentTime);

            long id = dbHelper.addBtRecord(userId, temperature, currentDate, currentTime, "Manual measurement");

            if (id != -1) {
                Log.d("BTFragment", "Record saved successfully with ID: " + id);
                Toast.makeText(requireContext(), "Record saved successfully", Toast.LENGTH_SHORT).show();
               // EventBus.getDefault().post(new Event.RecordAddedEvent());
            } else {
                Log.e("BTFragment", "Save failed: Database returned -1 ID");
                Toast.makeText(requireContext(), "Failed to save record", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("BTFragment", "Save failed with exception: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error saving record", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        BleManager.getInstance().setRawBtDataCallback(this);
        BleManager.getInstance().setBtResultListener(this);
        Log.d("BTFragment", "onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setRawBtDataCallback(null);
        BleManager.getInstance().setBtResultListener(null);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BTFragment", "onWriteSuccess: BT");
    }

    @Override
    public void onWriteFailed() {
        Log.e("BTFragment", "onWriteFailed: BT");
        handler.post(() -> {
            if (getBinding() != null) {
                getBinding().tvBt.setText("-- ℃");
                isCheckModel = false;
            }
        });
    }

    @Override
    public void onBtResult(double temperature) {
        int tempHundredths = (int)(temperature * 100);
        updateTempIndicator(tempHundredths);

        Log.e("BTFragment", "onBtResult: " + temperature);
        requireActivity().runOnUiThread(() -> {
            getBinding().tvBt.setText(temperature + " ℃");
            getBinding().btMeasureBt.setImageResource(R.drawable.ic_play);
            isCheckModel = false;
        });
    }

    @Override
    public void onBtRawData(int temperature, int black, int environment, int voltage) {
        Log.e("BTFragment", "onBtRawData: " + temperature);
        handler.post(() -> {
            if (getBinding() != null) {
                double temp = temperature / 100.0;
                getBinding().tvBt.setText(temp + " ℃");
                updateTempIndicator(temperature);

                getBinding().tvBlack.setText("Object: " + (black/100.0) + "℃");
                getBinding().tvEnvironment.setText("Env: " + (environment/100.0) + "℃");
                getBinding().tvVoltage.setText("Voltage: " + (voltage*1.0f/10000) + " mV");
                isCheckModel = false;
            }
        });
    }

    private void updateTempIndicator(int tempInHundredths) {
        if (tempIndicatorPointer == null) return;

        View indicatorBar = (View) tempIndicatorPointer.getParent();
        indicatorBar.post(() -> {
            int barWidth = indicatorBar.getWidth();
            if (barWidth == 0) return;

            float clampedTemp = Math.max(minTemp, Math.min(tempInHundredths, maxTemp));
            float ratio = (clampedTemp - minTemp) * 1f / (maxTemp - minTemp);

            int pointerPosition = (int) (barWidth * ratio) - (tempIndicatorPointer.getWidth() / 2);
            tempIndicatorPointer.setTranslationX(pointerPosition);
        });
    }

    public static BTFragment newInstance() {
        Bundle args = new Bundle();
        BTFragment fragment = new BTFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
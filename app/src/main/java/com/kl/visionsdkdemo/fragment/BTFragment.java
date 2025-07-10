package com.kl.visionsdkdemo.fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kl.visionsdkdemo.BTCheckActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.databinding.FragmentBtBinding;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.callback.IBtResultListener;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class BTFragment extends Fragment implements IBtResultListener, IBleWriteResponse, IRawBtDataCallback {
    private static final String TAG = "BTFragment";
    private boolean isCheckModel = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentBtBinding binding;
    String temp_val;
    String bold_val;
    String environment_val;
    String voltage_val;

    public static BTFragment newInstance() {
        Bundle args = new Bundle();
        BTFragment fragment = new BTFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBtBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        BleManager.getInstance().setRawBtDataCallback(this);
        BleManager.getInstance().setBtResultListener(this);
        Log.d(TAG, "onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setRawBtDataCallback(null);
        BleManager.getInstance().setBtResultListener(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btMeasureBt.setOnClickListener(v -> {

            if (isCheckModel) {
                // Start measurement
                BleManager.getInstance().startMeasure(MeasureType.TYPE_BT, BTFragment.this);
               // binding.btMeasureBt.setText(getString(R.string.end_measure));
                isCheckModel=true;
            } else {
                // Stop measurement
                BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
               // binding.btMeasureBt.setText(getString(R.string.start_measure));
                isCheckModel=false;
            }
        });

      /**  binding.btBtCheck.setOnClickListener(v -> {
            isCheckModel = true;
            startActivity(new Intent(requireActivity(), BTCheckActivity.class));
        });**/
    }

    @Override
    public void onWriteSuccess() {
        Log.e(TAG, "onWriteSuccess: BT");
    }

    @Override
    public void onWriteFailed() {
        Log.e(TAG, "onWriteFailed: BT");
        handler.post(() -> {
            if (binding != null) {
                binding.tvBt.setText("0 ℃");
              //  binding.btMeasureBt.setText(getString(R.string.start_measure));
            }
        });
    }


    @Override
    public void onBtResult(double temperature) {
        Log.e(TAG, "onBtResult: " + temperature);
        handler.post(() -> {
            if (binding != null) {
                binding.tvBt.setText(temperature + " ℃");
              //  binding.btMeasureBt.setText(getString(R.string.start_measure));

                double primaryTemperatureC = temperature / 100.0;

                SharedPreferences sharedPreferences = requireContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
                String savedDeviceName = sharedPreferences.getString("savedDeviceName", null); // Default value is null if not found
                Log.d("", "savedDeviceName_bt: " + savedDeviceName);


                Log.e("bold_date", "bold_val: "+ bold_val);
                Log.e("bold_date", "environment_val: "+ environment_val);
                Log.e("bold_date", "voltage_val: "+ voltage_val);

                if(bold_val==null && environment_val==null && voltage_val==null) {
                    postBTData(temperature, savedDeviceName, "Body Temperature", "NA", "NA", "NA");
                } else {
                    postBTData(temperature, savedDeviceName, "Body Temperature", bold_val, environment_val, voltage_val);
                }
            }
        });
    }


    private void postBTData(double temperature, String device_name, String module_name, String bold, String environment, String voltage) {
        // Create the JSON object
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("temperature", temperature);
            jsonObject.put("device_name", device_name);
            jsonObject.put("module_name", module_name);
            jsonObject.put("bold", bold);
            jsonObject.put("environment", environment);
            jsonObject.put("voltage", voltage);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Create the request
//        String url = "https://156.68.105.81:1880/TWL/parser";
        String url = "http://156.67.105.81:1880/TW/RHEMOS";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Handle the response
                        Log.d("API BT_Response", response.toString());
                        Toast.makeText(getContext(), "BT_Response: "+response.toString(), Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        Log.e("API BT_Error", error.toString());
                    }
                });

        // Add the request to the RequestQueue
        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }

    @Override
    public void onBtRawData(int temperature, int black, int environment, int voltage) {
        Log.e(TAG, "onBtRawData: temperature: " + temperature + ", black: " + black + ", environment: " + environment + ", voltage: " + voltage);

        handler.post(() -> {
            if (binding != null) {
                binding.tvBt.setText((temperature / 100.0) + " ℃");
                binding.tvBlack.setText((black / 100.0) + " ℃");
                binding.tvEnvironment.setText((environment / 100.0) + " ℃");
                binding.tvVoltage.setText((voltage / 10000.0) + " mV");

                Log.e("BTFragment", "temperature: " + temperature + " ℃");
                Log.e("BTFragment", "black: " + black + " ℃");
                Log.e("BTFragment", "environment: " + environment + " ℃");
                Log.e("BTFragment", "voltage: " + voltage + " mV");

                 temp_val = temperature + " ℃";
                 bold_val = black + " ℃";
                 environment_val = environment + " ℃";
                 voltage_val = voltage + " mV";


            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isCheckModel) {
            BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
            //binding.btMeasureBt.setText(getString(R.string.start_measure));
            isCheckModel=true;
        }
        handler.removeCallbacksAndMessages(null);
    }
}

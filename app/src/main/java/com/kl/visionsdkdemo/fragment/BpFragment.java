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
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kl.visionsdkdemo.R;
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
import java.util.ArrayList;

/**
 * Created by leopold on 2021/3/8
 * Description:
 */
public class BpFragment extends BaseMeasureFragment<FragmentBpBinding> implements IBleWriteResponse,
        IBpResultListener,Handler.Callback, IRawBpDataCallback {
    private static final int MSG_BP_RESULT = 1;
    private static final int MSG_BP_LEAK = 2;
    private static final int MSG_BP_ERROR = 3;
    private static final int MSG_BP_ADD_DATA = 4;
    private static final int MSG_BP_DE_DATA = 5;
    private static final int MSG_DURATION = 6;
    private static final String SYSTOLIC = "systolic";
    private static final String DIASTOLIC = "diastolic";
    private static final String HR = "hr";
    private static final String SAMPLE_TIME = "sample_time";
    private static final String DE_DATA_SIZE = "de_data_size";
    private static final String SAMPLE_RATE = "sample_rate";
    private final Handler handler = new Handler(Looper.getMainLooper(),this);
    private File bpDeTxtFile;
    private BufferedWriter deFileWriter;
    private File bpAddTxtFile;
    private BufferedWriter addFileWriter;
    private BufferedWriter rawFileWriter;
    private int measureDuration = 0;
    private boolean isTest = false;
    private int standardSystolic = 120;
    private int standardDiastolic = 80;
    private int standardHeartRate = 80;
    private final ArrayList<Short> bpDeDataList = new ArrayList<>(); //放压数据
    private final ArrayList<Short> bpAddDataList = new ArrayList<>(); //加压数据
    private MessageDialog messageDialog;


    public static BpFragment newInstance() {

        Bundle args = new Bundle();

        BpFragment fragment = new BpFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected FragmentBpBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBpBinding.inflate(inflater,container,false);
    }

    @Override
    protected void initView(View rootView) {
        BleManager.getInstance().setBpResultListener(this);
        BleManager.getInstance().setRawBpDataCallback(this);
        getBinding().groupPicker.setVisibility(View.GONE);
        getBinding().btMeasureBp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTest) {
                    isTest=true;
                    //开始测量
                    resetValue();
                    bpDeTxtFile = createFile("bpDeTxt.txt");
                    bpAddTxtFile = createFile("bpAddTxt.txt");
                    deFileWriter = createBufferWriter(bpDeTxtFile);
                    addFileWriter = createBufferWriter(bpAddTxtFile);
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    // The button text will be updated by the SDK's callbacks or the duration handler
                    // getBinding().btMeasureBp.setText(getString(R.string.end_measure));

                    handler.sendEmptyMessageDelayed(MSG_DURATION,1000);

                }else {
                    //停止测量
                    isTest=false;
                    closeFile();
                    handler.removeMessages(MSG_DURATION);
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    // The button text will be updated by the SDK's callbacks
                    // getBinding().btMeasureBp.setText(getString(R.string.start_measure));
                }
            }
        });
        /*getBinding().btTestBp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getBinding().btTestBp.getText().equals(getString(R.string.test_blood_pressure))){
                    resetValue();
                    getBinding().btTestBp.setText(R.string.stop);
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BP, BpFragment.this);
                    handler.sendEmptyMessageDelayed(MSG_DURATION,1000);
                }else {
                    handler.removeMessages(MSG_DURATION);
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, BpFragment.this);
                  //  getBinding().btTestBp.setText(getString(R.string.test_blood_pressure));
                }
            }
        });*/
  /*      getBinding().switchTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isTest = isChecked;
                if (isTest){
                    getBinding().groupPicker.setVisibility(View.VISIBLE);
                }else {
                    getBinding().groupPicker.setVisibility(View.GONE);
                }
            }
        });
*/
     //   getBinding().switchTest.setChecked(isTest);
        initPicker();
    }

    private void initPicker() {
        NumberPicker systolicPicker = getBinding().pickerSystolic;
        String[] sysValue = new String[23];
        for (int i = 3; i <=25 ; i++) {
            sysValue[i-3] = i*10+"";
        }
        systolicPicker.setMaxValue(22);
        systolicPicker.setMinValue(0);
        systolicPicker.setValue(9);
        systolicPicker.setDisplayedValues(sysValue);
        systolicPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                standardSystolic = Integer.parseInt(sysValue[newVal]);
            }
        });
        String[] diasValue = new String[14];
        for (int i = 3; i <=16 ; i++) {
            diasValue[i-3] = i*10+"";
        }

        NumberPicker diastolicPicker = getBinding().pickerDiastolic;
        diastolicPicker.setMaxValue(13);
        diastolicPicker.setMinValue(0);
        diastolicPicker.setValue(5);
        diastolicPicker.setDisplayedValues(diasValue);
        diastolicPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                Log.e("BpFragment", "onValueChange: "+diasValue[newVal]); // Corrected to diasValue
                standardDiastolic = Integer.parseInt(diasValue[newVal]);
                Toast.makeText(getActivity(), "standardDiastolic: " + standardDiastolic, Toast.LENGTH_SHORT).show();
            }
        });
        String[] hrValue = new String[]{"80","94"};
        NumberPicker hrPicker = getBinding().pickerHr;
        hrPicker.setMaxValue(1);
        hrPicker.setMinValue(0);
        hrPicker.setValue(0);
        hrPicker.setDisplayedValues(hrValue);
        hrPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                Log.e("BpFragment", "onValueChange: "+hrValue[newVal]); // Corrected to hrValue
                standardHeartRate = Integer.parseInt(hrValue[newVal]);
            }
        });
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BpFragment", "onWriteSuccess: BP" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BpFragment", "onWriteFailed: BP" );
        // Ensure UI is reset if measurement fails to start
        handler.post(() -> {
            resetValue();
            isTest = false; // Reset test flag
            //getBinding().btMeasureBp.setText(getString(R.string.start_measure));
           // getBinding().btTestBp.setText(getString(R.string.test_blood_pressure));
            handler.removeMessages(MSG_DURATION);
            closeFile();
        });
    }

    private void resetValue() {
        Log.e("BpFragment", "resetValue" );
        getBinding().tvSystolicValue.setText("-- / mmHg");
        getBinding().tvDiastolicValue.setText("-- / mmHg");
        getBinding().tvHrValue.setText("-- / BPM");
      //  getBinding().tvBpAdd.setText("0");
      //  getBinding().tvBpDe.setText("0");
      //  getBinding().tvSampleTime.setText( "0");
      //  getBinding().tvDeDataSize.setText("0");
      //  getBinding().tvSampleRate.setVisibility(View.GONE);
        bpDeDataList.clear();
        bpAddDataList.clear();
        measureDuration = 0;
        getBinding().tvDuration.setText(measureDuration+" s");
    }


    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (getBinding() == null){
            return false;
        }
        if (msg.what == MSG_BP_RESULT) {
            Bundle bundle = msg.getData();
            int hr = bundle.getInt(HR);
            int systolic = bundle.getInt(SYSTOLIC);
            int diastolic = bundle.getInt(DIASTOLIC);

            // Update with final results
            getBinding().tvSystolicValue.setText(systolic + " / mmHg");
            getBinding().tvDiastolicValue.setText(diastolic + " / mmHg");
            getBinding().tvHrValue.setText(hr + " / BPM");
           // getBinding().tvDeDataSize.setText(bpDeDataList.size()+"");

            Log.e("Value", "Final systolic: " + systolic + " / mmHg");
            Log.e("Value", "Final diastolic: " + diastolic + " / mmHg");
            Log.e("Value", "Final hr: " + hr + " / BPM");
            Log.e("Value", "bpDeDataList size: " + bpDeDataList.size()+"");

            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("VisionSDK", Context.MODE_PRIVATE);
            String savedDeviceName = sharedPreferences.getString("savedDeviceName", null);
            Log.d("", "savedDeviceName_Bp: " + savedDeviceName);

            postBpData(systolic, diastolic, hr, savedDeviceName, "Blood Pressure");

            bpDeDataList.clear();
            // Reset button text after final result is displayed
           // getBinding().btMeasureBp.setText(getString(R.string.start_measure));
            isTest = false; // Reset test flag
            return true;
        }else if (msg.what == MSG_BP_LEAK) {
            showErrorDialog(getString(R.string.leak));
           // getBinding().btMeasureBp.setText(getString(R.string.start_measure)); // Reset button text
            isTest = false; // Reset test flag
            return true;
        }else if (msg.what == MSG_BP_ERROR) {
            showErrorDialog(getString(R.string.measure_err));
           // getBinding().btMeasureBp.setText(getString(R.string.start_measure)); // Reset button text
            isTest = false; // Reset test flag
            return true;
        }else if (msg.what == MSG_BP_ADD_DATA) {
            int pressure = msg.arg1;
          //  getBinding().tvBpAdd.setText(String.valueOf(pressure));
            // Update systolic value with live pressurization data
            getBinding().tvSystolicValue.setText(pressure + " / mmHg");
            return true;
        }else if (msg.what == MSG_BP_DE_DATA) {
            int pressure = msg.arg1;
         //   getBinding().tvBpDe.setText(String.valueOf(pressure));
            // Update diastolic value with live decompression data
            getBinding().tvDiastolicValue.setText(pressure + " / mmHg");
            return true;
        }else if (msg.what == MSG_DURATION) {
            measureDuration ++ ;
            getBinding().tvDuration.setText(measureDuration + " s");
            handler.sendEmptyMessageDelayed(MSG_DURATION,1000);
        }
        return false;

    }


    private void postBpData(int systolic, int diastolic, int hr, String device_name, String module_name) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("systolic", systolic);
            jsonObject.put("diastolic", diastolic);
            jsonObject.put("hr", hr);
            jsonObject.put("device_name", device_name);
            jsonObject.put("module_name", module_name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://156.67.105.81:1880/TW/RHEMOS";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("API BP_Response", response.toString());
                        Toast.makeText(getContext(), "BP_Response: "+response.toString(), Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("API BP_Error", error.toString());
                        Toast.makeText(getContext(), "API BP_Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        Volley.newRequestQueue(getContext()).add(jsonObjectRequest);
    }


    private void showErrorDialog(String msg) {
        if (messageDialog == null) {
            messageDialog = MessageDialog.build();
        }
        messageDialog.setTitle(getString(R.string.hint));
        messageDialog.setMessage(msg);
        messageDialog.setOkButton(R.string.ok);
        messageDialog.show();
    }

    @Override
    public void onBpResult(int systolic, int diastolic, int heartRate) {
        Log.e("BpFragment", "onBpResult - systolic: "+systolic + ", diastolic: "+diastolic + ", heartRate: "+heartRate);
        closeFile();
        handler.removeMessages(MSG_DURATION);
        Message msg = handler.obtainMessage(MSG_BP_RESULT);
        Bundle bundle = new Bundle();
        bundle.putInt(SYSTOLIC,systolic);
        bundle.putInt(DIASTOLIC,diastolic);
        bundle.putInt(HR,heartRate);
        msg.setData(bundle);
        handler.sendMessage(msg); // Send message to handler to update UI with final result
    }

    @Override
    public void onLeadError() {
        Log.e("BpFragment", "onLeadError: ");
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
        Log.d("BPFragment", "pressurizationData: " + pressurizationData);
        bpAddDataList.add(pressurizationData);
        Message message = handler.obtainMessage(MSG_BP_ADD_DATA);
        message.arg1 = pressurizationData;
        handler.sendMessage(message); // Send message to handler to update UI with live data
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
        Log.d("BPFragment", "decompressionData: " + decompressionData);
        bpDeDataList.add(decompressionData);
        Message message = handler.obtainMessage(MSG_BP_DE_DATA);
        message.arg1 = decompressionData;
        handler.sendMessage(message); // Send message to handler to update UI with live data
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
        Log.d("BP", "onPressure: "+pressure);
        // This callback provides raw pressure. If you want to display this in a specific UI element
        // that's not tvBpAdd or tvBpDe, you would update it here.
        // For now, it's just logged.
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
            Log.d("BpFragment", "File writers closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isTest) {
            // Ensure measurement is stopped when fragment pauses if it's in test mode
            BleManager.getInstance().stopMeasure(MeasureType.TYPE_BP, this);
            isTest = false; // Reset test flag
            handler.removeMessages(MSG_DURATION);
            closeFile();
            // Reset button text and UI
            if (getBinding() != null) {
             //   getBinding().btMeasureBp.setText(getString(R.string.start_measure));
               // getBinding().btTestBp.setText(getString(R.string.test_blood_pressure));
                resetValue();
            }
        }
        handler.removeCallbacksAndMessages(null); // Remove all pending messages
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // It's crucial to clear listeners to prevent memory leaks.
        // If you set them in initView, clear them here.
        BleManager.getInstance().setBpResultListener(null); // Clear listener
        BleManager.getInstance().setRawBpDataCallback(null); // Clear listener
    }
}

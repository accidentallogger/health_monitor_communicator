package com.kl.visionsdkdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kl.visionsdkdemo.BTCheckActivity;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.Event;
import com.kl.visionsdkdemo.base.TempValue;
import com.kl.visionsdkdemo.guide.bt.BTGuideFragment;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.BleManager;
import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.mintti.visionsdk.ble.callback.IBtResultListener;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;
import com.kl.visionsdkdemo.databinding.FragmentBtBinding;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by leopold on 2021/3/9
 * Description:
 */
public class BTFragment extends BaseVBindingFragment<FragmentBtBinding>
        implements IBtResultListener, IBleWriteResponse, IRawBtDataCallback {
    private boolean isCheckModel=false;
    private View tempIndicatorPointer;
    private int minTemp = 3500; // 35.0°C * 100
    private int maxTemp = 3900; // 39.0°C * 100

    private final Handler handler = new Handler(Looper.getMainLooper());

    public static BTFragment newInstance() {
        Bundle args = new Bundle();
        BTFragment fragment = new BTFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected FragmentBtBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentBtBinding.inflate(inflater,container,false);
    }

    @Override
    public void onResume() {
        super.onResume();
        BleManager.getInstance().setRawBtDataCallback(this);
        BleManager.getInstance().setBtResultListener(this);
        Log.d("BTFragment","onResume");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleManager.getInstance().setRawBtDataCallback(null);
        BleManager.getInstance().setBtResultListener(null);
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        tempIndicatorPointer = getBinding().getRoot().findViewById(R.id.temp_indicator_pointer);

        getBinding().btMeasureBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBinding().btMeasureBt.setImageResource(!isCheckModel ? R.drawable.ic_stop : R.drawable.ic_play);

                if (!isCheckModel){

                    //开始测量
                    BleManager.getInstance().startMeasure(MeasureType.TYPE_BT, BTFragment.this);
                    //getBinding().btMeasureBt.setText(getString(R.string.end_measure));
                    isCheckModel=true;

                }else {
                    //停止测量
                    isCheckModel=false;
                    BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
                    //getBinding().btMeasureBt.setText(getString(R.string.start_measure));

                }
            }
        });
//        getBinding().btBtCheck.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                isCheckModel = true;
////                btGuideFragment.show(getChildFragmentManager(),"btGuide");
//                startActivity(new Intent(requireActivity(), BTCheckActivity.class));
//            }
//        });
    }

    @Override
    public void onWriteSuccess() {
        Log.e("BTFragment", "onWriteSuccess: BT" );
    }

    @Override
    public void onWriteFailed() {
        Log.e("BTFragment", "onWriteFailed: BT" );
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getBinding() != null) {
                    getBinding().tvBt.setText(0+" ℃");


                    isCheckModel=false;
                    //getBinding().btMeasureBt.setText(getString(R.string.start_measure));
                }
            }
        });
    }

    @Override
    public void onBtResult(double temperature) {
        int tempHundredths = (int)(temperature * 100);
        updateTempIndicator(tempHundredths);

        Log.e("BTFragment", "onBtResult: "+temperature );
       requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getBinding().tvBt.setText(temperature+" ℃");
                getBinding().btMeasureBt.setImageResource(R.drawable.ic_play);
                //getBinding().btMeasureBt.setText(getString(R.string.start_measure));
                isCheckModel=false;
            }
        });
    }

    @Override
    public void onBtRawData(int temperature, int black, int environment,int voltage) {
        Log.e("BTFragment", "onBtRawData: "+temperature );
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (getBinding() != null) {
                    double temp = temperature / 100.0;
                    getBinding().tvBt.setText(temp + " ℃");
                    updateTempIndicator(temperature);

                    getBinding().tvBlack.setText(black/100.0+" ℃");
                    getBinding().tvEnvironment.setText(environment/100.0+" ℃");
                    getBinding().tvVoltage.setText(voltage*1.0f/10000+" mV");
                    Log.d("BTFragment", "run: "+voltage);
                   // getBinding().btMeasureBt.setText(getString(R.string.start_measure));
                    isCheckModel=false;
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isCheckModel) {
            BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT, BTFragment.this);
            //  getBinding().btMeasureBt.setText(getString(R.string.start_measure));
            isCheckModel=false;
        }
        handler.removeCallbacksAndMessages(null);
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

}
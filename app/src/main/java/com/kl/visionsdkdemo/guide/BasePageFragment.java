package com.kl.visionsdkdemo.guide;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.App;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;


/**
 * author : wusheng
 * date   : 2021/8/27
 * desc   : 引导页基类
 */
public abstract class BasePageFragment extends BaseFragment{
    protected MaterialButton btMeasure;
    protected LinearLayout llNext;
    protected TextView tvTemp;
    protected OnNextClickListener onNextClickListener;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            int black = msg.arg1;
            tvTemp.setText(black / 100.0 + " ℃");
        }
    };

    public void setOnNextClickListener(OnNextClickListener onNextClickListener) {
        this.onNextClickListener = onNextClickListener;
    }


    @Override
    public void onBindView(@Nullable Bundle savedInstanceState, @Nullable View rootView) {
        //BleManager.getInstance().setRawBtDataCallback(this);
        btMeasure = rootView.findViewById(R.id.bt_measure);
        tvTemp = rootView.findViewById(R.id.tv_temp);
        llNext = rootView.findViewById(R.id.ll_next);
        btMeasure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtClick();
            }
        });
        llNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onNextClickListener != null) {
                    onNextClickListener.onNextClick();
                }
            }
        });
    }

    protected abstract void onBtClick();

//    @Override
//    public void onBtRawData(int temperature, int black, int environment, int voltage) {
//        Message message = Message.obtain();
//        message.arg1 = black;
//        handler.sendMessage(message);
//
//    }
}

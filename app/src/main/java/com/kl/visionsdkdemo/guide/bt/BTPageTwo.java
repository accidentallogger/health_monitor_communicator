package com.kl.visionsdkdemo.guide.bt;


import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.Event;
import com.kl.visionsdkdemo.guide.BasePageFragment;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * author : wusheng
 * date   : 2021/9/1
 * desc   :
 */
public class BTPageTwo extends BasePageFragment {
    private LinearLayout llPre;
    @Override
    public Object setLayout() {
        return R.layout.bt_page_two;
    }

    @Override
    public void onBindView(@Nullable Bundle savedInstanceState, @Nullable View rootView) {
        super.onBindView(savedInstanceState, rootView);
        llPre = rootView.findViewById(R.id.ll_pre);
        llPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onNextClickListener != null)
                    onNextClickListener.onPreClick();
            }
        });
    }

    @Override
    protected void onBtClick() {
        BleManager.getInstance().writeOrder(10, new IBleWriteResponse() {
            @Override
            public void onWriteSuccess() {

            }

            @Override
            public void onWriteFailed() {

            }
        });
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBus(Event event) {
        super.onEventBus(event);
        if (event.getData().getModel() == 1){
            tvTemp.setText(event.getData().getTemp()/100.0+" ℃");
        }
    }
}

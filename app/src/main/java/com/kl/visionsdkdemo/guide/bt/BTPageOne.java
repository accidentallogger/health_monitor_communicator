package com.kl.visionsdkdemo.guide.bt;


import android.util.Log;

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
public class BTPageOne extends BasePageFragment {
    @Override
    public Object setLayout() {
        return R.layout.bt_page_one;
    }

    @Override
    protected void onBtClick() {
        Log.d(TAG, "onBtClick: ");
        BleManager.getInstance().writeOrder(9, new IBleWriteResponse() {
            @Override
            public void onWriteSuccess() {
                Log.d(TAG, "写入成功 ");
//                if (onNextClickListener != null){
//                    onNextClickListener.onNextClick();
//                }
            }

            @Override
            public void onWriteFailed() {
                Log.d(TAG, "写入失败 ");
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBus(Event event) {
        super.onEventBus(event);
        if (event.getData().getModel() == 0){
            tvTemp.setText(event.getData().getTemp()/100.0+" ℃");
        }

    }
}

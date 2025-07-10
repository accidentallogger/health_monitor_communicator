package com.kl.visionsdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.kl.visionsdkdemo.base.App;
import com.kl.visionsdkdemo.base.BaseVBindingActivity;
import com.kl.visionsdkdemo.base.Event;
import com.kl.visionsdkdemo.databinding.ActivityBtCheckBinding;
import com.kl.visionsdkdemo.databinding.ActivityMeasureBinding;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.bean.MeasureType;
import com.mintti.visionsdk.ble.callback.IBleConnectionListener;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * author : wusheng
 * date   : 2022/10/9
 * desc   :
 */
public class BTCheckActivity extends BaseVBindingActivity<ActivityBtCheckBinding> {
    private int model = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isBTMeasuring = false;
    @Override
    public ActivityBtCheckBinding getViewBinding() {
       return ActivityBtCheckBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

    }

    @Override
    protected void onResume() {
        super.onResume();
        BleManager.getInstance().addConnectionListener(new IBleConnectionListener() {
            @Override
            public void onConnectSuccess(String mac) {

            }

            @Override
            public void onConnectFailed(String mac) {

            }

            @Override
            public void onDisconnected(String mac, boolean isActiveDisconnect) {
                Toast.makeText(BTCheckActivity.this, R.string.bluetooth_disconnected, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BTCheckActivity.this,MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        BleManager.getInstance().setBtResultListener(null);
        BleManager.getInstance().setRawBtDataCallback(new IRawBtDataCallback() {
            @Override
            public void onBtRawData(int temperature, int black, int environment, int voltage) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isBTMeasuring = false;
                        if (getBinding() != null) {
                            if (model == 0) {
                                getBinding().tv25Temp.setText(black/100.0+" ℃");
                                getBinding().tvDy.setText("电压："+voltage/10000.0);
                                getBinding().btMeasureTwo.setEnabled(true);

                                Log.e("BTCheckActivity", "black: " + black);
                                Log.e("BTCheckActivity", "voltage: " + voltage);

                            }else {
                                getBinding().tv37Temp.setText(black/100.0+" ℃");
                                getBinding().tvDy2.setText("电压："+voltage/10000.0);
                                getBinding().btMeasureTwo.setEnabled(false);


                                Log.e("BTCheckActivity", "else_black: " + black);
                                Log.e("BTCheckActivity", "else_voltage: " + voltage);

                            }
                        }
                    }
                });

            }
        });
    }

    private void initView() {
        getBinding().btMeasureOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBTMeasuring) {
                    return;
                }
                model = 0;
                isBTMeasuring = true;
                BleManager.getInstance().writeOrder(9, new IBleWriteResponse() {
                    @Override
                    public void onWriteSuccess() {
                        isBTMeasuring = true;
                    }
                    @Override
                    public void onWriteFailed() {
                        isBTMeasuring = false;
                    }
                });
            }
        });
        getBinding().btMeasureTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBTMeasuring) {
                    return;
                }
                model = 1;
                isBTMeasuring = true;
                BleManager.getInstance().writeOrder(10, new IBleWriteResponse() {
                    @Override
                    public void onWriteSuccess() {
                        isBTMeasuring = true;
                    }
                    @Override
                    public void onWriteFailed() {
                        isBTMeasuring = false;
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
//        BleManager.getInstance().stopMeasure(MeasureType.TYPE_BT,null);
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventBus(Event event) {
//        if (model == 0){
//            getBinding().tv25Temp.setText(event.getData().getTemp()/100.0+" ℃");
//            getBinding().tvDy.setText("电压："+event.getData().getDy()/10000.0);
//            getBinding().btMeasureTwo.setEnabled(true);
//        }else{
//            getBinding().tv37Temp.setText(event.getData().getTemp()/100.0+" ℃");
//            getBinding().tvDy2.setText("电压："+event.getData().getDy()/10000.0);
//            getBinding().btMeasureTwo.setEnabled(false);
//        }
//
//
//    }
}

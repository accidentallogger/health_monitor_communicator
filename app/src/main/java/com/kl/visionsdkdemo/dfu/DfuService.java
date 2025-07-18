package com.kl.visionsdkdemo.dfu;

import android.app.Activity;

import no.nordicsemi.android.dfu.DfuBaseService;


public class DfuService extends DfuBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return NotificationActivity.class;
    }
    @Override
    protected boolean isDebug() {
        // return BuildConfig.DEBUG;
        return true;
    }
}

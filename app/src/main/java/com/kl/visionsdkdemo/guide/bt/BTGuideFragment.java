package com.kl.visionsdkdemo.guide.bt;


import com.kl.visionsdkdemo.guide.BaseGuideFragment;
import com.mintti.visionsdk.ble.BleManager;
import com.mintti.visionsdk.ble.callback.IBleWriteResponse;
import com.mintti.visionsdk.ble.callback.IRawBtDataCallback;

/**
 * author : wusheng
 * date   : 2021/9/1
 * desc   : 测量引导页
 */
public class BTGuideFragment extends BaseGuideFragment{
    @Override
    protected void initPage() {
        page1 = new BTPageOne();
        page2 = new BTPageTwo();
//        page3 = new BTPageThree();
        fragments.add(page1);
        fragments.add(page2);
//        fragments.add(page3);
    }



    @Override
    protected void handlePage2Next() {
        dismiss();
    }


}

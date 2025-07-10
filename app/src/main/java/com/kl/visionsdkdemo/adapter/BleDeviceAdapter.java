package com.kl.visionsdkdemo.adapter;



import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.mintti.visionsdk.ble.BleDevice;
import com.kl.visionsdkdemo.R;

import java.util.List;

public class BleDeviceAdapter extends BaseQuickAdapter<BleDevice, BaseViewHolder> {
    public BleDeviceAdapter( List<BleDevice> data) {
        super(R.layout.item_ble_device, data);
    }

    @Override
    protected void convert(BaseViewHolder holder, BleDevice bleDevice) {
        holder.setText(R.id.bleMac,bleDevice.getMac());
        holder.setText(R.id.ble_rssi,bleDevice.getRssi()+"dpm");
        holder.setText(R.id.bleName,bleDevice.getName());
    }
}

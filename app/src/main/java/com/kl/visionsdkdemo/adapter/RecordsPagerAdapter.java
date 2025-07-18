package com.kl.visionsdkdemo.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.kl.visionsdkdemo.fragment.BpRecordsFragment;
import com.kl.visionsdkdemo.fragment.BtRecordsFragment;
import com.kl.visionsdkdemo.fragment.EcgRecordsFragment;
import com.kl.visionsdkdemo.fragment.Spo2RecordsFragment;

public class RecordsPagerAdapter extends FragmentStateAdapter {

    public RecordsPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new EcgRecordsFragment();
            case 1: return new Spo2RecordsFragment();
            case 2: return new BpRecordsFragment();
            case 3: return new BtRecordsFragment();
            default: return new EcgRecordsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
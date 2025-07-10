package com.kl.visionsdkdemo.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.kl.visionsdkdemo.fragment.BGFragment;
import com.kl.visionsdkdemo.fragment.DeviceInfoFragment;
import com.kl.visionsdkdemo.fragment.Spo2Fragment;
import com.kl.visionsdkdemo.fragment.BTFragment;
import com.kl.visionsdkdemo.fragment.BpFragment;
import com.kl.visionsdkdemo.fragment.ECGFragment;

import java.util.ArrayList;
import java.util.List;

public class MeasureFragmentAdapter extends FragmentPagerAdapter {
    private String[] title = {"BP","BG","BT","SpO2","Ecg","Device"};
    private List<Fragment> fragmentList = new ArrayList<>();


    public MeasureFragmentAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        fragmentList.add(BpFragment.newInstance());
        fragmentList.add(BGFragment.newInstance());
        fragmentList.add(BTFragment.newInstance());
        fragmentList.add(Spo2Fragment.newInstance());
        fragmentList.add(ECGFragment.newInstance());
        fragmentList.add(DeviceInfoFragment.newInstance());


    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return title[position];
    }

    @Override
    public int getCount() {
        return this.fragmentList.size();
    }
}

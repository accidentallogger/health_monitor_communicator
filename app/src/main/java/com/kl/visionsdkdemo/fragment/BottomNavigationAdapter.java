package com.kl.visionsdkdemo.fragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.kl.visionsdkdemo.fragment.MeasurementFragment;
import com.kl.visionsdkdemo.fragment.RecordFragment;
import com.kl.visionsdkdemo.fragment.SettingsFragment;

public class BottomNavigationAdapter extends FragmentPagerAdapter {
    
    public BottomNavigationAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new MeasurementFragment();
            case 1:
                return new RecordFragment();
            case 2:
                return new SettingsFragment();
            default:
                return new MeasurementFragment();
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
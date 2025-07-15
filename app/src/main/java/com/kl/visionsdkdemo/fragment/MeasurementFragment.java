package com.kl.visionsdkdemo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.kl.visionsdkdemo.R;

public class MeasurementFragment extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measurement, container, false);

        // Initialize buttons
        view.findViewById(R.id.btn_bp).setOnClickListener(this);
        view.findViewById(R.id.btn_bg).setOnClickListener(this);
        view.findViewById(R.id.btn_bt).setOnClickListener(this);
        view.findViewById(R.id.btn_spo2).setOnClickListener(this);
        view.findViewById(R.id.btn_ecg).setOnClickListener(this);

        view.findViewById(R.id.card_bp).setOnClickListener(this);
        view.findViewById(R.id.card_bt).setOnClickListener(this);
        view.findViewById(R.id.card_bg).setOnClickListener(this);
        view.findViewById(R.id.card_bo).setOnClickListener(this);
        view.findViewById(R.id.card_ecg).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        Class<? extends Fragment> fragmentClass = null;
        String title = "";

        int id = v.getId();
        if (id == R.id.btn_bp || id==R.id.card_bp) {
            fragmentClass = BpFragment.class;
            title = "Blood Pressure";
        } else if (id == R.id.btn_bg || id==R.id.card_bg) {
            fragmentClass = BGFragment.class;
            title = "Blood Glucose";
        } else if (id == R.id.btn_bt || id==R.id.card_bt) {
            fragmentClass = BTFragment.class;
            title = "Body Temperature";
        } else if (id == R.id.btn_spo2 || id==R.id.card_bo) {
            fragmentClass = Spo2Fragment.class;
            title = "SpO₂ Measurement";
        } else if (id == R.id.btn_ecg || id==R.id.card_ecg) {
            fragmentClass = ECGFragment.class;
            title = "ECG Measurement";
        }

        if (fragmentClass != null) {
            Intent intent = new Intent(getActivity(), FragmentHostActivity.class);
            intent.putExtra(FragmentHostActivity.EXTRA_FRAGMENT_CLASS, fragmentClass.getName());

            startActivity(intent);
        }
    }
}
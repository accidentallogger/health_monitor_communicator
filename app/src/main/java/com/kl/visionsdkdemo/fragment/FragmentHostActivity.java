package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.kl.visionsdkdemo.R;

public class FragmentHostActivity extends AppCompatActivity {
    public static final String EXTRA_FRAGMENT_CLASS = "fragment_class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_host);

        String fragmentClassName = getIntent().getStringExtra(EXTRA_FRAGMENT_CLASS);


        try {
            Class<?> fragmentClass = Class.forName(fragmentClassName);
            Fragment fragment = (Fragment) fragmentClass.newInstance();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
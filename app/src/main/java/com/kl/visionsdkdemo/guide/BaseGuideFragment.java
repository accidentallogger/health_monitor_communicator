package com.kl.visionsdkdemo.guide;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;


import com.kl.visionsdkdemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * author : wusheng
 * date   : 2021/8/26
 * desc   : 引导页基类
 */
public abstract class BaseGuideFragment extends DialogFragment {
    protected List<Fragment> fragments = new ArrayList<>();
    protected ViewPager2 vpGuide;
    private TextView dot1,dot2;
    protected BasePageFragment page1,page2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.CustomDialog);//ThemeOverlay_AppCompat_Dialog
        initPage();
    }

    protected abstract void initPage();


    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(requireContext().getResources().getColor(R.color.white)));
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.guide_fragment,container,false);
    }

    @Override
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            //在每个add事务前增加一个remove事务，防止连续的add
            manager.beginTransaction().remove(this).commit();
            super.show(manager, tag);
        } catch (Exception e) {
            //同一实例使用不同的tag会异常，这里捕获一下
            e.printStackTrace();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vpGuide = view.findViewById(R.id.vp_guide);
        vpGuide.setUserInputEnabled(false);
        dot1 = view.findViewById(R.id.dot_1);
        dot2 = view.findViewById(R.id.dot_2);


        vpGuide.setAdapter(new GuidePageAdapter(this));
        if (fragments.size() == 2) {
            dot2.setVisibility(View.VISIBLE);

            vpGuide.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    if (position == 0) {
                        dot1.setSelected(true);
                        dot2.setSelected(false);

                    } else if (position == 1) {
                        dot2.setSelected(true);
                        dot1.setSelected(false);

                    }
                }
            });
            setCurrentPage(0);
            page1.setOnNextClickListener(new OnNextClickListener() {
                @Override
                public void onNextClick() {
                    handlePage1Next();
                }

                @Override
                public void onPreClick() {

                }
            });
            page2.setOnNextClickListener(new OnNextClickListener() {
                @Override
                public void onNextClick() {
                    handlePage2Next();
                }

                @Override
                public void onPreClick() {
                    setCurrentPage(0);
                }
            });


        }
    }

    protected void handlePage1Next() {
        setCurrentPage(1);
    }
    protected void handlePage2Next() {
        setCurrentPage(2);
    }
    protected void handlePage3Next() {
        setCurrentPage(3);
    }

    protected void setCurrentPage(int page) {
        vpGuide.setCurrentItem(page);
    }
    public int getCurrentPage(){
        return vpGuide.getCurrentItem();
    }


    private class GuidePageAdapter extends FragmentStateAdapter {

        public GuidePageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}

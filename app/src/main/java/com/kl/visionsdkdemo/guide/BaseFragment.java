package com.kl.visionsdkdemo.guide;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.Event;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by gaoyingjie on 2019/10/17
 * Description:
 */
public abstract class BaseFragment extends Fragment {
    protected String TAG = this.getClass().getSimpleName();
    @Nullable
    LinearLayout mLlBack;
    @Nullable
    TextView mToolbarTitle;
    LinearLayout mLLRight;
    ImageView mIvRight;
    TextView mTvRight;

    protected Activity mActivity;
    protected boolean isViewBind = false;


    public abstract Object setLayout();//设置当前布局View

    public abstract void onBindView(@Nullable Bundle savedInstanceState, @Nullable View rootView);



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView;
        if (setLayout() instanceof Integer) {
            rootView = inflater.inflate((int) setLayout(), container, false);
        } else if (setLayout() instanceof View) {
            rootView = (View) setLayout();
        } else {
            throw new ClassCastException("type of setLayout() must be int or View!");
        }
        isViewBind = true;
        onBindView(savedInstanceState, rootView);
        EventBus.getDefault().register(this);
        return rootView;
    }







    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewBind = false;
        EventBus.getDefault().unregister(this);

    }

    protected int setTitleBar() {
        return 0;
    }

    protected int setStatusBarView() {
        return 0;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventBus(Event event){}


}

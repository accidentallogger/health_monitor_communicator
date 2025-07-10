package com.kl.visionsdkdemo.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.utils.SizeUtils;


/**
 * Created by gaoyingjie on 2019/4/8
 * Description:自定义dialog
 */
public class CommonSelectDialog extends Dialog implements View.OnClickListener {


    private Context mContext;
    private int mAnimation;
    private View mView;
    private int mHeight;
    private int mWidth;
    private boolean cancelTouchout;
    private String title;
    private OnClickListener onClickListener;
    private String okText;
    private String cancelText;

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.btn_cancel) {
            if (onClickListener !=null){
                onClickListener.onClick(this,false);
            }
        }else if (i == R.id.btn_ok){
            if (onClickListener !=null){
                onClickListener.onClick(this,true);
            }
        }
    }


    public interface OnClickListener {
        void onClick(CommonSelectDialog dialog, boolean isOk);
    }



    private CommonSelectDialog(Builder builder){
        //super(builder.context);
        this(builder,0);



    }



    private CommonSelectDialog(Builder builder, int themeResId) {
        super(builder.context, themeResId);
        this.mContext = builder.context;
        this.mAnimation = builder.animation;
        this.mWidth = builder.width;
        this.mHeight = builder.height;
        this.mView = LayoutInflater.from(mContext).inflate(R.layout.common_dialog_select,null);
        this.cancelTouchout = builder.cancelTouchOut;
        this.title = builder.title;
        this.onClickListener = builder.onClickListener;
        this.okText = builder.okText;
        this.cancelText = builder.cancelText;



    }

    public CommonSelectDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(mView);

        setCanceledOnTouchOutside(cancelTouchout);
        Window window = getWindow();
        if (mAnimation != -1){
            window.setWindowAnimations(mAnimation);
        }else {
            window.setWindowAnimations(R.style.Animation_Popup);
        }
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        if (mHeight == 0){
            mHeight = SizeUtils.dp2px(100);
        }
        if (mWidth == 0){
            mWidth = SizeUtils.dp2px(300);
        }
        lp.height = mHeight;
        lp.width = mWidth;
        window.setAttributes(lp);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        initView(mView);


    }

    private void initView(View view) {

        TextView title = view.findViewById(R.id.tv_title);
        Button cancel = view.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(this);
        Button ok = view.findViewById(R.id.btn_ok);
        ok.setOnClickListener(this);
        title.setText(this.title);
        if (!TextUtils.isEmpty(okText)){
            ok.setText(okText);
        }
        if (!TextUtils.isEmpty(cancelText)){
            cancel.setText(cancelText);
        }
    }


    public static final class Builder {
        private Context context;
        private int themeResId = -1;
        private int animation = -1;
        private int height = 0;
        private int width = 0;
        private boolean cancelTouchOut;
        private String title;
        private OnClickListener onClickListener;
        private String okText;
        private String cancelText;




        public Builder(Context context){
            this.context = context;

        }


        public Builder setTheme(int themeResId){
            this.themeResId = themeResId;
            return this;
        }


        public Builder setAnimation(int animation){
            this.animation = animation;
            return this;
        }

        public Builder setWidth(int width){
            this.width = width;
            return this;
        }

        public Builder setHeight(int height){
            this.height = height;
            return this;
        }

        public Builder setTitle(String title){
            this.title = title;
            return this;
        }
        public Builder setOnClickListener(OnClickListener onClickListener){
            this.onClickListener = onClickListener;
            return this;
        }
        public Builder setCanceledOnTouchOutside(boolean cancelTouchOut){
            this.cancelTouchOut = cancelTouchOut;
            return this;
        }

        public Builder setOkText(String okText){
            this.okText = okText;
            return this;
        }
        public Builder setCancelText(String cancelText){
            this.cancelText = cancelText;
            return this;
        }
        public CommonSelectDialog build(){
            CommonSelectDialog commonDialog;
            if (themeResId != -1){
                commonDialog = new CommonSelectDialog(this,themeResId);
            }else {
                commonDialog = new CommonSelectDialog(this);
            }
            return commonDialog;
        }

    }


}

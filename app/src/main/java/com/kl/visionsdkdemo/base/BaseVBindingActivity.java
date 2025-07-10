package com.kl.visionsdkdemo.base;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.viewbinding.ViewBinding;

import com.kl.visionsdkdemo.R;
import com.kl.visionsdkdemo.base.utils.ShareUtils;

import java.io.File;

/**
 * Created by leopold on 2021/3/26
 * Description:
 */
public abstract class BaseVBindingActivity<VB extends ViewBinding> extends BaseActivity {

    private VB vBinding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vBinding = getViewBinding();
        setContentView(vBinding.getRoot());

    }
    protected VB getBinding(){
        return vBinding;
    }
    public abstract VB getViewBinding();


    protected void showShareFileDialog(File[] files){
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.select_file_to_share);
        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File file = files[which];
                ShareUtils.shareFile(file, BaseVBindingActivity.this,"分享到。。。");
            }
        }).create().show();
    }



}

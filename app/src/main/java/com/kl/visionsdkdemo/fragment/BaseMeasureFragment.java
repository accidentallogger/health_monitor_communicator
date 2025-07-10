package com.kl.visionsdkdemo.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.kl.visionsdkdemo.base.BaseVBindingFragment;
import com.mintti.visionsdk.common.LogUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by leopold on 2021/3/9
 * Description:
 */
public abstract class BaseMeasureFragment<VB extends ViewBinding> extends BaseVBindingFragment<VB> {


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }
    protected File createFile(String fileName){
        try {
            Date date = new Date(System.currentTimeMillis());
            File fileDir = requireContext().getExternalFilesDir("data");
            for (File file : fileDir.listFiles()) {
                if (file.getName().endsWith(fileName)){
                    file.delete();
                }
            }
            File file = new File(fileDir,
                    UUID.randomUUID().toString().substring(0,8)+"_"+fileName);
            if (file.exists()){
                file.delete();
            }
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    protected FileOutputStream createFos(File file){
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected BufferedWriter createBufferWriter(File file){
        try {
            return new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void closeFile(){};

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeFile();
    }

    @Override
    public void onStart() {
        super.onStart();
        LogUtils.d(this.getClass().getSimpleName(), "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(this.getClass().getSimpleName(), "onResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        LogUtils.d(this.getClass().getSimpleName(), "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtils.d(this.getClass().getSimpleName(), "onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LogUtils.d(this.getClass().getSimpleName(), "onDestroyView");
    }
}

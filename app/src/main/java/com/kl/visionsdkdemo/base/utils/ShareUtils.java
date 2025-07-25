package com.kl.visionsdkdemo.base.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

public class ShareUtils {

    public static void shareFile(File file, Context context,String title){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            uri = FileProvider.getUriForFile(context,"com.kl.visionsdkdemo.fileprovider",file);
        }else {
            uri = Uri.fromFile(file);
        }
        sendIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sendIntent.setType("application/octet-stream");
        context.startActivity(Intent.createChooser(sendIntent,title));
    }

}

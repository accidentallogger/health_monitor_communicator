package com.kl.visionsdkdemo.base;

import java.io.Serializable;

/**
 * Created by gaoyingjie on 2019/4/1
 * Description:
 */
public class Event implements Serializable {

    private TempValue data;

    public Event(){

    }

    public Event(TempValue data) {
        this.data = data;
    }



    public TempValue getData() {
        return data;
    }

    public void setData(TempValue data) {
        this.data = data;
    }
}

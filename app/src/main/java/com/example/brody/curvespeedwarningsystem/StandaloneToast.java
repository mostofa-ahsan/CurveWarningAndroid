package com.example.brody.curvespeedwarningsystem;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Ruben! on 7/11/2016.
 */
public class StandaloneToast implements Runnable {

    private final Context mContext;
    String mText;

    public StandaloneToast(Context mContext, String text){
        this.mContext = mContext;
        mText = text;
    }

    public void run(){
        Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
    }
}

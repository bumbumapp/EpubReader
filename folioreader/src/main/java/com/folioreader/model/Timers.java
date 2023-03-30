package com.folioreader.model;


import android.os.CountDownTimer;

import com.folioreader.Constants;


public  class  Timers {



    public static CountDownTimer timer(){
        return new CountDownTimer(180000, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                 Constants.TIMER_FINISHED=true;
            }
        };
    }

}
package com.yyz.testsg90;

import android.app.Activity;
import android.os.Bundle;
/**
 * Created by yaoyongzhen on 2017/9/27.
*/
public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String PWM_NAME = "PWM0";
        SteeringEngine se = new SteeringEngine(PWM_NAME);
        se.startSwing(0,180,100);
        try {
            //当前线程这里可以做其他的事情，摆动是在其他线程执行的
            Thread.sleep(60*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        se.stopSwing();
        se.close();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}

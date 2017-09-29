package com.yyz.testsg90;

import android.util.Log;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.Pwm;
import java.io.IOException;

/**
 * Created by yaoyongzhen on 2017/9/27.
 *
 * 该类封装了SG90舵机的简单控制方法。
 *
 * 对外的接口主要有：
 * 1.构造函数
 * public SteeringEngine(String pwmName);
 * pwmName是舵机接到的PWM口的名字。树莓派3有两个PWM口：PWM0,PWM1
 * private static final String PWM_NAME = "PWM0";
 * private static final String PWM_NAME = "PWM1";
 *
 * 2.让舵机转到指定角度，degree在[0,180]之间
 * public void setDegree(double degree);
 * public void setDegree(double degree,int gap);
 * gap的单位是毫秒，ms。
 * gap用来控制完成转动的时间，也就是控制转动的速度。
 * 一次偏转需要多个脉冲周期，每个转动的脉冲信号后，gap的时间间隔里会发出平信号。
 *
 * 3.控制舵机开始在指定角度之间摆动,会创建新的线程执行。
 * public void startSwing(final double begin , final double end, final int gapTime);
 * gapTime控制摆动的速度，最快的速度是gapTime =0；gapTime是每次脉冲信号后，发出平信号的时间间隔。
 *
 * 4.控制舵机停止摆动，回收线程资源。
 * public void stopSwing();
 *
 * 5.关闭PWM口的连接：
 * public void close();
 *
 ******使用示例，让舵机在0到180度之间摆动1min，gap时间100ms*******
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
 *
 */

public class SteeringEngine {

    private Pwm mPwm;
    private final static int STATE_START = 1;
    private final static int STATE_STOP = 2;
    private int mState = STATE_STOP;
    private Thread swingThread;
    private static final int BEGIN_PLACE = 90;

    /**
     * pwmName是舵机接到的PWM口的名字。
     * 树莓派3有两个PWM口：PWM0,PWM1
     * @param pwmName
     */
    public SteeringEngine(String pwmName){
        PeripheralManagerService manager = new PeripheralManagerService();
        try {
            mPwm = manager.openPwm(pwmName);
            mPwm.setPwmFrequencyHz(50);
            Log.w("----->", "access PWM");
        } catch (IOException e) {
            Log.w("----->", "Unable to access PWM", e);
        }
    }

    /**
     * 控制舵机转到对应的角度，degree在[0,180]之间
     * @param degree
     */
    public void setDegree(double degree){
        if(degree<0 || degree>180)
            return;
        try {
            mPwm.setEnabled(true);
            int i =18;
            while (i>0){
                i--;
                try {
                    //Log.w("----->", "degree "+ (2.5 + 10 * degree / 180));
                    mPwm.setPwmDutyCycle(2.5 + 10 * degree / 180);
                    Thread.sleep(20);
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mPwm.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 控制舵机转到对应的角度，degree在[0,180]之间.
     * gap用来控制完成转动的时间，也就是控制转动的速度。
     * gap的单位是毫秒，ms。
     * 一次偏转需要多个脉冲周期，每个转动的脉冲信号后，gap的时间间隔里会发出平信号。
     * @param degree
     * @param gap
     */
    public void setDegree(double degree,int gap){
        if(degree<0 || degree>180)
            return;
        try {
            mPwm.setEnabled(true);
            int i =18;
            while (i>0){
                i--;
                try {
                    //Log.w("----->", "degree "+ (2.5 + 10 * degree / 180));
                    mPwm.setPwmDutyCycle(2.5 + 10 * degree / 180);
                    Thread.sleep(20);
                    mPwm.setPwmDutyCycle(0);
                    Thread.sleep(gap);
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mPwm.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 控制舵机在begin和end之间摆动.
     * gapTime控制摆动的速度，最快的速度是gapTime =0；gapTime是每次脉冲信号后，发出平信号的时间间隔。
     * 会创建新的线程执行。
     * @param begin
     * @param end
     * @param gapTime
     */
    public void startSwing(final double begin , final double end, final int gapTime){
        if(begin<0 || begin>180 || end<0 || end>180 ||gapTime<0)
            return;
        if (STATE_STOP == mState) {
            mState = STATE_START;
            swingThread = new Thread() {
                @Override
                public void run() {
                    swing( begin , end, gapTime);
                }
            };
            if (null != swingThread) {
                swingThread.start();
            }
        }
    }

    private void swing(double begin , double end,int gapTime){

        if(STATE_START == mState)
            setDegree(BEGIN_PLACE);
        while (STATE_START == mState) {
            setDegree(begin,gapTime);
            setDegree(end,gapTime);
        }
    }
    /**
     * 停止摆动，回收线程资源
     */
    public void stopSwing(){
        if (STATE_START == mState) {
            mState = STATE_STOP;
            Log.d("---->", "stop start");
            if (null != swingThread) {
                try {
                    Log.d("---->", "wait recognition thread exit");
                    swingThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    swingThread = null;
                }
            }
            Log.d("---->", "stop end");
        }
    }
    /**
     * 关闭和PWM口的连接
     */
    public void close(){
        if (mPwm != null) {
            try {
                //关闭连接
                mPwm.close();
                mPwm = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

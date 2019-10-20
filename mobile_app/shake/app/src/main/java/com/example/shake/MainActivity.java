package com.example.shake;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    //private Vibrator vibrator = null;
    //private boolean isShake = false;

    Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "OnCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        Log.d(TAG, "OnCreate: Registered accelerometer listener");
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        int sensorType = event.sensor.getType();
        // values[0]:X，values[1]：Y，values[2]：Z
        float[] values = event.values;
        Log.d(TAG, "onSensorChanged: X:" + Math.abs(values[0]) + " Y: " + Math.abs(values[1]) + " Z: " + Math.abs(values[2]));
//        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
//            if ((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 || Math
//                    .abs(values[2]) > 17) && !isShake) {
//                isShake = true;
//                new Thread() {
//                    public void run() {
//                        try {
//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                    vibrator.vibrate(300);
//                                }
//                            });
//                            Thread.sleep(500);
//                            runOnUiThread(new Runnable() {
//                                public void run() {
//                                    vibrator.vibrate(300);
//                                }
//                            });
//                            Thread.sleep(500);
//                            runOnUiThread(new Runnable() {
//
//                                @Override
//                                public void run() {
//                                    // TODO Auto-generated method stub
//                                    isShake = false;
//
//                                }
//                            });
//                        } catch (InterruptedException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    };
//                }.start();
//            }
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
}

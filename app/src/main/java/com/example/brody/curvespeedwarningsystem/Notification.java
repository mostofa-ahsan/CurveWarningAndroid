package com.example.brody.curvespeedwarningsystem;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class Notification extends AppCompatActivity {


    //Test Buttons
    private Button buttonTest;
    private Button buttonTest2;
    private Button buttonIncrement;
    private Button buttonDecrement;

    //Debug fields for direction degrees and char.
    private TextView TDIR;
    private TextView TDIRCHAR;

    private int mySpeedOffset = 0;
    private int warningTick = 0;

    //UI elements
    private ImageView warningImage;
    private ImageView speedometerCircle;
    private TextView warningSpeed;
    private TextView warningSlowDown;
    private ImageView notificationImage;
    private MediaPlayer warningBeep;
    private TextView mySpeed;
    private TextView notSpeed;
    String signAdvisorySpeed;

    private double defaultSpeed = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        warningImage = (ImageView) findViewById(R.id.warningImageView);
        warningSpeed = (TextView) findViewById(R.id.speedtextViewWarn);
        warningSlowDown = (TextView) findViewById(R.id.slowDown);
        notificationImage = (ImageView) findViewById(R.id.notificationImageView);
        speedometerCircle = (ImageView) findViewById(R.id.speedometerCircle);
        warningBeep = MediaPlayer.create(this,R.raw.beep);
        mySpeed = (TextView) findViewById(R.id.mySpeedtextView);
        notSpeed = (TextView) findViewById(R.id.speedtextViewNot);
        //TDIR = (TextView) findViewById(R.id.textViewdirection);
        //TDIRCHAR = (TextView) findViewById(R.id.textViewdirection2);

        //Get object from GeofenceTransition context
        Intent i = getIntent();
        CurveSign currentSign = (CurveSign)i.getSerializableExtra("currentSign");

        if (currentSign.AdvisorySpeed != 0)
        {
            signAdvisorySpeed = Integer.toString(currentSign.AdvisorySpeed);
        }
        else if (currentSign.AdvisoryType != 0)
        {
            signAdvisorySpeed = Integer.toString(currentSign.AdvisoryType);
        }
        else
        {
            signAdvisorySpeed = Double.toString(defaultSpeed);
        }

        warningSpeed.setText(signAdvisorySpeed);
        notSpeed.setText(signAdvisorySpeed);
        
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            mySpeed.setText(String.format(Locale.ENGLISH, "%02d", (Math.round(CurveSpeedWarningSystem.
                                        locationListener.currentSpeed) + mySpeedOffset)));
                                speedometerColor();

                                // Is it an emulator thing that my location speed doesn't update?
                                //Log.i("Current Speed", mySpeed.getText().toString());
                                //Log.i("Bearing", Double.toString(CurveSpeedWarningSystem.locationListener.currentBearing));
                            /*
                            //DEBUG CODE
                            TDIR.setText("" + CurveSpeedWarningSystem.locationListener.currentBearing);
                            TDIRCHAR.setText("" + CurveSpeedWarningSystem.locationListener.
                                    convertBearingToCardinalDirection(CurveSpeedWarningSystem.
                                    locationListener.currentBearing));
                                    */
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();

        Thread closingThread = new Thread() {
            @Override
            public void run() {
                try
                {
                    double speed = CurveSpeedWarningSystem.locationListener.currentSpeed;
                    //double signSpeed = Double.parseDouble(signAdvisorySpeed);
                    int baseSpeed = 35;
                    int sleepTicks = 11000;
                    //double timeMultiplier = speed / signSpeed;
                    if (speed > baseSpeed) {
                        sleepTicks = (int) ((baseSpeed / speed) * sleepTicks);
                    }
                    Thread.sleep(sleepTicks);
                    Log.e("DEBUG: ", "Exit Activity S: " + speed + " Ticks: "+sleepTicks+ " calc: " + (baseSpeed/speed));
                    closeActivity();
                } catch (Exception e) {}
            }
        };

        closingThread.start();
        wakeScreen();
    }

    private void wakeScreen(){
        //Wake the screen up
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
        wakeLock.acquire();

        //Bypass the lock screen

        //this method was deprecated
        //KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        //KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        //keyguardLock.disableKeyguard();

        //new method
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    //Show the curve speed warning.
    private void showWarning() {
        if (notificationImage.getVisibility() == View.VISIBLE) {
            warningBeep.setLooping(true);
            warningBeep.start();
            notificationImage.setVisibility(View.INVISIBLE);
            warningImage.setVisibility(View.VISIBLE);
            warningSpeed.setVisibility(View.VISIBLE);
            warningSlowDown.setVisibility(View.VISIBLE);
        }
    }

    //Close the Notification/Warning
    private void closeActivity() {
        warningBeep.stop();
        finish();
    }

    //Used for debugging buttons
    private void addMPH(int mph) {
        mySpeedOffset = mySpeedOffset + mph;
    }

    private void speedometerColor() {
        //Change color to yellow if mySpeed > warningSpeed and <= warningSpeed + 2.
        if (Integer.parseInt(warningSpeed.getText().toString()) < Integer.parseInt(mySpeed.getText().toString())
                && Integer.parseInt(warningSpeed.getText().toString()) + 2 >= Integer.parseInt(mySpeed.getText().toString())) {
            speedometerCircle.setColorFilter(Color.YELLOW);
            warningTick = 0; //Reset the ticker.

            //Change color to green if mySpeed <= warningSpeed.
        } else if (Integer.parseInt(warningSpeed.getText().toString()) >= Integer.parseInt(mySpeed.getText().toString())){
            speedometerCircle.setColorFilter(Color.GREEN);
            warningTick = 0; //Reset the ticker.

            //Change color to red if mySpeed > warningSpeed + 2
        } else {
            speedometerCircle.setColorFilter(Color.RED);
            warningTick++;
            if (warningTick >1 ) {
                showWarning();
            }

        }
    }
}

package com.example.brody.curvespeedwarningsystem;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Created by Alex on 2/29/2016.
 */
public class GeofenceTransition extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Handler mHandler;
    private CurveSign triggeredSign;

    public GeofenceTransition() {
        super(GeofenceTransition.class.getSimpleName());
        mHandler = new Handler(); //LOlolollolol some hacking for my toast.
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geoFenceEvent = GeofencingEvent.fromIntent(intent);
        Log.e("DEBUG", "onHandleIntent");

        // Time to make another Toasty!
        mHandler.post(new StandaloneToast(this, "Geofence Hit!"));

        if (geoFenceEvent.hasError()) {
            int errorCode = geoFenceEvent.getErrorCode();
            Log.e("DEBUG", "Location Services error: " + errorCode);
        } else {
            int transitionType = geoFenceEvent.getGeofenceTransition();
            List<Geofence> trigger = geoFenceEvent.getTriggeringGeofences();

            if (Geofence.GEOFENCE_TRANSITION_ENTER == transitionType) {
                Log.e("DEBUG", "Transition enter triggered");

                Log.e("GEOFENCE TRIGGERED ID: ", trigger.get(0).getRequestId());
                triggeredSign = CurveSign.getCurveSign(trigger, CurveSpeedWarningSystem.currentSignList);
               Log.e("TRIGGERED SIGN DIR: ", triggeredSign.DirectionDegrees);
                Log.e("TRIGGERED DRIV DIR: ", ""
                        + CurveSpeedWarningSystem.locationListener.
                        convertBearingToCardinalDirection(CurveSpeedWarningSystem
                                .locationListener.currentBearing));

                // Launch notification Intent if the directions are opposite? The directions should be the same. Don't flip sign!
                // Now kids, this is how you call static methods.
                char currentDirection = CustomLocationListener.
                        convertBearingToCardinalDirection(CurveSpeedWarningSystem
                                .locationListener.currentBearing);

               char triggerDirection = triggeredSign.getDirectionDegrees().charAt(0);

                getOppositeDirection(triggeredSign.getDirectionDegrees());

                if (currentDirection == triggerDirection){
                    Intent launchNotification = new Intent(this, Notification.class);

                    launchNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchNotification.putExtra("currentSign", triggeredSign);
                    startActivity(launchNotification);
                }

            } else if (Geofence.GEOFENCE_TRANSITION_EXIT == transitionType) {
                Log.e("DEBUG", "Exit triggered");
                for (Geofence item: trigger)
                {
                    if(item.getRequestId().equals("Main"))
                    {
                        CurveSpeedWarningSystem.refreshFlag = true;
                    }

                }
            } else if (Geofence.GEOFENCE_TRANSITION_DWELL == transitionType) {
                Log.e("DEBUG", "Dwell triggered");
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static char getOppositeDirection(String DirectionDegrees){

        switch(DirectionDegrees)
        {
            case("N"): return 'S';
            case("S"): return 'N';
            case("E"): return 'W';
            case("W"): return 'E';


        }
        return 'Z';
    }
}

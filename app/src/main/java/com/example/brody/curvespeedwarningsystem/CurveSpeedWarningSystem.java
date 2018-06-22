package com.example.brody.curvespeedwarningsystem;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//import static com.example.brody.curvespeedwarningsystem.DatabaseHelper.;

public class CurveSpeedWarningSystem extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // singleton stuff yea!
    private static CurveSpeedWarningSystem csws = null;

    /**
     * This is at the top because it represents my delayed loading of this class.
     * It shouldn't happen, but if it does try to maybe wait a few(30) seconds for stuff
     * to synchronize, else just give up.
     * @return
     */
    public static CurveSpeedWarningSystem GetCSWS_Instance()
    {
        int counter = 0;
        while(csws == null)
        {
            // In case there are threading issues. Man...
            synchronized(CurveSpeedWarningSystem.class) {
                try {
                    Thread.sleep(1000);
                    counter++;

                    if (counter > 30) {
                        throw new InterruptedException("Something seriously broke, because activity wasn't initialized.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return csws;
    }

    private GoogleMap mMap;
    private GoogleApiClient client;

    private Button buttonTest;
    private Button buttonStopService;

    private boolean initialLoad;
    public static List<CurveSign> currentSignList;
    public static CustomLocationListener locationListener;
    public static LocationManager locationManager;

    public static boolean refreshFlag;

    // Database helper should be static due to Async methods that might also need to query.
    // Which I'm not a fan of btw, but it sucks :3
    public static DatabaseHelper database;

    public static double latitude;
    public static double longitude;
    public static double currentSpeed;

    private Thread refreshThread;
    //Geo-fencing variables
    List<Geofence> mGeofenceList;

    private GoogleApiClient mApiClient;

    private NotificationCompat.Builder mBuilder;

    private PendingIntent mGeofencePendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        csws = this;

        setContentView(R.layout.activity_curve_speed_warning_system);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setUpMapIfNeeded();

        //Thread to check for a refresh on the data.
        refreshFlag = false;
        // what happened to refreshes?
        refreshFlag = true;
        initialLoad = true;
        mGeofenceList = new ArrayList();

        refreshThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(5000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Log.e("THREAD: ", "THREAD SLEEP");
                                // might as well just keep updating from the local DB :)
                                // Once in a while, pull from the Sql Server...
                                if (refreshFlag == true || initialLoad == true) {
                                    //Log.e("THREAD: ", "Check fences");
                                    // what's this for again? oh the geo-fences. cool..


                                    refreshFlag = false;
                                    // let's have a toast!
                                   //Toast.makeText(getApplicationContext(), "Refreshing!", Toast.LENGTH_SHORT).show();

                                    initialLoad = false;
                                    //This ends the method for pulling from the database

                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    // some stuff happened!
                    e.printStackTrace();
                }
            }
        };

        refreshThread.start();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        //LOCATION LISTENERS
        locationListener = new CustomLocationListener();

        /*{
            // initialize this right away. Call back functions don't need to be called again.
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                currentSpeed = location.getSpeed();
            }
        };
        */

        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        // Consider network provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

        database = new DatabaseHelper(this);

        //TEST BUTTON FOR NOTIFICATION
        buttonTest = (Button) findViewById(R.id.notificationTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), Notification.class);
                intent.putExtra("currentSign", new CurveSign(
                        1,9,0,46.9054, -96.61762, "2",
                        45,0,true, 55

                ));
                startActivity(intent);
            }
        });

        //More test buttons!
        Button buttonTestLocation = (Button) findViewById(R.id.locationTest);
        buttonTestLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Location test: lat: "
                        + latitude
                        + " long: "
                        + longitude
                        + " speed: "
                        + currentSpeed,
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        // Button for testing the database pull
        Button buttonTestDatabase = (Button) findViewById(R.id.databaseTest);
        buttonTestDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String response = "Database Pulling";
                ReadFromCurveDb rfc = new ReadFromCurveDb();
                rfc.execute();
                //response.toString();
            }
        });

        //Button to stop geo-fence service
        buttonStopService = (Button) findViewById(R.id.buttonStopService);
        buttonStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });

        //The following code is for Geo-fencing
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

        //Instantiate notification icon, all the API stuff needs intialization first.
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.csw_nobg)
                .setContentTitle("Curve Speed Warning")
                .setContentText("Geofence Service Running")
                .setOngoing(true);

        // Signs moved to after initial loading is complete. Location services maybe async.
        // still need initial location ?!
        checkNewGeofences();
    }



    public GoogleMap getGoogleMapsInstance()
    {
        return mMap;
    }

    private void setUpMapIfNeeded() {
        //checks to see if the map has already been initiated
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();

            //Checks to see if checking the map was successful
            if (mMap != null) {
                onMapReady(mMap);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * Why Sydney?
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng fargo = new LatLng(46.9054, -96.61762);
        latitude = 46.9054;
        longitude = -96.61762;
        mMap.addMarker(new MarkerOptions().position(fargo).title("First Point"));

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(fargo));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fargo, 10.0f));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CurveSpeedWarningSystem Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.brody.curvespeedwarningsystem/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "CurveSpeedWarningSystem Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.brody.curvespeedwarningsystem/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Get the PendingIntent for the geofence monitoring request.
        // Send a request to add the current geofences.

        // check all geofence crap, if we don't have any in range, prevent app from crashing!
        if(mGeofenceList.size() > 0) {
            // It only hits two of the three, I wonder why? One of those might be misconfigured? IDK....
            mGeofencePendingIntent = getGeofenceTransitionPendingIntent();
            LocationServices.GeofencingApi.addGeofences(mApiClient, getGeofencingRequest(),
                    mGeofencePendingIntent);
        }

        Toast.makeText(this, "Starting Geofence service", Toast.LENGTH_SHORT).show();
        Log.e("DEBUG", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        // If the error has a resolution, start a Google Play services activity to resolve it.
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, 9000);
            } catch (IntentSender.SendIntentException e) {
                Log.e("DEBUG", "Exception while resolving connection error.", e);
            }
        } else {
            int errorCode = connectionResult.getErrorCode();
            Log.e("DEBUG", "Connection to Google Play services failed with error code " + errorCode);
        }
    }

    public void createGeofences(List<CurveSign> listSigns) {
        mMap.clear();

        for ( CurveSign sign : listSigns ) {
            //Determine geofence size
            float geofenceSize;
            try {
                geofenceSize = (float)sign.HighwaySpeed * 4.5f ;
                if (geofenceSize < 200) { // 500 feet. It gets converted later :|
                    geofenceSize = 200.0f; //minimum value
                }

                //Convert feet to meters
                geofenceSize = geofenceSize / 3.2808f;

            } catch (java.lang.NullPointerException n) {
                geofenceSize = 100.5f; //Failsafe value (meters)
            }

            mGeofenceList.add(new Geofence.Builder()
                    .setRequestId(Integer.toString(sign.Id))
                    .setCircularRegion(
                        sign.Latitude,
                        sign.Longitude,
                        geofenceSize)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    //.setNotificationResponsiveness(500)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build());

            Log.e("CREATE: ", Integer.toString(sign.Id));

            LatLng locat = new LatLng(sign.Latitude, sign.Longitude);

            mMap.addMarker(new MarkerOptions()
                    .position(locat).title("Geofence + " + sign.Id)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.cw_dot)));

            // For Ruben's debugging purposes.
            mMap.addCircle(new CircleOptions()
                    .center(locat)
                    .radius(geofenceSize)
                    .fillColor(Color.argb(10, 10, 10, 10))
                    .strokeColor(Color.argb(100, 10, 10, 10))
                    .visible(true));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(this.latitude, this.longitude), 12.0f));

        //updateCurrentLocation(locationManager);
        /* // We don't need that extra point dang...
        mGeofenceList.add(new Geofence.Builder().setRequestId("Main").setCircularRegion(latitude,
                longitude, 40233.6f).setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT).build());
        */
    }

    private void stopService() {

        removeGeofences();

        NotificationManager notManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.cancel(001);
        refreshThread.interrupt();
    }

    public void removeGeofences()
    {
        if(mGeofenceList.size() > 0) {
            LocationServices.GeofencingApi.removeGeofences(mApiClient, getGeofenceTransitionPendingIntent());
        }
    }

    public void checkNewGeofences() {
       // updateCurrentLocation(locationManager);
        //LocationServices.GeofencingApi.removeGeofences(mApiClient, getGeofenceTransitionPendingIntent());
        removeGeofences();

        // Switch to actual database reads.
        //currentSignList = database.getAllCurveSignsTEST();
        // Geofences will be added, but not to the API until the API is CONNECTED.
        currentSignList = database.readAllCurveSigns();
        createGeofences(currentSignList);

        //mGeofencePendingIntent = getGeofenceTransitionPendingIntent();
        //LocationServices.GeofencingApi.addGeofences(mApiClient, getGeofencingRequest(),
        //        mGeofencePendingIntent);
        Log.e("DEBUG: ", "refresh triggered");
    }

    // These two methods are responsible for initializing geofence pending intents.
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    private PendingIntent getGeofenceTransitionPendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        // do we need that intent blocker thing?
        Intent intent = new Intent(this, GeofenceTransition.class);

        //Log.e("DEBUG", "getPendingIntent");
        // Whats this for?
        NotificationManager notManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.notify(001, mBuilder.build());

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    /*
    // This is diabled because it adds more location listeners to updates.
    public static void updateCurrentLocation (LocationManager lm){

        LocationListener locationListener2 = new LocationListener() {
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }
        };

        // before it would update every 2 seconds every 10 meters. Or something, but seemed that it didn't update at all...
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener2);
    }
    */
}

package com.example.brody.curvespeedwarningsystem;

/**
 * Created by Nick Ellingson on 3/31/2016.
 */

import android.location.Location;
import android.os.Bundle;

public class CustomLocationListener implements android.location.LocationListener{
    private Location lastLocation;
    public double currentSpeed;
    private double actualCurrentSpeed;
    private double actualPreviousSpeed;

    public double currentBearing;
    public char currentCardinal;

    public boolean locationInitialized = false;

    public CustomLocationListener(){
        currentSpeed = 0;
        actualCurrentSpeed = 0;
        actualPreviousSpeed = 0;
    }

    /*
    *
    * This does not seem to be used :(
    *
    */
    @Override
    public void onLocationChanged(Location currentLocation) {

        actualPreviousSpeed = actualCurrentSpeed;
        this.actualCurrentSpeed = getSpeed(currentLocation);

        if(actualCurrentSpeed != 0 && actualPreviousSpeed != 0)
        {
            currentSpeed = averageSpeed(actualCurrentSpeed, actualPreviousSpeed);
        }
        else{
            currentSpeed = actualCurrentSpeed;
        }



        this.currentBearing = getBearing(currentLocation);
        this.currentCardinal = convertBearingToCardinalDirection(currentBearing);

        // seriously updating this :)
        CurveSpeedWarningSystem.latitude = currentLocation.getLatitude();
        CurveSpeedWarningSystem.longitude = currentLocation.getLongitude();

        this.lastLocation = currentLocation;
        this.lastLocation.setSpeed((float)currentSpeed);
        this.lastLocation.setBearing((float)currentBearing);
    }

    public static double averageSpeed(double previousSpeed, double currentSpeed)
    {
        return (previousSpeed + currentSpeed) / 2;
    }

    public static double calculateDistance(Location currentLocation, Location previousLocation)
    {
        return calculateDistance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                previousLocation.getLatitude(), previousLocation.getLongitude());
    }

    //formula: sqrt(x^2 + y^2)
    //where x = 69.1*(lat2 - lat1)
    //where y = 69.1*(lon2 - lon1) * cos(lat1 / 57.3)
    //Formula found here: http://www.meridianworlddata.com/distance-calculation/
    public static double calculateDistance(double currentLat, double currentLong,
                                           double previousLat, double previousLong)
    {
        double deltaX = 69.1 * (currentLat - previousLat);
        double deltaY = 69.1 * ( currentLong - previousLong)
                * Math.cos(previousLat / 57.3);
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }

    private double getSpeed(Location currentLocation){
        double speed = 0;
        //I believe the following gives us change in latitude/longitude over nanoseconds; dude no, these are milliseconds!
        if (this.lastLocation != null) {

            double distance = calculateDistance(currentLocation, lastLocation);

            int MILLISECONDS_PER_HOUR = 3600000;
            speed = distance / (currentLocation.getTime() - this.lastLocation.getTime()) * MILLISECONDS_PER_HOUR;
        }
        return speed;
    }


    //References:
    //Main reference for formula: https://web.archive.org/web/20130807050748/http://www.yourhomenow.com/house/haversine.html
    //Implementation: http://stackoverflow.com/questions/3932502/calcute-angle-between-two-latitude-longitude-points
    private double getBearing(Location currentLocation) {
        double bearing = 0;

        double currentLat = currentLocation.getLatitude();
        double currentLong = currentLocation.getLongitude();
        double previousLat;
        double previousLong;
        try{
            previousLat = lastLocation.getLatitude();
            previousLong = lastLocation.getLongitude();
            double deltaLong = currentLong - previousLong;

            double y = Math.sin(deltaLong) * Math.cos(currentLat);
            double x = Math.cos(previousLat) * Math.sin(currentLat) - Math.sin(previousLat)
                    * Math.cos(currentLat) * Math.cos(deltaLong);

            bearing = Math.atan2(y, x);

            bearing = Math.toDegrees(bearing);
            bearing = (bearing + 360) % 360;

        } catch (java.lang.NullPointerException n) {

        }

        return bearing;

    }

    //TODO: make sure the numbers are correct here, north might be like 315 degrees to 45 degrees, etc
    // Lol
    public static char convertBearingToCardinalDirection(double bearing){
        char cardinal = 'Z';
        if(bearing >= 45 && bearing <= 135)
        {
            cardinal = 'W';
        }
        else if(bearing > 135 && bearing <= 225)
        {
            cardinal = 'S';
        }
        else if(bearing > 225 && bearing <= 315)
        {
            cardinal = 'E';
        }
        else if((bearing > 315 && bearing <= 360) || bearing < 45)
        {
            cardinal = 'N';
        }
        return cardinal;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

}

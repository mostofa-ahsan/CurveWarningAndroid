package com.example.brody.curvespeedwarningsystem;

import android.util.Log;

import com.google.android.gms.location.Geofence;

import java.io.Serializable;
import java.util.List;

/**
 * Created by surya on 3/31/16.
 */
public class CurveSign implements Serializable
{
    // Names are made so serialization can happen!


    public int Id;
    public int AgencyId;
    public int HighwayID;
    public double Latitude;
    public double Longitude;
    //Geograpgy type variable "Position" is part of the GRIT_Test Database: Latitude and Longitude aren't
    public String DirectionDegrees;  //int type in database, String for geofencing testing purposes
    public int AdvisorySpeed;
    public int AdvisoryType;
    public boolean Enabled;
    public int HighwaySpeed;




    @Override
    public String toString() {
        return "CurveSign{" +
                "Id=" + Id +
                ", AgencyId=" + AgencyId +
                ", HighwayID=" + HighwayID +
                ", Latitude=" + Latitude  +
                ", Longitude=" + Longitude +
                ", DirectionDegrees='" + DirectionDegrees + '\'' +
                ", AdvisorySpeed='" + AdvisorySpeed + '\'' +
                ", AdvisoryType=" + AdvisoryType +
                ", Enabled=" + Enabled +
                ", HighwaySpeed" + HighwaySpeed +
                '}';
    }


    public CurveSign(){}

    public CurveSign(int Id, int AgencyId, int HighwayID, double Latitude, double Longitude, String DirectionDegrees,
                     int AdvisorySpeed, int AdvisoryType, boolean Enabled, int HighwaySpeed) {
        this.Id = Id;
        this.AgencyId = AgencyId;
        this.HighwayID = HighwayID;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.DirectionDegrees = DirectionDegrees;
        this.AdvisorySpeed = AdvisorySpeed;
        this.AdvisoryType = AdvisoryType;
        this.Enabled = Enabled;
        this.HighwaySpeed = HighwaySpeed;



    }

  public String getDirectionDegrees()
  {
      return DirectionDegrees;
  }
    /**
     * Not sure how this one actually works, why are we iterating through all geofences to find the
     * triggered sign? Shouldn't it be just one specific sign?
     *
     * Anyways, improvement can be done here to only to pass in 1 geofence. Since we should only trigger one at a time in general.
     */
    public static CurveSign getCurveSign(List<Geofence> listGeofence, List<CurveSign> listSigns)
    {
        for (Geofence geofence : listGeofence) {
            Log.e("geofence ids in list: ", geofence.getRequestId());
            for (CurveSign sign : listSigns) {
                Log.e("sign ids in list: ", Integer.toString(sign.Id));
                if (Integer.toString(sign.Id).equals(geofence.getRequestId()))
                {
                    // D:
                    // ) == geofence.getRequestId()) {
                    Log.e("return: ", Integer.toString(sign.Id));
                    return sign;
                }
            }
        }

        Log.e("return: ","null");
        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if(o instanceof CurveSign && o != null)
        {
            CurveSign c = (CurveSign)o;

            return this.Id == c.Id
                    && this.AgencyId == c.AgencyId
                    && this.HighwayID == c.HighwayID
                    && this.Latitude == c.Latitude
                    && this.Longitude == c.Longitude
                    && this.DirectionDegrees == c.DirectionDegrees
                    && this.AdvisorySpeed == c.AdvisorySpeed
                    && this.AdvisoryType == c.AdvisoryType
                    && this.Enabled == c.Enabled
                    && this.HighwaySpeed == c.HighwaySpeed;

        }

        return false;
    }
}

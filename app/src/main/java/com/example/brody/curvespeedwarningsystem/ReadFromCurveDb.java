package com.example.brody.curvespeedwarningsystem;

/**
 * Created by dotsc_56 on 7/12/2016.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Here you go little guy, you get your own class :)
 * Created by Ruben! on 7/08/2016.
 **/
public class ReadFromCurveDb extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {

        // params comes from the execute() call: params[0] is the url.
        try {

            // add params! ?Range=50&Lat=47.448817730&Long=-100.118603784
            //double simLat = 47.448817730;
            //double simLong = -100.118603784;
            int simRange = 100;
            String response = "";

            // Get by distance (this is smarter)
            Uri builder = new Uri.Builder()
                    .scheme("http")
                   // .encodedAuthority("134.129.126.107:3004")
                    .encodedAuthority("dotsc.ugpti.ndsu.nodak.edu:3004")
                    .path("getAllNew")
                    
                    //.appendQueryParameter("Range", Integer.toString(simRange))

                    // Warning may need locking!
                    //.appendQueryParameter("Lat", Double.toString(CurveSpeedWarningSystem.latitude))
                    //.appendQueryParameter("Long", Double.toString(CurveSpeedWarningSystem.longitude))

                    .build();

            URL url = new URL(builder.toString());
            URLConnection conn = url.openConnection();
            Log.d("test url",builder.toString());
            //conn.setDoOutput(true);
            //conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20000); //20 seconds should be enough :3
            conn.setReadTimeout(20000);

            // OutputStreamWriter outSw = new OutputStreamWriter(conn.getOutputStream());
            // conn.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String data = "";
            while((data = reader.readLine()) != null)
            {
                response += data;
            }

            reader.close();
            return response;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {

        // Write this to the local db. oh geeze :S
        // So I'm guessing the c# contract and java contracts are 1 to 1.
        List<CurveSign> signList = new ArrayList();
        //Log.d("result", result);
        try
        {
            if(result != null) {

                JSONArray jsonArray = new JSONArray(result);

                for (int i = 0; i < jsonArray.getJSONArray(0).length(); i++)
                {
                    JSONObject jsonObject = new JSONObject(jsonArray.getJSONArray(0).get(i).toString());

                    signList.add(new CurveSign(
                            Integer.parseInt(jsonObject.getString("Id")),
                            Integer.parseInt(jsonObject.getString("AgencyId")), // highway speed is missing, and I thought I could serialize it, guess not :(
                            Integer.parseInt(jsonObject.getString("HighwayID")),
                            Double.parseDouble(jsonObject.getString("Latitude")),
                            Double.parseDouble(jsonObject.getString("Longitude")),
                            jsonObject.getString("DirectionDegrees"),
                            Integer.parseInt(jsonObject.getString("AdvisorySpeed")),
                            Integer.parseInt(jsonObject.getString("AdvisoryType")),
                            Boolean.parseBoolean(jsonObject.getString("Enabled")),
                            Integer.parseInt(jsonObject.getString("HighwaySpeed"))


                    ));
                }
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Do stuff with that result!
        CurveSpeedWarningSystem.database.SynchronizeCurveSigns(signList);

        // how to call instance?
        CurveSpeedWarningSystem csws = CurveSpeedWarningSystem.GetCSWS_Instance();
        csws.removeGeofences();
        csws.createGeofences(signList);
    }
}

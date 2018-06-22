package com.example.brody.curvespeedwarningsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by surya on 4/3/16.
 * Database helper class.
 * This should be deprecated, since new database helper model is underway.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static String DB_PATH = "/data/data/com.example.brody.curvespeedwarningsystem/";
    //private static String DB_NAME = "csw";
    private static String DB_NAME = "mockdb"; // Welcome to ruben's world!
    //private static String DB_PATH = "dotsc-data.ugpti.ndsu.nodak.edu/GRIT_Test";
   // private static String DB_NAME = "GRIT_Test";
    private static  Context myContext;
    private static SQLiteDatabase myDataBase;

    /**
     * Database helper constructor method.
     *
     * @param context
     */
    public  DatabaseHelper(Context context) {
        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }



    /**
     * Open the database (or attempts to if running for the first time).
     * Checks to see if a database object has been loaded.
     *
     * @return
     */
    private static boolean checkDataBase() {

        SQLiteDatabase checkDB = null;

        try {
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        } catch (SQLiteException e) {
            // database does't exist yet.
            //e.printStackTrace();
            Log.d("Database", "Database is being loaded.");
        }catch (Exception e)
        {
            Log.d("Database", "Database is being loaded.... Some other error occurred?!");
        }

        if (checkDB != null) {
            checkDB.close();
        }

        return checkDB != null ? true : false;
    }

    /**
     * Create a new database by copying the prototype db if one does
     * not yet exist.
     *
     * @throws IOException
     */
    public void checkOrCreateDataBase() throws IOException {

        boolean dbExist = checkDataBase();

        if (!dbExist) {
            //copy stock database to directory
            this.getReadableDatabase();

            try
            {
                copyDataBase();
            }
            catch (IOException e)
            {
                throw new Error("Error copying database");
            }
        }
        // otherwise we will update it
        else {
            // Doing other stuff.
        }
    }

    /**
     * Copy over fresh database.
     *
     * @throws IOException
     */
    private static void copyDataBase() throws IOException {

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0)
        {
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    /**
     * Get curve signs from local DB.
     *
     * @return
     */
    public List<CurveSign> readAllCurveSigns() {
        final List<CurveSign> curveSigns = new ArrayList();

        OpenDatabaseOperation(new DatabaseHelperCallback() {
            @Override
            public void ExecuteQuery() {
                String query = "SELECT * FROM curve";
                //String query = "SELECT * FROM DynamicWarningSign";

                // now commence reade operation!
                Cursor cursor = myDataBase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    do {
                        CurveSign curveSign = ParseCurveSign(cursor);

                        double distance = CustomLocationListener.calculateDistance(
                                CurveSpeedWarningSystem.latitude, CurveSpeedWarningSystem.longitude,
                                curveSign.Latitude, curveSign.Longitude); // Much better :)

                        if (distance < 25) {
                            curveSigns.add(curveSign);
                            Log.e("ADDED: ", Integer.toString(curveSign.Id));
                        }

                        //100 Geo-fence limit, reserve last spot for "main" TODO: Sort and pick to 100!
                    } while (cursor.moveToNext());
                }
            }
        });

        return curveSigns;
    }

    /**
     * @param cursor
     * @return
     */
    private static CurveSign ParseCurveSign(Cursor cursor) {
        CurveSign curveSign = new CurveSign();
        //curveSign.ID = counter;

        //TODO: Uncomment this field when the database with highway_speed is implemented. Replace x with the data position and update the rest.
        //curveSign.highway_speed = cursor.getString(x);
        curveSign.Id = cursor.getInt(0);
        curveSign.AgencyId = cursor.getInt(1);
        curveSign.HighwayID = cursor.getInt(2);
        curveSign.Latitude = cursor.getDouble(3);
        curveSign.Longitude = cursor.getDouble(4);
        curveSign.DirectionDegrees = cursor.getString(5);
        curveSign.AdvisorySpeed = cursor.getInt(6);
        curveSign.AdvisoryType = cursor.getInt(7);
        curveSign.HighwaySpeed = cursor.getInt(8);

        return curveSign;
    }

    /**
     * Write or update curve signs in the database.
     * We should also have a way to deprecate signs that are removed.
     *
     * @param curveSigns
     */
    public void SynchronizeCurveSigns(final List<CurveSign> curveSigns) {
        OpenDatabaseOperation(
                new DatabaseHelperCallback() {
                    @Override
                    public void ExecuteQuery() {
                        // Scan the entire database and check if this record exists.
                        HashMap<Integer, CurveSign> localSigns = new HashMap<Integer, CurveSign>();
                        String query = "SELECT * FROM curve";

                        // now commence reade operation!

                        Cursor cursor = myDataBase.rawQuery(query, null);

                        if (cursor.moveToFirst()) {
                            do {
                                CurveSign curveSign = ParseCurveSign(cursor);
                                localSigns.put(curveSign.Id, curveSign);
                            }
                            while (cursor.moveToNext());
                        }

                        for (CurveSign cs : curveSigns) {

                            // commence insertion!
                            if (!localSigns.containsKey(cs.Id)) {
                                InsertCurveSign(cs);
                            } else if (localSigns.containsKey(cs.Id) && cs.Enabled) {
                                // decide whether to update?
                                UpdateCurveSign(cs, localSigns);
                            }
                            // commence deletion!
                            else {
                                // Note: this only deletes in database, don't delete from list, or you'll give me a headache! :S
                                DeleteCurveSign(cs);
                            }
                        }
                    }
                }
        );
    }

    /**
     * Insert
     * Note: No need to call on the dank DB stuff again. I got this covered.
     *
     * @param curveSign
     */
    private static void InsertCurveSign(CurveSign curveSign) {

        ContentValues cv = new ContentValues();
        cv.put(CurveEntry.COLUMN_NAME_Id, curveSign.Id);
        cv.put(CurveEntry.COLUMN_NAME_AgencyId, curveSign.AgencyId);
        cv.put(CurveEntry.COLUMN_NAME_HighwayID, curveSign.HighwayID);
        cv.put(CurveEntry.COLUMN_NAME_X, curveSign.Latitude);
        cv.put(CurveEntry.COLUMN_NAME_Y, curveSign.Longitude);
        cv.put(CurveEntry.COLUMN_NAME_DirectionDegrees, curveSign.DirectionDegrees);
        cv.put(CurveEntry.COLUMN_NAME_AdvisorySpeed, curveSign.AdvisorySpeed);
        cv.put(CurveEntry.COLUMN_NAME_AdvisoryType, curveSign.AdvisoryType);
        cv.put(CurveEntry.COLUMN_NAME_HighwaySpeed, curveSign.HighwaySpeed);




        myDataBase.insert(CurveEntry.TABLE_NAME, null, cv);
    }

    /**
     * Update
     *
     * @param curveSign
     */
    private static void UpdateCurveSign(CurveSign curveSign, HashMap<Integer, CurveSign> localSigns) {
        // compare curve sign with existing
        if (!curveSign.equals(localSigns.get(curveSign.Id))) {
            // There is a discrepancy!

            ContentValues cv = new ContentValues();
            cv.put(CurveEntry.COLUMN_NAME_Id, curveSign.Id);
            cv.put(CurveEntry.COLUMN_NAME_AgencyId, curveSign.AgencyId);
            cv.put(CurveEntry.COLUMN_NAME_HighwayID, curveSign.HighwayID);
            cv.put(CurveEntry.COLUMN_NAME_X, curveSign.Latitude);
            cv.put(CurveEntry.COLUMN_NAME_Y, curveSign.Longitude);
            cv.put(CurveEntry.COLUMN_NAME_DirectionDegrees, curveSign.DirectionDegrees);
            cv.put(CurveEntry.COLUMN_NAME_AdvisorySpeed, curveSign.AdvisorySpeed);
            cv.put(CurveEntry.COLUMN_NAME_AdvisoryType, curveSign.AdvisoryType);
            cv.put(CurveEntry.COLUMN_NAME_HighwaySpeed, curveSign.HighwaySpeed);



            String[] args = { Integer.toString(curveSign.Id) };
            myDataBase.update(CurveEntry.TABLE_NAME, cv, CurveEntry.COLUMN_NAME_Id + " = ?", args);
        }
    }

    /**
     * Delete
     *
     * @param curveSign
     */
    private static void DeleteCurveSign(CurveSign curveSign)
    {
        String[] args = { Integer.toString(curveSign.Id) };
        myDataBase.delete(CurveEntry.TABLE_NAME, CurveEntry.COLUMN_NAME_Id + " = ?", args);
    }

    /**
     * My callback helper class to take care of tedious database operations.
     *
     * @param helperCallback
     */
    private void OpenDatabaseOperation(DatabaseHelperCallback helperCallback) {
        try {
            // Check if DB exists
            checkOrCreateDataBase();

            // Then open it for read/write.
            String myPath = DB_PATH + DB_NAME;
            myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);

            // do stuff in here. I might make an anonymous function for fun.
            helperCallback.ExecuteQuery();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // whatever else can go wrong print it out here.
            e.printStackTrace();
        } finally {
            // always close when done. (note to self, maybe that sync thing down there does it?
            myDataBase.close();
        }

    }

    @Override
    public synchronized void close() {
        if (myDataBase != null) {
            myDataBase.close();
        }

        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private interface DatabaseHelperCallback {
        void ExecuteQuery();
    }

    public static abstract class CurveEntry implements BaseColumns {
        // Schema.
        public static final String TABLE_NAME = "Curve";
        public static final String COLUMN_NAME_HighwayID = "HighwayID";
        public static final String COLUMN_NAME_AgencyId = "AgencyId";
        public static final String COLUMN_NAME_AdvisorySpeed = "AdvisorySpeed";
        public static final String COLUMN_NAME_AdvisoryType = "AdvisoryType";
        public static final String COLUMN_NAME_DirectionDegrees = "DirectionDegrees";
        public static final String COLUMN_NAME_X = "x"; // Longitude
        public static final String COLUMN_NAME_Y = "y"; // Latitude
        public static final String COLUMN_NAME_Id = "Id";
        public static final String COLUMN_NAME_HighwaySpeed = "HighwaySpeed";
    }
}

package com.artmap.manzo.artmap;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.ParseObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

/**
 * Created by Manzo on 06/01/2015.
 */
public class ArtEntriesDbHelper extends SQLiteOpenHelper {


    public static final String DATABASE_NAME = "ArtDatabase.db";
    public static final String DB_PATH = "/data/data/"+ "com.artmap.manzo.artmap" +"/databases/";

    //If database schema is changed, increment the database version
    public static final int DATABASE_VERSION = 10;

    private static Context myContext = null;

    public ArtEntriesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (!doesDbExists()) {
            db.execSQL(ArtDatabaseContract.SQL_CREATE_ENTRIES);
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE artDb ADD COLUMN checkin TEXT DEFAULT false");
        }

//        switch (oldVersion) {
//            case 10:
//
//        }
    }

    void copyDbFromAssets() throws Exception {


        AssetManager assetManager = myContext.getAssets();
        OutputStream outputStream = new FileOutputStream(String.valueOf(myContext.getDatabasePath("ArtDatabase.db")));
        byte[] bytes = new byte[100];

        int readInt;
        InputStream inputStream = assetManager.open("dbWritten.db");
        while ((readInt = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, readInt);
        }
        Log.d("DATABASE_HELPER", "Copying the database ");
        inputStream.close();
        outputStream.close();
    }

    public void readFromDb(ClusterManager<MyClusterItem> mClusterManager) {

        SQLiteDatabase db = getReadableDatabase();

        // Define a projection of what columns you will use after this query
        String[] projection = {
                ArtDatabaseContract.ArtEntries._ID,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_USER,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_LATITUDE,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_LONGITUDE,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_SNIPPET,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_TITLE,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHOR,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHORLINK,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_YEAR,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_VISIBILITY,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_TAG,
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN
        };

        //How you want the result sorted in the resulting Cursor
        String sortOrder =
                ArtDatabaseContract.ArtEntries._ID + " DESC";




        //Cursor
        Cursor cursor = null;
        cursor = db.query(
                ArtDatabaseContract.ArtEntries.TABLE_NAME,  //The table to query
                projection,                                 //The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order

        );

//        Iteration trough database: The cursor starts BEFORE the first result row, so on the first
//        iteration this moves to the first result if it exists. If the cursor is empty, or the last
//        row has already been processed, then the loop exits neatly.

        //cursor.moveToFirst();
        while (cursor.moveToNext()) {
            mClusterManager.addItem(new MyClusterItem(
                            cursor.getInt(0),           //ID
                            cursor.getString(1),        //USER
                            cursor.getDouble(2),        //LAT
                            cursor.getDouble(3),        //LON
                            cursor.getString(4),      //SNIPPET
                            cursor.getString(5),      //TITLE
                            cursor.getString(6),    //AUTHOR
                            cursor.getString(7),    //AUTHORLINK
                            cursor.getInt(8),       //YEAR
                            cursor.getInt(9),       //VISIBILITY
                            cursor.getString(10),    //TAG
                            cursor.getString(11)    //CHECKIN
                    )
            );

        }

        cursor.close();


    }

    /////////////// search for single item /////////////////
    public boolean isAlreadyChecked (int id) throws SQLException {


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] projection = {
                ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN
        };
        String[] whereArgs = {
                String.valueOf(id)
        };

        cursor = db.query(
                ArtDatabaseContract.ArtEntries.TABLE_NAME,
                projection,
                "_id = ?",
                whereArgs, // replaces ? in WHERE statement
                null,
                null,
                null
        );


        String result = "false";
        while (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        cursor.close();

        return Boolean.parseBoolean(result);
    }


    /////////////// search for single item /////////////////
    public String readStringCellFromId(int id, String columnName) throws SQLException {


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] projection = {
                columnName
        };
        String[] whereArgs = {
                String.valueOf(id)
        };

        cursor = db.query(
                ArtDatabaseContract.ArtEntries.TABLE_NAME,
                projection,
                "_id = ?", whereArgs, // replaces ? in WHERE statement
                null, null,
                null
        );


        String result = "No result";
        while (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        cursor.close();


        return result;
    }

    /////////////// search for multiple of the same /////////////////
    public int countSameInColumn(String stringToFind, String columnName) throws SQLException {


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] projection = {
                columnName
        };
        String[] whereArgs = {
                stringToFind
        };


        cursor = db.query(
                ArtDatabaseContract.ArtEntries.TABLE_NAME,
                projection,
                columnName + " LIKE ?", whereArgs, // replaces ? in WHERE statement
                null, null,
                null
        );


        cursor.moveToNext();
        return cursor.getCount();


    }




    /////////////// get last row itemId /////////////////
    public String getLastItemId () throws SQLException {


        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String[] projection = {
                ArtDatabaseContract.ArtEntries._ID
        };

        cursor = db.query(
                ArtDatabaseContract.ArtEntries.TABLE_NAME,
                projection,
                null, null,
                null, null,
                ArtDatabaseContract.ArtEntries._ID + " DESC", "1"
        );



        String result = "0";
        while (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        cursor.close();

        return result;
    }

    ////// Get number of rows of table //////

    public int countRows() {

        SQLiteDatabase db = getReadableDatabase();
        int restult = (int) DatabaseUtils.queryNumEntries(db, ArtDatabaseContract.ArtEntries.TABLE_NAME);
        return restult;
    }

    ////////////////////// Insert data in single row

    public void insertInRow (int rowId, String columnName, String dataToInsert) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues dataValues = new ContentValues();
        dataValues.put(columnName, dataToInsert);

        String[] whereArgs = new String[] {String.valueOf(rowId)};

        int rowsEffected = db.update(ArtDatabaseContract.ArtEntries.TABLE_NAME,
                dataValues,
                "_id = ?",
                whereArgs
        );
        Log.d("Updated rows:", String.valueOf(rowsEffected));
    }

    public void addRandomsToDb(Integer n) {


        int i;
        for (i=0; i<n ; i++) {
            ContentValues values = new ContentValues();
            LatLng randomCoords = randomCoordsGenerator();

            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TITLE, "Marker " + i);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LATITUDE, randomCoords.latitude);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LONGITUDE, randomCoords.longitude);

            getWritableDatabase().insert(ArtDatabaseContract.ArtEntries.TABLE_NAME, null, values);


        }

    }

    ///////////////////////// createNewRow ///////////////////////////////
    public void createNewRow(
            String user,
            long latitude,
            long longitude,
            String snippet,
            String title,
            String author,
            String authorlink,
            int year,
            int visibility,
            String tag
            ) {


        ContentValues values = new ContentValues();
//
//                ArtDatabaseContract.ArtEntries._ID,

            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_USER, user);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LATITUDE, latitude);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LONGITUDE, longitude);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_SNIPPET, snippet);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TITLE, title);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHOR, author);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHORLINK, authorlink);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_YEAR, year);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_VISIBILITY, visibility);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TAG, tag);
            values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN, "false");


            getWritableDatabase().insert(ArtDatabaseContract.ArtEntries.TABLE_NAME, null, values);


    }

    public boolean createNewRowFromParseObject(ParseObject parseObject) {


        ContentValues values = new ContentValues();

        String user = parseObject.getString("user");

        values.put(ArtDatabaseContract.ArtEntries._ID, String.valueOf(parseObject.getInt("artId")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_USER, user);
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LATITUDE, String.valueOf(parseObject.getDouble("latitude")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LONGITUDE, String.valueOf(parseObject.getDouble("longitude")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_SNIPPET, parseObject.getString("snippet"));

        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TITLE, parseObject.getString("title"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHOR, parseObject.getString("author"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHORLINK, parseObject.getString("authorlink"));

        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_YEAR, String.valueOf(parseObject.getInt("year")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_VISIBILITY, String.valueOf(parseObject.getInt("visibility")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TAG, parseObject.getString("tag"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN, "false");


        SQLiteDatabase db = getWritableDatabase();
        long rowInserted = db.insert(ArtDatabaseContract.ArtEntries.TABLE_NAME, null, values);

        SharedPreferences sharedPreferences = myContext.getSharedPreferences("LoginStatus", 0);
        String currentUser = sharedPreferences.getString("Username", "");
        return currentUser != null && currentUser.equals(user);

    }


    public boolean udpateRowFromParseObject(ParseObject parseObject) {


        ContentValues values = new ContentValues();

        String user = parseObject.getString("user");
        int id = parseObject.getInt("artId");
        Log.wtf("Id Inserted", "-"+id+"-");

//        values.put(ArtDatabaseContract.ArtEntries._ID, id);
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_USER, user);
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LATITUDE, String.valueOf(parseObject.getDouble("latitude")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_LONGITUDE, String.valueOf(parseObject.getDouble("longitude")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_SNIPPET, parseObject.getString("snippet"));

        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TITLE, parseObject.getString("title"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHOR, parseObject.getString("author"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_AUTHORLINK, parseObject.getString("authorlink"));

        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_YEAR, String.valueOf(parseObject.getInt("year")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_VISIBILITY, String.valueOf(parseObject.getInt("visibility")));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_TAG, parseObject.getString("tag"));
        values.put(ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN, "false");


        SQLiteDatabase db = getWritableDatabase();
//        long rowInserted = db.replace(ArtDatabaseContract.ArtEntries.TABLE_NAME, null, values);
        db.update(ArtDatabaseContract.ArtEntries.TABLE_NAME, values, "_id = ?", new String[]{String.valueOf(id)});

        SharedPreferences sharedPreferences = myContext.getSharedPreferences("LoginStatus", 0);
        String currentUser = sharedPreferences.getString("Username", "");
        return currentUser.equals(user);

    }


    private LatLng randomCoordsGenerator () {

        double latMin = 41.880000;
        double latMax = 41.899999;
        double lat = Math.random() * (latMax - latMin) + latMin;

        double lngMin = 12.480000;
        double lngMax = 12.499999;
        double lng = Math.random() * (lngMax - lngMin) + lngMin;


        return new LatLng(lat, lng);

    }




    //////////////////// Check if DB already exists /////////////////

    public static boolean doesDbExists() {
        File dbFile;
        dbFile = myContext.getDatabasePath(DATABASE_NAME);
        return dbFile.exists();
    }










}

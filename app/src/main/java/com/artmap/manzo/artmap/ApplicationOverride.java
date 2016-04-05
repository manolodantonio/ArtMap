package com.artmap.manzo.artmap;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseFacebookUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by manolo on 03/03/2015.
 */
public class ApplicationOverride extends Application {


    static SharedPreferences firstRun;
    static ArtEntriesDbHelper mDbHelper;

    @Override
    public void onCreate() {
        super.onCreate();

    ///////////////// DATABASE INITIALIZE ////////////
        //create new empty database


        mDbHelper = new ArtEntriesDbHelper(this);
        Boolean dbExists = ArtEntriesDbHelper.doesDbExists();

        if (!dbExists) {

            //calling getWritable or getReadable on SQLopenHelper will create DB if not exists.
            mDbHelper.getWritableDatabase();

            firstRun = getSharedPreferences("com.artmap.manzo.artmap", MODE_PRIVATE);

            if (firstRun.getBoolean("firstRun", true)) {

                try {
                    mDbHelper.copyDbFromAssets();
                } catch (Exception e) {
                    e.printStackTrace();
                }


//                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//                String firstDate = simpleDateFormat.format(new Date());
                String firstDate = "2015-06-08T10:23:17.361Z";
                firstRun.edit().putString("LastUpdated", firstDate).apply();
                firstRun.edit().putBoolean("firstRun", false).apply();


            }
        }

    //////////////// PARSE.COM INITIALIZE //////////////////
        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
//        Parse.initialize(this, "xcOaOqfRorTUBCeSMxXskMZnvRPZ9lJRbmjKWRD0", "ZckF2p9D9QhLrmXVxl6EqPu3AhgRVyDsYFIuJQpD"); // Copy
        Parse.initialize(this, "xcOaOqfRorTUBCeSMxXskMZnvRPZ9lJRbmjKWRD0", "ZckF2p9D9QhLrmXVxl6EqPu3AhgRVyDsYFIuJQpD");
        ParseFacebookUtils.initialize(this);


    /////////////// FACEBOOK INITIALIZE ////////////////////

        //Link to Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

    }


    private void deleteDatabase() {
        this.deleteDatabase("ArtDatabase.db");
        String TAG = "tag";
        Log.d(TAG, "DB Deleted");
    }




}

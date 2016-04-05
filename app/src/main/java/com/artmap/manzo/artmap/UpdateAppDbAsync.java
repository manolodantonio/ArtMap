package com.artmap.manzo.artmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Manzo on 03/07/2015.
 */
public class UpdateAppDbAsync extends AsyncTask <int[], String, String> {

    private Context mContext;
    private Context mEndContext;
    public UpdateAppDbAsync(Context startContext){

        mContext = startContext;
    }


    final ArtEntriesDbHelper mDbHelper = ApplicationOverride.mDbHelper;
    int listSize;
    int localDbLenght;
    int rowsDifference;
    int myPhotoCounter = 0;
    String userFromPreferences;


    @Override
    protected String doInBackground(int[]... params) {
        final String[] result = {"no result"};

//        //Check Connectivity
//        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
//        boolean isConnected = networkInfo != null &&
//                                networkInfo.isConnectedOrConnecting();


        if (isConnected(mContext)) {


            // Start Spash screen
            // Get Parse Rows
            final int[] serverDbLenght = new int[1];
            final Date[] lastUpdated = new Date[1];
            ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery("MainDB");
            parseQuery.orderByDescending("artId");
            parseQuery.getFirstInBackground(new GetCallback<ParseObject>() {
                @Override
                public void done(ParseObject parseObject, ParseException e) {
                    if (e != null) {
                        Log.wtf("Parse error", e.getMessage());
                        Toast.makeText(mContext, R.string.error_connection_update, Toast.LENGTH_SHORT).show();
                    } else {
                        serverDbLenght[0] = parseObject.getInt("artId") + 1;
                        Log.wtf("SERVER ROWS", String.valueOf(serverDbLenght[0]));

                        // Count internal DB rows
                        localDbLenght = mDbHelper.countRows();
                        Log.wtf("LOCAL ROWS", String.valueOf(localDbLenght));

                        if (serverDbLenght[0] > localDbLenght) {
                            rowsDifference = (serverDbLenght[0] - localDbLenght);

                            ParseQuery<ParseObject> queryForMissing = ParseQuery.getQuery("MainDB");
                            queryForMissing.orderByDescending("artId").setLimit(rowsDifference);

                            queryForMissing.findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> list, ParseException e) {
                                    if (e != null) {
                                        Log.wtf("Parse error", e.getMessage());
                                        Toast.makeText(mContext, R.string.error_connection_update, Toast.LENGTH_SHORT).show();
                                    } else {
                                        listSize = list.size();
                                        Log.wtf("Returned objects", String.valueOf(listSize));
                                        int listcounter = listSize - 1;


                                        SharedPreferences sharedPreferences = mContext.getSharedPreferences("LoginStatus", 0);
                                        userFromPreferences = sharedPreferences.getString("Username", "noUser");

                                        myPhotoCounter = 0;

                                        while (listcounter != -1) {
                                            mDbHelper.createNewRowFromParseObject(list.get(listcounter));
                                            int id = list.get(listcounter).getInt("artId");
                                            Log.wtf("Added object with ID", String.valueOf(id));

                                            // Check if downloaded photo has current user as photografer
                                            String userFromObject = list.get(listcounter).getString("user");
                                            if (!userFromPreferences.equals("noUser") &&
                                                    userFromPreferences.equals(userFromObject)) {

                                                SharedPreferences.Editor editor = sharedPreferences.edit();

                                                int incrementCounter = sharedPreferences.getInt(userFromPreferences + "_UploadedPhotosCounter", 0);
                                                editor.putInt(userFromPreferences + "_UploadedPhotosCounter", incrementCounter + 1);
//
//                                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//                                                String currentTime = simpleDateFormat.format(new Date());
//                                                editor.putString("LastUpdated", currentTime);

                                                editor.commit();
                                                myPhotoCounter = myPhotoCounter + 1;

                                                Log.wtf("UserPhoto!!", userFromObject);
                                            }


                                            listcounter = listcounter - 1;
                                        }

                                        checkForDbMods(serverDbLenght[0], rowsDifference);

                                        if (!userFromPreferences.equals("noUser") ) { //Todo: inserire  && myPhotoCounter != 0  quando gli utenti si stabilizzano. 18.09
                                            updateUserData();
//                                            LoginActivity.setUsernameScore(sharedPreferences); //Moved in updateUserData
                                        }


//
                                        if (MapsActivity.mClusterManager != null) {
                                            MapsActivity.mClusterManager.clearItems();
                                            mDbHelper.readFromDb(MapsActivity.mClusterManager);
                                            MapsActivity.mClusterManager.cluster();
                                            Log.wtf("Clusterer", "Reclustered");
                                        } else {
                                            Log.wtf("Clusterer", "not instantiated");
                                        }


                                        Log.wtf("Update", "Complete");
                                        result[0] = "UPDATED";

//                                    ParseObject newObject;
//                                    newObject = list.get(0);
//                                    int id = newObject.getInt("artId");
//                                    Log.wtf("First returned id", String.valueOf(id));


                                    }
                                }
                            });
                        } else {
                            Log.wtf("Database", "Up to date");
                            checkForDbMods(serverDbLenght[0], rowsDifference);

                        }

                    }
                }
            });
        } else {
            Log.wtf("Connection", "Not available");
        }


        return result[0];
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);



        //Do semething when get an user updated photo
//        if (myPhotoCounter) {
//            Toast.makeText(mContext, "PHOTO ON THE MAP", Toast.LENGTH_SHORT).show();
//        }

//        mDbHelper.readFromDb(MapsActivity.mClusterManager);
//        MapsActivity.mClusterManager.cluster();

//        String check = mDbHelper.checkFieldFromId(50, ArtDatabaseContract.ArtEntries._ID);
//
//        Log.wtf("Check", check);


    }

    private void checkForDbMods (int serverDbLenght, int rowsDifference) {

        // Check for mods to db entries

        //Get lastUpdated Date
        SharedPreferences applicationSharedPreferences = mContext.getSharedPreferences("com.artmap.manzo.artmap", 0);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); //Parse.com format
        String lastUpdated = applicationSharedPreferences.getString("LastUpdated",
                simpleDateFormat.format(new Date())
        );
        Date dateLastUpdate = null;
        try {
            dateLastUpdate = simpleDateFormat.parse(lastUpdated);
        } catch (java.text.ParseException e1) {
            e1.printStackTrace();
        }
        //Search for not downloaded updates
        if (dateLastUpdate != null) {
            applicationSharedPreferences.edit().putString("LastUpdated", simpleDateFormat.format(new Date())).apply(); //set last update check to NOW if noone found
            ParseQuery<ParseObject> queryForUpdate = ParseQuery.getQuery("MainDB");
            queryForUpdate.orderByDescending("updatedAt")
                    .whereLessThanOrEqualTo("artId", (serverDbLenght - rowsDifference))
                    .whereGreaterThan("updatedAt", dateLastUpdate);
            queryForUpdate.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (e == null) {
                        int size = list.size();
                        Log.wtf("Modified entries found", String.valueOf(size));
                        while (size != 0) {
                            String updateFound = String.valueOf(list.get(size - 1).get("artId"));
                            Log.wtf("Updated Entry", updateFound);

                            mDbHelper.udpateRowFromParseObject(list.get(size - 1));

                            size = size - 1;
                        }
                    } else {
                        Log.e("Parse Error", e.getMessage());
                    }
                }
            });

        }

    }


    private void updateUserData () {
        final SharedPreferences sharedPreferences = mContext.getSharedPreferences("LoginStatus", 0);
        int photoCounter = sharedPreferences.getInt(userFromPreferences + "_UploadedPhotosCounter", 0);
        ParseUser user = ParseUser.getCurrentUser();
        user.fetchInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null) {
                    int userReports = parseObject.getInt("reports");
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("ReportsCounter", userReports).commit();

                    LoginActivity.setUsernameScore(sharedPreferences);

                } else {
                    Log.e("Parse Error", e.getMessage());
                }
            }
        });
        user.put("publishedPhotos", photoCounter);
        user.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.wtf("Parse user", "updated");
                } else {
                    Log.e("Parse error", e.getMessage());
                }
            }
        });
    }

    public static boolean isConnected(Context context) {
        //Check Connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null) && (networkInfo.isConnectedOrConnecting() );
    }

}

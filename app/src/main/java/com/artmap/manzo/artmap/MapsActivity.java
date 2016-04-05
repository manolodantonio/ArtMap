package com.artmap.manzo.artmap;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //Take camera picture initialize
    private static final int CARD_REQUEST = 4489;
    private static final int LOGIN_REQUEST = 7618;
    private static final int CAMERA_PIC_REQUEST = 1597;
    private static final File PATH_TO_PUBLIC_PICTURES =
            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                    File.separator +
                    "ArtMap" +
                    File.separator
            );
    public static String mCurrentPhotoPath;
    public static String mCurrentImagePath;
    public static String mCurrentFilename;

    double mCurrentLatitude;
    double mCurrentLongitude;
    int locationAccuracy;

    static boolean mUserIsLogged;
    static boolean mIsActivityRestarting;

    SharedPreferences loginStatusPreferences;
    boolean mItemIsChecked;
    boolean markerIsShown;
    int mLastClickedItemId;
    double mCurrentItemLatitude;
    double mCurrentItemLongitude;


    //Declare Map and ApiClient
    public static GoogleMap mMap; // Might be null if Google Play services APK is not available.
    GoogleApiClient mGoogleApiClient = null;

    //Declare a variable for the cluster manager
    public static ClusterManager<MyClusterItem> mClusterManager = null;


    //New DB
    ArtEntriesDbHelper mDbHelper = ApplicationOverride.mDbHelper;





    //Flag for open dialog windows
    static Dialog currentDialog;
    static Boolean userDismissed = true;
    MyClusterItem savedClickedClusterItem = null;
    CameraPosition savedCameraPosition = null;

    //Static reference to TextView showing username
    public static TextView userNameTV;
    public static TextView userScoreTV;

    //Static refs for admin panel
    public static Button adminButton;
    public static LinearLayout adminWrap;
    public static LinearLayout adminButtonWrap;
    public static LinearLayout navWrap;
    public static Button adminButtonRotate;
    public static Button adminButtonOk;
    public static Button adminButtonNo;
    public static Button adminButtonClose;
    public static ImageView adminClickableImage;
    public static TextView adminLoadingImageTxt;

    //FABs
    FloatingActionButton checkInButton;
    FloatingActionButton checkFailButton;
    FloatingActionButton checkDoneButton;
    FloatingActionButton navigateButton;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set Layout xml
        setContentView(R.layout.activity_maps);
        //setup Api Client
        buildGoogleApiClient();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //saved state resume
        if (savedInstanceState != null) {

//            mapFragment.getMapAsync(new OnMapReadyCallback() {
//                @Override
//                public void onMapReady(GoogleMap googleMap) {
//                    mMap = googleMap;
//                }
//            });

            mMap = null; //Todo Find the problem. Actually best solution.

            //
            mCurrentPhotoPath = savedInstanceState.getString("CurrentPhotoPath");
            mCurrentImagePath = savedInstanceState.getString("CurrentImagePath");
            mCurrentFilename = savedInstanceState.getString("CurrentFilename");

//            locationIsBeingTracked = savedInstanceState.getBoolean("LocationIsBeingTracked");
//            if (locationIsBeingTracked) {
//                startLocationTracking();
//                currentLat = savedInstanceState.getDouble("CurrentLat");
//                currentLng = savedInstanceState.getDouble("CurrentLng");
//            }

            //camera position
            savedCameraPosition = savedInstanceState.getParcelable("CurrentCameraPosition");
//            if (savedCameraPosition != null) {
//                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPosition));
//            }

            //image popup
            savedClickedClusterItem = savedInstanceState.getParcelable("ClickedMarker");
            if (savedClickedClusterItem != null) {
                showImageDialog(this, savedClickedClusterItem);
            }
        }
//        else { //Retain
//            mapFragment.setRetainInstance(true);
//        }


        //setup Map if needed
        setUpMapIfNeeded();

        mUserIsLogged = userIsLogged();

        ImageButton photoButton = (ImageButton)findViewById(R.id.btn_photo);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mUserIsLogged) {


                    Intent cameraIntent = new Intent(MapsActivity.this, PhotoActivity.class);
                    startActivity(cameraIntent);


                } else {
                    Toast.makeText(MapsActivity.this, R.string.toast_photoregister, Toast.LENGTH_LONG).show();
                }
            }
        });

        //User/Login Button Click

        ImageButton btn_login = (ImageButton)findViewById(R.id.imgbtn_login);
        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mUserIsLogged) {
                    Intent userIntent = new Intent(MapsActivity.this, UserProfileActivity.class);
                    userIntent.putExtra("userName", "currentUser");
                    startActivity(userIntent);
                } else {
                    Intent loginIntent = new Intent(MapsActivity.this, LoginActivity.class);
                    loginIntent.putExtra("USER_IS_LOGGED", userIsLogged());
                    startActivityForResult(loginIntent, LOGIN_REQUEST);
                }

            }
        });

        userNameTV = (TextView)findViewById(R.id.tv_userNameShow);
        userScoreTV = (TextView)findViewById(R.id.tv_userScoreShow);

        //Setup button appearance
        if (mUserIsLogged) {

            if (loginStatusPreferences == null) {
                loginStatusPreferences = MapsActivity.this.getSharedPreferences("LoginStatus", 0);
            }
            String currentUsername = loginStatusPreferences.getString("Username", "");
            LoginActivity.setUsernameButton(currentUsername);

            LoginActivity.setUsernameScore(loginStatusPreferences);

            if (LoginActivity.isAdministrator(getApplicationContext())) {
                setAdministratorInterface();
                LoginActivity.setAdministrator(MapsActivity.this, getLayoutInflater());
            }
//
//            userScoreTV.setText(String.valueOf(
//                    (loginStatusPreferences.getInt("CheckInCounter", 0) * 20) +
//                    (loginStatusPreferences.getInt(currentUsername + "_UploadedPhotosCounter", 0) * 100) +
//                    (loginStatusPreferences.getInt("Reports", 0) * 50)
//                    )
//            );


        }

        //Setup button behaviour
        userNameTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUserIsLogged) {
                    Intent userIntent = new Intent(MapsActivity.this, UserProfileActivity.class);
                    userIntent.putExtra("userName", "currentUser");
                    startActivity(userIntent);
                } else {
                    Intent loginIntent = new Intent(MapsActivity.this, LoginActivity.class);
                    loginIntent.putExtra("USER_IS_LOGGED", userIsLogged());
                    startActivity(loginIntent);
                }

            }
        });




        checkInButton = (FloatingActionButton)findViewById(R.id.fab_checkin);
        checkFailButton = (FloatingActionButton)findViewById(R.id.fab_checkin_forbidden);
        checkDoneButton = (FloatingActionButton)findViewById(R.id.fab_checkin_done);




        FloatingActionButton myLocationButton = (FloatingActionButton)findViewById(R.id.fab_mylocation);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomToCurrentPosition();

            }
        });

//        navWrap = (LinearLayout)findViewById(R.id.ll_fabNav_wrap);
        navigateButton = (FloatingActionButton)findViewById(R.id.fab_navigate);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) navigateButton.getLayoutParams();
//            p.setMargins(0, 0, 0, 0); // get rid of margins since shadow area is now the margin
//            navigateButton.setLayoutParams(p);
            navigateButton.setTranslationY(dpToPx(getApplicationContext(), 48));
//            navigateButton.bringToFront();
        }


    }






    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);


        userDismissed = false;
        if (currentDialog != null) {
            currentDialog.dismiss();
        }

        outState.putString("CurrentPhotoPath", mCurrentPhotoPath);
        outState.putString("CurrentFilename", mCurrentFilename);
        outState.putString("CurrentImagePath", mCurrentImagePath);

        //store camera position
        if (mMap != null) {
            outState.putParcelable("CurrentCameraPosition", mMap.getCameraPosition());
        }

        //if popup is open, store clicked item
        if (savedClickedClusterItem != null) {
            outState.putParcelable("ClickedMarker",  savedClickedClusterItem);

        }


    }


//
//    @Override
//    protected void onRestart() {
//        super.onRestart();
//
////        mIsActivityRestarting = true;
//
//        Log.wtf("OnRestart", "Done");
//
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
////        if (mIsActivityRestarting) {
////            mMap = null;
////            Log.wtf("GoogleMap", "Nullified, forced Setup");
////            setUpMapIfNeeded();
////        }
////
////        mIsActivityRestarting = false;
//
//        Log.wtf("OnResume", "Done");
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        Log.wtf("OnStart", "Done");
//
//    }

    @Override
    public void onBackPressed() {
    super.onBackPressed();
        mMap = null;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }




    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            mMap = googleMap;

                            if (mMap != null) {
                                setUpMap();
                                setUpClusterer();
                                mDbHelper.readFromDb(mClusterManager);
                                if (LoginActivity.isAdministrator(MapsActivity.this)) {
                                    mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                                        @Override
                                        public void onMarkerDragStart(Marker marker) {

                                        }

                                        @Override
                                        public void onMarkerDrag(Marker marker) {

                                        }

                                        @Override
                                        public void onMarkerDragEnd(Marker marker) {

                                        }
                                    });

                                }
                            }




                            //camera position;
                            if (savedCameraPosition != null) {
                                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPosition));
                            }

                        }
                    });



        }
    }


    ////////////////// Popup Image window ///////////////////

    private void showImageDialog(final Context context, final MyClusterItem clickedClusterItem) {


        final Dialog imageDialog = new Dialog(context);
        imageDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        imageDialog.setContentView(getLayoutInflater().inflate(R.layout.popup_image, null));
        ImageView popupImage = (ImageView) imageDialog.findViewById(R.id.iv_popup_image);
        boolean isDonwnloaded = MyInfoWindowAdapter.setImageViewToClickedItemImage(context, popupImage, clickedClusterItem);
        savedClickedClusterItem = clickedClusterItem;
        userDismissed = true;
        currentDialog = imageDialog;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            /// Set name of the photographer
            final String photoUser;
            if (isDonwnloaded) {
                int itemId = clickedClusterItem.getmId();
                photoUser = mDbHelper.readStringCellFromId(itemId, "user");
            } else {
                photoUser = clickedClusterItem.getmUser();
            }

            if (photoUser != null && !photoUser.isEmpty()) {
                TextView photographerName = (TextView) imageDialog.findViewById(R.id.tv_photographer_name);
                photographerName.setText(photoUser);
                photographerName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (UpdateAppDbAsync.isConnected(context)) {
                            Intent openUser = new Intent(MapsActivity.this, UserProfileActivity.class);
                            openUser.putExtra("userName", photoUser);
                            startActivity(openUser);
                        } else {
                            Toast.makeText(context, R.string.error_connection, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }


            Button popupBack = (Button) imageDialog.findViewById(R.id.btn_popup_back);
            popupBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userDismissed = true;
                    imageDialog.dismiss();
                    currentDialog = null;
                }
            });


            Button popupCard = (Button) imageDialog.findViewById(R.id.btn_popup_card);
            popupCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent openCard = new Intent(MapsActivity.this, CardActivity.class);
                    openCard.putExtra("CLICKED_MCITEM", clickedClusterItem);
                    startActivityForResult(openCard, CARD_REQUEST);


                }
            });
        } else {

            popupImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    userDismissed = true;
                    imageDialog.dismiss();
                    currentDialog = null;
                }
            });

        }

        imageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (userDismissed) {
                    savedClickedClusterItem = null;
                }
            }
        });
        imageDialog.show();


    }

    ///////////////// Basic Map Setup //////////////////////

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
//        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        if (savedCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(savedCameraPosition));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                            (new LatLng(41.890230, 12.492335)), 11 // roma
                    (new LatLng(49.4828744,10.6078578)), 4
                    )
            );
        }


        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                locationAccuracy = (int) location.getAccuracy();
                mCurrentLatitude = location.getLatitude();
                mCurrentLongitude = location.getLongitude();

                setFabStatus();


            }
        });




    }



    /////////////////// Clusters ///////////////////////


    public void setUpClusterer() {
        //Initialize the manager with the context and the map.
        //(Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyClusterItem>(this, mMap);
        final MyClusterRenderer mClusterRenderer = new MyClusterRenderer<MyClusterItem>(this, mMap, mClusterManager);
        mClusterManager.setRenderer(mClusterRenderer);
        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyClusterItem>() {
            @Override
            public boolean onClusterClick(Cluster<MyClusterItem> myClusterItemCluster) {
                mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                myClusterItemCluster.getPosition(),
                                (mMap.getCameraPosition().zoom) + 1
                        ), 400, null
                );
                return true;
            }
        });

        //on marker click show checkin button
        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyClusterItem>() {
            @Override
            public boolean onClusterItemClick(MyClusterItem myClusterItem) {
//                mUserIsLogged = userIsLogged();
                mLastClickedItemId = myClusterItem.getmId();
                mItemIsChecked = mDbHelper.isAlreadyChecked(mLastClickedItemId);
                markerIsShown = true;
                mCurrentItemLatitude = myClusterItem.getmLatitude();
                mCurrentItemLongitude = myClusterItem.getmLongitude();
                setFabStatus();
                return false;
            }
        });

        //on marker click, move camera to center marker and lower position on screen to show infowin

//        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyClusterItem>() {
//            @Override
//            public boolean onClusterItemClick(MyClusterItem myClusterItem) {
//                mMap.animateCamera(
//                        CameraUpdateFactory.newLatLng(
//                                new LatLng(
//                                        myClusterItem.getmLatitude(),
//                                        myClusterItem.getmLongitude() - 00.000100
//                                        )
//                        ), 400, null
//                );
//                mClusterRenderer.getMarker(myClusterItem).showInfoWindow();
//                return true;
//            }
//        });






        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);





        // Installing custom InfoWindowAdapter to MarkerManager
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MyInfoWindowAdapter(this, mClusterRenderer));

        final Context myContext = this;

        // Set listener on click Infowindow
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                MyClusterItem clickedClusterItem = mClusterRenderer.getClusterItem(marker);
                marker.hideInfoWindow();
                markerIsShown = false;
                hideFabButtons();
                showImageDialog(myContext, clickedClusterItem);

            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideFabButtons();
                markerIsShown = false;
            }
        });





    }


    ///////// CHECK IF USER IS LOGGED //////////////


    public boolean userIsLogged () {
        if (loginStatusPreferences == null) {
            loginStatusPreferences = MapsActivity.this.getSharedPreferences("LoginStatus", 0);
        }
        boolean status = loginStatusPreferences.getBoolean("LoginBoolStatus", false);
        return status;
    }



    //////////// Marker Buttons Commands ///////////


    public void setFabStatus() {
        if (markerIsShown) {

            navigateButton.setVisibility(View.VISIBLE);
            navigateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri gmapsNavUri = Uri.parse("google.navigation:q=" + mCurrentItemLatitude + "," + mCurrentItemLongitude);
                    Intent navigateIntent = new Intent(Intent.ACTION_VIEW, gmapsNavUri);
                    navigateIntent.setPackage("com.google.android.apps.maps");
                    startActivity(navigateIntent);
                }
            });


            if (mUserIsLogged) {
                if (!mItemIsChecked) {

                    float[] distance = new float[2]; //numero risultati da mettere in memoria
                    Location.distanceBetween(mCurrentItemLatitude, mCurrentItemLongitude,
                            mCurrentLatitude, mCurrentLongitude, distance);

                        if (locationAccuracy < 31 && distance[0] < 31) {
                            checkInButton.setVisibility(View.VISIBLE);
                            checkDoneButton.setVisibility(View.GONE);
                            checkFailButton.setVisibility(View.GONE);
                            checkInButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    if (loginStatusPreferences == null) {
                                        loginStatusPreferences = MapsActivity.this.getSharedPreferences("LoginStatus", 0);
                                    }

                                    String email = loginStatusPreferences.getString("Email", "");
                                    updateCheckCounterToParse(email);

                                }
                            });
                        } else {
                            checkFailButton.setVisibility(View.VISIBLE);
                            checkFailButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(getApplicationContext(), R.string.toast_checkin_getnear, Toast.LENGTH_SHORT).show();
                                }
                            });
                            checkInButton.setVisibility(View.GONE);
                            checkDoneButton.setVisibility(View.GONE);
                        }

                } else {
                    checkDoneButton.setVisibility(View.VISIBLE);
                    checkFailButton.setVisibility(View.GONE);
                    checkInButton.setVisibility(View.GONE);
                }

            }

        }
    }

    public void hideFabButtons() {
        checkInButton.setVisibility(View.GONE);
        checkFailButton.setVisibility(View.GONE);
        checkDoneButton.setVisibility(View.GONE);

        navigateButton.setVisibility(View.GONE);
    }

    public void zoomToCurrentPosition() {



        if (mCurrentLatitude != 0.0) {
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mCurrentLatitude, mCurrentLongitude),
                            15
                    ), 1400, null
            );
        }
    }

    ///////////////////////////////////////////////


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CARD_REQUEST) {
            if (resultCode == RESULT_OK) {
                // delete temp file created for share
                // get path of file from result intent
//                String pathToDelete = data.getStringExtra("filepathImageDelete");
                // uri to file
//                File fileToDelete = new File(pathToDelete);
                // refresh media library
                getContentResolver().delete(Uri.parse(data.getStringExtra("contentPath")), null, null);
                // delete actual file from storage
                try {
//                    Boolean deleted = fileToDelete.delete();
                    boolean isdeleted = CardActivity.mCurrentSharedPhotofile.delete();
                } catch (RuntimeException e) {
                    Log.d("File delete error:" , e.toString());
                }

            }
        } else if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                setAdministratorInterface();
                LoginActivity.setAdministrator(MapsActivity.this, getLayoutInflater());
            }
        }
    }

    public void setAdministratorInterface() {
        adminButton = (Button)findViewById(R.id.btn_admin_access);
        navWrap = (LinearLayout)findViewById(R.id.ll_navigation_fabs);
        adminWrap = (LinearLayout)findViewById(R.id.ll_admin);
        adminButtonWrap = (LinearLayout)findViewById(R.id.ll_admin_button_wrap);
        adminButtonRotate = (Button)findViewById(R.id.btn_admin_rotate);
        adminButtonOk = (Button)findViewById(R.id.btn_admin_ok);
        adminButtonNo = (Button)findViewById(R.id.btn_admin_no);
        adminButtonClose = (Button)findViewById(R.id.btn_admin_close);
        adminClickableImage = (ImageView)findViewById(R.id.ib_admin_image);
        adminLoadingImageTxt = (TextView)findViewById(R.id.tv_admin_imageloading);
    }




    public void updateCheckCounterToParse(String email) {
//        showProgress(true);
        //check if email is registered
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("email", email);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> objects, ParseException e) {
                if (e == null) {

                    ParseUser parseUser = ParseUser.getCurrentUser();
                    Log.d("Checkcounter", String.valueOf(parseUser.getInt("checkCounter")));
                    parseUser.increment("checkCounter"); //incrementa di 1
                    parseUser.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e("Parse error", e.getMessage());
                                Toast.makeText(getApplicationContext(), R.string.error_connection, Toast.LENGTH_SHORT).show();
                            } else {
                                int checkIns = loginStatusPreferences.getInt("CheckInCounter", 0);
                                SharedPreferences.Editor editor = loginStatusPreferences.edit();
                                editor.putInt("CheckInCounter", checkIns + 1).commit();

                                mDbHelper.insertInRow(mLastClickedItemId,
                                        ArtDatabaseContract.ArtEntries.COLUMN_NAME_CHECKIN, "true");
                                mItemIsChecked = true;
//
                                checkInButton.setVisibility(View.GONE);
                                checkDoneButton.setVisibility(View.VISIBLE);
                                Toast.makeText(getApplicationContext(), R.string.toast_checkin_ok, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
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

    public static int dpToPx(Context context, float dp) {
        // Reference http://stackoverflow.com/questions/8309354/formula-px-to-dp-dp-to-px-android
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) ((dp * scale) + 0.5f);
    }

}

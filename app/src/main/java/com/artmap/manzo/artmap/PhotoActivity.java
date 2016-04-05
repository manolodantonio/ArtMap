package com.artmap.manzo.artmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class PhotoActivity extends FragmentActivity {


    private static final int CAMERA_PIC_REQUEST = 1597;

    GoogleMap mPhotoMap;
    Integer desiredMapHeight;
    Integer locationAccuracy;
    Integer mSavedAccuracy;

    byte[] imageByte;


    EditText etTitle;
    EditText etAuthor;
    EditText etYear;

    Point displaySize;
    TextView tvTrackingAccuracy;
    TextView tvCurrentPreview;
    Button ibTakePhoto;

    Button mSendButton;
    Boolean isHd = false;
    LinearLayout mSendSpinner;

    ParseFile photoToUpload;

    double mCurrentLatitude;
    double mCurrentLongitude;
    String mTitle = "";
    String mAuthor = "";
    String mYear = "";
//    String mLocationAccuracy = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            imageByte = savedInstanceState.getByteArray("IMAGEBYTE");
        }


        setContentView(R.layout.activity_photo);

        getDisplaySize();

        tvTrackingAccuracy = (TextView)findViewById(R.id.tv_current_accuracy);
        tvCurrentPreview = (TextView)findViewById(R.id.tv_current_preview);

        setUpMapIfNeeded();

        LinearLayout photoBack = (LinearLayout)findViewById(R.id.ll_photoBack);
        photoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ibTakePhoto = (Button)findViewById(R.id.ib_take_photo);
        if (imageByte != null) {
            //Set button thumbnail
            Drawable thumbnail = new BitmapDrawable(getApplicationContext().getResources(), PhotoUtilities.lastResizedBitmap);
            ibTakePhoto.setBackground(thumbnail);
            ibTakePhoto.setText("");
            tvCurrentPreview.setText("Anteprima: tocca per scattare un'altra foto.");
        }

        ibTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imageByte = null;
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = PhotoUtilities.createEmptyImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), "Errore nel creare il file immagine.", Toast.LENGTH_SHORT).show();
                    Log.e("imgFile creation", e.getMessage());
                }

                cameraIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile)
                );

                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);


            }
        });

        mSendButton = (Button) findViewById(R.id.bt_upload_photo);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Upload to Parse

                if (imageByte == null) {
                    Toast.makeText(getApplicationContext(), R.string.toast_nophoto, Toast.LENGTH_SHORT).show();
                } else {
                    if (locationAccuracy == null || locationAccuracy > 30) {
                        showLowAccuracyDialog(PhotoActivity.this);
                    } else {
                        isHd = false;
                        mSavedAccuracy = locationAccuracy;
                        showImageDialog(PhotoActivity.this);
                    }
                }
            }
        });



        mSendSpinner = (LinearLayout)findViewById(R.id.pb_photosend_progress);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putByteArray("IMAGEBYTE", imageByte);

    }

    private void uploadPhotoToParse() {
        showPhotoUpProgress(true);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat rightNow = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
        String filename = rightNow.format(calendar.getTime()) + ".jpg";
        if (isHd) {
            photoToUpload = new ParseFile(filename, PhotoUtilities.photoLab(true));
        }
        else {
            photoToUpload = new ParseFile(filename, imageByte);
        }

        photoToUpload.saveInBackground(new SaveCallback() {
            @Override
            public void done(com.parse.ParseException e) {
                if (e != null) {
                    showPhotoUpProgress(false);
                    Toast.makeText(getApplicationContext(), R.string.toast_nosendcauseinternet, Toast.LENGTH_LONG).show();
                    Log.e("//////Parse error", e.toString());
                    Log.e("//////Error code", String.valueOf(e.getCode()));
                } else {
                    ParseObject imageObject = new ParseObject("UploadedImage");

                    SharedPreferences userNamePref = getApplicationContext().getSharedPreferences("LoginStatus", 0);
                    String currentUsername = userNamePref.getString("Username", "NoUsername");

                    imageObject.put("imageId", MapsActivity.mCurrentFilename);
                    imageObject.put("imageFile", photoToUpload);
                    imageObject.put("user", currentUsername);
                    imageObject.put("title", mTitle);
                    imageObject.put("author", mAuthor);
                    imageObject.put("year", mYear);
                    imageObject.put("latitude", mCurrentLatitude);
                    imageObject.put("longitude", mCurrentLongitude);
                    imageObject.put("geoAccuracy", mSavedAccuracy);

                    imageObject.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(com.parse.ParseException e) {
                            showPhotoUpProgress(false);
                            if (e != null) {
                                Toast.makeText(getApplicationContext(), R.string.toast_nosendcauseinternet, Toast.LENGTH_LONG).show();
                                Log.e("//////Parse error", e.toString());
                                Log.e("//////Error code", String.valueOf(e.getCode()));
                            } else {
                                ibTakePhoto.setBackgroundColor(Color.parseColor("#ff009688"));
                                tvCurrentPreview.setText(" ");
                                ibTakePhoto.setText(R.string.photo_touchtoget);
                                imageByte = null;
                                Toast.makeText(getApplicationContext(), R.string.toast_photoissent, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {


            //shrink photo
            imageByte = PhotoUtilities.photoLab(false);

            //Set button thumbnail
            Drawable thumbnail = new BitmapDrawable(getApplicationContext().getResources(), PhotoUtilities.lastResizedBitmap);
            ibTakePhoto.setBackground(thumbnail);
            ibTakePhoto.setText("");
            tvCurrentPreview.setText("Anteprima: tocca per scattare un'altra foto.");


        }
    }



    //////////////////// Show Dialog ///////////////////////


    private void showImageDialog(Context context) {


        final Dialog moreinfoDialog = new Dialog(context);
        moreinfoDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        moreinfoDialog.setContentView(getLayoutInflater().inflate(R.layout.popup_moreinfo, null));

        etTitle = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_title);
        etAuthor = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_author);
        etYear = (EditText)moreinfoDialog.findViewById(R.id.et_moreinfo_year);

        Button popupBack = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_back);
        popupBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreinfoDialog.dismiss();
            }
        });


        Button popupForward = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_forward);
        popupForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHd = false;
                getDialogFields();
                moreinfoDialog.dismiss();
                uploadPhotoToParse();
            }
        });

        Button popupForwardHD = (Button)moreinfoDialog.findViewById(R.id.btn__moreinfo_hd);
        popupForwardHD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isHd = true;
                getDialogFields();
                moreinfoDialog.dismiss();
                uploadPhotoToParse();
            }
        });

        moreinfoDialog.show();

    }

    private void getDialogFields() {
        try {
            mTitle = etTitle.getText().toString();
            if (mTitle.isEmpty()) {
                mTitle = "Unknown";
            }
        } catch (NullPointerException e) {
            mTitle = "Error! Call Manzo!";
        }

        try {
            mAuthor = etAuthor.getText().toString();
            if (mAuthor.isEmpty()) {
                mAuthor = "Unknown";
            }
        } catch (NullPointerException e) {
            mAuthor = "Error! Call Manzo!";
        }

        try {
            mYear = etYear.getText().toString();
            if (mYear.isEmpty()) {
                mYear = "Unknown";
            }
        } catch (NullPointerException e) {
            mYear = "Error! Call Manzo!";
        }
    }


    /////////// Low Accuracy Dialog


    private void showLowAccuracyDialog(Context context) {


        final Dialog accDialog = new Dialog(context);
        accDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        accDialog.setContentView(getLayoutInflater().inflate(R.layout.popup_lowaccuracy, null));

        Button popupBack = (Button)accDialog.findViewById(R.id.btn_lowacc_back);
        popupBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accDialog.dismiss();
            }
        });


        Button popupForward = (Button)accDialog.findViewById(R.id.btn_lowacc_ok);
        popupForward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isHd = false;
                mSavedAccuracy = locationAccuracy;
                showImageDialog(PhotoActivity.this);

                accDialog.dismiss();
            }
        });


        accDialog.show();

    }


    /////////////////// Get Display Size /////////////////////

    private void getDisplaySize() {
        Display display = PhotoActivity.this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        displaySize = size;
    }

    /////////////////// MAP ///////////////////////////

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mPhotoMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapPhoto))
                    .getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            mPhotoMap = googleMap;
                            setUpMap();
                        }
                    });
        }
    }


    private void setUpMap() {

        desiredMapHeight = (displaySize.y) / 4;

        SupportMapFragment photoMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapPhoto);
        ViewGroup.LayoutParams params = photoMapFragment.getView().getLayoutParams();
        params.height = desiredMapHeight;
        photoMapFragment.getView().setLayoutParams(params);

        mPhotoMap.setMyLocationEnabled(true);
        mPhotoMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        (new LatLng(41.890230, 12.492335)), 11
                )
        );

        mPhotoMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            int zoomDynamic = 11;
            String accuracyValue = "Molto bassa";

            @Override
            public void onMyLocationChange(Location location) {

                locationAccuracy = (int) location.getAccuracy();


                if (locationAccuracy <= 15) {zoomDynamic = 18; accuracyValue = "Ottima";}
                else if (locationAccuracy >15 && locationAccuracy <= 30) {zoomDynamic = 18; accuracyValue = "Buona";}
                else if (locationAccuracy >30 && locationAccuracy <= 50) {zoomDynamic = 17; accuracyValue = "Bassa";}
                else if (locationAccuracy >50 && locationAccuracy <= 250) {zoomDynamic = 17; accuracyValue = "Molto Bassa";}
                else if (locationAccuracy >250) {zoomDynamic = 13; accuracyValue = "Insufficiente";}

                mCurrentLatitude = location.getLatitude();
                mCurrentLongitude = location.getLongitude();

                mPhotoMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                new LatLng(
                                        mCurrentLatitude,
                                        mCurrentLongitude
                                ),
                                zoomDynamic
                        )
                );
                tvTrackingAccuracy.setText(
                        "Accuratezza posizione: " +
                        locationAccuracy +
                        " metri " +
                        "(" + accuracyValue + ")"
                );


            }
        });



    }

    ////////////////////////////// Button Spinner //////////////////////////

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showPhotoUpProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mSendButton.setVisibility(show ? View.GONE : View.VISIBLE);
            mSendButton.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSendButton.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mSendSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
            mSendSpinner.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSendSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mSendButton.setVisibility(show ? View.VISIBLE : View.GONE);
            mSendSpinner.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.wtf("OnResume", "Done");
    }


}

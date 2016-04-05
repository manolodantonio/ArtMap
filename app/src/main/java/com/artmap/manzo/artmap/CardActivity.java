package com.artmap.manzo.artmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CardActivity extends ActionBarActivity {

    ShareActionProvider mShareActionProvider;
    BroadcastReceiver broadcastReceiver;

    Resources resources;
    GoogleMap mCardMap;
    Integer desiredMapHeight;

    Point displaySize;
    TextView tvTrackingAccuracy;

    double mCurrentLatitude;
    double mCurrentLongitude;
    int locationAccuracy;

    LinearLayout llBack;
    LinearLayout llReportSend;
    LinearLayout llSpinner;

    Drawable mCurrentDrawable;
    String mCurrentImgName;
    ImageView cardImage;
    TextView tvTitle;
    TextView tvAuthor;
    TextView tvYear;
    TextView tvStatus;

    EditText etTitle;
    EditText etAuthor;
    EditText etYear;
    Spinner etStatus;

    RadioGroup radioGroup;

    Button btnReport;
    Button btnReportBack;
    Button btnReportSubmit;

    ImageView hdButton;

    ScrollView svCardInfo;
    ScrollView svReport;

    MyClusterItem clickedItem;
    String imgName;

    static File mCurrentSharedPhotofile;
    String mCurrentMediaStorePath;
    Intent mDefaultShareIntent;
    Boolean intentCreated = false;


    //Download Image
    String filenameHD;
    String filename;
    File downloadedFile;
    private long enqueue;
    private DownloadManager dm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);

        resources = getResources();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        clickedItem = getIntent().getExtras().getParcelable("CLICKED_MCITEM");




        svCardInfo = (ScrollView)findViewById(R.id.sv_card_info);
        svReport = (ScrollView)findViewById(R.id.sv_report_wrap);

        cardImage = (ImageView)findViewById(R.id.iv_card_photo);
        tvTitle = (TextView)findViewById(R.id.tv_card_title);
        tvAuthor = (TextView)findViewById(R.id.tv_card_author);
        tvYear = (TextView)findViewById(R.id.tv_card_year);
        tvStatus = (TextView)findViewById(R.id.tv_card_status);

        btnReport = (Button)findViewById(R.id.btn_card_begin_report);
        btnReportBack = (Button)findViewById(R.id.btn_report_back);

        setupCardInfo();



        CreateDefaultIntent createDefaultIntent = new CreateDefaultIntent();
        createDefaultIntent.execute();


        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userIsLogged()) {
                    svCardInfo.setVisibility(View.GONE);
                    svReport.setVisibility(View.VISIBLE);
                    getDisplaySize();
                    tvTrackingAccuracy = (TextView) findViewById(R.id.tv_current_accuracy);
                    setUpMapIfNeeded();
                    setupReportInfo();
                } else {
                    Toast.makeText(CardActivity.this, R.string.toast_reportregister, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReportBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                svCardInfo.setVisibility(View.VISIBLE);
                svReport.setVisibility(View.GONE);
            }
        });



        setupHdButton();

        setupImageToGalleryClick();


        //Dismiss previous imageDialog
        MapsActivity.userDismissed = true;
        MapsActivity.currentDialog.dismiss();
        MapsActivity.currentDialog = null;



    }

    private void setupImageToGalleryClick() {
        if (MyInfoWindowAdapter.localImageFile.exists()) {
            cardImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent imageToGallery = new Intent();
                    imageToGallery.setAction(Intent.ACTION_VIEW);
//                    Uri uri = Uri.fromFile(MyInfoWindowAdapter.localImageFile);
                    Uri uri = Uri.parse(mCurrentMediaStorePath);
                    imageToGallery.setDataAndType(uri, "image/jpg");
                    startActivity(imageToGallery);
                }
            });
        }
    }

    private void setupHdButton() {
        hdButton = (ImageView) findViewById(R.id.iv_card_hdbutton);
        if (MyInfoWindowAdapter.isHd) {
            ImageView hdActive = (ImageView) findViewById(R.id.iv_card_hdactive);
            hdButton.setVisibility(View.GONE);
            hdActive.setVisibility(View.VISIBLE);
        } else {


            setupBroadcastReceiver();
            registerReceiver(broadcastReceiver, new IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            hdButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int itemID = clickedItem.getmId();
                    filename = "art" + itemID + ".jpg";
                    filenameHD = "art" + itemID + "HD.jpg";
                    Uri downloadUri = Uri.parse("http://artmap.a78.org/img/" + filenameHD);
//                    MyInfoWindowAdapter.localImageFile = new File(Environment.DIRECTORY_PICTURES + "/ArtMap/" + filenameHD);
                    dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    DownloadManager.Request request = new DownloadManager.Request(downloadUri);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "ArtMap/" + filenameHD)
                            .allowScanningByMediaScanner();
                    enqueue = dm.enqueue(request);

                }
            });
        }
    }

    public BroadcastReceiver setupBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor c = dm.query(query);
                    if (c.moveToFirst()) {
                        int columnIndex = c
                                .getColumnIndex(DownloadManager.COLUMN_STATUS);
                        if (DownloadManager.STATUS_SUCCESSFUL == c
                                .getInt(columnIndex)) {

                            MyInfoWindowAdapter.localImageFile = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + filenameHD);

                            Log.wtf("Downloaded File Dimensions", String.valueOf(MyInfoWindowAdapter.localImageFile.length()));

                            if (MyInfoWindowAdapter.localImageFile.length() > 10000) { // If Dimension is less than 10k the file has not been downloaded

                                File oldFileLD = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + filename);
                                boolean exists = oldFileLD.exists();
                                if (oldFileLD.exists()) {
                                    oldFileLD.delete();

                                }
//
                                ImageView hdActive = (ImageView) findViewById(R.id.iv_card_hdactive);
                                hdButton.setVisibility(View.GONE);
                                hdActive.setVisibility(View.VISIBLE);


                                Bitmap bitmap = BitmapFactory.decodeFile(MyInfoWindowAdapter.localImageFile.getAbsolutePath());
                                MyInfoWindowAdapter.lastDrawableFromBitmap = new BitmapDrawable(resources, bitmap);
                                cardImage.setImageDrawable(MyInfoWindowAdapter.lastDrawableFromBitmap);

                                CreateDefaultIntent createDefaultIntent = new CreateDefaultIntent();
                                createDefaultIntent.execute();



                                Toast.makeText(getApplicationContext(), R.string.toast_downloadcomplete, Toast.LENGTH_SHORT).show();
                                unregisterReceiver(broadcastReceiver);
                            } else {
                                boolean isdeleted = MyInfoWindowAdapter.localImageFile.delete();
                                Toast.makeText(getApplicationContext(), R.string.toast_nohdfile, Toast.LENGTH_SHORT).show();
                            }



                        }
                    }
                }
            }
        };
        return broadcastReceiver;
    }

    class SaveByteToFile extends AsyncTask <byte[], String, String> {

        @Override
        protected String doInBackground(byte[]... jpeg) {

            File savedImage = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, "photo.jpg");
            if (savedImage.exists()) {
                savedImage.delete();
            }

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(savedImage);
                fileOutputStream.write(jpeg[0]);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {

                Log.e("SaveFile", "File Not Found" + e.toString());

            } catch (IOException e) {
                Log.e("SaveFile", "ERROR" + e.toString());
            }

            return null;
        }
    }

    private void setupReportInfo() {



        etTitle = (EditText)findViewById(R.id.et_report_title);
        etTitle.setHint(tvTitle.getText());

        etAuthor = (EditText)findViewById(R.id.et_report_author);
        etAuthor.setHint(tvAuthor.getText());

        etYear = (EditText)findViewById(R.id.et_report_year);
        etYear.setHint(tvYear.getText());

        etStatus = (Spinner)findViewById(R.id.spnr_report_status);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(
                CardActivity.this, R.array.report_status, android.R.layout.simple_spinner_item );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        etStatus.setAdapter(adapter);
        final String[] spnStatus = new String[1];
        etStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spnStatus[0] = parent.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spnStatus[0] = "No Changes";

            }
        });

        llReportSend = (LinearLayout)findViewById(R.id.ll_report_send_wrap);
        llSpinner = (LinearLayout)findViewById(R.id.ll_report_progress);

        radioGroup = (RadioGroup)findViewById(R.id.rgYesNo);

        btnReportSubmit = (Button)findViewById(R.id.btn_report_submit);
        btnReportSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float[] distance = new float[2]; //nuemro risultati da mettere in memoria
                Location.distanceBetween(clickedItem.getmLatitude(), clickedItem.getmLongitude(),
                        mCurrentLatitude, mCurrentLongitude, distance);

//                if (locationAccuracy < 31 && distance[0] < 31) {
                    showReportUpProgress(true);

                    RadioButton selectedButton = (RadioButton) findViewById(
                            radioGroup.getCheckedRadioButtonId());
                    String isInPosition = selectedButton.getText().toString();

                    SharedPreferences userNamePref = CardActivity.this.getSharedPreferences("LoginStatus", 0);
                    String currentUsername = userNamePref.getString("Username", "");

                    ParseObject report = new ParseObject("Report");
                    report.put("username", currentUsername);
                    report.put("artId", clickedItem.getmId());
                    report.put("title", etTitle.getText().toString());
                    report.put("author", etAuthor.getText().toString());
                    report.put("year", etYear.getText().toString());
                    report.put("status", spnStatus[0]);
                    report.put("latitude", mCurrentLatitude);
                    report.put("longitude", mCurrentLongitude);
                    report.put("geoAccuracy", locationAccuracy);
                    report.put("isInPosition", isInPosition);
                    report.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                showReportUpProgress(false);
                                Toast.makeText(CardActivity.this, R.string.toast_error, Toast.LENGTH_SHORT).show();
                                Log.e("///Parse error: ", e.toString());
                            } else {
                                showReportUpProgress(false);
                                Toast.makeText(CardActivity.this, R.string.toast_reportsent, Toast.LENGTH_LONG).show();
                            }
                        }
                    });

//                } else {
//                    Toast.makeText(CardActivity.this, R.string.toast_shouldbenear, Toast.LENGTH_LONG).show();
//                }
            }
        });


    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showReportUpProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            llReportSend.setVisibility(show ? View.GONE : View.VISIBLE);
            llReportSend.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    llReportSend.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            llSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
            llSpinner.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    llSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            llReportSend.setVisibility(show ? View.VISIBLE : View.GONE);
            llSpinner.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    private void setupCardInfo() {

        //Photo

        imgName = "art"+(clickedItem.getmId());
        int drawableId = resources.getIdentifier(imgName, "drawable", "com.artmap.manzo.artmap");
//        mCurrentSharedPhotofile = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + imgName +"SHARED.jpg");

        if (MyInfoWindowAdapter.localImageFile.exists()) { //previously: check on mCurrentSharedPhotofile
//            Bitmap bitmap = BitmapFactory.decodeFile(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + imgName + ".jpg");
//            BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
            cardImage.setImageDrawable(MyInfoWindowAdapter.lastDrawableFromBitmap);
        } else {

            Drawable drawableName = null;
            Drawable noImage = resources.getDrawable(R.drawable.no_image);

            try {
                drawableName = resources.getDrawable(drawableId);
            } catch (Resources.NotFoundException e) {
                cardImage.setImageDrawable(noImage);
                mCurrentDrawable = noImage;
                mCurrentImgName = "no_image";
            } finally {
                if (drawableName != null) {
                    cardImage.setImageDrawable(drawableName);
                    mCurrentDrawable = drawableName;
                    mCurrentImgName = imgName;
                }
            }
        }


        //Title
        String titleFeed = clickedItem.getmTitle();
        if (titleFeed != null && !titleFeed.isEmpty()) {
            tvTitle.setText(titleFeed);
        }


        String authorYear = clickedItem.getmSnippet();
        String[] splitSnippet = authorYear.split("[,]");

        //Author
        String authorFeed = clickedItem.getmAuthor();
        if (authorFeed != null && !authorFeed.isEmpty()) {
            tvAuthor.setText(authorFeed);
        } else if (splitSnippet[0] != null && !splitSnippet[0].isEmpty()) {
            tvAuthor.setText(
                    splitSnippet[0].trim()
            );
        }

        //Year
        String yearFeed= String.valueOf(clickedItem.getmYear());
        if (yearFeed != null && !yearFeed.isEmpty() && !yearFeed.equals("0")) {
            tvYear.setText(yearFeed);
        } else {
            try {
                yearFeed = splitSnippet[1].trim();
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("CardSetting catched", e.toString());
            }
        }


        //Status

        tvStatus.setText("Visibile"); //Todo controllare numerazione

    }


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mCardMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapCard))
                    .getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            mCardMap = googleMap;
                            setUpMap();
                        }
                    });
        }
    }

    private void getDisplaySize() {
        Display display = CardActivity.this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        displaySize = size;
    }

    private void setUpMap() {

        desiredMapHeight = (displaySize.y) / 4;

        SupportMapFragment photoMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapCard);
        ViewGroup.LayoutParams params = photoMapFragment.getView().getLayoutParams();
        params.height = desiredMapHeight;
        photoMapFragment.getView().setLayoutParams(params);

        mCardMap.setMyLocationEnabled(true);
        mCardMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        (new LatLng(41.890230, 12.492335)), 11
                )
        );

        mCardMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {

            int zoomDynamic = 11;
            String accuracyValue = "Molto bassa";

            @Override
            public void onMyLocationChange(Location location) {

                locationAccuracy = (int) location.getAccuracy();


                if (locationAccuracy <= 15) {
                    zoomDynamic = 18;
                    accuracyValue = "Ottima";
                } else if (locationAccuracy > 15 && locationAccuracy <= 30) {
                    zoomDynamic = 18;
                    accuracyValue = "Buona";
                } else if (locationAccuracy > 30 && locationAccuracy <= 50) {
                    zoomDynamic = 17;
                    accuracyValue = "Bassa";
                } else if (locationAccuracy > 50 && locationAccuracy <= 250) {
                    zoomDynamic = 17;
                    accuracyValue = "Molto Bassa";
                } else if (locationAccuracy > 250) {
                    zoomDynamic = 13;
                    accuracyValue = "Insufficiente";
                }

                mCurrentLatitude = location.getLatitude();
                mCurrentLongitude = location.getLongitude();

                mCardMap.animateCamera(
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

    public void zoomToCurrentLocation() {
        mCardMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(
                                mCurrentLatitude,
                                mCurrentLongitude
                        ),
                        17
                )
        );
    }

    public boolean userIsLogged () {
        SharedPreferences loginStatus = CardActivity.this.getSharedPreferences("LoginStatus", 0);
        return loginStatus.getBoolean("LoginBoolStatus", false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_card, menu);

        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        mShareActionProvider.setShareIntent(mDefaultShareIntent);

//        mShareActionProvider.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
//            @Override
//            public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
//                if (mCurrentSharedPhotofile.exists()) {
//                    mCurrentSharedPhotofile.delete();
//                }
//                invalidateOptionsMenu();
//                return true;
//            }
//        });




        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (intentCreated) {
            mShareActionProvider.setShareIntent(mDefaultShareIntent);
            intentCreated = false;
        }
        return true;
    }

    private Intent getDefaultIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        Bitmap bitmap;

        if (MyInfoWindowAdapter.localImageFile.exists()) {
            bitmap = BitmapFactory.decodeFile(MyInfoWindowAdapter.localImageFile.getAbsolutePath());
        }
        else {
            bitmap = BitmapFactory.decodeResource(resources, resources.getIdentifier(mCurrentImgName, "drawable", getPackageName()));
        }

        if (mCurrentMediaStorePath!=null && !mCurrentMediaStorePath.isEmpty()) {
            getContentResolver().delete(Uri.parse(mCurrentMediaStorePath), null, null);
        }
        if (mCurrentSharedPhotofile == null){
            mCurrentSharedPhotofile = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, "tempShareFile");

        }

        String filePath = mCurrentSharedPhotofile.getAbsolutePath();
        mCurrentMediaStorePath = null;
        try {
            FileOutputStream fOut = new FileOutputStream(mCurrentSharedPhotofile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.close();
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mCurrentSharedPhotofile)));
            mCurrentMediaStorePath = MediaStore.Images.Media.insertImage(
                    getContentResolver(), filePath , "Title", "ArtMap"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }



        Uri imageUri2 = Uri.parse(mCurrentMediaStorePath);

        //Set filepath for result, to delete image when closing Card
        Intent returnIntent = new Intent();
//        String imagefilepath = getFilePathFromContentUri(imageUri2, getContentResolver());
//        returnIntent.putExtra("filepathImageDelete", imagefilepath );
        returnIntent.putExtra("contentPath", mCurrentMediaStorePath);
        setResult(RESULT_OK, returnIntent);


        String stringDescription =
                        resources.getString(R.string.card_author) + " " + tvAuthor.getText().toString() + "\n" +
                        resources.getString(R.string.card_title) + " " + tvTitle.getText().toString() + "\n" +
                        resources.getString(R.string.card_year) + " " + tvYear.getText().toString() + "\n\n" +
                        "Shared with ArtMap! - https://play.google.com/store/apps/details?id=com.artmap.manzo.artmap"
                ;

        intent.putExtra(Intent.EXTRA_TEXT, stringDescription);

        intent.putExtra(Intent.EXTRA_STREAM, imageUri2);

//        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mCurrentSharedPhotofile) + ".jpg");
//        getContentResolver().delete(imageUri2, null, null); //todo delete on result

        return intent;
    }

    private String getFilePathFromContentUri(Uri selectedVideoUri,
                                             ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                NavUtils.navigateUpFromSameTask(this);
//        }



//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }


        return super.onOptionsItemSelected(item);
    }

    public class CreateDefaultIntent extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            mDefaultShareIntent = getDefaultIntent();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (mShareActionProvider != null) {
                invalidateOptionsMenu(); //forces menu recreate with OnPrepareOptionsMenu
                setupImageToGalleryClick();
                intentCreated = true;
            }
        }
    }

//    private void facebookSharePhoto(Bitmap bitmap) {
//        SharePhoto photo = new SharePhoto.Builder()
//                .setBitmap(bitmap)
//                .build();
//
//        SharePhotoContent content = new SharePhotoContent.Builder()
//                .addPhoto(photo)
//                .build();
//
//    }


    @Override
    public void onPause() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("Catched", e.getMessage());
        }
//
//        if (mCurrentMediaStorePath!=null && !mCurrentMediaStorePath.isEmpty()) {
//            getContentResolver().delete(Uri.parse(mCurrentMediaStorePath), null, null);
//        }

        super.onPause();
    }

    @Override
    public void onResume() {
        if (broadcastReceiver == null) {
            setupBroadcastReceiver();
        }

        registerReceiver(broadcastReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

//        CreateDefaultIntent createDefaultIntent = new CreateDefaultIntent();
//        createDefaultIntent.execute();

        super.onResume();
    }

}

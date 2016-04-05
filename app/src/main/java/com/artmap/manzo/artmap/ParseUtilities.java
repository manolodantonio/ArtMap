package com.artmap.manzo.artmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.jibble.simpleftp.SimpleFTP;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Manzo on 25/11/2015.
 */
public class ParseUtilities {

    static Marker tempMarker;
    static String lastObjectId;
    static String lastModeratedImagePath;
    static ParseObject lastModeratedObject;
    public static Boolean hasCompleted = false;



    public static void getFirstUnmoderated() {
        MapsActivity.adminButtonWrap.setVisibility(View.GONE);
        ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery("UploadedImage");
        parseQuery.orderByDescending("createdAt");
        parseQuery.whereNotEqualTo("moderated", 1);
        parseQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e == null) {
                    // download image and save
                    ParseFile imageFile = (ParseFile) parseObject.get("imageFile");
                    imageFile.getDataInBackground(new GetDataCallback() {
                        @Override
                        public void done(byte[] bytes, ParseException e) {
                            if (e == null) {
                                SaveAndSetModPhoto saveAndSetModPhoto = new SaveAndSetModPhoto();
                                saveAndSetModPhoto.execute(bytes);
                            } else {
                                Log.d("Parse: ", e.getMessage());
                            }
                        }
                    });

                    lastModeratedObject = parseObject;
                    lastObjectId = parseObject.getObjectId();
                    LatLng modLocation = new LatLng(parseObject.getDouble("latitude"), parseObject.getDouble("longitude"));

                    MapsActivity.mMap.moveCamera( //zoom at moderated position
                            CameraUpdateFactory.newLatLngZoom(modLocation, 20)
                    );
                    if (tempMarker != null) {
                        tempMarker.remove();
                    }
                    tempMarker = MapsActivity.mMap.addMarker(new MarkerOptions().position(modLocation).draggable(true)); //add Marker at moderated position
//
                } else {
                    Log.wtf("Parse: ", e.getMessage());
                }
            }
        });
    }

    public static void setAsModerated(String objectId) {
        MapsActivity.adminClickableImage.setImageDrawable(null);
        MapsActivity.adminLoadingImageTxt.setVisibility(View.VISIBLE);
        ParseObject moderatedUpdate = ParseObject.createWithoutData("UploadedImage", objectId);
        moderatedUpdate.put("moderated", 1);
        moderatedUpdate.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    getFirstUnmoderated();
                } else {
                    Log.e("Parse", e.getMessage());
                }
            }
        });

    }



    static class uploadViaFTP extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... filepath) {

            SimpleFTP ftp = new SimpleFTP();

            try {
                //Connect
                ftp.connect("ftp.artmapworld.com", 21, "artmapworld.com", "diongain");
                //Set Binary Mode
                ftp.bin();
                //Change Dir on server
                ftp.cwd("/htdocs/appImages");
                //Upload File
                ftp.stor(new File(filepath[0]));

                //Close connection to FTP server
                ftp.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean s) {
            super.onPostExecute(s);

        }
    }

    static class SaveAndSetModPhoto extends AsyncTask<byte[], String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MapsActivity.adminButtonWrap.setVisibility(View.GONE);
            MapsActivity.adminClickableImage.setImageDrawable(null);
            MapsActivity.adminClickableImage.setVisibility(View.GONE);
            MapsActivity.adminLoadingImageTxt.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(byte[]... jpeg) {

            PhotoUtilities.PATH_TO_PUBLIC_PICTURES.mkdirs();
            File savedImage = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, "tempArtMod.jpg");

            if (savedImage.exists()) {
                savedImage.delete();
            }

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(savedImage);
                fileOutputStream.write(jpeg[0]);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {

                Log.e("SaveFile", "File Not Found" + e.toString());

            } catch (IOException e) {
                Log.e("SaveFile", "ERROR" + e.toString());
            }

//            lastCreatedFile = savedImage;
            return savedImage.getAbsolutePath();
        }

        @Override
        protected void onPostExecute(String savedImagePath) {
            lastModeratedImagePath = savedImagePath;
            super.onPostExecute(savedImagePath);

            MapsActivity.adminLoadingImageTxt.setVisibility(View.GONE);
            MapsActivity.adminClickableImage.setVisibility(View.VISIBLE);
            MapsActivity.adminClickableImage.setImageDrawable(Drawable.createFromPath(savedImagePath));
            MapsActivity.adminButtonWrap.setVisibility(View.VISIBLE);
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_VIEW);
//            intent.setDataAndType(Uri.parse("file://" + savedImagePath), "image/*");
//            MapsActivity.adminClickableImage.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    //// TODO: startactivity for gallery
//                }
//            });
        }
    }

    public static void uploadToMainDb (final ParseObject parseObject, final String filePath) {
        final LinearLayout ll_infoFields = (LinearLayout)LoginActivity.moreinfoDialog.findViewById(R.id.ll_moreinfo_fields_wrap);
        ll_infoFields.setVisibility(View.GONE);
        final Button btn_admin_ok = (Button)LoginActivity.moreinfoDialog.findViewById(R.id.btn__moreinfo_forward);
        btn_admin_ok.setVisibility(View.GONE);
        Button btn_admin_back = (Button)LoginActivity.moreinfoDialog.findViewById(R.id.btn__moreinfo_back);
        btn_admin_back.setVisibility(View.GONE);
        LoginActivity.tv_adminStatus = (TextView)LoginActivity.moreinfoDialog.findViewById(R.id.tv_admin_upload_status);
        LoginActivity.tv_adminStatus.setVisibility(View.VISIBLE);
        LoginActivity.tv_adminStatus.setText("Checking last Main DB object..");

        ParseQuery<ParseObject> parseQuery = ParseQuery.getQuery("MainDB");
        parseQuery.orderByDescending("artId");
        parseQuery.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject countObject, ParseException e) {
                if (e == null) {

                    final int newId = (countObject.getInt("artId")) + 1;
                    String filename = "art" + (newId) + ".jpg";
                    String filenameHd = "art" + (newId) + "HD.jpg";

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(filePath, options);
//                    options.inJustDecodeBounds = false;
                    if (options.outWidth > 600 || options.outHeight > 600) {

                        File elab = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, filenameHd);
                        try {
                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(elab));
                            PhotoUtilities.resizeFileToHD(filePath).compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.flush();
                            outputStream.close();
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                        String hdFilePath = elab.getAbsolutePath(); //Todo Erase on complete



                        ParseUtilities.uploadViaFTP uploadViaFTP = new ParseUtilities.uploadViaFTP();
                        uploadViaFTP.execute(hdFilePath);

                        LoginActivity.tv_adminStatus.setText("Uploading HD image.. Creating LD file..");
                        LoginActivity.photoToUpload = new ParseFile(filename, PhotoUtilities.bitmapToByteArray(PhotoUtilities.resizeFileToLD(filePath)));
                    } else {
                        LoginActivity.tv_adminStatus.setText("Creating LD imagefile..");
                        LoginActivity.photoToUpload = new ParseFile(filename, PhotoUtilities.bitmapToByteArray(PhotoUtilities.bitmapFromFile(filePath)));
                    }

                    LoginActivity.photoToUpload.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(com.parse.ParseException e) {
                            if (e != null) {
                                LoginActivity.moreinfoDialog.dismiss();
                                Log.e("Parse ", e.getMessage());
                            } else {
                                LoginActivity.tv_adminStatus.setText("Updating DB..");
                                final ParseObject imageObject = new ParseObject("MainDB");
                                String year = LoginActivity.aEtYear.getText().toString();
                                String title = LoginActivity.aEtTitle.getText().toString().isEmpty() ? LoginActivity.aEtTitle.getHint().toString() : LoginActivity.aEtTitle.getText().toString();
                                String author = LoginActivity.aEtAuthor.getText().toString().isEmpty() ? LoginActivity.aEtAuthor.getHint().toString() : LoginActivity.aEtAuthor.getText().toString();
                                String tag = LoginActivity.aEtTag.getText().toString().isEmpty() ? LoginActivity.aEtTag.getHint().toString() : LoginActivity.aEtTag.getText().toString();

                                imageObject.put("artId", newId);
                                imageObject.put("user", parseObject.getString("user"));
                                imageObject.put("title", title);
                                imageObject.put("author", author);
                                if (!year.equals("")){
                                    imageObject.put("year", Integer.parseInt(year));
                                }
                                imageObject.put("tag", tag);

                                imageObject.put("latitude", tempMarker.getPosition().latitude);
                                imageObject.put("longitude", tempMarker.getPosition().longitude);
                                imageObject.put("image", LoginActivity.photoToUpload);

                                imageObject.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(com.parse.ParseException e) {
                                        LoginActivity.moreinfoDialog.dismiss();
                                        if (e != null) {
                                            Log.e("Parse: ", e.getMessage());
                                        } else {
                                            MapsActivity.adminClickableImage.setImageDrawable(null);
                                            ParseUtilities.setAsModerated(ParseUtilities.lastObjectId);
                                        }
                                    }
                                });
                            }
                        }
                    });

                } else {
                    LoginActivity.moreinfoDialog.dismiss();
                    Log.e("parse", e.getMessage());
                }
            }
        });



    }


}

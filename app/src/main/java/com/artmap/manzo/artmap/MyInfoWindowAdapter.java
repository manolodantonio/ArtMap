package com.artmap.manzo.artmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by Manzo on 12/02/2015.
 */
class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private static Context myContext = null;
    static ImageView mImageView;
    static RelativeLayout infoImageWrap;
    static RelativeLayout infoProgressWrap;
    private static LayerDrawable baseImageLayer;
    public static BitmapDrawable resizedBitDraw;
    private static MyClusterRenderer<MyClusterItem> myClusterRenderer = null;
    private static int mCurrentDrawable;

    static Marker lastClickedMarker;
    static int lastClickedID;
    static File lastCreatedFile;
    static File localImageFile;
    public static Drawable lastDrawableFromBitmap;

    String fileName;
    static boolean isHd;

    static Drawable drawableName = null;

    public MyInfoWindowAdapter (Context context, MyClusterRenderer clusterRenderer) {
        this.myContext = context;
        myClusterRenderer = clusterRenderer;
    }




    @Override
    public View getInfoWindow(Marker marker) {

        /////// Save last clicked marker
        lastClickedMarker = marker;


        ////////// marker a oggetto del cluster
        MyClusterItem clickedClusterItem = myClusterRenderer.getClusterItem(marker);
        lastClickedID = clickedClusterItem.getmId();


        ///////// InfoWindow Layout Setup
        LayoutInflater layoutInflater = LayoutInflater.from(myContext);
        View infowinView = layoutInflater.inflate(R.layout.custom_info_window, null);

        /////////   IMAGE
        Resources resources = myContext.getResources();
        baseImageLayer = (LayerDrawable) resources.getDrawable(R.drawable.infowin_img); //LayerDrawable to modify ToDo remove?

        infoImageWrap = (RelativeLayout) infowinView.findViewById(R.id.rl_infoImageWrap);
        infoProgressWrap = (RelativeLayout) infowinView.findViewById(R.id.rl_infoProgressWrap);

        ImageView markerImage = (ImageView) infowinView.findViewById(R.id.markerImage);
//        markerImage.setAdjustViewBounds(true);

        String resourceName = "art"+(clickedClusterItem.getmId());
        int drawableId = resources.getIdentifier(resourceName, "drawable", "com.artmap.manzo.artmap");

        //// CHECK FOR LOCAL FILE
        isHd = false;
//        if (localImageFile != null &&
//                localImageFile.exists()) { /// Check last downloaded file
//            isHd = true;
//            markerImage.setImageDrawable(lastDrawableFromBitmap);
//
//        } else { //Check file in drive

            fileName = resourceName + "HD.jpg";
            localImageFile = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + fileName);

            if (localImageFile.exists()) { // CHECK FOR HD FILE
                isHd = true;
                Bitmap bitmap = BitmapFactory.decodeFile(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + fileName);
                lastDrawableFromBitmap = new BitmapDrawable(resources, bitmap);
                markerImage.setImageDrawable(lastDrawableFromBitmap);

            } else {  // CHECK FOR LD FILE

                fileName = resourceName + ".jpg";
                localImageFile = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + fileName);

                if (localImageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(PhotoUtilities.PATH_TO_PUBLIC_PICTURES + "/" + fileName);
                    lastDrawableFromBitmap = new BitmapDrawable(resources, bitmap);
//            baseImageLayer.setDrawableByLayerId(R.id.layer_item_image, lastDrawableFromBitmap);
//            markerImage.setImageDrawable(baseImageLayer);
                    markerImage.setImageDrawable(lastDrawableFromBitmap);

                } else if (drawableId != 0) { //DRAWABLE IS IN LOCAL

                    //                LayerDrawable modifiedLayer = setItemImage(myContext, baseImageLayer, clickedClusterItem); //set item image with ResourceID
                    //                markerImage.setImageDrawable(modifiedLayer);
                    markerImage.setImageDrawable(resources.getDrawable(drawableId));

                } else { // DOWNLOAD FROM INTERNET
                    downloadImageFromOnlineDB(lastClickedID);
                }
            }
//        }


        return infowinView;
    }

    public static boolean setImageViewToClickedItemImage (Context context, ImageView imageView, MyClusterItem clickedClusterItem ) {

        boolean isDownloaded = false;

        imageView.setAdjustViewBounds(true);

        if (localImageFile.exists()){
            BitmapDrawable resizedBitDraw = new BitmapDrawable(
                    context.getResources(),
                    resizeFileToScreenProportions(context,localImageFile)
                    );
//            imageView.setBackground(resizedBitDraw);
            imageView.setImageDrawable(resizedBitDraw);
            isDownloaded = true;

        } else {

            Resources resources = context.getResources();
            lastClickedID = clickedClusterItem.getmId();
            String imgName = "art"+(lastClickedID);
            int drawableId = resources.getIdentifier(imgName, "drawable", "com.artmap.manzo.artmap");

            try {
                drawableName = resources.getDrawable(drawableId);
                ////////// Resize to Background. Slower
                BitmapDrawable resizedBitDraw = new BitmapDrawable(
                        context.getResources(),
                        resizeImgToScreenProportions(context, drawableId)
                );
                imageView.setBackground(resizedBitDraw);
            } catch (Resources.NotFoundException e) {
//            downloadImageFromOnlineDB(lastClickedID);
                Log.d("DialogImage: ", "ResourceNotFound");
            }

//            imageView.setImageResource(R.drawable.no_image);
//        } finally {
//            if (drawableName != null) {
//                ///////////No resize. More performance. White bands.
////                imageView.setImageDrawable(drawableName);
//
//                ////////// Resize to Background. Slower
//                BitmapDrawable resizedBitDraw = new BitmapDrawable(
//                        context.getResources(),
//                        resizeImgToScreenProportions(context, drawableId)
//                    );
//                imageView.setBackground(resizedBitDraw);
//            }
//        }
        } //else

        return isDownloaded;
    }



    public static Bitmap resizeImgToScreenProportions (Context context, Integer drawableID ) {
        // Get screen size
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        //Get original image size
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawableID);
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        bitmap.recycle();

        //Math for destination image dimensions
        float scaleFactor;
        int targetHeight;
        int targetWidth;

        if (bitmapWidth >= bitmapHeight) { //horizontal image
            targetWidth = screenWidth;
            scaleFactor = ((float)targetWidth / bitmapWidth );
            targetHeight = Math.round(bitmapHeight * scaleFactor);

            if (targetHeight >= screenHeight) { //if scaled image is bigger than screen
                targetHeight = screenHeight;
                scaleFactor = ( (float)targetHeight / bitmapHeight );
                targetWidth = Math.round(bitmapWidth * scaleFactor);
            }

        } else { //vertical image
            targetHeight = screenHeight;
            scaleFactor = ( (float)targetHeight / bitmapHeight );
            targetWidth = Math.round(bitmapWidth * scaleFactor);

            if (targetWidth >= screenWidth) { //if scaled image is bigger than screen
                targetWidth = screenWidth;
                scaleFactor = ( (float)targetWidth / bitmapWidth );
                targetHeight = Math.round(bitmapHeight * scaleFactor);
            }
        }


        // Sony image resize
        // Part 1: Decode image

        Bitmap unscaledBitmap = ScalingUtilities.decodeResource(context.getResources(), drawableID,
                targetWidth, targetHeight, ScalingUtilities.ScalingLogic.FIT);

        // Part 2: Scale image
        Bitmap resizedBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, targetWidth,
                targetHeight, ScalingUtilities.ScalingLogic.FIT);
        unscaledBitmap.recycle();

        return resizedBitmap;
    }

    public static Bitmap resizeFileToScreenProportions (Context context, File imageFile ) {
        // Get screen size
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        //Get original image size
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        bitmap.recycle();

        //Math for destination image dimensions
        float scaleFactor;
        int targetHeight;
        int targetWidth;

        if (bitmapWidth >= bitmapHeight) { //horizontal image
            targetWidth = screenWidth;
            scaleFactor = ((float)targetWidth / bitmapWidth );
            targetHeight = Math.round(bitmapHeight * scaleFactor);

            if (targetHeight >= screenHeight) { //if scaled image is bigger than screen
                targetHeight = screenHeight;
                scaleFactor = ( (float)targetHeight / bitmapHeight );
                targetWidth = Math.round(bitmapWidth * scaleFactor);
            }

        } else { //vertical image
            targetHeight = screenHeight;
            scaleFactor = ( (float)targetHeight / bitmapHeight );
            targetWidth = Math.round(bitmapWidth * scaleFactor);

            if (targetWidth >= screenWidth) { //if scaled image is bigger than screen
                targetWidth = screenWidth;
                scaleFactor = ( (float)targetWidth / bitmapWidth );
                targetHeight = Math.round(bitmapHeight * scaleFactor);
            }
        }


        // Sony image resize
        // Part 1: Decode image

        Bitmap unscaledBitmap = ScalingUtilities.decodeFile(imageFile,
                targetWidth, targetHeight, ScalingUtilities.ScalingLogic.FIT);

        // Part 2: Scale image
        Bitmap resizedBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, targetWidth,
                targetHeight, ScalingUtilities.ScalingLogic.FIT);
        unscaledBitmap.recycle();

        return resizedBitmap;
    }


    public static LayerDrawable setItemImage (Context context, LayerDrawable layerDrawable, MyClusterItem clickedClusterItem ) {
        Resources resources = context.getResources();
        String imgName = "art"+(clickedClusterItem.getmId());
        int drawableId = resources.getIdentifier(imgName, "drawable", "com.artmap.manzo.artmap");
        Drawable drawableName = null;

        Drawable noImage = resources.getDrawable(R.drawable.no_image);

        /////
        try {
            drawableName = resources.getDrawable(drawableId);
//            ////////// Resize to Background. Slower
//            BitmapDrawable resizedBitDraw = new BitmapDrawable(
//                    context.getResources(),
//                    resizeImgToScreenProportions(context, drawableId)
//            );
////            imageView.setBackground(resizedBitDraw);
              layerDrawable.setDrawableByLayerId(R.id.layer_item_image, drawableName);
        } catch (Resources.NotFoundException e) {
//            downloadImageFromOnlineDB(lastClickedID);
        }
        //////////

//        try {
//            drawableName = resources.getDrawable(drawableId);
//              layerDrawable.setDrawableByLayerId(R.id.layer_item_image, drawableName);
//
//              mCurrentDrawable = drawableId;
//        } catch (Resources.NotFoundException e) {
//            baseImageLayer = layerDrawable;
//            downloadImageFromOnlineDB(lastClickedID);
////            layerDrawable.setDrawableByLayerId(R.id.layer_item_image, noImage);
//        }
//        finally {
//            if (drawableName != null) {
//                layerDrawable.setDrawableByLayerId(R.id.layer_item_image, drawableName);
//                mCurrentDrawable = drawableId;
//            }
//        }
//
//        while (baseImageLayer == null) {
//            Log.d("Layercheck - ", "Running");
//        }
//
//        return baseImageLayer;

        return layerDrawable;

    }


    public static void downloadImageFromOnlineDB(final int imageId) {

        showDownloadImageProgress(true);
        ParseQuery<ParseObject> imageQuery = ParseQuery.getQuery("MainDB");
        imageQuery.whereEqualTo("artId", imageId);
        imageQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    ParseObject downObject = list.get(0);
                    // set photouser in local db
                    ArtEntriesDbHelper mDbHelper = ApplicationOverride.mDbHelper;
                    String photoUserName = downObject.getString("user");
                    mDbHelper.insertInRow(imageId, "user", photoUserName);

                    // download image and save
                    ParseFile imageFile = (ParseFile) downObject.get("image");
                    imageFile.getDataInBackground(new GetDataCallback() {
                        @Override
                        public void done(byte[] bytes, ParseException e) {
                            if (e == null) {
                                SaveByteToFile saveByteToFile = new SaveByteToFile();
                                saveByteToFile.execute(bytes);
//                                        Toast.makeText(getApplicationContext(), "OK", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("QUERY", "Error" + e.getMessage());
                                showDownloadImageProgress(false);
                            }
                        }
                    });
                } else {
                    Log.d("QUERY", "Error" + e.getMessage());
                    showDownloadImageProgress(false);
                }
            }
        });
    }

    static class SaveByteToFile extends AsyncTask<byte[], String, String> {

        @Override
        protected String doInBackground(byte[]... jpeg) {

            PhotoUtilities.PATH_TO_PUBLIC_PICTURES.mkdirs();
            File savedImage = new File(PhotoUtilities.PATH_TO_PUBLIC_PICTURES, "art"+lastClickedID+".jpg");

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

            lastCreatedFile = savedImage;
            return savedImage.getAbsolutePath();
        }

        @Override
        protected void onPostExecute(String savedImagePath) {
            super.onPostExecute(savedImagePath);
            showDownloadImageProgress(false);

//            drawableName = Drawable.createFromPath(savedImagePath); // remove? double of getInfoWIn
//            baseImageLayer.setDrawableByLayerId(R.id.layer_item_image, drawableName); // remove? double
            lastClickedMarker.showInfoWindow();
//            String imgName = "art"+(lastClickedID);
//            int drawableId = myContext.getResources().getIdentifier(imgName, "drawable", "com.artmap.manzo.artmap");
//            ////////// Resize to Background. Slower
//            resizedBitDraw = new BitmapDrawable(
//                    myContext.getResources(),
//                    resizeFileToScreenProportions(myContext, lastCreatedFile)
//            );
//            if (baseImageLayer != null) {
//                baseImageLayer.setDrawableByLayerId(R.id.layer_item_image, resizedBitDraw);
//
//                ///////// InfoWindow Layout Setup
//                LayoutInflater layoutInflater = LayoutInflater.from(myContext);
//                View v = layoutInflater.inflate(R.layout.custom_info_window, null);
//                ImageView markerImage = (ImageView) v.findViewById(R.id.markerImage);
//                markerImage.setImageDrawable(baseImageLayer);
//            }
//            if (mImageView != null) {
//                mImageView.setBackground(resizedBitDraw);
//            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public static void showDownloadImageProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = myContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

            infoImageWrap.setVisibility(show ? View.GONE : View.VISIBLE);
            infoImageWrap.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    infoImageWrap.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            infoProgressWrap.setVisibility(show ? View.VISIBLE : View.GONE);
            infoProgressWrap.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    infoProgressWrap.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            infoImageWrap.setVisibility(show ? View.VISIBLE : View.GONE);
            infoProgressWrap.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }


}

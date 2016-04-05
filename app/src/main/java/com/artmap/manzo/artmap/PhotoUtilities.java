package com.artmap.manzo.artmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Manzo on 07/04/2015.
 */
public class PhotoUtilities {

    public static final File PATH_TO_PUBLIC_PICTURES =
            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +
                    File.separator +
                    "ArtMap" +
                    File.separator
            );

    public static Bitmap lastResizedBitmap;
    public static Boolean hasCompleted = false;



    public static File createEmptyImageFile() throws IOException {
        //Create an image file Name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG" + timeStamp + "_";
        PATH_TO_PUBLIC_PICTURES.mkdirs();
        File image = File.createTempFile(imageFileName, ".jpg", PATH_TO_PUBLIC_PICTURES);

        //save filepath for use with ACTION_VIEW intents
        MapsActivity.mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        MapsActivity.mCurrentImagePath = image.getAbsolutePath();
        MapsActivity.mCurrentFilename = imageFileName;
        return image;
    }

    public static Bitmap bitmapFromFile(String path) {
        return BitmapFactory.decodeFile(path);
    }

    public static Bitmap rotateBitmap(Bitmap originalBitmap) {
        Matrix rotator = new Matrix();
        rotator.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.getWidth(), originalBitmap.getHeight(),
                rotator, true);
        return rotatedBitmap;
    }

    public static byte[] bitmapToByteArray (Bitmap bitmap) {
        hasCompleted = false;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bytes = stream.toByteArray();
        try {
            stream.flush();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        hasCompleted = true;
        return bytes;
    }

    public static Bitmap resizeFileToLD (String filePath) {
        //Get original image size
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        bitmap.recycle();

        //Math for destination image dimensions
        float scaleFactor;
        int targetHeight;
        int targetWidth;

        if (bitmapWidth >= bitmapHeight) { //horizontal image
            targetWidth = 600;
            scaleFactor = ((float)targetWidth / bitmapWidth );
            targetHeight = Math.round(bitmapHeight * scaleFactor);


        } else { //vertical image
            targetHeight = 600;
            scaleFactor = ( (float)targetHeight / bitmapHeight );
            targetWidth = Math.round(bitmapWidth * scaleFactor);

        }


        // Sony image resize
        // Part 1: Decode image

        File toDecode = new File(filePath);
        Bitmap unscaledBitmap = ScalingUtilities.decodeFile(toDecode,
                targetWidth, targetHeight, ScalingUtilities.ScalingLogic.FIT);

        // Part 2: Scale image
        Bitmap resizedBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, targetWidth,
                targetHeight, ScalingUtilities.ScalingLogic.FIT);
        unscaledBitmap.recycle();

        return resizedBitmap;
    }


    public static Bitmap resizeFileToHD (String filePath) {
        //Get original image size
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        int bitmapHeight = bitmap.getHeight();
        int bitmapWidth = bitmap.getWidth();
        bitmap.recycle();

        //Math for destination image dimensions
        float scaleFactor;
        int targetHeight;
        int targetWidth;

        if (bitmapWidth >= bitmapHeight) { //horizontal image
            targetWidth = 1920;
            scaleFactor = ((float)targetWidth / bitmapWidth );
            targetHeight = Math.round(bitmapHeight * scaleFactor);


        } else { //vertical image
            targetHeight = 1920;
            scaleFactor = ( (float)targetHeight / bitmapHeight );
            targetWidth = Math.round(bitmapWidth * scaleFactor);

        }


        // Sony image resize
        // Part 1: Decode image

        File toDecode = new File(filePath);
        Bitmap unscaledBitmap = ScalingUtilities.decodeFile(toDecode,
                targetWidth, targetHeight, ScalingUtilities.ScalingLogic.FIT);

        // Part 2: Scale image
        Bitmap resizedBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, targetWidth,
                targetHeight, ScalingUtilities.ScalingLogic.FIT);
        unscaledBitmap.recycle();

        return resizedBitmap;
    }

    public static byte[] photoLab(Boolean isHd)

    {

        //Get image in bytes
        //link to image
        File imageFile = new File(MapsActivity.mCurrentImagePath);
        //Setup output stream
        ByteArrayOutputStream stream = new ByteArrayOutputStream();


        ////////////////////////////////////Bitmap Resize////////////////////////////////////////////

        ///////////////////////////////////////////Check if image has been saved
        int height = 0;
        int width = 0;

        Bitmap originalBitmap = null;

        while (height == 0) {  //CHECK IF IMAGE HAS BEEN SAVED by verifying height

            //Setup BitmapFactory to include getting width and height
            BitmapFactory.Options bfOptions = new BitmapFactory.Options();
            bfOptions.inJustDecodeBounds = true;
            //Decode file to bitmap
            originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bfOptions);
            //get dimensions
            height = bfOptions.outHeight;
            width = bfOptions.outWidth;

        }



        if (isHd) {
            //// HD Image
            originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            originalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//            lastResizedBitmap = originalBitmap;

        } else {

            //////////// set scaled width and height - largest side is 600px, keep aspect ratio
            float scaleFactor = 1;
            int otherSide = 600;
            int rzHeight;
            int rzWidth;


            if (height >= width) {
                scaleFactor = ((float) 600 / height);
                rzWidth = Math.round(width * scaleFactor);
                rzHeight = otherSide;
            } else {
                scaleFactor = ((float) 600 / width);
                rzHeight = Math.round(height * scaleFactor);
                rzWidth = otherSide;
            }


            //////////////////////////////////////////// Recreate bitmap

//            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, rzWidth, rzHeight, false);

//            //Recreate bitmap by matrix - not a great increase
//
//            float scaleWidth = ((float) rzWidth) / width;
//            float scaleHeight = ((float) rzHeight) / height;
//
//            Matrix matrix = new Matrix();
//            matrix.postScale(scaleWidth, scaleHeight);
//
//            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

            // Sony image method

            // Part 1: Decode image
            Bitmap unscaledBitmap = ScalingUtilities.decodeFile(imageFile,
                    rzWidth, rzHeight, ScalingUtilities.ScalingLogic.FIT);

            // Part 2: Scale image
            Bitmap resizedBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, rzWidth,
                    rzHeight, ScalingUtilities.ScalingLogic.FIT);
            unscaledBitmap.recycle();


            ///////////////////////////////////Bitmap Compress
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        lastResizedBitmap = resizedBitmap;
        }


        //Bitmap to byte
        byte[] imageByte = stream.toByteArray();


        return imageByte;




    }
}

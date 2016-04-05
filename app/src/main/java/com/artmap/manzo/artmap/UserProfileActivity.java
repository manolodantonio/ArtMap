package com.artmap.manzo.artmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;


public class UserProfileActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);



        LinearLayout backLayout = (LinearLayout)findViewById(R.id.ll_userBack);
        backLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        final Button logOutButton = (Button) findViewById(R.id.btn_user_logout);

        final String user = getIntent().getExtras().getString("userName", "currentUser");
        if (user.equals("currentUser")) {
            setupCurrentUserData();
        } else {
            setupUserLookoutData(user);
        }


        if (user.equals("currentUser")) {
            logOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParseUser.logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) {
                                Log.e("///Parse Error", e.getMessage());
                            }
                        }
                    });
                    changeLoginStatus(false, null);
                    MapsActivity.userNameTV.setText(null);
                    MapsActivity.userScoreTV.setText(null);
                    Toast.makeText(getApplicationContext(), R.string.toast_logout, Toast.LENGTH_SHORT).show();
                    if (LoginActivity.isAdministrator(getApplicationContext())) {
                        LoginActivity.unsetAdministrator();
                    }
                    finish();
                }
            });
        } else {
            logOutButton.setText(R.string.action_back);
            logOutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

    }

    //////////////// Change user stauts logged\not logged /////////////
    private void changeLoginStatus(Boolean status, String username) {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("LoginStatus", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Username", username);
        editor.putBoolean("LoginBoolStatus", status);
        MapsActivity.mUserIsLogged = status;

        editor.commit();

    }


    private void setupCurrentUserData() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("LoginStatus", 0);

        TextView username = (TextView)findViewById(R.id.tv_userpage_username);
        String currentUsername = sharedPreferences.getString("Username", "");
        username.setText(currentUsername);



        TextView checkInsNumber = (TextView)findViewById(R.id.tv_checknumber);
        checkInsNumber.setText(String.valueOf(
                sharedPreferences.getInt("CheckInCounter" , 0)
        ));

        TextView photosPublished = (TextView)findViewById(R.id.tv_photonumber);
        photosPublished.setText(String.valueOf(
                sharedPreferences.getInt(currentUsername + "_UploadedPhotosCounter", 0)
        ));


        TextView reportsPublished = (TextView)findViewById(R.id.tv_reportnumber);
        reportsPublished.setText(String.valueOf(
                sharedPreferences.getInt("ReportsCounter", 0)
        ));

        TextView scoreTotal = (TextView)findViewById(R.id.tv_userpage_scoretotal);
        String scoreSum = String.valueOf(
                (Integer.parseInt(checkInsNumber.getText().toString()) * 20) +
                        (Integer.parseInt(photosPublished.getText().toString()) * 100) +
                        (Integer.parseInt(reportsPublished.getText().toString()) * 50)
        );
        scoreTotal.setText(scoreSum);
    }

    private void setupUserLookoutData(String usernameToFind) {


        showUserProgress(true);
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", usernameToFind);
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null) {
//                    if (parseUser != null) {
                        //Set Username
                        TextView username = (TextView) findViewById(R.id.tv_userpage_username);
                        username.setText(parseUser.getUsername());

                        int checkCounter = parseUser.getInt("checkCounter");
                        int photoCounter = parseUser.getInt("publishedPhotos");
                        int reportsCounter = parseUser.getInt("reports");

                        //Set Checkins no.
                        TextView checkInsNumber = (TextView) findViewById(R.id.tv_checknumber);
                        checkInsNumber.setText(String.valueOf(checkCounter));


                        // Set photos no.
                        TextView photosPublished = (TextView) findViewById(R.id.tv_photonumber);
                        photosPublished.setText(String.valueOf(photoCounter));


                        // Set reports no.
                        TextView reportsPublished = (TextView) findViewById(R.id.tv_reportnumber);
                        reportsPublished.setText(String.valueOf(reportsCounter));

                        //Set scoreTotal
                        TextView scoreTotal = (TextView) findViewById(R.id.tv_userpage_scoretotal);
                        String scoreSum = String.valueOf(
                                (checkCounter * 20) +
                                        (photoCounter * 100) +
                                        (reportsCounter * 50)
                        );
                        scoreTotal.setText(scoreSum);
                        showUserProgress(false);
//                    } else {
////                        showUserProgress(false);
//                        Toast.makeText(getApplicationContext(), R.string.toast_noprofile, Toast.LENGTH_SHORT).show();
//                        finish();
//                    }

                } else {
                    Log.e("Parse Error " + e.getCode(), e.getMessage());
                    Toast.makeText(getApplicationContext(), R.string.toast_noprofile, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });


    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showUserProgress(final boolean show) {

        final ScrollView dataWrap = (ScrollView) findViewById(R.id.sv_user_datawrap);
        final LinearLayout spinnerLayout = (LinearLayout) findViewById(R.id.ll_user_progress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);


            dataWrap.setVisibility(show ? View.GONE : View.VISIBLE);
            dataWrap.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dataWrap.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            spinnerLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            spinnerLayout.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    spinnerLayout.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            dataWrap.setVisibility(show ? View.VISIBLE : View.GONE);
            spinnerLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


}

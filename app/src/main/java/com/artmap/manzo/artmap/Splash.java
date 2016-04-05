package com.artmap.manzo.artmap;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class Splash extends Activity {

    private final int SPLASH_DURATION = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        UpdateAppDbAsync updateAppDbAsync = new UpdateAppDbAsync(this);
        updateAppDbAsync.execute(new int[1]);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        Intent goToMain = new Intent(Splash.this, MapsActivity.class);
                        startActivity(goToMain);
                        finish();

                    }
                }, SPLASH_DURATION
        );

    }



}

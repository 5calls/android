package org.a5calls.android.a5calls;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.security.Provider;

public class LocationActivity extends AppCompatActivity {

    private static final String TAG = "LocationActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    /**
     * 60647 is split district
     * 3240 W. Fullerton Ave Chicago IL 60647 should resolve to Gutierrez
     * 2076 N Hoyne Ave Chicago IL 60647-4559 is a Quigley
     */

    private boolean mFromMain = false;
    private LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Load the zip code the user last used, if any.
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        String code = pref.getString(MainActivity.KEY_USER_ZIP, "");

        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        if (!TextUtils.isEmpty(code)) {
            zipEdit.setText(code);
            setFromMain();
        } else if (!TextUtils.isEmpty(pref.getString(MainActivity.KEY_LONGITUDE, ""))) {
            setFromMain();
        }
        zipEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitZip();
                    return true;
                }
                return false;
            }
        });
        Button zipButton = (Button) findViewById(R.id.zip_code_submit);
        zipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitZip();
            }
        });

        Button gpsButton = (Button) findViewById(R.id.btn_gps);
        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryGettingLocation();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        if (pref.getBoolean(MainActivity.KEY_ALLOW_ANALYTICS, true)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    @Override
    protected void onPause() {
        if (mLocationListener != null) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationListener);
        }
        super.onPause();
    }

    private void submitZip() {
        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        String code = zipEdit.getText().toString();
        // Is it a string that is exactly 5 characters long?
        if (TextUtils.isEmpty(code) || code.length() != 5) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        try {
            // Make sure it is a number, too, by trying to parse it.
            Integer.parseInt(code);
        } catch (NumberFormatException e) {
            zipEdit.setError(getResources().getString(R.string.zip_error));
            return;
        }
        // If we made it here, the zip is valid! Update the UI and send the request.
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        pref.edit().putString(MainActivity.KEY_USER_ZIP, code).apply();

        // Take out the lat/long flags if we had them in, because the user specifically requested
        // a zip and we default to lat/long.
        pref.edit().putString(MainActivity.KEY_LATITUDE, "")
                .putString(MainActivity.KEY_LONGITUDE, "").apply();

        returnToMain();
    }

    private void returnToMain() {
        // If we came from MainActivity and return with another Intent, it will create a deep stack
        // of activities!
        if (mFromMain) {
            onBackPressed();
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            tryGettingLocation();
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * @return the last know best location
     */
    private void tryGettingLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            }
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(new Criteria(), false);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location == null) {
            mLocationListener = new LocationListener() {

                @Override
                public void onLocationChanged(Location location) {
                    updateSharedPrefs(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            locationManager.requestLocationUpdates(provider, 10, 0, mLocationListener);
        } else {
            updateSharedPrefs(location);
        }
    }

    private void updateSharedPrefs(Location location) {
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        pref.edit().putString(MainActivity.KEY_LONGITUDE, String.valueOf(location.getLongitude()))
                .putString(MainActivity.KEY_LATITUDE, String.valueOf(location.getLatitude()))
                .apply();
        returnToMain();
    }

    private void setFromMain() {
        // If we already have a zip, this isn't our first time to the app, so we can go "up"
        // to return to the main activity.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mFromMain = true;
    }
}
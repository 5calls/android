package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.databinding.ActivityLocationBinding;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.util.AnalyticsManager;

public class LocationActivity extends AppCompatActivity {
    private static final String TAG = "LocationActivity";

    // Allows parent activity to control the home button
    public static final String ALLOW_HOME_UP_KEY = "allowHomeUp";
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private final AccountManager accountManager = AccountManager.Instance;

    /**
     * 60647 is split district
     * 3240 W. Fullerton Ave Chicago IL 60647 should resolve to Gutierrez
     * 2076 N Hoyne Ave Chicago IL 60647-4559 is a Quigley
     */

    private boolean allowsHomeUp = false;
    private LocationListener mLocationListener;

    private ActivityLocationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle(R.string.menu_location);
        }

        // Allow home up if required
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra(ALLOW_HOME_UP_KEY, false)) {
                if (supportActionBar != null) {
                    supportActionBar.setDisplayHomeAsUpEnabled(true);
                }
                allowsHomeUp = true;
            }
        }

        // Load the address the user last used, if any.
        String zip = accountManager.getAddress(this);
        if (!TextUtils.isEmpty(zip)) {
            binding.addressEdit.setText(zip);
        }

        // Set listeners
        binding.addressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onSubmitAddress(binding.addressEdit.getText().toString());
                    return true;
                }
                return false;
            }
        });

        binding.addressSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitAddress(binding.addressEdit.getText().toString());
            }
        });

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            binding.gpsButton.setVisibility(View.VISIBLE);
            binding.gpsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tryGettingLocation();
                }
            });
        } else {
            // No GPS available, so don't show the GPS location section.
            binding.gpsButton.setVisibility(View.GONE);
        }

        new AnalyticsManager().trackPageview("/location", this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLocationListener != null) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.removeUpdates(mLocationListener);
        }
        super.onPause();
    }

    private void returnToMain() {
        // Make sure we're still alive
        if (isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
                return;
        }

        // If we came from MainActivity and return with another Intent, it will create a deep stack
        // of activities!
        if (allowsHomeUp) {
            finish();
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
                returnToMain();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && (
                grantResults[0] == PackageManager.PERMISSION_GRANTED || (grantResults.length > 1 &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
            // Try getting the location in a Runnable because it is possible that if the location
            // is cached we get it so fast that we returnToMain before we are done resuming, which
            // causes a crash.
            binding.gpsButton.post(new Runnable() {
                @Override
                public void run() {
                    tryGettingLocation();
                }
            });
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
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
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
                    onReceiveLocation(location);
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
            onReceiveLocation(location);
        }
    }

    private void onSubmitAddress(String address) {
        address = address.trim();
        if (TextUtils.isEmpty(address)) {
            binding.addressEdit.setError(getResources().getString(R.string.error_address_empty));
            return;
        }
        // Super simple check for valid address: If it's less than 5 characters it isn't valid.
        // If it is 5 characters and they aren't digits, it isn't valid.
        // To do more comprehensive checking we'd need to use an API or await the response
        // from `FiveCallsApi`.
        if (address.length() < 5 ||
                (address.length() == 5 && !TextUtils.isDigitsOnly(address))) {
            binding.addressEdit.setError(getResources().getString(R.string.error_address_empty));
            return;
        }
        // Update the UI and send the request.
        accountManager.setAddress(this, address);

        // Delete lat/lng, because the user specifically requested an address and we default to
        // lat/long.
        accountManager.setLat(this, null);
        accountManager.setLng(this, null);

        returnToMain();
    }


    private void onReceiveLocation(Location location) {
        accountManager.setLat(this, String.valueOf(location.getLatitude()));
        accountManager.setLng(this, String.valueOf(location.getLongitude()));
        accountManager.setAddress(this, null);

        // Update the address field from the location
        // TODO: Now that this is no longer only a zip field, this doesn't make sense, because
        // we could enter any level of detail.
        // For now, leave it blank. Revisit in the future.
        /*
        if (location != null) {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                if (addresses != null && addresses.size() > 0) {
                    accountManager.setAddress(this, addresses.get(0).getPostalCode());
                }
            } catch (IOException e) {
                // Do nothing
            }
        }
        */

        returnToMain();
    }
}

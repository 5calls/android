package org.a5calls.android.a5calls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LocationActivity extends AppCompatActivity {

    /**
     * 60647 is split district
     * 3240 W. Fullerton Ave Chicago IL 60647 should resolve to Gutierrez
     * 2076 N Hoyne Ave Chicago IL 60647-4559 is a Quigley
     */

    private boolean mFromMain = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Load the zip code the user last used, if any.
        SharedPreferences pref = getSharedPreferences(MainActivity.PREFS_FILE, MODE_PRIVATE);
        String code = pref.getString(MainActivity.KEY_USER_ZIP, "");

        // TODO: Option to get user's location from GPS instead of just entering a zip code.
        EditText zipEdit = (EditText) findViewById(R.id.zip_code);
        if (!TextUtils.isEmpty(code)) {
            zipEdit.setText(code);
            // If we already have a zip, this isn't our first time to the app, so we can go "up"
            // to return to the main activity.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mFromMain = true;
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
}

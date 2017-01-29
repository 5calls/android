package org.a5calls.android.a5calls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.List;

/**
 * Tutorial / splash screen activity
 */
public class TutorialActivity extends AppCompatActivity {

    private JsonController.RequestStatusListener mStatusListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);
        findViewById(R.id.get_started_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Return to the main activity
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_FILE, 0);
                prefs.edit().putBoolean(MainActivity.KEY_INITIALIZED, true).apply();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // TODO: Re-use this listener between AboutActivity and here, since it's really the same.
        mStatusListener = new JsonController.RequestStatusListener() {
            @Override
            public void onRequestError() {
                Snackbar.make(findViewById(R.id.calls_to_date),
                        getResources().getString(R.string.request_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onJsonError() {
                Snackbar.make(findViewById(R.id.calls_to_date),
                        getResources().getString(R.string.json_error),
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onIssuesReceived(List<Issue> issues) {
                // unused
            }

            @Override
            public void onCallCount(int count) {
                TextView callsToDate = (TextView) findViewById(R.id.calls_to_date);
                // TODO: Format with commas
                callsToDate.setText(String.format(
                        getResources().getString(R.string.calls_to_date), count));
            }

            @Override
            public void onCallReported() {
                // unused
            }
        };
        JsonController controller = AppSingleton.getInstance(getApplicationContext())
                .getJsonController();
        controller.registerStatusListener(mStatusListener);
        controller.getCallCount();
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatusListener(mStatusListener);
        super.onDestroy();
    }
}

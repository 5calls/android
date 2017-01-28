package org.a5calls.android.a5calls;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private JsonController mJsonController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Consider using fragments
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mJsonController = new JsonController(getApplicationContext(),
                new JsonController.RequestStatusListener() {
                    @Override
                    public void onRequestError() {
                        // TODO: Display the error info to the user.
                    }

                    @Override
                    public void onJsonError() {
                        // TODO: Display the error info to the user.
                    }
                });

        // TODO: Option to get user's location from GPS instead of just a zip code
        final Button zipButton = (Button) findViewById(R.id.zip_code_submit);
        zipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText zipEdit = (EditText) findViewById(R.id.zip_code);
                String code = zipEdit.getText().toString();
                // Is it a string that is exactly 5 characters long?
                if (TextUtils.isEmpty(code) || code.length() != 5) {
                    zipEdit.setError(getResources().getString(R.string.zip_error));
                    return;
                }
                try {
                    // Make sure it is a number, too, by trying to parse it.
                    int unused = Integer.parseInt(code);
                } catch (NumberFormatException e) {
                    zipEdit.setError(getResources().getString(R.string.zip_error));
                    return;
                }
                // If we made it here, the zip is valid! Send the request.
                mJsonController.getZip(code);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mJsonController.onDestroy();
        super.onDestroy();
    }
}

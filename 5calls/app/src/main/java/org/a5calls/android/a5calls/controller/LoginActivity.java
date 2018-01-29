package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.result.Credentials;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AuthenticationManager;
import org.a5calls.android.a5calls.model.User;

/**
 * An activity which can handle first-time login.
 */
public abstract class LoginActivity extends AppCompatActivity {
    // Login from a totally logged out state.
    protected void login() {
        final AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        authManager.doLogin(this, new AuthCallback() {
            @Override
            public void onFailure(@NonNull Dialog dialog) {
                dialog.show();
            }

            @Override
            public void onFailure(AuthenticationException exception) {
                cancelLoginOnError(authManager, exception.getDescription());
            }

            @Override
            public void onSuccess(@NonNull final Credentials credentials) {
                // Save and then use new credentials to try logging in.
                authManager.onLogin(credentials);
                authManager.getUserInfo(getApplicationContext(), credentials,
                        new AuthenticationManager.UserCredentialsCallback() {
                            @Override
                            public void onCredentialsFailure(CredentialsManagerException error) {
                                cancelLoginOnError(authManager, error.getMessage());
                            }

                            @Override
                            public void onAuthenticationException(AuthenticationException error) {
                                cancelLoginOnError(authManager, error.getDescription());
                            }
                        }, new BaseCallback<User, ManagementException>() {

                            @Override
                            public void onFailure(ManagementException error) {
                                cancelLoginOnError(authManager, error.getDescription());
                            }

                            @Override
                            public void onSuccess(final User user) {
                                runOnUiThread(new Runnable() {

                                    public void run() {
                                        onLoginSuccess(credentials, user);
                                        showBackupStatsDialog(user.getUserId(), credentials.getIdToken());
                                    }
                                });
                            }
                        });
            }
        });
    }

    private void cancelLoginOnError(AuthenticationManager authManager, final String message) {
        // Login failed. Show the user a message and set the state back to the not logged in state.
        // Delete current credentials and try again. Since the user hasn't fully logged in yet,
        // it's ok to delete before we sync.
        authManager.removeAccount(getApplicationContext());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(getSnackbarView(),
                        getResources().getString(R.string.login_canceled_on_error, message),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    public abstract void onLoginSuccess(Credentials credentials, User user);

    public abstract View getSnackbarView();

    private void showBackupStatsDialog(String userId, String idToken) {

        // TODO: Show a dialog that says Login Success, and we can will now associate all the stats
        // from this device with your account. If you log out, those stats will not be displayed any
        // more. And gives a chance to cancel.
        //AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper().getStats(userId);
    }
}

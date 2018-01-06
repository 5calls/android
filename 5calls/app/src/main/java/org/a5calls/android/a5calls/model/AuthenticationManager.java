package org.a5calls.android.a5calls.model;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;

/**
 * Handles authentication and account management with Auth0.
 */
public class AuthenticationManager {
    private final AuthenticationAPIClient mApiClient;
    private final CredentialsManager mCredentialsManager;
    private final Auth0 mAccount;

    public interface BackgroundLoginCallback {
        void onSuccess(UserProfile payload);
        void onLoginFailure(AuthenticationException error);
        void onCredentialsFailure(CredentialsManagerException error);
    }

    public AuthenticationManager(Context context) {
        mAccount = new Auth0(context);
        mAccount.setOIDCConformant(true);
        mApiClient = new AuthenticationAPIClient(mAccount);
        mCredentialsManager = new CredentialsManager(mApiClient,
                new SharedPreferencesStorage(context));
    }

    public void doLogin(Activity activity, AuthCallback authCallback) {
        WebAuthProvider.init(mAccount)
                // Request offline_access to get a token
                .withScope("openid offline_access profile email")
                .start(activity, authCallback);
    }

    public void onLogin(Credentials credentials) {
        mCredentialsManager.saveCredentials(credentials);
    }

    public void removeAccount() {
        mCredentialsManager.clearCredentials();
    }

    public boolean hasSavedCredentials() {
        return mCredentialsManager.hasValidCredentials();
    }

    // Tries to use the existing saved account to log in. Uses the BackgroundLoginCallback
    // to inform the caller if there is no existing account, if login failed, or if it succeeded.
    public void loginWithSavedCredentials(final BackgroundLoginCallback callback) {
        mCredentialsManager.getCredentials(
                new BaseCallback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials credentials) {
                //Use credentials
                getUserInfo(credentials, callback);
            }

            @Override
            public void onFailure(CredentialsManagerException error) {
                //No credentials were previously saved or they couldn't be refreshed
                callback.onCredentialsFailure(error);
            }
        });
    }

    public void getUserInfo(Credentials credentials, final BackgroundLoginCallback callback) {
        mApiClient.userInfo(credentials.getAccessToken()).start(
                new BaseCallback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(final UserProfile payload) {
                        callback.onSuccess(payload);
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        //Delete current credentials and try again.
                        removeAccount();
                        callback.onLoginFailure(error);
                    }
                });
    }
}

package org.a5calls.android.a5calls.model;

import android.app.Activity;
import android.content.Context;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.management.UsersAPIClient;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles authentication and account management with Auth0.
 */
public class AuthenticationManager {
    private final AuthenticationAPIClient mApiClient;
    private final CredentialsManager mCredentialsManager;
    private final Auth0 mAccount;

    public interface UserCredentialsCallback {
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

    private void cacheUserProfile(Context context, User user) {
        AccountManager.Instance.cacheUserProfile(context, user);
    }

    public User getCachedUserProfile(Context context) {
        return AccountManager.Instance.getCachedUserProfile(context);
    }

    public void onLogin(Credentials credentials) {
        mCredentialsManager.saveCredentials(credentials);
    }

    public void removeAccount(Context context) {
        mCredentialsManager.clearCredentials();
        AccountManager.Instance.clearUserProfile(context);
    }

    public boolean hasSavedCredentials() {
        return mCredentialsManager.hasValidCredentials();
    }

    // Tries to use the existing saved account to log in. Uses the UserCredentialsCallback
    // to inform the caller if there is no existing account, if login failed, or if it succeeded.
    public void loginWithSavedCredentials(final Context context,
            final UserCredentialsCallback loginCallback,
            final BaseCallback<User, AuthenticationException> callback) {
        mCredentialsManager.getCredentials(
                new BaseCallback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(Credentials credentials) {
                //Use credentials
                getUserInfo(context, credentials, callback);
            }

            @Override
            public void onFailure(CredentialsManagerException error) {
                //No credentials were previously saved or they couldn't be refreshed
                loginCallback.onCredentialsFailure(error);
            }
        });
    }

    public void getUserInfo(final Context context, final Credentials credentials,
                            final BaseCallback<User, AuthenticationException> callback) {
        mApiClient.userInfo(credentials.getAccessToken()).start(
                new BaseCallback<UserProfile, AuthenticationException>() {
            @Override
            public void onSuccess(UserProfile payload) {
                // Get the full user profile so that we can get metadata.
                UsersAPIClient users = new UsersAPIClient(mAccount, credentials.getIdToken());
                users.getProfile(payload.getId()).start(
                        new BaseCallback<UserProfile, ManagementException>() {
                    @Override
                    public void onSuccess(UserProfile payload) {
                        final User user = new User(payload);
                        cacheUserProfile(context, user);
                        callback.onSuccess(user);
                    }

                    @Override
                    public void onFailure(ManagementException error) {
                        // TODO: Add more to this callback.
                        //callback.onFailure(error);
                    }
                });

            }

            @Override
            public void onFailure(AuthenticationException error) {
                callback.onFailure(error);
            }
        });
    }

    // For example, "nickname", "katie".
    public void updateUserData(final Context context, final String key, final Object value,
                               final UserCredentialsCallback loginCallback,
                               final BaseCallback<User, ManagementException> callback) {
        // First get the credentials so we can use the token to update the user profile.
        mCredentialsManager.getCredentials(
                new BaseCallback<Credentials, CredentialsManagerException>() {
            @Override
            public void onSuccess(final Credentials credentials) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put(key, value);
                UsersAPIClient users = new UsersAPIClient(mAccount, credentials.getIdToken());
                users.updateMetadata(getCachedUserProfile(context).getUserId(), metadata)
                        .start(new BaseCallback<UserProfile, ManagementException>() {
                    @Override
                    public void onSuccess(UserProfile userProfile) {
                        User user = new User(userProfile);
                        cacheUserProfile(context, user);
                        callback.onSuccess(user);
                    }

                    @Override
                    public void onFailure(ManagementException error) {
                        callback.onFailure(error);
                    }
                });
            }

            @Override
            public void onFailure(CredentialsManagerException error) {
                loginCallback.onCredentialsFailure(error);
            }
        });

    }
}

package org.a5calls.android.a5calls.model;

import android.app.Activity;
import android.content.Context;

import com.auth0.android.Auth0;
import com.auth0.android.provider.AuthCallback;
import com.auth0.android.provider.WebAuthProvider;
import com.auth0.android.result.Credentials;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.controller.MainActivity;

/**
 * Handles authentication and account management with Auth0.
 */
public class AuthenticationManager {
    Auth0 mAccount;

    public AuthenticationManager(Context context) {
        mAccount = new Auth0(context);
        mAccount.setOIDCConformant(true);
    }

    public void doAuthentication(Activity activity, AuthCallback authCallback) {
        WebAuthProvider.init(mAccount)
                // Request offline_access to get a token
                .withScope("openid offline_access")
                .start(activity, authCallback);
    }

    public void onAuthentication(Context context, Credentials credentials) {
        AccountManager.Instance.saveCredentials(context, credentials);
    }

    public String getAccessToken(Context context) {
        return AccountManager.Instance.getCredentials(context).getAccessToken();
    }

    public void removeAccount(Context context) {
        AccountManager.Instance.deleteCredentials(context);
    }
}

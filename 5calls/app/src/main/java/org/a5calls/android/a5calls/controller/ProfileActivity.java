package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.auth0.android.result.Credentials;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AuthenticationManager;
import org.a5calls.android.a5calls.model.Stat;
import org.a5calls.android.a5calls.model.StatSummary;
import org.a5calls.android.a5calls.model.User;
import org.a5calls.android.a5calls.net.FiveCallsApi;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity to show a user's profile.
 * TODO: Profile editing -- photo
 * TODO: Error handling
 * TODO: "share" profile has a link to a 5calls.org page?
 * TODO: Display a user's teams, let a user join teams
 */
public class ProfileActivity extends AppCompatActivity {
    private static final String KEY_IS_EDITING = "keyIsEditing";
    private static final String KEY_SAVED_NICKNAME = "keySavedNickname";
    private User mUser;

    @BindView(R.id.user_picture) ImageView mPicture;
    @BindView(R.id.user_name) TextView mNickname;
    @BindView(R.id.user_email) TextView mEmail;
    @BindView(R.id.user_stats) TextView mUserStats;
    @BindView(R.id.btn_stats_activity) Button mStatsButton;
    @BindView(R.id.btn_edit_nickname) ImageButton mEditNicknameButton;
    @BindView(R.id.btn_save_nickname) ImageButton mNicknameSaveButton;
    @BindView(R.id.user_name_edittext) EditText mNicknameEditText;
    private FiveCallsApi.StatsRequestListener mStatsRequestListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        mUser = authManager.getCachedUserProfile(getApplicationContext());
        if (mUser == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getResources().getString(R.string.profile_activity));
        }

        Drawable drawable = mEditNicknameButton.getDrawable().mutate();
        drawable.setColorFilter(
                getResources().getColor(R.color.textColorLightGrey), PorterDuff.Mode.MULTIPLY);
        mEditNicknameButton.setImageDrawable(drawable);

        drawable = mNicknameSaveButton.getDrawable().mutate();
        drawable.setColorFilter(
                getResources().getColor(R.color.textColorLightGrey), PorterDuff.Mode.MULTIPLY);
        mNicknameSaveButton.setImageDrawable(drawable);
        mEditNicknameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allowEditNickname(true);
            }
        });
        mNicknameSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Verify that edit is appropriate with the user's current string.
                String newNickname = mNicknameEditText.getText().toString();
                if (TextUtils.isEmpty(newNickname)) {
                    Snackbar.make(mNicknameEditText,
                            getResources().getString(R.string.empty_nickname_error),
                            Snackbar.LENGTH_SHORT).show();
                } else if (TextUtils.equals(newNickname, mUser.getNickname())) {
                    // No change. Don't update it.
                    allowEditNickname(false);
                    return;
                }
                AppSingleton
                        .getInstance(getApplicationContext())
                        .getAuthenticationManager()
                        .updateUserData(getApplicationContext(), User.KEY_NICKNAME,
                                mNicknameEditText.getText().toString(),
                                new AuthenticationManager.UserCredentialsCallback() {
                                    @Override
                                    public void onCredentialsFailure(
                                            final CredentialsManagerException error) {
                                        showErrorOnUiThread(error.getMessage());
                                    }

                                    @Override
                                    public void onAuthenticationException(
                                            AuthenticationException error) {
                                        showErrorOnUiThread(error.getMessage());
                                    }
                                }, new BaseCallback<User, ManagementException>() {
                                    @Override
                                    public void onSuccess(User user) {
                                        mUser = user;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mNickname.setText(mUser.getNickname());
                                                allowEditNickname(false);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(final ManagementException error) {
                                        showErrorOnUiThread(error.getDescription());
                                    }
                                });
            }
        });

        mStatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });

        mStatsRequestListener = new FiveCallsApi.StatsRequestListener() {
            @Override
            public void onRequestError() {
                mUserStats.setText(R.string.request_error);
            }

            @Override
            public void onJsonError() {
                mUserStats.setText("Error loading stats from server");
            }

            @Override
            public void onStatsReceived(StatSummary summary) {
                mUserStats.setText(getResources().getString(R.string.profile_stats,
                        summary.getTotalCalls()));
            }

            @Override
            public void onStatsUpdated(List<Stat> stats) {
                // Refresh the user data if the stats list was updated.
                loadUserData();
            }
        };
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .registerStatsRequestListener(mStatsRequestListener);

        if (savedInstanceState != null) {
            boolean isEditing = savedInstanceState.getBoolean(KEY_IS_EDITING, false);
            if (isEditing) {
                allowEditNickname(true);
                String savedNickname = savedInstanceState.getString(KEY_SAVED_NICKNAME,
                        mUser.getNickname());
                mNicknameEditText.setText(savedNickname);
            }
        }
    }

    @Override
    protected void onDestroy() {
        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                .unregisterStatsRequestListener(mStatsRequestListener);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_EDITING, mNicknameEditText.getVisibility() == View.VISIBLE);
        outState.putString(KEY_SAVED_NICKNAME, mNicknameEditText.getText().toString());
    }

    private void allowEditNickname(boolean isEditing) {
        mNicknameEditText.setVisibility(isEditing ? View.VISIBLE : View.GONE);
        mNicknameSaveButton.setVisibility(isEditing? View.VISIBLE : View.GONE);
        mNickname.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        mEditNicknameButton.setVisibility(isEditing ? View.GONE : View.VISIBLE);
        mNicknameEditText.setText(mUser.getNickname());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_logout:
                logout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mNicknameSaveButton.getVisibility() == View.VISIBLE) {
            // The user just wants to close out of editing mode.
            allowEditNickname(false);
            return;
        }
        super.onBackPressed();
    }

    private void loadUserData() {
        mNickname.setText(mUser.getNickname());
        mEmail.setText(mUser.getEmail());
        Glide.with(getApplicationContext())
                .load(mUser.getPicture())
                .asBitmap()
                .centerCrop()
                .into(new BitmapImageViewTarget(mPicture) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(
                                mPicture.getContext().getResources(), resource);
                        drawable.setCircular(true);
                        drawable.setGravity(Gravity.TOP);
                        mPicture.setImageDrawable(drawable);
                    }
                });
        AppSingleton.getInstance(getApplicationContext()).getAuthenticationManager().getCredentials(
                new BaseCallback<Credentials, CredentialsManagerException>() {
                    @Override
                    public void onSuccess(Credentials credentials) {
                        AppSingleton.getInstance(getApplicationContext()).getJsonController()
                                .getUserStats(credentials.getIdToken());
                    }

                    @Override
                    public void onFailure(CredentialsManagerException error) {
                        mUserStats.setText(R.string.request_error);
                    }
        });
    }

    private void logout() {
        // TODO: Show the user a dialog that logging out (will? will not?) clear their local data.
        // Don't actually log out until they confirm the dialog.
        AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        authManager.removeAccount(getApplicationContext());
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showErrorOnUiThread(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(findViewById(R.id.activity_profile),
                        getResources().getString(R.string.edit_nickname_failure, message),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
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

import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.management.ManagementException;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AuthenticationManager;
import org.a5calls.android.a5calls.model.User;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Activity to show a user's profile.
 * TODO: Profile editing -- nickname and photo
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

        int callCount = AppSingleton.getInstance(getApplicationContext()).getDatabaseHelper()
                .getCallsCount();
        mUserStats.setText(getResources().getString(R.string.profile_stats, callCount));


        Drawable drawable = mEditNicknameButton.getDrawable().mutate();
        drawable.setColorFilter(
                getResources().getColor(R.color.textColorDarkGrey), PorterDuff.Mode.MULTIPLY);
        mEditNicknameButton.setImageDrawable(drawable);

        drawable = mNicknameSaveButton.getDrawable().mutate();
        drawable.setColorFilter(
                getResources().getColor(R.color.textColorDarkGrey), PorterDuff.Mode.MULTIPLY);
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
                AppSingleton
                        .getInstance(getApplicationContext())
                        .getAuthenticationManager()
                        .updateUserData(getApplicationContext(), User.KEY_NICKNAME,
                                mNicknameEditText.getText().toString(),
                                new AuthenticationManager.UserCredentialsCallback() {
                                    @Override
                                    public void onCredentialsFailure(CredentialsManagerException e) {
                                        // TODO show a toast
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
                                    public void onFailure(ManagementException error) {
                                        // TODO show a toast
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

    private void logout() {
        // TODO: Show the user a dialog that logging out (will? will not?) clear their local data.
        // Don't actually log out yet.
        AuthenticationManager authManager = AppSingleton.getInstance(getApplicationContext())
                .getAuthenticationManager();
        authManager.removeAccount(getApplicationContext());
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

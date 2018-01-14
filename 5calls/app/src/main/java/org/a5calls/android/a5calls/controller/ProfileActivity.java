package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
 * TODO: Profile editing
 * TODO: "share" profile has a link to a 5calls.org page?
 * TODO: Display a user's teams, let a user join teams
 */
public class ProfileActivity extends AppCompatActivity {
    private User mUser;

    @BindView(R.id.user_picture) ImageView mPicture;
    @BindView(R.id.user_name) TextView mNickname;
    @BindView(R.id.user_email) TextView mEmail;
    @BindView(R.id.user_stats) TextView mUserStats;
    @BindView(R.id.btn_stats_activity) Button mStatsButton;

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

        // TODO: Leave immediately if no one is logged in.

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

        mStatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, StatsActivity.class);
                startActivity(intent);
            }
        });
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

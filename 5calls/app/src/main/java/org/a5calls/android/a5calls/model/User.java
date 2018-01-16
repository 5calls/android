package org.a5calls.android.a5calls.model;

import android.text.TextUtils;

import com.auth0.android.result.UserProfile;

/**
 * Represents an Auth0 User.
 * Any fields 5calls cares about should be added here from the UserProfile object.
 * This is separate from a UserProfile because we do not need to store and track every part of that
 * object.
 */
public class User {
    public static final String KEY_NICKNAME = "nickname";
    private final String mEmail;
    private final String mNickname;
    private final String mPicture;
    private final String mUserId;

    public User(String email, String nickname, String picture, String userId) {
        mEmail = email;
        mNickname = nickname;
        mPicture = picture;
        mUserId = userId;
    }

    // Use the auth0 generated nickname if the user hasn't set their own yet.
    // We can only edit the userMetadata, not the nickname itself directly.
    public User(UserProfile payload) {
        mEmail = payload.getEmail();
        if (payload.getUserMetadata().containsKey("nickname")) {
            mNickname = (String) payload.getUserMetadata().get("nickname");
        } else {
            mNickname = payload.getNickname();
        }
        mPicture = payload.getPictureURL();
        mUserId = payload.getId();
    }

    public String getEmail() {
        return mEmail;
    }

    public String getNickname() {
        return mNickname;
    }

    public String getPicture() {
        return mPicture;
    }

    public String getUserId() {
        return mUserId;
    }
}

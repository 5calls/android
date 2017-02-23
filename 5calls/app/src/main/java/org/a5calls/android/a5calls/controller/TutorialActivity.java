package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.model.FiveCallsApi;
import org.a5calls.android.a5calls.model.Issue;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Tutorial / splash screen activity
 */
public class TutorialActivity extends AppCompatActivity {
    private static final String TAG = "TutorialActivity";

    @BindView(R.id.view_pager) ViewPager viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

        viewPager.setAdapter(new TutorialPagerAdapter(getSupportFragmentManager()));
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }

    public void onNextPressed() {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We allow Analytics opt-out.
        if (AccountManager.Instance.allowAnalytics(this)) {
            // Obtain the shared Tracker instance.
            FiveCallsApplication application = (FiveCallsApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();
            tracker.setScreenName(TAG);
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
        }
    }

    private class TutorialPagerAdapter extends FragmentPagerAdapter {

        public TutorialPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return FirstTutorialPageFragment.newInstance();
            } else if (position == 1) {
                return SecondTutorialPageFragment.newInstance();
            } else if (position == 2) {
                return ThirdTutorialPageFragment.newInstance();
            }
            return null;
        }
    }

    public static class FirstTutorialPageFragment extends Fragment {
        public static FirstTutorialPageFragment newInstance() {
            FirstTutorialPageFragment fragment = new FirstTutorialPageFragment();
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.tutorial_item_1, container, false);
            rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TutorialActivity) getActivity()).onNextPressed();
                }
            });
            return rootView;
        }
    }

    public static class SecondTutorialPageFragment extends Fragment {

        public static SecondTutorialPageFragment newInstance() {
            SecondTutorialPageFragment fragment = new SecondTutorialPageFragment();
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.tutorial_item_2, container, false);
            rootView.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TutorialActivity) getActivity()).onNextPressed();
                }
            });
            return rootView;
        }
    }

    public static class ThirdTutorialPageFragment extends Fragment {

        private FiveCallsApi.RequestStatusListener mStatusListener;
        private TextView callsToDate;

        public static ThirdTutorialPageFragment newInstance() {
            ThirdTutorialPageFragment fragment = new ThirdTutorialPageFragment();
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onDestroy() {
            AppSingleton.getInstance(getActivity()).getJsonController()
                    .unregisterStatusListener(mStatusListener);
            super.onDestroy();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.tutorial_item_3, container, false);
            callsToDate = (TextView) rootView.findViewById(R.id.calls_to_date);

            // TODO: Re-use this listener between AboutActivity and here, since it's really the same.
            mStatusListener = new FiveCallsApi.RequestStatusListener() {
                @Override
                public void onRequestError() {
                    if (!isAdded()) {
                        // No longer attached to the activity!
                        return;
                    }
                    Snackbar.make(callsToDate,
                            getResources().getString(R.string.request_error),
                            Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onJsonError() {
                    if (!isAdded()) {
                        // No longer attached to the activity!
                        return;
                    }
                    Snackbar.make(callsToDate,
                            getResources().getString(R.string.json_error),
                            Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onIssuesReceived(String locationName, List<Issue> issues) {
                    // unused
                }

                @Override
                public void onCallCount(int count) {
                    if (!isAdded()) {
                        // No longer attached to the activity!
                        return;
                    }
                    callsToDate.setText(String.format(
                            getResources().getString(R.string.calls_to_date),
                            NumberFormat.getNumberInstance(Locale.US).format(count)));
                }

                @Override
                public void onCallReported() {
                    // unused
                }
            };
            FiveCallsApi controller = AppSingleton.getInstance(getActivity())
                    .getJsonController();
            controller.registerStatusListener(mStatusListener);
            controller.getCallCount();

            rootView.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            rootView.findViewById(R.id.get_started_btn).setOnClickListener(
                    new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Set that the user has seen info about reminders.
                    AccountManager.Instance.setRemindersInfoShown(getActivity(), true);
                    SettingsActivity.turnOnReminders(getActivity(), AccountManager.Instance);

                    // Return to the main activity
                    AccountManager.Instance.setTutorialSeen(getActivity(), true);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            });
            return rootView;
        }
    }
}

package org.a5calls.android.a5calls.controller;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.databinding.ActivityTutorialBinding;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FiveCallsApi;
import org.a5calls.android.a5calls.util.AnalyticsManager;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tutorial / splash screen activity
 */
public class TutorialActivity extends AppCompatActivity {
    private static final String TAG = "TutorialActivity";

    private ActivityTutorialBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTutorialBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new TutorialPagerAdapter(getSupportFragmentManager()));

        new AnalyticsManager().trackPageview("/tutorial", this);
    }

    @Override
    public void onBackPressed() {
        if (binding.viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1);
        }
    }

    public void onNextPressed() {
        binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
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

        private FiveCallsApi.CallRequestListener mStatusListener;
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
                    .unregisterCallRequestListener(mStatusListener);
            super.onDestroy();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.tutorial_item_3, container, false);
            callsToDate = (TextView) rootView.findViewById(R.id.calls_to_date);

            // TODO: Re-use this listener between AboutActivity and here, since it's really the same.
            mStatusListener = new FiveCallsApi.CallRequestListener() {
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
                public void onReportReceived(int count, boolean donateOn) {
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
            controller.registerCallRequestListener(mStatusListener);
            controller.getReport();

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

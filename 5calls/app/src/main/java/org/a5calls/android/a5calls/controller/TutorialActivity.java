package org.a5calls.android.a5calls.controller;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.FiveCallsApplication;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.databinding.ActivityTutorialBinding;
import org.a5calls.android.a5calls.model.AccountManager;
import org.a5calls.android.a5calls.net.FiveCallsApi;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tutorial / splash screen activity
 */
public class TutorialActivity extends AppCompatActivity {
    private static final String TAG = "TutorialActivity";
    private static final int PLACEHOLDER_CALL_COUNT = 12500000;

    private ActivityTutorialBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityTutorialBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new TutorialPagerAdapter(getSupportFragmentManager()));
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                            WindowInsetsCompat.Type.displayCutout());
            binding.viewPager.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.viewPager.getCurrentItem() == 0) {
                    // If the user is currently looking at the first step, allow the system to handle the
                    // Back button. Call finish() on this activity and pop the back stack.
                    finish();
                } else {
                    // Otherwise, select the previous step.
                    onPreviousPagePressed();
                }
            }
        });

        FiveCallsApplication.analyticsManager().trackPageview("/tutorial", this);
    }

    public void onPreviousPagePressed() {
        if (binding.viewPager.getCurrentItem() > 0) {
            binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() - 1);
        }
    }

    public void onNextPagePressed() {
        binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
    }

    private class TutorialPagerAdapter extends FragmentPagerAdapter {

        public TutorialPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return FirstTutorialPageFragment.newInstance();
            } else if (position == 1) {
                return StaticTutorialPageFragment.newInstance(R.layout.tutorial_item_2);
            } else if (position == 2) {
                return StaticTutorialPageFragment.newInstance(R.layout.tutorial_item_3);
            } else if (position == 3) {
                return FourthTutorialPageFragment.newInstance();
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
                    ((TutorialActivity) getActivity()).onNextPagePressed();
                }
            });
            return rootView;
        }
    }

    public static class StaticTutorialPageFragment extends Fragment {

        private final int layoutId;

        public static StaticTutorialPageFragment newInstance(int layoutId) {
            return new StaticTutorialPageFragment(layoutId);
        }

        public StaticTutorialPageFragment(int layoutId) {
            this.layoutId = layoutId;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(layoutId, container, false);
            rootView.findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TutorialActivity) getActivity()).onPreviousPagePressed();
                }
            });
            rootView.findViewById(R.id.btn_next).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((TutorialActivity) getActivity()).onNextPagePressed();
                }
            });
            return rootView;
        }
    }

    public static class FourthTutorialPageFragment extends Fragment {

        private FiveCallsApi.CallRequestListener mStatusListener;
        private TextView callsToDate;
        private Button remindersBtn;
        private TextView remindersDoneText;

        private ActivityResultLauncher<String> mNotificationPermissionRequest;

        public static FourthTutorialPageFragment newInstance() {
            return new FourthTutorialPageFragment();
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNotificationPermissionRequest =
                    SettingsActivity.createNotificationPermissionRequest(this, (isGranted) -> {
                        // If the user denied the notification permission, set the preference to false
                        // Otherwise they granted and we will set the permission to true.
                        if (isGranted) {
                            turnOnReminders();
                        } else {
                            remindersBtn.setVisibility(View.GONE);
                            remindersDoneText.setText(R.string.about_reminders_off);
                            remindersDoneText.setVisibility(View.VISIBLE);
                        }
                    });
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
            View rootView = inflater.inflate(R.layout.tutorial_item_4, container, false);
            callsToDate = rootView.findViewById(R.id.calls_to_date);
            remindersBtn = rootView.findViewById(R.id.reminders_btn);
            remindersDoneText = rootView.findViewById(R.id.reminders_done);

            remindersBtn.setOnClickListener(v -> {
                if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Show notification permission then turn on reminders depending on the result.
                        mNotificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                } else {
                    // Don't need to ask for notification permission, can simply
                    // directly enable reminders.
                    turnOnReminders();
                }
            });

            mStatusListener = new FiveCallsApi.CallRequestListener() {
                @Override
                public void onRequestError() {
                    // unused
                }

                @Override
                public void onJsonError() {
                    // unused
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

            rootView.findViewById(R.id.btn_back).setOnClickListener(v ->
                    ((TutorialActivity) getActivity()).onPreviousPagePressed());
            rootView.findViewById(R.id.get_started_btn).setOnClickListener(
                    v -> {
                        // Set that the user has seen info about reminders.
                        AccountManager.Instance.setRemindersInfoShown(getActivity(), true);

                        // Return to the main activity
                        AccountManager.Instance.setTutorialSeen(getActivity(), true);
                        Intent intent = new Intent(getActivity(), LocationActivity.class);
                        intent.putExtra(LocationActivity.ALLOW_HOME_UP_KEY, false);
                        startActivity(intent);
                        getActivity().finish();
                    });

            callsToDate.setText(String.format(
                    getResources().getString(R.string.calls_to_date),
                    NumberFormat.getNumberInstance(Locale.US).format(PLACEHOLDER_CALL_COUNT)));

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            if (AccountManager.Instance.getAllowReminders(getContext())) {
                remindersBtn.setVisibility(View.GONE);
                remindersDoneText.setVisibility(View.VISIBLE);
            }
        }

        private void turnOnReminders() {
            AccountManager.Instance.setAllowReminders(getActivity(), true);
            SettingsActivity.turnOnReminders(getActivity(), AccountManager.Instance);
            remindersBtn.setVisibility(View.GONE);
            remindersDoneText.setVisibility(View.VISIBLE);
        }
    }
}

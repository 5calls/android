package org.a5calls.android.a5calls.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.DatabaseHelper;
import org.a5calls.android.a5calls.model.Issue;
import org.a5calls.android.a5calls.util.StateMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class IssuesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // TODO: Use an enum.
    public static final int NO_ERROR = 10;
    public static final int ERROR_REQUEST = 11;
    public static final int ERROR_ADDRESS = 12;
    public static final int NO_ISSUES_YET = 13;
    public static final int ERROR_SEARCH_NO_MATCH = 14;

    private static final int VIEW_TYPE_EMPTY_REQUEST = 0;
    private static final int VIEW_TYPE_ISSUE = 1;
    private static final int VIEW_TYPE_EMPTY_ADDRESS = 2;
    private static final int VIEW_TYPE_NO_SEARCH_MATCH = 3;

    private List<Issue> mIssues = new ArrayList<>();
    private List<Issue> mAllIssues = new ArrayList<>();
    private int mErrorType = NO_ISSUES_YET;
    private int mAddressErrorType = NO_ISSUES_YET;

    private List<Contact> mContacts = new ArrayList<>();
    private final Activity mActivity;
    private final Callback mCallback;

    public interface Callback {

        void refreshIssues();

        void launchLocationActivity();

        void launchSearchDialog();

        void startIssueActivity(Context context, Issue issue);
    }

    public IssuesAdapter(Activity activity, Callback callback) {
        mActivity = activity;
        mCallback = callback;
    }

    /**
     * Sets the full list of available issues. Does not update the visible list unless there
     * is an error; {@code #setFilterAndSearch} should be called separately to update the
     * visible list.
     * @param issues The full list of available issues.
     * @param errorType The error, if there is one.
     */
    public void setAllIssues(List<Issue> issues, int errorType) {
        mAllIssues = issues;
        mErrorType = errorType;
        if (mErrorType != NO_ERROR) {
            mIssues.clear();
            notifyDataSetChanged();
        }
    }

    public void setAddressError(int error) {
        mAddressErrorType = error;
        mContacts.clear();
        if (!mAllIssues.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    public void setContacts(List<Contact> contacts, int error) {
        // Check if the contacts have returned after the issues list. If so, notify data set
        // changed.
        mAddressErrorType = error;
        boolean notify = false;
        if (!mAllIssues.isEmpty() && mContacts.isEmpty()) {
            notify = true;
        }
        mContacts = contacts;
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public void setFilterAndSearch(String filterText, String searchText) {
        if (mErrorType == ERROR_SEARCH_NO_MATCH) {
            // If we previously had a search error, reset it: this is a new
            // filter or search.
            mErrorType = NO_ERROR;
        }
        if (!TextUtils.isEmpty(searchText)) {
            mIssues = sortIssuesWithMetaPriority(filterIssuesBySearchText(searchText, mAllIssues));
            // If there's no other error, show a search error.
            if (mIssues.isEmpty() && mErrorType == NO_ERROR) {
                mErrorType = ERROR_SEARCH_NO_MATCH;
            }
        } else {
            // Search text is empty.
            if (TextUtils.equals(filterText,
                    mActivity.getResources().getString(R.string.all_issues_filter))) {
                // Include everything
                mIssues = sortIssuesWithMetaPriority(mAllIssues);
            } else if (TextUtils.equals(filterText,
                    mActivity.getResources().getString(R.string.top_issues_filter))) {
                mIssues = sortIssuesWithMetaPriority(filterActiveIssues());
            } else {
                // Filter by the category string.
                mIssues = sortIssuesWithMetaPriority(filterIssuesByCategory(filterText));
            }
        }
        notifyDataSetChanged();
    }

    @VisibleForTesting
    public static ArrayList<Issue> filterIssuesBySearchText(String searchText, List<Issue> allIssues) {
        ArrayList<Issue> tempIssues = new ArrayList<>();
        // Should we .trim() the whitespace?
        String lowerSearchText = searchText.toLowerCase();

        /*
         * When name and category fields are searched, String#contains is used.
         * However, searching reason fields uses a different strategy: searching
         * for words (or sequences of words) that start with searchText
         * Motivating example:
         * """
         * "ice" [shouldn't] match "averice" but [should match] just ICE"
         * """
         * In the JDK's regex implementation, \b expresses "word boundary".
         * https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
         * However, different regex interpretations may have subtly different
         * definitions of "word boundary"; see
         * https://www.rexegg.com/regex-boundaries.php#wordboundary
         *
         * {@link Pattern#quote} is used to quote searchText.
         * Motivating example:
         * if searchText := "[", the old implementation would attempt to create
         * a regex pattern through Pattern.compile("\\s[") and then throw an
         * exception because "\\s[" is not a valid regex.
         * Another motivating example:
         * if searchText := "land.", the old pattern would create regex pattern
         * Pattern.compile("\\sland."), which will match " lands", " lander",
         * " land-swap", etc.
         * XXX TODO: Search texts like "[" and ")" may match Markdown for links
         *      (example: "[a link](www.example.com)"), which is not visible in
         *      rendered text; is this acceptable?
         */
        Pattern pattern = Pattern.compile(
                String.format("\\b%s", Pattern.quote(searchText)),
                Pattern.CASE_INSENSITIVE
        );

        for (Issue issue : allIssues) {
            // Search the name and the categories for the search term.
            if (issue.name.toLowerCase().contains(lowerSearchText)) {
                tempIssues.add(issue);
            } else {
                boolean found = false;
                for (int i = 0; i < issue.categories.length; i++) {
                    if (issue.categories[i].name.toLowerCase().contains(lowerSearchText)) {
                        tempIssues.add(issue);
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                // Search through the issue's reason for words that start with the
                // search text. This is better than substring matching so that text
                // like "ice" doesn't match "averice" but just ICE.
                if (pattern.matcher(issue.reason).find()) {
                    tempIssues.add(issue);
                }
            }
        }
        return tempIssues;
    }

    private ArrayList<Issue> filterActiveIssues() {
        // Add only the active ones.
        ArrayList<Issue> tempIssues = new ArrayList<>();
        for (Issue issue : mAllIssues) {
            if (issue.active) {
                tempIssues.add(issue);
            }
        }
        return tempIssues;
    }

    private ArrayList<Issue> filterIssuesByCategory(String activeCategory) {
        ArrayList<Issue> tempIssues = new ArrayList<>();
        for (Issue issue : mAllIssues) {
            for (Category category : issue.categories) {
                if (TextUtils.equals(activeCategory, category.name)) {
                    tempIssues.add(issue);
                }
            }
        }
        return tempIssues;
    }

    public void updateIssue(Issue issue) {
        for (int i = 0; i < mIssues.size(); i++) {
            if (TextUtils.equals(issue.id, mIssues.get(i).id)) {
                mIssues.set(i, issue);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public boolean hasContacts() {
        return !mContacts.isEmpty();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY_REQUEST) {
            View empty = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.empty_issues_view, parent, false);
            return new EmptyRequestViewHolder(empty);
        } else if (viewType == VIEW_TYPE_EMPTY_ADDRESS) {
            View empty = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.empty_issues_address_view, parent, false);
            return new EmptyAddressViewHolder(empty);
        } else if (viewType == VIEW_TYPE_NO_SEARCH_MATCH) {
            View empty = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.empty_issues_search_view, parent, false);
            return new EmptySearchViewHolder(empty);
        } else {
            ConstraintLayout v = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_view, parent, false);
            return new IssueViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == VIEW_TYPE_ISSUE) {
            IssueViewHolder vh = (IssueViewHolder) holder;
            final Issue issue = mIssues.get(position);
            vh.name.setText(issue.name);

            // Show state indicator if issue has meta (state abbreviation) and we can map it to a state name
            if (!TextUtils.isEmpty(issue.meta)) {
                String stateName = StateMapping.getStateName(issue.meta);
                if (!TextUtils.isEmpty(stateName)) {
                    vh.stateIndicator.setText(stateName);
                    vh.stateIndicator.setVisibility(View.VISIBLE);

                } else {
                    vh.stateIndicator.setVisibility(View.GONE);
                }
            } else {
                vh.stateIndicator.setVisibility(View.GONE);
            }
            
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.startIssueActivity(holder.itemView.getContext(), issue);
                }
            });

            if (mAddressErrorType != NO_ERROR) {
                // If there was an address error, clear the number of calls to make.
                vh.numCalls.setText("");
                vh.previousCallStats.setVisibility(View.GONE);
                return;
            }

            // Sometimes an issue is shown with no contact areas in order to
            // inform users that a major vote or change has happened.
            if (issue.contactAreas.isEmpty()) {
                vh.numCalls.setText(
                        mActivity.getResources().getString(R.string.no_contact_areas_message));
                vh.previousCallStats.setVisibility(View.GONE);
                return;
            }

            issue.contacts = new ArrayList<Contact>();
            int houseCount = 0;  // Only add the first contact in the house for each issue.
            for (String contactArea : issue.contactAreas) {
                for (Contact contact : mContacts) {
                    if (TextUtils.equals(contact.area, contactArea) &&
                            !issue.contacts.contains(contact)) {
                        if (TextUtils.equals(contact.area, "US House")) {
                            houseCount++;
                            if (houseCount > 1) {
                                issue.isSplit = true;
                                continue;
                            }
                        }

                        issue.contacts.add(contact);
                    }
                }
            }
            displayPreviousCallStats(issue, vh);
        } else if (type == VIEW_TYPE_EMPTY_REQUEST) {
            EmptyRequestViewHolder vh = (EmptyRequestViewHolder) holder;
            vh.refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.refreshIssues();
                }
            });
        } else if (type == VIEW_TYPE_EMPTY_ADDRESS) {
            EmptyAddressViewHolder vh = (EmptyAddressViewHolder) holder;
            vh.locationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.launchLocationActivity();
                }
            });
        } else if (type == VIEW_TYPE_NO_SEARCH_MATCH) {
            EmptySearchViewHolder vh = (EmptySearchViewHolder) holder;
            vh.searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.launchSearchDialog();
                }
            });
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof IssueViewHolder) {
            holder.itemView.setOnClickListener(null);
        } else if (holder instanceof EmptyRequestViewHolder) {
            ((EmptyRequestViewHolder) holder).refreshButton.setOnClickListener(null);
        } else if (holder instanceof EmptyAddressViewHolder) {
            ((EmptyAddressViewHolder) holder).locationButton.setOnClickListener(null);
        } else if (holder instanceof EmptySearchViewHolder) {
            ((EmptySearchViewHolder) holder).searchButton.setOnClickListener(null);
        }
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        if (mErrorType == ERROR_REQUEST || mErrorType == ERROR_SEARCH_NO_MATCH) {
            // For these special types of errors, we will hide the issues.
            return 1;
        }
        return mIssues.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mIssues.isEmpty() && position == 0) {
            if (mErrorType == ERROR_REQUEST) {
                return VIEW_TYPE_EMPTY_REQUEST;
            }
            if (mErrorType == ERROR_SEARCH_NO_MATCH) {
                return VIEW_TYPE_NO_SEARCH_MATCH;
            }
        }
        return VIEW_TYPE_ISSUE;
    }

    private void displayPreviousCallStats(Issue issue, IssueViewHolder vh) {
        DatabaseHelper dbHelper =  AppSingleton.getInstance(mActivity.getApplicationContext())
                .getDatabaseHelper();
        // Calls ever made.
        int totalUserCalls = dbHelper.getTotalCallsForIssueAndContacts(issue.id, issue.contacts);

        // Calls today only.
        int callsLeft = issue.contacts.size();
        for (Contact contact : issue.contacts) {
            if(dbHelper.hasCalledToday(issue.id, contact.id)) {
                callsLeft--;
            }
        }
        if (totalUserCalls == 0) {
            // The user has never called on this issue before. Show a simple number of calls
            // text, without the word "today".
            vh.previousCallStats.setVisibility(View.GONE);
            if (callsLeft == 1) {
                vh.numCalls.setText(
                        mActivity.getResources().getString(R.string.call_count_one));
            } else {
                vh.numCalls.setText(String.format(
                        mActivity.getResources().getString(R.string.call_count), callsLeft));
            }
        } else {
            vh.previousCallStats.setVisibility(View.VISIBLE);

            // Previous call stats
            if (totalUserCalls == 1) {
                vh.previousCallStats.setText(mActivity.getResources().getString(
                        R.string.previous_call_count_one));
            } else {
                vh.previousCallStats.setText(
                        mActivity.getResources().getString(
                                R.string.previous_call_count_many, totalUserCalls));
            }

            // Calls to make today.
            if (callsLeft == 0) {
                vh.numCalls.setText(
                        mActivity.getResources().getString(R.string.call_count_today_done));
            } else if (callsLeft == 1) {
                vh.numCalls.setText(
                        mActivity.getResources().getString(R.string.call_count_today_one));
            } else {
                vh.numCalls.setText(String.format(
                        mActivity.getResources().getString(R.string.call_count_today), callsLeft));
            }
        }
    }

    private static class IssueViewHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView numCalls;
    public TextView previousCallStats;
    public TextView stateIndicator;

    public IssueViewHolder(View itemView) {
        super(itemView);
        name = (TextView) itemView.findViewById(R.id.issue_name);
        numCalls = (TextView) itemView.findViewById(R.id.issue_call_count);
        previousCallStats = (TextView) itemView.findViewById(R.id.previous_call_stats);
        stateIndicator = (TextView) itemView.findViewById(R.id.state_indicator);
    }
}

// TODO: Combine EmptyRequestViewHolder and EmptyAddressViewHolder, change strings dynamically.
private static class EmptyRequestViewHolder extends RecyclerView.ViewHolder {
    public Button refreshButton;

    public EmptyRequestViewHolder(View itemView) {
        super(itemView);
        refreshButton = (Button) itemView.findViewById(R.id.refresh_btn);
        // Tinting the compound drawable only works API 23+, so do this manually.
        refreshButton.getCompoundDrawablesRelative()[0].mutate().setColorFilter(
                refreshButton.getResources().getColor(R.color.colorAccent),
                PorterDuff.Mode.MULTIPLY);
    }
}

private static class EmptyAddressViewHolder extends RecyclerView.ViewHolder {
    public Button locationButton;

    public EmptyAddressViewHolder(View itemView) {
        super(itemView);
        locationButton = (Button) itemView.findViewById(R.id.location_btn);
        // Tinting the compound drawable only works API 23+, so do this manually.
        locationButton.getCompoundDrawablesRelative()[0].mutate().setColorFilter(
                locationButton.getResources().getColor(R.color.colorAccent),
                PorterDuff.Mode.MULTIPLY);
    }
}

private static class EmptySearchViewHolder extends RecyclerView.ViewHolder {
    public Button searchButton;

    public EmptySearchViewHolder(View itemView) {
        super(itemView);
        searchButton = (Button) itemView.findViewById(R.id.search_btn);
        // Tinting the compound drawable only works API 23+, so do this manually.
        searchButton.getCompoundDrawablesRelative()[0].mutate().setColorFilter(
                searchButton.getResources().getColor(R.color.colorAccent),
                PorterDuff.Mode.MULTIPLY);
    }
}

    /**
     * Sorts a list of issues to prioritize those with meta values (state abbreviations) at the top,
     * then sorts the remaining issues. Both groups maintain their internal sort order.
     */
    @VisibleForTesting
    ArrayList<Issue> sortIssuesWithMetaPriority(List<Issue> issues) {
        ArrayList<Issue> withMeta = new ArrayList<>();
        ArrayList<Issue> withoutMeta = new ArrayList<>();
        
        // Separate issues with and without meta values
        for (Issue issue : issues) {
            if (!TextUtils.isEmpty(issue.meta)) {
                withMeta.add(issue);
            } else {
                withoutMeta.add(issue);
            }
        }
        
        // Sort each group independently by sort field (maintaining consistent order)
        Collections.sort(withMeta, (a, b) -> Integer.compare(a.sort, b.sort));
        Collections.sort(withoutMeta, (a, b) -> Integer.compare(a.sort, b.sort));
        
        // Combine: meta issues first, then regular issues
        ArrayList<Issue> result = new ArrayList<>();
        result.addAll(withMeta);
        result.addAll(withoutMeta);
        
        return result;
    }
}

package org.a5calls.android.a5calls.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.a5calls.android.a5calls.AppSingleton;
import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.Category;
import org.a5calls.android.a5calls.model.Contact;
import org.a5calls.android.a5calls.model.Issue;

import java.util.ArrayList;
import java.util.List;

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

    // |searchText| takes priority over |filterText|.
    public void setIssues(List<Issue> issues, int errorType) {
        mAllIssues = issues;
        mErrorType = errorType;
        mIssues = new ArrayList<>();
    }

    public void setContacts(List<Contact> contacts) {
        // Check if the contacts have returned after the issues list. If so, notify data set
        // changed.
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
        if (!TextUtils.isEmpty(searchText)) {
            mIssues = new ArrayList<>();
            // Should we .trim() the whitespace?
            String lowerSearchText = searchText.toLowerCase();
            for (Issue issue : mAllIssues) {
                // Search the name and the categories for the search term.
                // TODO: Searching full text is less straight forward, as a simple "contains"
                // matches things like "ice" to "avarice" or whatever.
                if (issue.name.toLowerCase().contains(lowerSearchText)) {
                    mIssues.add(issue);
                } else {
                    for (int i = 0; i < issue.categories.length; i++) {
                        if (issue.categories[i].name.toLowerCase().contains(lowerSearchText)) {
                            mIssues.add(issue);
                        }
                    }
                }
            }
            // If there's no other error, show a search error.
            if (mIssues.size() == 0 && mErrorType == NO_ERROR) {
                mErrorType = ERROR_SEARCH_NO_MATCH;
            }
        } else {
            if (TextUtils.equals(filterText,
                    mActivity.getResources().getString(R.string.all_issues_filter))) {
                // Include everything
                mIssues = mAllIssues;
            } else if (TextUtils.equals(filterText,
                    mActivity.getResources().getString(R.string.top_issues_filter))) {
                // Add only the active ones.
                mIssues = new ArrayList<>();
                for (Issue issue : mAllIssues) {
                    if (issue.active) {
                        mIssues.add(issue);
                    }
                }
            } else {
                // Filter by the string
                mIssues = new ArrayList<>();
                for (Issue issue : mAllIssues) {
                    for (Category category : issue.categories) {
                        if (TextUtils.equals(filterText, category.name)) {
                            mIssues.add(issue);
                        }
                    }
                }
            }
        }
        notifyDataSetChanged();
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

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
            RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.issue_view, parent, false);
            return new IssueViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == VIEW_TYPE_ISSUE) {
            IssueViewHolder vh = (IssueViewHolder) holder;
            final Issue issue = mIssues.get(position);
            vh.name.setText(issue.name);
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallback.startIssueActivity(holder.itemView.getContext(), issue);
                }
            });

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

            int totalCalls = issue.contacts.size();
            List<String> contacted = AppSingleton.getInstance(mActivity.getApplicationContext())
                    .getDatabaseHelper().getCallsForIssueAndContacts(issue.id, issue.contacts);
            int callsLeft = totalCalls - contacted.size();
            if (callsLeft == totalCalls) {
                if (totalCalls == 1) {
                    vh.numCalls.setText(
                            mActivity.getResources().getString(R.string.call_count_one));
                } else {
                    vh.numCalls.setText(String.format(
                            mActivity.getResources().getString(R.string.call_count), totalCalls));
                }
            } else {
                if (callsLeft == 1) {
                    vh.numCalls.setText(String.format(
                            mActivity.getResources().getString(R.string.call_count_remaining_one),
                            totalCalls));
                } else {
                    vh.numCalls.setText(String.format(
                            mActivity.getResources().getString(R.string.call_count_remaining),
                            callsLeft, totalCalls));
                }
            }
            vh.doneIcon.setImageLevel(callsLeft == 0 && totalCalls > 0 ? 1 : 0);
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
        if (mIssues.size() == 0 && (mErrorType == ERROR_REQUEST || mErrorType == ERROR_ADDRESS
                || mErrorType == ERROR_SEARCH_NO_MATCH)) {
            return 1;
        }
        return mIssues.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mIssues.size() == 0 && position == 0) {
            if (mErrorType == ERROR_REQUEST) {
                return VIEW_TYPE_EMPTY_REQUEST;
            }
            if (mErrorType == ERROR_ADDRESS) {
                return VIEW_TYPE_EMPTY_ADDRESS;
            }
            if (mErrorType == ERROR_SEARCH_NO_MATCH) {
                return VIEW_TYPE_NO_SEARCH_MATCH;
            }
        }
        return VIEW_TYPE_ISSUE;
    }

    private static class IssueViewHolder extends RecyclerView.ViewHolder {
    public TextView name;
    public TextView numCalls;
    public ImageView doneIcon;

    public IssueViewHolder(View itemView) {
        super(itemView);
        name = (TextView) itemView.findViewById(R.id.issue_name);
        numCalls = (TextView) itemView.findViewById(R.id.issue_call_count);
        doneIcon = (ImageView) itemView.findViewById(R.id.issue_done_img);
    }
}

// TODO: Combine EmptyRequestViewHolder and EmptyAddressViewHolder, change strings dynamically.
private static class EmptyRequestViewHolder extends RecyclerView.ViewHolder {
    public Button refreshButton;

    public EmptyRequestViewHolder(View itemView) {
        super(itemView);
        refreshButton = (Button) itemView.findViewById(R.id.refresh_btn);
        // Tinting the compound drawable only works API 23+, so do this manually.
        refreshButton.getCompoundDrawables()[0].mutate().setColorFilter(
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
        locationButton.getCompoundDrawables()[0].mutate().setColorFilter(
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
        searchButton.getCompoundDrawables()[0].mutate().setColorFilter(
                searchButton.getResources().getColor(R.color.colorAccent),
                PorterDuff.Mode.MULTIPLY);
    }
}

}

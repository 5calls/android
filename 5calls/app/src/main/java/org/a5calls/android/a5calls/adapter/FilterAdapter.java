package org.a5calls.android.a5calls.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.a5calls.android.a5calls.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the filter dropdown that shows hard-coded filters (All issues, Top issues, Saved)
 * followed by a divider, then dynamic category/state filters.
 */
public class FilterAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_DIVIDER = 1;

    /** Number of hard-coded filter items before the divider. */
    public static final int HARD_CODED_COUNT = 3;

    private final Context mContext;
    private final List<String> mItems;

    public FilterAdapter(Context context, List<String> items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        if (mItems.size() > HARD_CODED_COUNT) {
            return mItems.size() + 1; // +1 for the divider
        }
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < HARD_CODED_COUNT) {
            return mItems.get(position);
        }
        if (position == HARD_CODED_COUNT && mItems.size() > HARD_CODED_COUNT) {
            return null; // divider
        }
        return mItems.get(position - 1); // offset by divider
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == HARD_CODED_COUNT && mItems.size() > HARD_CODED_COUNT) {
            return VIEW_TYPE_DIVIDER;
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != VIEW_TYPE_DIVIDER;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == VIEW_TYPE_DIVIDER) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext)
                        .inflate(R.layout.filter_divider_item, parent, false);
            }
            return convertView;
        }

        if (convertView == null || convertView.getId() == R.id.filter_divider) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.filter_list_item, parent, false);
        }
        String text = getFilterText(position);
        ((TextView) convertView).setText(text);
        return convertView;
    }

    /**
     * Returns the actual filter text for a given adapter position, accounting for the divider.
     */
    public String getFilterText(int position) {
        if (position < HARD_CODED_COUNT) {
            return mItems.get(position);
        }
        if (position == HARD_CODED_COUNT && mItems.size() > HARD_CODED_COUNT) {
            return null; // divider
        }
        return mItems.get(position - 1);
    }

    public void notifyItemsChanged() {
        notifyDataSetChanged();
    }
}

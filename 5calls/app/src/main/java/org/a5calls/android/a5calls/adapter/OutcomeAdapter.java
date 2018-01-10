package org.a5calls.android.a5calls.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.a5calls.android.a5calls.R;
import org.a5calls.android.a5calls.model.Outcome;

import java.util.Arrays;
import java.util.List;

public class OutcomeAdapter extends RecyclerView.Adapter<OutcomeAdapter.ViewHolder> {

    public static final List<Outcome> DEFAULT_OUTCOMES = Arrays.asList(
            new Outcome("unavailable", Outcome.Status.UNAVAILABLE),
            new Outcome("voicemail", Outcome.Status.VOICEMAIL),
            new Outcome("contact", Outcome.Status.CONTACT));

    private List<Outcome> outcomes;
    private boolean enabled;
    private Callback callback;

    public OutcomeAdapter(List<Outcome> outcomes, boolean enabled, Callback callback) {
        this.outcomes = outcomes;
        this.enabled = enabled;
        this.callback = callback;
    }

    public OutcomeAdapter(List<Outcome> outcomes, Callback callback) {
        this(outcomes, true, callback);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyDataSetChanged();
    }

    @Override
    public OutcomeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_outcome, parent, false));
    }

    @Override
    public void onBindViewHolder(OutcomeAdapter.ViewHolder holder, int position) {
        ViewHolder.bind(holder, outcomes.get(position), enabled, callback);
    }

    @Override
    public int getItemCount() {
        return outcomes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Button outcomeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            outcomeButton = (Button) itemView.findViewById(R.id.outcome_button);
        }

        public static void bind(ViewHolder holder, final Outcome outcome, boolean enabled,
                                final Callback callback) {
            holder.outcomeButton.setText(Outcome.getDisplayString(holder.itemView.getContext(),
                    outcome.label));
            holder.outcomeButton.setEnabled(enabled);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null) {
                        callback.onOutcomeClicked(outcome);
                    }
                }
            });
        }
    }

    public interface Callback {
        void onOutcomeClicked(Outcome outcome);
    }
}

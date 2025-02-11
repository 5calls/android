package org.a5calls.android.a5calls.controller;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.a5calls.android.a5calls.model.AccountManager;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
    public interface TimePickerCallback {
        void onTimeSelected(int hourOfDay, int minute);
    }

    private TimePickerCallback callback;

    public void setCallback(TimePickerCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final int storedMinutes = AccountManager.Instance.getReminderMinutes(requireContext());
        final int hourOfDay = storedMinutes / 60;
        final int minute = storedMinutes % 60;

        return new TimePickerDialog(
                requireActivity(),
                this,
                hourOfDay,
                minute,
                DateFormat.is24HourFormat(requireContext())
        );
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (callback != null) {
            callback.onTimeSelected(hourOfDay, minute);
        }
    }
}

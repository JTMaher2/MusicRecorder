package io.github.jtmaher2.pianorepertoireapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;


import com.ikovac.timepickerwithseconds.MyTimePickerDialog;
import com.ikovac.timepickerwithseconds.TimePicker;

import java.text.SimpleDateFormat;

public class TimePickerFragment extends DialogFragment {

    private static final String TIME_PICKER_TYPE = "time_picker_type",
                                CHILD_IDX = "child_index";

    public interface OnTimeDialogListener {
        void onTimeSet(com.ikovac.timepickerwithseconds.TimePicker view, int hourOfDay, int minute, int second, String startOrEnd, int childIdx);
    }

    OnTimeDialogListener mListener;
    private Context mContext;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = getContext();
        try {
            mListener = (OnTimeDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement OnTimeDialogListener");
        }
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String type = null;
        int childIdx = -1;
        if (args != null) {
            type = args.getString(TIME_PICKER_TYPE);
            childIdx = args.getInt(CHILD_IDX);
        }

        java.text.DateFormat sdf = SimpleDateFormat.getTimeInstance();
        java.util.Calendar cal = sdf.getCalendar();

        // Create a new instance of TimePickerDialog and return it
        final String finalType = type;
        final int finalChildIdx = childIdx;
        MyTimePickerDialog mtpd = new MyTimePickerDialog(mContext, (view, hourOfDay, minute, seconds) -> mListener.onTimeSet(view, hourOfDay, minute, seconds, finalType, finalChildIdx), cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cal.get(java.util.Calendar.SECOND), true);
        mtpd.updateTime(0,0,0);
        return mtpd;
    }
}
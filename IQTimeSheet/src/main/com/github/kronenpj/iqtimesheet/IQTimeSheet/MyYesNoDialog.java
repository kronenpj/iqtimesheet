package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;


public class MyYesNoDialog extends DialogFragment {

    public static MyYesNoDialog newInstance(int title) {
        MyYesNoDialog frag = new MyYesNoDialog();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");

        return new AlertDialog.Builder(getActivity())
                // .setIcon(R.drawable.alert_dialog_icon)
                .setCancelable(true)
                .setTitle(title)
                .setPositiveButton(R.string.accept,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                ((TimeSheetActivity) getActivity())
                                        .doRestoreClick();
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                dismiss();
                                // ((TimeSheetActivity)getActivity()).doNegativeClick();
                            }
                        }).create();
    }
}

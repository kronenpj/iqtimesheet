package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment


class MyYesNoDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments.getInt("title")

        return AlertDialog.Builder(activity)
                // .setIcon(R.drawable.alert_dialog_icon)
                .setCancelable(true)
                .setTitle(title)
                .setPositiveButton(R.string.accept
                ) { dialog, whichButton ->
                    (activity as TimeSheetActivity)
                            .doRestoreClick()
                }
                .setNegativeButton(R.string.cancel
                ) { dialog, whichButton ->
                    dismiss()
                    // ((TimeSheetActivity)getActivity()).doNegativeClick();
                }.create()
    }

    companion object {

        fun newInstance(title: Int): MyYesNoDialog {
            val frag = MyYesNoDialog()
            val args = Bundle()
            args.putInt("title", title)
            frag.arguments = args
            return frag
        }
    }
}

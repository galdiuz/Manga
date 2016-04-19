package com.galdiuz.manga;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class LoadingDialog extends DialogFragment {

    public interface LoadingDialogCallbacks {
        void onLoadingDialogCancel();
    }

    public static final String TAG = "loadingdialog";

    public static void showDialog(Activity activity, String message) {
        Bundle b = new Bundle();
        b.putString("message", message);
        LoadingDialog dialog = new LoadingDialog();
        dialog.setArguments(b);
        dialog.show(activity.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle b = getArguments();
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(b.getString("message"));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final Activity activity = getActivity();
        if(activity instanceof LoadingDialogCallbacks) {
            ((LoadingDialogCallbacks)activity).onLoadingDialogCancel();
        }
    }
}

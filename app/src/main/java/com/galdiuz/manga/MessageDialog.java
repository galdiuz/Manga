package com.galdiuz.manga;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

public class MessageDialog extends DialogFragment {

    public interface MessageDialogCallbacks {
        void onMessageDialogDismiss();
    }

    public static final String TAG = "messagedialog";

    public static void showDialog(Activity activity, String message) {
        showDialog(activity, false, null, message);
    }

    public static void showDialog(Activity activity, boolean callback, String message) {
        showDialog(activity, callback, null, message);
    }

    public static void showDialog(Activity activity, String title, String message) {
        showDialog(activity, false, title, message);
    }

    public static void showDialog(Activity activity, boolean callback, String title, String message) {
        Bundle b = new Bundle();
        b.putBoolean("callback", callback);
        b.putString("title", title);
        b.putString("message", message);
        DialogFragment dialog = new MessageDialog();
        dialog.setArguments(b);
        dialog.show(activity.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle args = getArguments();
        String title = args.getString("title");
        if(title != null) {
            builder.setTitle(title);
        }
        builder.setMessage(getArguments().getString("message"))
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }

                });
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN;
            }
        });
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if(getArguments().getBoolean("callback")) {
            final Activity activity = getActivity();
            if(activity != null) {
                if (activity instanceof MessageDialogCallbacks) {
                    ((MessageDialogCallbacks) activity).onMessageDialogDismiss();
                } else {
                    throw new IllegalStateException("Activity must implement MessageDialogCallbacks");
                }
            }
        }

    }
}

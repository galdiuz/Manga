package com.galdiuz.manga.other;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;

import com.galdiuz.manga.App;

public class CustomOnLongClickListener implements View.OnLongClickListener {
    CustomPhotoViewAttacher attacher;

    public CustomOnLongClickListener(CustomPhotoViewAttacher attacher) {
        this.attacher = attacher;
    }

    @Override
    public boolean onLongClick(View v) {
        Vibrator vibrator = (Vibrator) App.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if(vibrator.hasVibrator()) {
            vibrator.vibrate(30);
        }
        attacher.onLongClick();
        return false;
    }
}

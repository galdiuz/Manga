package com.galdiuz.manga.other;

import android.view.MotionEvent;

import uk.co.senab.photoview.DefaultOnDoubleTapListener;
import uk.co.senab.photoview.PhotoViewAttacher;

public class CustomOnDoubleTapListener extends DefaultOnDoubleTapListener {

    private PhotoViewAttacher photoViewAttacher;

    public CustomOnDoubleTapListener(PhotoViewAttacher photoViewAttacher) {
        super(photoViewAttacher);
        this.photoViewAttacher = photoViewAttacher;
    }

    @Override
    public boolean onDoubleTap(MotionEvent ev) {
        if (photoViewAttacher == null)
            return false;

        try {
            float scale = photoViewAttacher.getScale();
            float x = ev.getX();
            float y = ev.getY();

            if (scale > photoViewAttacher.getMinimumScale()) {
                photoViewAttacher.setScale(photoViewAttacher.getMinimumScale(), x, y, true);
            }
            else {
                photoViewAttacher.setScale(photoViewAttacher.getMediumScale(), x, y, true);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // Can sometimes happen when getX() and getY() is called
        }

        return true;
    }
}

package com.galdiuz.manga.other;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import uk.co.senab.photoview.PhotoViewAttacher;
//import com.galdiuz.manga.imageadapter.PhotoViewAttacher;

public class CustomPhotoViewAttacher extends PhotoViewAttacher {
    private boolean longPressZoom;
    private float longPressX;
    private float longPressY;
    private OnDragInterceptor onDragInterceptor;

    public CustomPhotoViewAttacher(ImageView imageView) {
        super(imageView);
    }

    @Override
    public void onDrag(float dx, float dy) {
        if(longPressZoom) {
            //Log.d("CustomPhotoViewAttacher", String.format("onDrag dx:%.2f dy:%.2f", dx, dy));
            onScale(1 + (-dy / 250), longPressX, longPressY);
        }
        else {
            if(onDragInterceptor == null || !onDragInterceptor.OnDragIntercept(this, dx, dy)) {
                super.onDrag(dx, dy);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        int action = ev.getAction();
        if(action == MotionEvent.ACTION_DOWN) {
            longPressX = ev.getX();
            longPressY = ev.getY();
            //Log.d("CustomPhotoViewAttacher", String.format("onTouch x:%.2f y:%.2f", longPressX, longPressY));
        }
        else if(ev.getAction() == MotionEvent.ACTION_UP) {
            longPressZoom = false;
            //Log.d("CustomPhotoViewAttacher", "ACTION_UP");
        }
        return super.onTouch(v, ev);
    }

    public void onLongClick() {
        longPressZoom = true;

    }

    public void setOnDragInterceptor(OnDragInterceptor interceptor) {
        onDragInterceptor = interceptor;
    }

    public interface OnDragInterceptor {
        boolean OnDragIntercept(CustomPhotoViewAttacher attacher, float dx, float dy);
    }
}

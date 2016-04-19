package com.galdiuz.manga.other;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextViewAlpha extends TextView {
    public TextViewAlpha(Context context) {
        super(context);
    }

    public TextViewAlpha(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewAlpha(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTextAlpha(int alpha) {
        setTextColor(getTextColors().withAlpha(alpha));
    }
}

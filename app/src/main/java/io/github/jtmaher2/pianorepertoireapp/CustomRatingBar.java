package io.github.jtmaher2.pianorepertoireapp;

import android.content.Context;
import androidx.appcompat.widget.AppCompatRatingBar;
import android.util.AttributeSet;

public class CustomRatingBar extends AppCompatRatingBar {
    public CustomRatingBar(Context context) {
        super(context);
    }
    public CustomRatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    @Override
    public boolean performClick() {
        super.performClick();

        return true;
    }
}

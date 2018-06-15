package ch.amiv.android_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.*;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

import ch.amiv.android_app.core.Requests;

public class CustomNetworkImageView extends NetworkImageView {

    private static final int FADE_IN_TIME_MS = 200;
    public Requests.OnDataReceivedCallback onImageLoaded;

    public CustomNetworkImageView(Context context) {
        super(context);
    }

    public CustomNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        if(onImageLoaded != null)
            onImageLoaded.OnDataReceived();

        TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                new ColorDrawable(getResources().getColor(android.R.color.transparent)),
                new BitmapDrawable(getContext().getResources(), bm)
        });

        setImageDrawable(td);
        td.startTransition(FADE_IN_TIME_MS);

    }
}
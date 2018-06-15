package ch.amiv.android_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.*;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.android.volley.toolbox.NetworkImageView;

import ch.amiv.android_app.core.Requests;

/**
 * This is a class to override the networkimageview which is a very easy class for a layout where volley will load the image for use automatically and cache the image etc
 * We add extra fading and callback functionality
 */
public class CustomNetworkImageView extends NetworkImageView {

    private static final int FADE_IN_TIME_MS = 200;
    public Requests.OnDataReceivedCallback onImageLoaded;   //use this as a callback when the image was loaded

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
                new ColorDrawable(ContextCompat.getColor(getContext(), android.R.color.transparent)),
                new BitmapDrawable(getContext().getResources(), bm)
        });

        setImageDrawable(td);
        td.startTransition(FADE_IN_TIME_MS);

    }
}
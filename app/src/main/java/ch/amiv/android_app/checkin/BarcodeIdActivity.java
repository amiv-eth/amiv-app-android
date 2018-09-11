package ch.amiv.android_app.checkin;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.lang.reflect.Field;

import ch.amiv.android_app.R;
import ch.amiv.android_app.core.UserInfo;
import ch.amiv.android_app.util.Util;

public class BarcodeIdActivity extends AppCompatActivity {

    private ImageView barcodeImageView;
    private SwipeRefreshLayout swipeRefreshLayout;

    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_activity_barcode_id);

        //Set up toolbar and back button
        Util.SetupToolbar(this, true);


        //Generate barcode after the UI has been setup
        barcodeImageView = findViewById(R.id.barcodeImage);
        barcodeImageView.post(new Runnable() {
            @Override
            public void run() {
                GenerateBarcode();
            }
        });

        InitSwipeRefreshUI();
    }

    //Setup swipe down to refresh, adds the amiv logo and rotate animation
    private void InitSwipeRefreshUI()
    {

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        //swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {    //This sets what function is called when we swipe down to refresh
            @Override
            public void onRefresh() {
                GenerateBarcode();

                try {
                    Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
                    f.setAccessible(true);
                    ImageView img = (ImageView)f.get(swipeRefreshLayout);

                    RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setRepeatMode(Animation.INFINITE);
                    rotate.setDuration(1000);
                    rotate.setInterpolator(new LinearInterpolator());
                    img.startAnimation(rotate);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
            f.setAccessible(true);
            ImageView img = (ImageView)f.get(swipeRefreshLayout);
            img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_amiv_logo_icon_scaled, null));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Will generate a barcode bitmap and apply it to the barcodeImageview. Note: barcode will not fill whole constraint as the width of the bars is determined in pixels, to prevent distortion
     */
    public void GenerateBarcode(){
        if(UserInfo.current == null || (UserInfo.current.legi.isEmpty() && UserInfo.current.nethz.isEmpty() && UserInfo.current.email.isEmpty())){
            Snackbar.make(swipeRefreshLayout, R.string.not_logged_in, Snackbar.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        //prioritise legi > nethz > email
        String encode = UserInfo.current.legi.isEmpty() ? (UserInfo.current.nethz.isEmpty() ? UserInfo.current.email : UserInfo.current.nethz) : UserInfo.current.legi;
        encode = encode.replace('@',' ').toUpperCase();
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(encode, BarcodeFormat.CODE_39, ((View)barcodeImageView.getParent()).getHeight(), ((View)barcodeImageView.getParent()).getWidth());
            //barcodeImageView.setColorFilter(null);
            barcodeImageView.setImageBitmap(barcodeEncoder.createBitmap(bitMatrix));
        }
        catch (IllegalArgumentException e){
            Log.e("barcode", "The given string to encode to a barcode/QR is not supported with the current barcode format, usually illegal characters. String: " + encode);
            e.printStackTrace();
        }
        catch (WriterException e) {
            e.printStackTrace();
        }

        swipeRefreshLayout.setRefreshing(false);
    }
}

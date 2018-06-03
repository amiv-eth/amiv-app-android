package ch.amiv.android_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.MaskFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;

import java.net.URL;

public class EventDetailActivity extends AppCompatActivity {

    private int eventIndex = 0;

    ImageView posterImage;
    View posterMask;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        GetIntentData();
        InitUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GetIntentData();
    }

    private void GetIntentData (){
        Intent intent = getIntent();
        eventIndex = intent.getIntExtra("eventIndex", 0);
    }

    private void InitUI (){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView title = findViewById(R.id.eventTitle);
        TextView content = findViewById(R.id.eventDetail);
        scrollView = findViewById(R.id.scrollView_event);
        posterImage = findViewById(R.id.eventPoster);
        posterMask = findViewById(R.id.posterMask);


        title.setText(Events.eventInfos.get(eventIndex).title);
        content.setText(Events.eventInfos.get(eventIndex).description);

        if(!Events.eventInfos.get(eventIndex).posterUrl.isEmpty()) {
            //generate URL to image
            StringBuilder posterUrl = new StringBuilder();
            posterUrl.append(Events.eventInfos.get(eventIndex).posterUrl);
            if(posterUrl.charAt(0) == '/')
                posterUrl.deleteCharAt(0);
            posterUrl.insert(0, Settings.API_URL);

            Log.e("request", "image url: " + posterUrl.toString());

            ImageRequest posterRequest = new ImageRequest(posterUrl.toString(),
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            posterImage.setImageBitmap(bitmap);
                            //Will adjust the empty space/mask at the top of the scrollview so we can see the whole image
                            posterImage.post(new Runnable() {
                                @Override
                                public void run() {
                                    ViewGroup.LayoutParams layoutParams = posterMask.getLayoutParams();
                                    layoutParams.height = posterImage.getHeight();
                                    posterMask.setLayoutParams(layoutParams);
                                    findViewById(R.id.posterBg).setLayoutParams(layoutParams);
                                }
                            });
                        }
                    }, 0, 0, null, null,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            posterImage.setImageResource(R.drawable.ic_error_white);
                        }
                    });
            Requests.SendRequest(posterRequest, getApplicationContext());
        }
    }

    public void ScrollToTop (View view) {
        scrollView.fullScroll(ScrollView.FOCUS_UP);
    }
}

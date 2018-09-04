package ch.amiv.android_app.core;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import java.lang.reflect.Field;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventsListAdapter;
import ch.amiv.android_app.jobs.JobListAdapter;

/**
 * This class is a fragment for a list screen used in the main activity by the page viewer for events, jobs, it will use the given page position to tell which one it is
 * NOTE: This fragment will lose its connection to the parent activity, when the app is resumed, use MainActivity.instance as the activity and context
 */
public class ListFragment extends Fragment {
    private int pagePosition; //the fragments page in the pageview of the main activity
    public static final class PageType {
        public static final int COUNT          = 2;
        public static final int EVENTS         = 0;
        public static final int JOBS           = 1;
        //public static final int NOTIFICATIONS  = 2;
    }

    private RecyclerView recyclerView;
    private BaseRecyclerAdapter recyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Request.OnDataReceivedCallback cancelRefreshCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            SetRefreshUI(false);
        }
    };

    public Request.OnDataReceivedCallback onEventsListUpdatedCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            Request.FetchEventSignups(MainActivity.instance, onSignupsUpdatedCallback, null, "");
            RefreshList(true);
        }
    };

    public Request.OnDataReceivedCallback onJobsListUpdatedCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            RefreshList(true);
        }
    };

    private Request.OnDataReceivedCallback onSignupsUpdatedCallback = new Request.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            RefreshList(false);
        }
    };

    public static ListFragment NewInstance(int pageNum_) {
        ListFragment f = new ListFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("pagePosition", pageNum_);
        f.setArguments(args);

        return f;
    }

    public ListFragment () {}

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pagePosition = getArguments() != null ? getArguments().getInt("pagePosition") : 0;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = getView().findViewById(R.id.recyclerView);
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = getView().findViewById(R.id.swipeRefresh);
        InitSwipeRefreshUI();

        //refresh on activity start
        SetRefreshUI(true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        recyclerLayoutAdapter = new LinearLayoutManager(MainActivity.instance);
        recyclerView.setLayoutManager(recyclerLayoutAdapter);

        // specify an adapter (see also next example)
        if(pagePosition == PageType.EVENTS)
            recyclerAdapter = new EventsListAdapter((MainActivity.instance));
        else if (pagePosition == PageType.JOBS)
            recyclerAdapter = new JobListAdapter(MainActivity.instance);

        if(recyclerAdapter != null) {
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(MainActivity.instance, R.anim.layout_anim_falldown));
            recyclerView.setAdapter(recyclerAdapter);
            AnimateList(null);

            //Used to show feedback when touching item
            recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                    return false;
                }

                @Override
                public void onTouchEvent(RecyclerView rv, MotionEvent e) { }
                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
            });
        }

        RefreshList(true);
    }

    //Setup swipe down to refresh, adds the amiv logo and rotate animation
    private void InitSwipeRefreshUI()
    {
        //Set on resfresh functionality
        swipeRefreshLayout = getView().findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {    //This sets what function is called when we swipe down to refresh
            @Override
            public void onRefresh() {
                OnSwipeRefreshed();

                try {
                    Field f = swipeRefreshLayout.getClass().getDeclaredField("mCircleView");
                    f.setAccessible(true);
                    ImageView img = (ImageView)f.get(swipeRefreshLayout);

                    RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    rotate.setRepeatMode(Animation.INFINITE);
                    rotate.setDuration(1000);
                    rotate.setRepeatCount(5);
                    rotate.setInterpolator(new LinearInterpolator());
                    img.startAnimation(rotate);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        //Set Image of swipe refresh
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

    private void OnSwipeRefreshed(){
        if(pagePosition == PageType.EVENTS)
            Request.FetchEventList(MainActivity.instance, onEventsListUpdatedCallback, cancelRefreshCallback, "");
        else if (pagePosition == PageType.JOBS)
            Request.FetchJobList(MainActivity.instance, onJobsListUpdatedCallback, cancelRefreshCallback, "");
    }

    public void RefreshList(boolean animate)
    {
        if(recyclerAdapter == null)
            return;

        SetRefreshUI(false);
        recyclerAdapter.RefreshData();
        if(animate)
            AnimateList(null);
    }


    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.core_frag_list, container, false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(recyclerAdapter != null)
            recyclerAdapter.RefreshData();

        //Reconnect the fragment to the mainactivity
        MainActivity.instance.pagerAdapter.ReconnectFragment(this, pagePosition);
    }

    private void SetRefreshUI(boolean isRefreshing){
        //Disable the refresh animation after a timeout
        swipeRefreshLayout.setRefreshing(isRefreshing);

        if(!isRefreshing)
            return;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000*15);
    }

    /**
     * Animation is stored in an xml in the res/anim folder, it is applied to the views in xml, this just triggers the anim
     * @param view Used to allow UI elems to call this, pass null otherwise
     */
    public void AnimateList(View view)
    {
        if(recyclerAdapter == null)
            return;

        recyclerView.post(new Runnable() {
            public void run() {
                recyclerView.invalidate();
                recyclerView.scheduleLayoutAnimation();
            }
        });
    }

}


package ch.amiv.android_app.core;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.EventsListAdapter;
import ch.amiv.android_app.jobs.JobListAdapter;

/**
 * This class is a fragment for a list screen used in the main activity by the page viewer for events, jobs, it will use the given page position to tell which one it is
 */
public class ListFragment extends Fragment {
    private int pagePosition; //the fragments page in the pageview of the main activity
    public static final class PageType {
        public static final int COUNT          = 3;
        public static final int EVENTS         = 0;
        public static final int NOTIFICATIONS  = 1;
        public static final int JOBS           = 2;
    }

    private RecyclerView recyclerView;
    private BaseRecyclerAdapter recyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutAdapter;

    private SwipeRefreshLayout swipeRefreshLayout;
    private Requests.OnDataReceivedCallback cancelRefreshCallback = new Requests.OnDataReceivedCallback() {
        @Override
        public void OnDataReceived() {
            swipeRefreshLayout.setRefreshing(false);
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
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = getView().findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(!(getActivity() instanceof MainActivity))
                    return;

                if(pagePosition == PageType.EVENTS)
                    Requests.FetchEventList(getContext(), ((MainActivity)getActivity()).onEventsListUpdatedCallback, cancelRefreshCallback, "");
                else if (pagePosition == PageType.JOBS)
                    Requests.FetchJobList(getContext(), ((MainActivity)getActivity()).onJobsListUpdatedCallback, cancelRefreshCallback, "");
            }
        });
        //refresh on activity start
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if(!(getActivity() instanceof MainActivity))
                    return;

                if(pagePosition == PageType.EVENTS) {
                    swipeRefreshLayout.setRefreshing(true);
                    Requests.FetchEventList(getContext(), ((MainActivity)getActivity()).onEventsListUpdatedCallback, cancelRefreshCallback, "");
                }
                else if(pagePosition == PageType.JOBS){
                    swipeRefreshLayout.setRefreshing(true);
                    Requests.FetchJobList(getContext(), ((MainActivity)getActivity()).onJobsListUpdatedCallback, cancelRefreshCallback, "");
                }
            }
        });

        //Disable the refresh animation after a timeout
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        }, 1000*15);

        recyclerView = getView().findViewById(R.id.recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        recyclerLayoutAdapter = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerLayoutAdapter);

        // specify an adapter (see also next example)
        if(pagePosition == PageType.EVENTS)
            recyclerAdapter = new EventsListAdapter(getActivity());
        else if (pagePosition == PageType.JOBS)
            recyclerAdapter = new JobListAdapter(getActivity());

        if(recyclerAdapter != null) {
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim_falldown));
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
    }

    public void RefreshList(boolean animate)
    {
        if(recyclerAdapter == null)
            return;

        swipeRefreshLayout.setRefreshing(false);
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
    }

    /**
     * Animation is stored in an xml in the res/anim folder, it is applied to the views in xml, this just triggers the anim
     * @param view Used to allow UI elems to call this, pass null otherwise
     */
    public void AnimateList(View view)
    {
        if(recyclerAdapter == null)
            return;

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                recyclerView.invalidate();
                recyclerView.scheduleLayoutAnimation();
            }
        });
    }

}


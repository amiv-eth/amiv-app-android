package ch.amiv.android_app.core;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

/**
 * An example fragment, the central view in MainActivity, for showing a list, should be replaced by a standard fragment with a custom recyclerView, create one different class for different views
 */
public class ListFragment extends Fragment {
    int pagePosition; //the fragments page in the pageview of the main activity
    RecyclerView recyclerView;
    EventsListAdapter recylcerAdaper;
    RecyclerView.LayoutManager recyclerLayoutAdapter;

    SwipeRefreshLayout swipeRefreshLayout;
    Requests.OnDataReceivedCallback cancelRefreshCallback = new Requests.OnDataReceivedCallback() {
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
        pagePosition = getArguments() != null ? getArguments().getInt("pagePosition") : 1;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = getView().findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if(pagePosition == 0 && getActivity() instanceof MainActivity)
                    Requests.FetchEventList(getContext(), ((MainActivity)getActivity()).onEventsListUpdatedCallback, cancelRefreshCallback, "");
            }
        });
        //refresh on activity start
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if(pagePosition == 0 && getActivity() instanceof MainActivity) {
                    swipeRefreshLayout.setRefreshing(true);
                    Requests.FetchEventList(getContext(), ((MainActivity)getActivity()).onEventsListUpdatedCallback, cancelRefreshCallback, "");
                }
            }
        });

        recyclerView = getView().findViewById(R.id.recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        recyclerLayoutAdapter = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerLayoutAdapter);

        // specify an adapter (see also next example)
        if(pagePosition == 0) {
            recylcerAdaper = new EventsListAdapter(getActivity());
            recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_anim_falldown));
            recyclerView.setAdapter(recylcerAdaper);
        }

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        AnimateList(null);
    }

    public void RefreshList(boolean animate)
    {
        recylcerAdaper.RefreshData();
        swipeRefreshLayout.setRefreshing(false);
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

        if(recylcerAdaper != null)
            recylcerAdaper.RefreshData();
    }

    /**
     * Animation is stored in an xml in the res/anim folder, it is applied to the views in xml, this just triggers the anim
     * @param view Used to allow UI elems to call this, pass null otherwise
     */
    public void AnimateList(View view)
    {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                recyclerView.invalidate();
                recyclerView.scheduleLayoutAnimation();
            }
        });
    }

}


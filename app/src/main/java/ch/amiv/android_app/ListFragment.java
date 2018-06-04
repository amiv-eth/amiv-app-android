package ch.amiv.android_app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

/**
 * An example fragment, the central view in MainActivity, for showing a list, should be replaced by a standard fragment with a custom recyclerView, create one different class for different views
 */
public class ListFragment extends Fragment {
    int mPagePosition; //the fragments page in the pageview
    RecyclerView recyclerView;
    RecyclerView.Adapter recylcerAdaper;
    RecyclerView.LayoutManager recyclerLayoutAdapter;

    public static ListFragment NewInstance(int pageNum_) {
        ListFragment f = new ListFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putInt("mPagePosition", pageNum_);
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
        mPagePosition = getArguments() != null ? getArguments().getInt("mPagePosition") : 1;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = getView().findViewById(R.id.recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        recyclerLayoutAdapter = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerLayoutAdapter);

        // specify an adapter (see also next example)
        if(mPagePosition == 0) {
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

    public void RefreshList()
    {
        recylcerAdaper.notifyDataSetChanged();
        AnimateList(null);
    }


    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        View tv = v.findViewById(R.id.text);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(recylcerAdaper != null)
            recylcerAdaper.notifyDataSetChanged();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //old  setListAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, new String[]{"A", "B", "C"}));
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


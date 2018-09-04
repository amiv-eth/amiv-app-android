package ch.amiv.android_app.checkin;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.List;

import ch.amiv.android_app.R;

public class SearchMembersActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;  //for displaying the list of members in the same way as in the memberListActivity
    RecyclerView.Adapter mRecyclerAdapter;
    RecyclerView.LayoutManager mRecyclerLayoutAdapter;

    public List<Member> searchResults = new ArrayList<Member>();    //This is the list of search results, will dynamically change size of course
    String prevQuery = "";

    private final Handler handler = new Handler();  //similar as in scanActivity, to keep refreshing the data
    private Runnable refreshMemberDB = new Runnable() {    //Refresh stats every x seconds
        @Override
        public void run() {
            ServerRequests.UpdateMemberDB(getApplicationContext(), new ServerRequests.OnDataReceivedCallback() {
                @Override
                public void OnDataReceived() {
                    UpdateList();
                    ActionBar actionBar = getSupportActionBar();
                    if(!EventDatabase.instance.eventData.name.isEmpty() && actionBar != null)
                        actionBar.setTitle(EventDatabase.instance.eventData.name);
                }
            });
            if(SettingsActivity.GetAutoRefresh(getApplicationContext()))
                handler.postDelayed(this, SettingsActivity.GetRefreshFrequency(getApplicationContext()));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkin_activity_search_members);
        UpdateList();
    }

    /**
     * Used to create the search bar in the action bar at the top
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.checkin_ac_search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menuSearch);
        final SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {    //Called when we press enter/search, should run our "more efficient" query here
                RunSearch(query, false);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(EventDatabase.instance.members.size() < 25) {
                    RunSearch(newText, false);
                    if(newText.isEmpty()) {
                        searchResults.clear();
                        mRecyclerAdapter.notifyDataSetChanged();
                    }
                }
                return false;
            }
        });

        searchView.setIconified(false); //immediately open the searchview in the action bar when the activity starts

        return true;
    }

    @Override
    protected void onPause() {  //stop refreshing the data when the app is not active
        super.onPause();
        handler.removeCallbacks(refreshMemberDB);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(refreshMemberDB, 0);
    }

    private void InitialiseListView()   //same as in the memberlistactivity
    {
        //=====Recycler View====
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        mRecyclerLayoutAdapter = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mRecyclerLayoutAdapter);

        mRecyclerAdapter = new SearchMembersListAdapter(searchResults);
        mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_anim_falldown));     //Here we give the searchResults array and use the SearchMemberListAdapter
        mRecyclerView.setAdapter(mRecyclerAdapter);

        AnimateList(null);
    }

    /**
     * The function will create a list of all members that match the query and store it in the searchResults
     * @param query The string to search for in names, legi, email, nethz
     * @return Returns if the search has caused the recyclerview to be updated
     */
    public boolean RunSearch(String query, boolean hasDataSetChanged)
    {
        if(query.isEmpty() || (query == prevQuery && !hasDataSetChanged))
            return false;
        prevQuery = query;
        Log.e("search", "Initiating query of members: " + query);
        searchResults.clear();
        for (int i = 0; i < EventDatabase.instance.members.size(); i++)
        {
            if(EventDatabase.instance.members.get(i).firstname.contains(query) || EventDatabase.instance.members.get(i).lastname.contains(query) ||
                    EventDatabase.instance.members.get(i).nethz.contains(query) || EventDatabase.instance.members.get(i).legi.contains(query) || EventDatabase.instance.members.get(i).email.contains(query))
                searchResults.add(EventDatabase.instance.members.get(i));
        }

        mRecyclerAdapter.notifyDataSetChanged();
        return true;
    }

    private void UpdateList()
    {
        if(mRecyclerView == null)
            InitialiseListView();

        if(!RunSearch(prevQuery, true))
            mRecyclerAdapter.notifyDataSetChanged();
    }

    /**
     * Animation is stored in an xml in the res/anim folder, it is applied to the views in xml, this just triggers the anim
     * @param view Used to allow UI elems to call this, pass null otherwise
     */
    public void AnimateList(View view)
    {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mRecyclerView.invalidate();
                mRecyclerView.scheduleLayoutAnimation();
            }
        });
    }
}

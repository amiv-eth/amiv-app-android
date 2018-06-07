package ch.amiv.android_app.checkin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;

import ch.amiv.android_app.R;

/**
 * This activity is for displaying the list of signed in members, similar to what is seen in the checkin website in the other amiv checkin project.
 * Mostly handles updating the data by fetching from the server and then updating the listview. Note the list view is customised, ie the individual item, this is what the CustomListAdapter class and list_item_memberber.xml are for
 */

public class MemberListActivity extends AppCompatActivity {

    RecyclerView mRecylerView;
    RecyclerView.Adapter mRecylcerAdaper;
    RecyclerView.LayoutManager mRecyclerLayoutAdapter;

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
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.checkin_activity_member_list);

        UpdateList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.checkin_ac_member_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Is used to add the search button to the action bar, as well as onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menuSearch) {
            StartSearchActivity();
        }
        else if(id == R.id.menuSort_Membership) {
            EventDatabase.instance.SetMemberSortingType(EventDatabase.MemberComparator.Membership);
            mRecylcerAdaper.notifyDataSetChanged();
        }
        else if(id == R.id.menuSort_Status) {
            EventDatabase.instance.SetMemberSortingType(EventDatabase.MemberComparator.Status);
            mRecylcerAdaper.notifyDataSetChanged();
        }
        else if(id == R.id.menuSort_Name) {
            EventDatabase.instance.SetMemberSortingType(EventDatabase.MemberComparator.Name);
            mRecylcerAdaper.notifyDataSetChanged();
        }
        else if(id == R.id.menuSort_Legi) {
            EventDatabase.instance.SetMemberSortingType(EventDatabase.MemberComparator.Legi);
            mRecylcerAdaper.notifyDataSetChanged();
        }

        return super.onOptionsItemSelected(item);
    }

    private void InitialiseListView()
    {
        //=====Recycler View====
        mRecylerView = findViewById(R.id.recyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecylerView.setHasFixedSize(true);

        // use a linear layout manager
        mRecyclerLayoutAdapter = new LinearLayoutManager(this);
        mRecylerView.setLayoutManager(mRecyclerLayoutAdapter);

        // specify an adapter (see also next example)
        mRecylcerAdaper = new MemberListAdapter(EventDatabase.instance.members, EventDatabase.instance.stats, EventDatabase.instance.eventData.GetInfosAsKeyValuePairs());
        mRecylerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.layout_anim_falldown));
        mRecylerView.setAdapter(mRecylcerAdaper);

        AnimateList(null);
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

    /**
     * Animation is stored in an xml in the res/anim folder, it is applied to the views in xml, this just triggers the anim
     * @param view Used to allow UI elems to call this, pass null otherwise
     */
    public void AnimateList(View view)
    {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mRecylerView.invalidate();
                mRecylerView.scheduleLayoutAnimation();
            }
        });
    }

    private void UpdateList()
    {
        if(mRecylerView == null)
            InitialiseListView();

        mRecylcerAdaper.notifyDataSetChanged();
    }

    public void RefreshListData(View view)
    {
        refreshMemberDB.run();
        AnimateList(view);
    }

    //====Activity Transitions======
    private void StartSearchActivity()
    {
        Intent intent = new Intent(this, SearchMembersActivity.class);
        startActivity(intent);
    }
}

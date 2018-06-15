package ch.amiv.android_app.core;

import android.support.v7.widget.RecyclerView;

/**
 * A class to simplify refreshsing a recyclerview, used in events and jobs list for example
 */
public abstract class BaseRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    public void RefreshData(){
        BuildDataset();
        notifyDataSetChanged();
    }

    public void BuildDataset(){}
}

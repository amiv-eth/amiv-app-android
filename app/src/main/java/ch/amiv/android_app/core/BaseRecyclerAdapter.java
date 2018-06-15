package ch.amiv.android_app.core;

import android.support.v7.widget.RecyclerView;

public abstract class BaseRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {

    public void RefreshData(){
        BuildDataset();
        notifyDataSetChanged();
    }

    public void BuildDataset(){}
}

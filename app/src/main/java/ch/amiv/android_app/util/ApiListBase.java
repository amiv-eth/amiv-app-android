package ch.amiv.android_app.util;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.amiv.android_app.core.Settings;
import ch.amiv.android_app.events.Events;

/**
 * This is the base class for Events and Jobs, with the common functionality merged here.
 * It handles getting the data from the json, getting an item, adding/updating items
 * Here item represents and EventInfo or JobInfo, as the type T
 * At the beginning there are several abstract methods that the Events or Jobs needs to implement
 * Attention! data and sorted indexes may change, if you are accessing items in the long term use their API id
 */
public abstract class ApiListBase<T extends ApiItemBase> {

    public List<T> data = new ArrayList<>(); //This is a list of ALL items as received from the api, we will not use this directly
    public List<List<T>> sorted = new ArrayList<>(GetSortedSize1());   //A list of lists which has been sorted according to the EventGroup configuration

    public boolean hasInitialised = false;

    /**
     * A constant class to easily set extras intents accessing items from a list. Used in Job/EventDetailActivity
     */
    public static final class LauncherExtras {
        public static final String ITEM_GROUP = "itemGroup";
        public static final String ITEM_INDEX = "itemIndex";
        public static final String ITEM_ID = "itemId";
        public static final String RELOAD_FIRST = "reloadFirst";
    }

    //Functions that need to be implemented by the derived class, eg. Events
    public abstract Comparator<T> GetItemComparator();
    protected abstract int GetSortedSize1();
    protected abstract int GetSortedSize2Expected(int category);
    public abstract T CreateItem(JSONObject json);
    public abstract int GetItemCategory(T item);
    public abstract void SaveToCache(Context context);
    public abstract void LoadFromCache(Context context);    //Should be implemented in serial, not async

    public void Initialise(Context context){
        if(hasInitialised) return;
        hasInitialised = true;

        LoadFromCache(context);
    }

    /**
     * This is the key function of this class. It converts a json for several items to java items
     * Update the items with the data received from the api.
     * @param json json array of the items.
     */
    public void UpdateAll(Context context, JSONArray json)
    {
        boolean isInitialising = data.size() == 0;

        for (int i = 0; i < json.length(); i++)
        {
            try {
                //if we are not initialising, search for the item id and then update it, else add a new one to the list. This ensures we do not lose the signup data
                JSONObject jsonItem = json.getJSONObject(i);
                String _id = T.GetId(jsonItem);
                if(_id.isEmpty())
                    continue;

                if(isInitialising || !UpdateItem(jsonItem, _id, context))
                    data.add(CreateItem(jsonItem));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        GenerateSortedLists(isInitialising);

        SaveToCache(context);

        hasInitialised = true;
    }

    /**
     * This will create the 'sorted' list from an already filled data list. It will also sort the data
     * @param isInitialising Do we need to initialise sorted?
     */
    public void GenerateSortedLists(boolean isInitialising)
    {
        if(isInitialising){
            sorted = new ArrayList<>(GetSortedSize1());
            for (int k = 0; k < GetSortedSize1(); k++)
                sorted.add(new ArrayList<T>(GetSortedSize2Expected(k)));
        }
        else {
            for (int k = 0; k < sorted.size(); k++)
                sorted.get(k).clear();
        }

        //Sort list and update sorted list
        if(!data.isEmpty()){
            //sort so first elem has an advertising start date furthest in the future
            Collections.sort(data, GetItemComparator());

            //fill in the sorted list according to the dates of the items
            for (int i = 0; i < data.size(); i++){
                AddItemToSorted(data.get(i), false);
            }
        }
    }

    /**
     * Will add the item to the *sorted* list. Use AddItem to add an item, this is just used to update the sorted list accordingly
     * @param sortAfterInsert Should we re-sort the group after insertion?
     */
    private void AddItemToSorted(T item, boolean sortAfterInsert){
        int category = GetItemCategory(item);

        if(sorted == null || sorted.size() == 0) {
            GenerateSortedLists(true);
            return;
        }
        else
            sorted.get(category).add(item);

        if(sortAfterInsert) {
            Collections.sort(data, GetItemComparator());
            Collections.sort(sorted.get(category), GetItemComparator());
        }
    }




//region -   ==========Item Functions======

    /**
     * Will add a new item to the data AND sorted, and update if the item already exists
     * @return The index of the item in data. If the item already exists it will return that index. -1 if the json is invalid, ie no _id found
     */
    public int AddItem(JSONObject json, Context context){
        Initialise(context);

        String id = T.GetId(json);
        if(id.isEmpty()) return -1;

        int index = GetItemIndexFromId(id, context);
        if(index >= 0) {
            data.get(index).Update(json);
            return index;
        }

        T i = CreateItem(json);
        data.add(i);
        AddItemToSorted(i, true);

        SaveToCache(context);

        return data.size() -1;
    }

    /**
     * @return true if the item was found
     */
    public boolean UpdateItem(JSONObject json, @NonNull String itemId, Context context){
        Initialise(context);
        T item = GetItem(itemId, context);
        if(item == null) return false;

        item.Update(json);
        return true;
    }

    //region -   Getting Items
    /**
     * @param dataIndex The index in the unsorted 'data' array
     */
    public T GetItem(int dataIndex, Context context){
        Initialise(context);
        return data.get(dataIndex);
    }

    /**
     * @param sortedCategory The 1st dim of the sorted list
     * @param index The 2nd dim
     */
    public T GetItem(int sortedCategory, int index, Context context){
        Initialise(context);
        return sorted.get(sortedCategory).get(index);
    }

    /**
     * Note: Try to use the sorted/data lists to access by index. Only use this if you are having issues due to the indexes changing from the lists.
     * @param id The API item _id
     */
    public T GetItem(String id, Context context){
        Initialise(context);
        int index = GetItemIndexFromId(id, context);
        if(index >= 0)
            return data.get(index);

        return null;
    }

    /**
     * @return Will extract the item from an intent with extras set using the LauncherExtras constants
     */
    public T GetItem (Intent intent, Context context){
        if(!(intent.hasExtra(LauncherExtras.ITEM_INDEX) || intent.hasExtra(LauncherExtras.ITEM_ID))) return null;

        if(intent.getBooleanExtra(LauncherExtras.RELOAD_FIRST, false))
            LoadFromCache(context);

        T item = null;
        if(intent.hasExtra(LauncherExtras.ITEM_INDEX))
        {
            int group = intent.getIntExtra(LauncherExtras.ITEM_GROUP, -1);
            int index = intent.getIntExtra(LauncherExtras.ITEM_INDEX, 0);
            if(group == -1)
                item = GetItem(index, context);
            else
                item = GetItem(group, index, context);

            if (item == null)
                Log.e("events", "invalid event index selected during InitUI(), (groupIndex, eventIndex): (" + group + "," + index + "), total event size" + Events.get.data.size() + ". Ensure that you are not clearing/overwriting the events list while viewing an event. Returning to calling activity...");
        }
        else if(intent.hasExtra(LauncherExtras.ITEM_ID))
        {
            item = GetItem(intent.getStringExtra(LauncherExtras.ITEM_ID), context);

            if(item == null)
                Log.e("events", "No event found from eventId=" + intent.getStringExtra(LauncherExtras.ITEM_ID) + " in intent, have you used intent.putStringExtra. Returning to calling activity...");
        }

        return item;
    }

    /**
     * @param id The API item _id
     * @return The index in data (unsorted list)
     */
    public int GetItemIndexFromId(String id, Context context){
        Initialise(context);
        if(id == null || id.isEmpty()) return -1;

        for (int i = 0; i < data.size(); ++i)
            if(data.get(i)._id.equalsIgnoreCase(id))
                return i;

        return -1;
    }
    //endregion
    //endregion
}

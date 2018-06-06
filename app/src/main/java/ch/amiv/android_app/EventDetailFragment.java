package ch.amiv.android_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnEventDetailFragmentListener} interface
 * to handle interaction events.
 * Use the {@link EventDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventDetailFragment extends Fragment {
    private static final String eventIndex_key = "eventIndex";
    private int eventIndex;

    private OnEventDetailFragmentListener mListener;

    public EventDetailFragment() {}

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param eventIndex_ The index in the EventInfos array of the event to display
     * @return A new instance of fragment EventDetailFragment.
     */
    public static EventDetailFragment newInstance(int eventIndex_) {
        EventDetailFragment fragment = new EventDetailFragment();

        Bundle args = new Bundle();
        args.putInt(eventIndex_key, eventIndex_);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            eventIndex = getArguments().getInt(eventIndex_key);
        else
            eventIndex = 0;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView title = getView().findViewById(R.id.eventTitle);
        TextView content = getView().findViewById(R.id.eventDetail);
        final ImageView poster = getView().findViewById(R.id.eventPoster);

        title.setText(Events.eventInfos.get(eventIndex).GetTitle(getResources()));
        content.setText(Events.eventInfos.get(eventIndex).GetDescription(getResources()));

        String posterUrl = "https://www.amiv.ethz.ch/sites/all/themes/amiv15/logo.png";
        ImageRequest posterRequest = new ImageRequest(posterUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        poster.setImageBitmap(bitmap);
                    }
                }, 0, 0, ImageView.ScaleType.CENTER, Bitmap.Config.RGB_565,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        poster.setImageResource(R.drawable.ic_error_white);
                    }
                });
        Requests.SendRequest(posterRequest, getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_detail, container, false);
    }

//region Callbacks for Parent Activity
    /**
     * Call this when we have registered for the event in the fragment to notify the parent activity
     */
    public void onRegisterEvent() {
        if (mListener != null) {
            mListener.onRegisteredForEvent(eventIndex);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        //Add the parent activity as a listener for our callback
        if (context instanceof OnEventDetailFragmentListener)
            mListener = (OnEventDetailFragmentListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement OnEventDetailFragmentListener");

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnEventDetailFragmentListener {
        // TODO: Update argument type and name
        void onRegisteredForEvent(int eventIndex);
    }
//endregion
}

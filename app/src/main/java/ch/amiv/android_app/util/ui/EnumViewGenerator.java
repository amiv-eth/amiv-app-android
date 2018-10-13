package ch.amiv.android_app.util.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ch.amiv.android_app.R;
import ch.amiv.android_app.events.AdditField;

public class EnumViewGenerator {
    public interface OnButtonIndexClicked {
        void OnClick(int enumIndex);
    }

    public static void InitialiseEnumList(final Activity activity, final OnButtonIndexClicked onClick, AdditField field, boolean addOtherField){
        TextView titleView = activity.findViewById(R.id.title_enum_view);
        titleView.setText(field.title(activity.getApplicationContext()));

        ViewGroup parent = activity.findViewById(R.id.listParent);
        parent.removeAllViews();

        //Add list of options as buttons as children of the list parent (Linear layout)
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        for (int i = 0; i < field.possibleValues.length + (addOtherField ? -1:0); i++) {
            Button btn = (Button) inflater.inflate(R.layout.core_intro_pref_button, null);
            parent.addView(btn);
            btn.setText(field.possibleValues[i]);

            final int index = i;
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClick.OnClick(index);
                }
            });
        }

        //add other text field, show the save button other is tapped
        if(addOtherField){
            EditText text = LayoutInflater.from(parent.getContext()).inflate(R.layout.core_intro_pref_other_field, parent).findViewById(R.id.otherField);
            text.setHint(field.possibleValues[field.possibleValues.length-1]);

            final Button btnNext = activity.findViewById(R.id.buttonNext);
            //Show save button when editing text
            text.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    btnNext.setVisibility(View.VISIBLE);
                    btnNext.setText(R.string.save);
                    return false;
                }
            });
        }
    }
}

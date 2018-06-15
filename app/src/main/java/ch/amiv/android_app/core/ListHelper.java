package ch.amiv.android_app.core;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import ch.amiv.android_app.R;

public final class ListHelper {

    public static class Pair {
        public int type;
        public Object value;

        public Pair(int type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {
        public TextView nameField;

        public HeaderHolder(View view) {
            super(view);
            nameField = view.findViewById(R.id.titleField);
        }
    }

    public static class SpaceHolder extends RecyclerView.ViewHolder {
        public View space;

        public SpaceHolder(View view) {
            super(view);
            space = view.findViewById(R.id.space);
        }
    }
}

package com.github.kronenpj.iqtimesheet.IQTimeSheet;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

class MyArrayAdapter<T> extends ArrayAdapter<T> {
    /**
     * The resource indicating what views to inflate to display the content of
     * this array adapter.
     */
    private int mResource;

    /**
     * If the inflated resource is not a TextView, #mFieldId is used to
     * find a TextView inside the inflated views hierarchy. This field must
     * contain the identifier that matches the one defined in the resource file.
     */
    private int mFieldId = 0;

    private LayoutInflater mInflater;

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        init(context, resource, textViewResourceId);
    }

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int textViewResourceId, T[] objects) {
        super(context, textViewResourceId, objects);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int resource,
                          int textViewResourceId, T[] objects) {
        super(context, resource, textViewResourceId, Arrays.asList(objects));
        init(context, resource, textViewResourceId);
    }

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int textViewResourceId,
                          List<T> objects) {
        super(context, textViewResourceId, objects);
        init(context, textViewResourceId, 0);
    }

    /**
     * {@inheritDoc}
     */
    public MyArrayAdapter(Context context, int resource,
                          int textViewResourceId, List<T> objects) {
        super(context, resource, textViewResourceId, objects);
        init(context, resource, textViewResourceId);
    }

    private void init(Context context, int resource, int textViewResourceId) {
        mResource = resource;
        mFieldId = textViewResourceId;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }

    /**
     * {@inheritDoc}
     */
    private View createViewFromResource(int position, View convertView,
                                        ViewGroup parent, int resource) {
        View view;
        TextView text;

        mResource = resource;
        view = super.getView(position, convertView, parent);
        text = (TextView) view;
        text.setTextSize(TimeSheetActivity.prefs.getFontSizeTaskList());

        return text;
    }
}

package com.github.kronenpj.iqtimesheet.IQTimeSheet

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.jetbrains.anko.applyRecursively
import org.jetbrains.anko.custom.style
import org.jetbrains.anko.textColor

import java.util.Arrays

internal class MyArrayAdapter<T> : ArrayAdapter<T> {
    /**
     * The resource indicating what views to inflate to display the content of
     * this array adapter.
     */
    private var mResource: Int = 0

    /**
     * If the inflated resource is not a TextView, #mFieldId is used to
     * find a TextView inside the inflated views hierarchy. This field must
     * contain the identifier that matches the one defined in the resource file.
     */
    private var mFieldId = 0

    private var mInflater: LayoutInflater? = null

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, resource: Int, textViewResourceId: Int) : super(context, resource, textViewResourceId) {
        init(context, resource, textViewResourceId)
    }

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, textViewResourceId: Int) : super(context, textViewResourceId) {
        init(context, textViewResourceId, 0)
    }

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, textViewResourceId: Int, objects: Array<T>) : super(context, textViewResourceId, objects) {
        init(context, textViewResourceId, 0)
    }

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, resource: Int,
                textViewResourceId: Int, objects: Array<T>) : super(context, resource, textViewResourceId, Arrays.asList(*objects)) {
        init(context, resource, textViewResourceId)
    }

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, textViewResourceId: Int,
                objects: List<T>) : super(context, textViewResourceId, objects) {
        init(context, textViewResourceId, 0)
    }

    /**
     * {@inheritDoc}
     */
    constructor(context: Context, resource: Int,
                textViewResourceId: Int, objects: List<T>) : super(context, resource, textViewResourceId, objects) {
        init(context, resource, textViewResourceId)
    }

    private fun init(context: Context, resource: Int, textViewResourceId: Int) {
        mResource = resource
        mFieldId = textViewResourceId
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    /**
     * {@inheritDoc}
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, mResource)
    }

    /**
     * {@inheritDoc}
     */
    private fun createViewFromResource(position: Int, convertView: View?,
                                       parent: ViewGroup, resource: Int): View {
        val view: View
        val text: TextView

        mResource = resource
        view = super.getView(position, convertView, parent)
        text = view as TextView
        // FIXME: Horrible hack to get the list visible in a dark theme.
        // I've invested hours trying to figure out the proper way to fix this.
        text.setTextColor(Color.WHITE)

        return text
    }
}

package com.cogitator.dynamikgridview

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*
import kotlin.collections.ArrayList


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 24/05/2018 (MM/DD/YYYY)
 */
abstract class DynamikAdapter(private val recyclerView: RecyclerView) : RecyclerView.Adapter<DynamikAdapter.ViewHolder>() {
    private val mOnItemClickListener: OnItemClickListener? = null
    private val mSelectedDataIndexSet: MutableList<Int>
    public val mOnItemClickLitener: OnItemClickListener? = null

    init {
        mSelectedDataIndexSet = ArrayList()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        if (mSelectedDataIndexSet.contains(position)) {
            updateSelectedItem(holder)
        } else {
            updateUnselectedItem(holder)
        }

        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener({ mOnItemClickListener.onItemClick(holder, holder.adapterPosition) })
            holder.itemView.setOnLongClickListener({ mOnItemClickListener.onItemLongClick(holder, holder.adapterPosition) })
        }
    }

    fun getSelectedItems(): ArrayList<Int> {
        val arrayList = ArrayList(mSelectedDataIndexSet)
        if (!mSelectedDataIndexSet.isEmpty()) {
            mSelectedDataIndexSet.sort()
            Collections.copy(arrayList, mSelectedDataIndexSet)
            mSelectedDataIndexSet.clear()
        }
        return arrayList
    }

    fun resetSelectedItems() {

        val listIterator = mSelectedDataIndexSet.listIterator()
        while (listIterator.hasNext()) {
            val position = listIterator.next()
            val holder = recyclerView.findViewHolderForAdapterPosition(position) as ViewHolder
            updateUnselectedItem(holder)
        }
        mSelectedDataIndexSet.clear()

    }

    fun reverseSelect(holder: ViewHolder, position: Int) {
        if (mSelectedDataIndexSet.contains(position)) {
            mSelectedDataIndexSet.remove(position)
            updateUnselectedItem(holder)
        } else if (!mSelectedDataIndexSet.contains(position)) {
            mSelectedDataIndexSet.add(position)
            updateSelectedItem(holder)
        }
    }

    fun selectItem(holder: ViewHolder, position: Int): Boolean {
        return if (!mSelectedDataIndexSet.contains(position)) {
            mSelectedDataIndexSet.add(position)
            updateSelectedItem(holder)
            true
        } else false
    }

    protected abstract fun updateSelectedItem(holder: ViewHolder)

    protected abstract fun updateUnselectedItem(holder: ViewHolder)

    public interface OnItemClickListener {
        fun onItemClick(holder: ViewHolder, position: Int)
        fun onItemLongClick(holder: ViewHolder, position: Int): Boolean
    }

    open inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
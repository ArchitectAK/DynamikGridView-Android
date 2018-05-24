package com.cogitator.dynamikgridview

import android.support.v7.widget.RecyclerView
import android.view.View
import java.util.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 24/05/2018 (MM/DD/YYYY)
 */
abstract class DynamikAdapter(private val recyclerView: RecyclerView, private val mSelectedDataIndexSet: MutableList<Int>) : RecyclerView.Adapter<DynamikAdapter.ViewHolder>() {
    private val mOnItemClickListener: OnItemClickListener? = null

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

    fun selectItem(holder: ViewHolder, position: Int) {
        if (!mSelectedDataIndexSet.contains(position)) {
            mSelectedDataIndexSet.add(position)
            updateSelectedItem(holder)
        }
    }

    protected abstract fun updateSelectedItem(holder: ViewHolder)

    protected abstract fun updateUnselectedItem(holder: ViewHolder)

    interface OnItemClickListener {
        fun onItemClick(holder: ViewHolder, position: Int)
        fun onItemLongClick(holder: ViewHolder, position: Int): Boolean
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
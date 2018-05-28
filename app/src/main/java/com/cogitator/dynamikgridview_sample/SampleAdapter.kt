package com.cogitator.dynamikgridview_sample

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.cogitator.dynamikgridview.DynamikAdapter
import android.view.LayoutInflater
import android.view.View
import com.cogitator.dynamikgridview.DynamikLayoutManager
import kotlinx.android.synthetic.main.grid_item_text.view.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
class SampleAdapter(val context: Context, rec: RecyclerView, private val data: MutableList<String>, private val widthNums: MutableList<Int>, private val heightNums: MutableList<Int>) : DynamikAdapter(rec) {
    override fun updateSelectedItem(holder: ViewHolder) {
        holder.itemView.iv_check.visibility = View.VISIBLE
        holder.itemView.tv_num.isActivated = true
    }

    override fun updateUnselectedItem(holder: ViewHolder) {
        holder.itemView.iv_check.visibility = View.GONE
        holder.itemView.tv_num.isActivated = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.grid_item_text, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.tv_num.text = data[position]
        setViewParams(holder.itemView, position)
        holder.itemView.setOnClickListener({
            reverseSelect(holder, position)
        })
        holder.itemView.setOnLongClickListener({
            selectItem(holder, position)
        })
        super.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun setViewParams(v: View, position: Int) {
        val lp = v.layoutParams as DynamikLayoutManager.LayoutParams
        lp.widthNum = widthNums.get(position)
        lp.heightNum = heightNums.get(position)
    }

    inner class MyViewHolder(view: View) : DynamikAdapter.ViewHolder(view)
}
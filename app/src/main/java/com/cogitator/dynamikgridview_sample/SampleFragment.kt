package com.cogitator.dynamikgridview_sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cogitator.dynamikgridview.DynamikFragment
import com.cogitator.dynamikgridview.DynamikItemAnimator
import com.cogitator.dynamikgridview.DynamikLayoutManager
import kotlinx.android.synthetic.main.fragment_sample.*
import java.util.*
import kotlin.collections.ArrayList


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
class SampleFragment : DynamikFragment() {

    private var mStringData: MutableList<String> = ArrayList()
    private var widthNums: MutableList<Int> = ArrayList()
    private var heightNums: MutableList<Int> = ArrayList()
    private var mAdapter: SampleAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sample, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = DynamikLayoutManager(4)
        irregular_gridview?.layoutManager = layoutManager

        mAdapter = SampleAdapter(context!!, irregular_gridview!!, mStringData, widthNums, heightNums)
        irregular_gridview?.adapter = mAdapter
        val animator = DynamikItemAnimator()
        irregular_gridview?.itemAnimator = animator

    }

    override fun initData() {
        mStringData = ArrayList()
        for (i in 0..99) {
            mStringData.add("" + i)
        }
        val r = Random()
        widthNums = ArrayList()
        heightNums = ArrayList()
        for (i in 0 until mStringData.size) {
            val widthNum: Int
            val heightNum: Int
            val nextInt = r.nextInt(100)
            when {
                nextInt > 80 -> {
                    widthNum = 2
                    heightNum = 2
                }
                nextInt > 60 -> {
                    widthNum = 2
                    heightNum = 1
                }
                nextInt > 40 -> {
                    widthNum = 1
                    heightNum = 2
                }
                else -> {
                    widthNum = 1
                    heightNum = 1
                }
            }
            widthNums.add(widthNum)
            heightNums.add(heightNum)
        }
    }

    override fun deleteSelectedItems() {
        val list = mAdapter?.getSelectedItems()
        for (i in list?.size!! - 1 downTo 0) {
            val index = list[i]
            mStringData.removeAt(index)
            widthNums.remove(index)
            heightNums.remove(index)
            mAdapter?.notifyItemRemoved(index)
        }
    }

    override fun resetSelectedItems() {
        mAdapter?.resetSelectedItems()
    }

}
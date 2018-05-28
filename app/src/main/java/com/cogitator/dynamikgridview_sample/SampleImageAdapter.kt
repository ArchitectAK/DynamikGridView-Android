package com.cogitator.dynamikgridview_sample

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cogitator.dynamikgridview.DynamikAdapter
import com.cogitator.dynamikgridview.DynamikLayoutManager
import kotlinx.android.synthetic.main.grid_item_image.view.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
class SampleImageAdapter(private val context: Context, private val recyclerView: RecyclerView, private val mImageDataPath: MutableList<String>) : DynamikAdapter(recyclerView) {

    private var sizePerSpan: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.grid_item_image, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val lp = setViewParams(mImageDataPath[position],
                holder.itemView)

        if (sizePerSpan == 0) {
            sizePerSpan = (recyclerView.layoutManager as DynamikLayoutManager).getSizePerSpan() + 1
        }

//        Glide.with(context)
//                .load("file://" + mImageDataPath[position])
//                .override(lp.widthNum * sizePerSpan, lp.heightNum * sizePerSpan)
//                .centerCrop()
//                .placeholder(R.drawable.bg_empty_photo)
//                .into((holder.itemView.iv_photo))


        Glide.with(context)
                .load("file://" + mImageDataPath[position])
                .apply(RequestOptions()
                        .override(lp.widthNum * sizePerSpan, lp.heightNum * sizePerSpan)
                        .centerCrop()
                        .placeholder(R.drawable.bg_empty_photo)
                        .error(R.drawable.bg_empty_photo))
                .into(holder.itemView?.iv_photo!!)



        holder.itemView.setOnClickListener({
            reverseSelect(holder, position)
        })
        holder.itemView.setOnLongClickListener({
            selectItem(holder, position)
        })


        super.onBindViewHolder(holder, position)
    }

    override fun getItemCount(): Int {
        return mImageDataPath.size
    }

    inner class MyViewHolder(v: View) : DynamikAdapter.ViewHolder(v)

    override fun updateSelectedItem(holder: ViewHolder) {
        holder.itemView.iv_check.visibility = View.VISIBLE
        holder.itemView.iv_photo.setColorFilter(Color.argb(120, 0, 0, 0))
    }

    override fun updateUnselectedItem(holder: ViewHolder) {
        holder.itemView.iv_check.visibility = View.GONE
        holder.itemView.iv_photo.clearColorFilter()
    }

    private fun setViewParams(path: String, view: View): DynamikLayoutManager.LayoutParams {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val imageHeight = options.outHeight
        val imageWidth = options.outWidth
        var widthNum = 1
        var heightNum = 1
        if (imageWidth >= 1200 && imageHeight >= 1200) {
            widthNum = 2
            heightNum = 2
        } else if (imageWidth >= imageHeight * 1.5) {
            widthNum = 2
        } else if (imageHeight >= imageWidth * 1.3) {
            heightNum = 2
        }
        val lp = view.layoutParams as DynamikLayoutManager.LayoutParams
        lp.widthNum = widthNum
        lp.heightNum = heightNum
        return lp
    }
}
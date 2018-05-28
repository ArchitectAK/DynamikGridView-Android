package com.cogitator.dynamikgridview_sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cogitator.dynamikgridview.DynamikFragment
import com.cogitator.dynamikgridview.DynamikItemAnimator
import com.cogitator.dynamikgridview.DynamikLayoutManager
import kotlinx.android.synthetic.main.fragment_sample.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
class SampleImageFragment : DynamikFragment() {

    private var mImageDataPath: MutableList<String> = ArrayList()
    private var mImageAdapter: SampleImageAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sample, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = DynamikLayoutManager(4)
        irregular_gridview.layoutManager = layoutManager

        mImageAdapter = SampleImageAdapter(context!!, irregular_gridview, mImageDataPath)
        irregular_gridview.adapter = mImageAdapter

        irregular_gridview.itemAnimator = DynamikItemAnimator()
    }

    override fun initData() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (!checkIfAlreadyHavePermission()) {
                requestForSpecificPermission()
            } else getImages()
        } else getImages()
    }

    private fun requestForSpecificPermission() {
        ActivityCompat.requestPermissions(activity as ScrollingActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
    }

    private fun checkIfAlreadyHavePermission(): Boolean {
        val result = ActivityCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            101 -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getImages()
            } else {
                //not granted
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun getImages() {
        mImageDataPath = ArrayList()
        val picFolder = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM).absolutePath
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = MediaStore.Images.Media.query(context!!.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
                "", null, MediaStore.Images.Media.DATE_TAKEN + " DESC")
        if (cursor != null) {
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                if (path != null && path.startsWith(picFolder)) {
                    mImageDataPath.add(path)
                }
            }
        }
        cursor!!.close()
    }

    override fun deleteSelectedItems() {
        val list = mImageAdapter?.getSelectedItems()
        for (i in list?.size!! - 1 downTo 0) {
            val index = list[i]
            mImageDataPath.removeAt(index)
            mImageAdapter?.notifyItemRemoved(index)
        }
    }

    override fun resetSelectedItems() {
        mImageAdapter?.resetSelectedItems()
    }

}
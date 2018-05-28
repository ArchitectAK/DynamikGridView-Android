package com.cogitator.dynamikgridview

import android.support.v4.app.Fragment
import android.support.v7.view.ActionMode
import android.view.MenuInflater
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
abstract class DynamikFragment : Fragment(), ActionMode.Callback {


    private var mActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initData()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.appbar_menu, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select -> {
                mActionMode = (context as AppCompatActivity)
                        .startSupportActionMode(this@DynamikFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Called when the action mode is created; startActionMode() was called
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Inflate a menu resource providing context menu items
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.context_menu, menu)
        return true
    }

    // Called each time the action mode is shown. Always called after onCreateActionMode, but
    // may be called multiple times if the mode is invalidated.
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false // Return false if nothing is done
    }

    // Called when the user selects a contextual menu item
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                deleteSelectedItems()
                mode.finish() // Action picked, so close the CAB
                true
            }
            else -> false
        }
    }

    // Called when the user exits the action mode
    override fun onDestroyActionMode(mode: ActionMode) {
        resetSelectedItems()
        mActionMode = null
    }

    abstract fun initData()

    abstract fun deleteSelectedItems()

    abstract fun resetSelectedItems()

}
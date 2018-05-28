package com.cogitator.dynamikgridview

import android.support.v7.widget.SimpleItemAnimator
import android.support.v4.view.ViewPropertyAnimatorListener
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView

import android.view.View


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 28/05/2018 (MM/DD/YYYY)
 */
class DynamikItemAnimator : SimpleItemAnimator() {
    private val DEBUG = false

    private val mPendingRemovals: MutableList<RecyclerView.ViewHolder> = ArrayList()
    private val mPendingAdditions: MutableList<RecyclerView.ViewHolder> = ArrayList()
    private val mPendingMoves: MutableList<MoveInfo> = ArrayList()
    private val mPendingChanges: MutableList<ChangeInfo> = ArrayList()

    private val mAdditionsList: MutableList<MutableList<RecyclerView.ViewHolder>> = ArrayList()
    private val mMovesList: MutableList<MutableList<MoveInfo>> = ArrayList()
    private val mChangesList: MutableList<MutableList<ChangeInfo>> = ArrayList()

    private val mAddAnimations: MutableList<RecyclerView.ViewHolder> = ArrayList()
    private val mMoveAnimations: MutableList<RecyclerView.ViewHolder> = ArrayList()
    private val mRemoveAnimations: MutableList<RecyclerView.ViewHolder> = ArrayList()
    private val mChangeAnimations: MutableList<RecyclerView.ViewHolder> = ArrayList()

    private class MoveInfo(var holder: RecyclerView.ViewHolder, var fromX: Int, var fromY: Int, var toX: Int, var toY: Int, var fromWidth: Int, var fromHeight: Int, var toWidth: Int, var toHeight: Int)

    private class ChangeInfo private constructor(var oldHolder: RecyclerView.ViewHolder?, var newHolder: RecyclerView.ViewHolder?) {
        var fromX: Int = 0
        var fromY: Int = 0
        var toX: Int = 0
        var toY: Int = 0
        var fromWidth: Int = 0
        var fromHeight: Int = 0
        var toWidth: Int = 0
        var toHeight: Int = 0

        constructor(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder?,
                    fromX: Int, fromY: Int, toX: Int, toY: Int, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int) : this(oldHolder, newHolder) {
            this.fromX = fromX
            this.fromY = fromY
            this.toX = toX
            this.toY = toY
            this.fromWidth = fromWidth
            this.fromHeight = fromHeight
            this.toWidth = toWidth
            this.toHeight = toHeight
        }

        override fun toString(): String {
            return "ChangeInfo{" +
                    "oldHolder=" + oldHolder +
                    ", newHolder=" + newHolder +
                    ", fromX=" + fromX +
                    ", fromY=" + fromY +
                    ", toX=" + toX +
                    ", toY=" + toY +
                    ", fromWidth=" + fromWidth +
                    ", fromHeight=" + fromHeight +
                    ", toWidth=" + toWidth +
                    ", toHeight=" + toHeight +
                    '}'.toString()
        }
    }


    override fun animateDisappearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo, postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo?): Boolean {
        val oldLeft = preLayoutInfo.left
        val oldRight = preLayoutInfo.right
        val oldTop = preLayoutInfo.top
        val oldBottom = preLayoutInfo.bottom
        val disappearingItemView = viewHolder.itemView
        val lp = disappearingItemView.layoutParams as RecyclerView.LayoutParams
        val newLeft = postLayoutInfo?.left ?: disappearingItemView.left
        val newRight = postLayoutInfo?.right ?: disappearingItemView.right
        val newTop = postLayoutInfo?.top ?: disappearingItemView.top
        val newBottom = postLayoutInfo?.bottom ?: disappearingItemView.bottom
        if (!lp.isItemRemoved && (oldLeft != newLeft || oldRight != newRight || oldTop != newTop || oldBottom != newBottom)) {
            disappearingItemView.layout(newLeft, newTop,
                    newLeft + disappearingItemView.width,
                    newTop + disappearingItemView.height)
            return animateMove(viewHolder, oldLeft, oldTop, newLeft, newTop, oldRight - oldLeft, oldBottom - oldTop, newRight - newLeft, newBottom - newTop)
        } else {
            return animateRemove(viewHolder)
        }
    }

    override fun animateAppearance(viewHolder: RecyclerView.ViewHolder, preLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo?, postLayoutInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        if (preLayoutInfo != null) {
            val fromWidth = preLayoutInfo.right - preLayoutInfo.left
            val fromHeight = preLayoutInfo.bottom - preLayoutInfo.top
            val toWidth = postLayoutInfo.right - postLayoutInfo.left
            val toHeight = postLayoutInfo.bottom - postLayoutInfo.top
            if (preLayoutInfo.left != postLayoutInfo.left
                    || preLayoutInfo.top != postLayoutInfo.top
                    || fromWidth != toWidth
                    || fromHeight != toHeight) {
                // slide items in if before/after locations differ
                return animateMove(viewHolder, preLayoutInfo.left, preLayoutInfo.top,
                        postLayoutInfo.left, postLayoutInfo.top, fromWidth, fromHeight, toWidth, toHeight)
            }
        }
        return animateAdd(viewHolder)

    }

    override fun animatePersistence(viewHolder: RecyclerView.ViewHolder,
                                    preInfo: RecyclerView.ItemAnimator.ItemHolderInfo, postInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        val fromWidth = preInfo.right - preInfo.left
        val fromHeight = preInfo.bottom - preInfo.top
        val toWidth = postInfo.right - postInfo.left
        val toHeight = postInfo.bottom - postInfo.top
        if (preInfo.left != postInfo.left || preInfo.top != postInfo.top || fromWidth != toWidth || fromHeight != toHeight) {
            return animateMove(viewHolder,
                    preInfo.left, preInfo.top, postInfo.left, postInfo.top, fromWidth, fromHeight, toWidth, toHeight)
        }
        dispatchMoveFinished(viewHolder)
        return false
    }

    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
                               preInfo: RecyclerView.ItemAnimator.ItemHolderInfo, postInfo: RecyclerView.ItemAnimator.ItemHolderInfo): Boolean {
        val fromWidth = preInfo.right - preInfo.left
        val fromHeight = preInfo.bottom - preInfo.top
        val toWidth = postInfo.right - postInfo.left
        val toHeight = postInfo.bottom - postInfo.top

        // Bug notation.
        // The code in the SimpleItemAnimator is as follows.
        // Note that shouldIgnore() cannot be accessed out of the package.
        // And I cannot find any replaceable method.

        //        if (newHolder.shouldIgnore()) {
        //            toLeft = preInfo.left;
        //            toTop = preInfo.top;
        //        } else {
        //            toLeft = postInfo.left;
        //            toTop = postInfo.top;
        //        }

        return animateChange(oldHolder, newHolder, preInfo.left, preInfo.top, postInfo.left, postInfo.top, fromWidth, fromHeight, toWidth, toHeight)
    }


    override fun runPendingAnimations() {
        val removalsPending = !mPendingRemovals.isEmpty()
        val movesPending = !mPendingMoves.isEmpty()
        val changesPending = !mPendingChanges.isEmpty()
        val additionsPending = !mPendingAdditions.isEmpty()
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            // nothing to animate
            return
        }
        // First, remove stuff
        for (holder in mPendingRemovals) {
            animateRemoveImpl(holder)
        }
        mPendingRemovals.clear()
        // Next, move stuff
        if (movesPending) {
            val moves: MutableList<MoveInfo> = ArrayList()
            moves.addAll(mPendingMoves)
            mMovesList.add(moves)
            mPendingMoves.clear()
            val mover = Runnable {
                for (moveInfo in moves) {
                    animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY,
                            moveInfo.fromWidth, moveInfo.fromHeight, moveInfo.toWidth, moveInfo.toHeight)
                }
                moves.clear()
                mMovesList.remove(moves)
            }
            if (removalsPending) {
                val view = moves.get(0).holder.itemView
                ViewCompat.postOnAnimationDelayed(view, mover, removeDuration)
            } else {
                mover.run()
            }
        }
        // Next, change stuff, to run in parallel with move animations
        if (changesPending) {
            val changes: MutableList<ChangeInfo> = ArrayList()
            changes.addAll(mPendingChanges)
            mChangesList.add(changes)
            mPendingChanges.clear()
            val changer = Runnable {
                for (change in changes) {
                    animateChangeImpl(change)
                }
                changes.clear()
                mChangesList.remove(changes)
            }
            if (removalsPending) {
                val holder = changes[0].oldHolder
                ViewCompat.postOnAnimationDelayed(holder?.itemView, changer, removeDuration)
            } else {
                changer.run()
            }
        }
        // Next, add stuff
        if (additionsPending) {
            val additions: MutableList<RecyclerView.ViewHolder> = ArrayList()
            additions.addAll(mPendingAdditions)
            mAdditionsList.add(additions)
            mPendingAdditions.clear()
            val adder = Runnable {
                for (holder in additions) {
                    animateAddImpl(holder)
                }
                additions.clear()
                mAdditionsList.remove(additions)
            }
            if (removalsPending || movesPending || changesPending) {
                val removeDuration = if (removalsPending) removeDuration else 0
                val moveDuration = if (movesPending) moveDuration else 0
                val changeDuration = if (changesPending) changeDuration else 0
                val totalDelay = removeDuration + Math.max(moveDuration, changeDuration)
                val view = additions[0].itemView
                ViewCompat.postOnAnimationDelayed(view, adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        mPendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = ViewCompat.animate(view)
        mRemoveAnimations.add(holder)
        animation.setDuration(removeDuration)
                .alpha(0f).setListener(object : VpaListenerAdapter() {
                    override fun onAnimationStart(view: View) {
                        dispatchRemoveStarting(holder)
                    }

                    override fun onAnimationEnd(view: View) {
                        animation.setListener(null)
                        ViewCompat.setAlpha(view, 1f)
                        dispatchRemoveFinished(holder)
                        mRemoveAnimations.remove(holder)
                        dispatchFinishedWhenDone()
                    }
                }).start()
    }

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        resetAnimation(holder)
        ViewCompat.setAlpha(holder.itemView, 0f)
        mPendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: RecyclerView.ViewHolder) {
        val view = holder.itemView
        val animation = ViewCompat.animate(view)
        mAddAnimations.add(holder)
        animation.alpha(1f).setDuration(addDuration).setListener(object : VpaListenerAdapter() {
            override fun onAnimationStart(view: View) {
                dispatchAddStarting(holder)
            }

            override fun onAnimationCancel(view: View) {
                ViewCompat.setAlpha(view, 1f)
            }

            override fun onAnimationEnd(view: View) {
                animation.setListener(null)
                dispatchAddFinished(holder)
                mAddAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }

    override fun animateMove(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        return animateMove(holder, fromX, fromY, toX, toY, 1, 1, 1, 1)
    }

    fun animateMove(holder: RecyclerView.ViewHolder, fromXX: Int, fromYY: Int, toX: Int, toY: Int,
                    fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): Boolean {
        var fromX = fromXX
        var fromY = fromYY
        val view = holder.itemView
        fromX += ViewCompat.getTranslationX(holder.itemView).toInt()
        fromY += ViewCompat.getTranslationY(holder.itemView).toInt()
        resetAnimation(holder)
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        var scaleX = toWidth.toFloat() / fromWidth
        var scaleY = toHeight.toFloat() / fromHeight
        if (scaleX == 0f) scaleX = 1f
        if (scaleY == 0f) scaleY = 1f
        if (deltaX == 0 && deltaY == 0 && scaleX == 1f && scaleY == 1f) {
            dispatchMoveFinished(holder)
            return false
        }
        view.pivotX = 0f
        view.pivotY = 0f
        if (scaleX != 1f) {
            ViewCompat.setScaleX(view, 1 / scaleX)
        }
        if (scaleY != 1f) {
            ViewCompat.setScaleY(view, 1 / scaleY)
        }
        if (deltaX != 0) {
            ViewCompat.setTranslationX(view, (-deltaX).toFloat())
        }
        if (deltaY != 0) {
            ViewCompat.setTranslationY(view, (-deltaY).toFloat())
        }

        mPendingMoves.add(MoveInfo(holder, fromX, fromY, toX, toY, fromWidth, fromHeight, toWidth, toHeight))
        return true
    }

    private fun animateMoveImpl(holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int, toX: Int, toY: Int, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int) {
        val view = holder.itemView
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        val scaleX = if (toWidth == 0) 1 else toWidth.toFloat() / fromWidth
        val scaleY = if (toHeight == 0) 1 else toHeight.toFloat() / fromHeight
        if (deltaX != 0) {
            ViewCompat.animate(view).translationX(0f)
        }
        if (deltaY != 0) {
            ViewCompat.animate(view).translationY(0f)
        }
        if (scaleX != 1f) {
            ViewCompat.animate(view).scaleX(1f)
        }
        if (scaleY != 1f) {
            ViewCompat.animate(view).scaleY(1f)
        }

        // TODO: make EndActions end listeners instead, since end actions aren't called when
        // vpas are canceled (and can't end them. why?)
        // need listener functionality in VPACompat for this. Ick.
        val animation = ViewCompat.animate(view)
        mMoveAnimations.add(holder)
        animation.setDuration(moveDuration).setListener(object : VpaListenerAdapter() {
            override fun onAnimationStart(view: View) {
                dispatchMoveStarting(holder)
            }

            override fun onAnimationCancel(view: View) {
                if (deltaX != 0) {
                    ViewCompat.setTranslationX(view, 0f)
                }
                if (deltaY != 0) {
                    ViewCompat.setTranslationY(view, 0f)
                }
                if (scaleX != 1f) {
                    ViewCompat.setScaleX(view, 1f)
                }
                if (scaleY != 1f) {
                    ViewCompat.setScaleY(view, 1f)
                }
            }

            override fun onAnimationEnd(view: View) {
                animation.setListener(null)
                dispatchMoveFinished(holder)
                mMoveAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }


    override fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
                               fromX: Int, fromY: Int, toX: Int, toY: Int): Boolean {
        return animateChange(oldHolder, newHolder, fromX, fromY, toX, toY, 1, 1, 1, 1)
    }

    fun animateChange(oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder?,
                      fromX: Int, fromY: Int, toX: Int, toY: Int, fromWidth: Int, fromHeight: Int, toWidth: Int, toHeight: Int): Boolean {
        if (oldHolder === newHolder) {
            // Don't know how to run change animations when the same view holder is re-used.
            // run a move animation to handle position changes.
            return animateMove(oldHolder, fromX, fromY, toX, toY, fromWidth, fromHeight, toWidth, toHeight)
        }
        val prevTranslationX = ViewCompat.getTranslationX(oldHolder.itemView)
        val prevTranslationY = ViewCompat.getTranslationY(oldHolder.itemView)
        val prevScaleX = ViewCompat.getScaleX(oldHolder.itemView)
        val prevScaleY = ViewCompat.getScaleY(oldHolder.itemView)
        val prevAlpha = ViewCompat.getAlpha(oldHolder.itemView)
        resetAnimation(oldHolder)
        val deltaX = (toX.toFloat() - fromX.toFloat() - prevTranslationX).toInt()
        val deltaY = (toY.toFloat() - fromY.toFloat() - prevTranslationY).toInt()
        var scaleX = toWidth.toFloat() / fromWidth
        var scaleY = toHeight.toFloat() / fromHeight
        if (scaleX == 0f) scaleX = 1f
        if (scaleY == 0f) scaleY = 1f
        // recover prev translation state after ending animation
        ViewCompat.setTranslationX(oldHolder.itemView, prevTranslationX)
        ViewCompat.setTranslationY(oldHolder.itemView, prevTranslationY)

        ViewCompat.setScaleX(oldHolder.itemView, prevScaleX)
        ViewCompat.setScaleY(oldHolder.itemView, prevScaleY)
        ViewCompat.setAlpha(oldHolder.itemView, prevAlpha)
        if (newHolder != null) {
            // carry over translation values
            resetAnimation(newHolder)
            ViewCompat.setTranslationX(newHolder.itemView, (-deltaX).toFloat())
            ViewCompat.setTranslationY(newHolder.itemView, (-deltaY).toFloat())
            newHolder.itemView.pivotX = 0f
            newHolder.itemView.pivotY = 0f
            ViewCompat.setScaleX(newHolder.itemView, 1 / scaleX)
            ViewCompat.setScaleY(newHolder.itemView, 1 / scaleY)
            ViewCompat.setAlpha(newHolder.itemView, 0f)
        }
        mPendingChanges.add(ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY, fromWidth, fromHeight, toWidth, toHeight))
        return true
    }

    private fun animateChangeImpl(changeInfo: ChangeInfo) {
        val holder = changeInfo.oldHolder
        val view = holder?.itemView
        val newHolder = changeInfo.newHolder
        val newView = newHolder?.itemView
        if (view != null) {
            val oldViewAnim = ViewCompat.animate(view).setDuration(
                    changeDuration)
            mChangeAnimations.add(changeInfo.oldHolder!!)
            oldViewAnim.translationX((changeInfo.toX - changeInfo.fromX).toFloat())
            oldViewAnim.translationY((changeInfo.toY - changeInfo.fromY).toFloat())
            var scaleX = changeInfo.toWidth.toFloat() / changeInfo.fromWidth
            var scaleY = changeInfo.toHeight.toFloat() / changeInfo.fromHeight
            if (scaleX == 0f) scaleX = 1f
            if (scaleY == 0f) scaleY = 1f

            oldViewAnim.scaleX(scaleX)
            oldViewAnim.scaleY(scaleY)
            oldViewAnim.alpha(0f).setListener(object : VpaListenerAdapter() {
                override fun onAnimationStart(view: View) {
                    dispatchChangeStarting(changeInfo.oldHolder, true)
                }

                override fun onAnimationEnd(view: View) {
                    oldViewAnim.setListener(null)
                    ViewCompat.setAlpha(view, 1f)
                    ViewCompat.setTranslationX(view, 0f)
                    ViewCompat.setTranslationY(view, 0f)
                    ViewCompat.setScaleX(view, 1f)
                    ViewCompat.setScaleY(view, 1f)
                    dispatchChangeFinished(changeInfo.oldHolder, true)
                    mChangeAnimations.remove(changeInfo.oldHolder!!)
                    dispatchFinishedWhenDone()
                }
            }).start()
        }
        if (newView != null) {
            val newViewAnimation = ViewCompat.animate(newView)
            mChangeAnimations.add(changeInfo.newHolder!!)

            newViewAnimation.translationX(0f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(changeDuration).alpha(1f).setListener(object : VpaListenerAdapter() {
                override fun onAnimationStart(view: View) {
                    dispatchChangeStarting(changeInfo.newHolder, false)
                }

                override fun onAnimationEnd(view: View) {
                    newViewAnimation.setListener(null)
                    ViewCompat.setAlpha(newView, 1f)
                    ViewCompat.setTranslationX(newView, 0f)
                    ViewCompat.setTranslationY(newView, 0f)
                    ViewCompat.setScaleX(view, 1f)
                    ViewCompat.setScaleY(view, 1f)
                    dispatchChangeFinished(changeInfo.newHolder, false)
                    mChangeAnimations.remove(changeInfo.newHolder!!)
                    dispatchFinishedWhenDone()
                }
            }).start()
        }
    }

    private fun endChangeAnimation(infoList: MutableList<ChangeInfo>, item: RecyclerView.ViewHolder) {
        for (i in infoList.indices.reversed()) {
            val changeInfo = infoList[i]
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.remove(changeInfo)
                }
            }
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder)
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder)
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo, item: RecyclerView.ViewHolder?): Boolean {
        var oldItem = false
        if (changeInfo.newHolder === item) {
            changeInfo.newHolder = null
        } else if (changeInfo.oldHolder === item) {
            changeInfo.oldHolder = null
            oldItem = true
        } else {
            return false
        }
        ViewCompat.setAlpha(item!!.itemView, 1f)
        ViewCompat.setTranslationX(item.itemView, 0f)
        ViewCompat.setTranslationY(item.itemView, 0f)
        ViewCompat.setScaleX(item.itemView, 1f)
        ViewCompat.setScaleY(item.itemView, 1f)
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        val view = item.itemView
        // this will trigger end callback which should set properties to their target values.
        ViewCompat.animate(view).cancel()
        // TODO if some other animations are chained to end, how do we cancel them as well?
        for (i in mPendingMoves.size - 1 downTo 0) {
            val moveInfo = mPendingMoves.get(i)
            if (moveInfo.holder === item) {
                ViewCompat.setTranslationY(view, 0f)
                ViewCompat.setTranslationX(view, 0f)
                ViewCompat.setScaleX(view, 1f)
                ViewCompat.setScaleY(view, 1f)
                dispatchMoveFinished(item)
                mPendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(mPendingChanges, item)
        if (mPendingRemovals.remove(item)) {
            ViewCompat.setAlpha(view, 1f)
            dispatchRemoveFinished(item)
        }
        if (mPendingAdditions.remove(item)) {
            ViewCompat.setAlpha(view, 1f)
            dispatchAddFinished(item)
        }

        for (i in mChangesList.size - 1 downTo 0) {
            val changes = mChangesList.get(i)
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) {
                mChangesList.removeAt(i)
            }
        }
        for (i in mMovesList.size - 1 downTo 0) {
            val moves = mMovesList.get(i)
            for (j in moves.size - 1 downTo 0) {
                val moveInfo = moves.get(j)
                if (moveInfo.holder === item) {
                    ViewCompat.setTranslationY(view, 0f)
                    ViewCompat.setTranslationX(view, 0f)
                    ViewCompat.setScaleX(view, 1f)
                    ViewCompat.setScaleY(view, 1f)
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) {
                        mMovesList.removeAt(i)
                    }
                    break
                }
            }
        }
        for (i in mAdditionsList.size - 1 downTo 0) {
            val additions = mAdditionsList.get(i)
            if (additions.remove(item)) {
                ViewCompat.setAlpha(view, 1f)
                dispatchAddFinished(item)
                if (additions.isEmpty()) {
                    mAdditionsList.removeAt(i)
                }
            }
        }

        // animations should be ended by the cancel above.
        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mRemoveAnimations.remove(item) && DEBUG) {
            throw IllegalStateException("after animation is cancelled, item should not be in " + "mRemoveAnimations list")
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mAddAnimations.remove(item) && DEBUG) {
            throw IllegalStateException("after animation is cancelled, item should not be in " + "mAddAnimations list")
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mChangeAnimations.remove(item) && DEBUG) {
            throw IllegalStateException("after animation is cancelled, item should not be in " + "mChangeAnimations list")
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (mMoveAnimations.remove(item) && DEBUG) {
            throw IllegalStateException("after animation is cancelled, item should not be in " + "mMoveAnimations list")
        }
        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: RecyclerView.ViewHolder) {
//        AnimatorCompatHelper.clearInterpolator(holder.itemView)
        endAnimation(holder)
    }

    override fun isRunning(): Boolean {
        return !mPendingAdditions.isEmpty() ||
                !mPendingChanges.isEmpty() ||
                !mPendingMoves.isEmpty() ||
                !mPendingRemovals.isEmpty() ||
                !mMoveAnimations.isEmpty() ||
                !mRemoveAnimations.isEmpty() ||
                !mAddAnimations.isEmpty() ||
                !mChangeAnimations.isEmpty() ||
                !mMovesList.isEmpty() ||
                !mAdditionsList.isEmpty() ||
                !mChangesList.isEmpty()
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call [.dispatchAnimationsFinished] to notify any
     * listeners.
     */
    private fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    override fun endAnimations() {
        var count = mPendingMoves.size
        for (i in count - 1 downTo 0) {
            val item = mPendingMoves[i]
            val view = item.holder.itemView
            ViewCompat.setTranslationY(view, 0f)
            ViewCompat.setTranslationX(view, 0f)
            ViewCompat.setScaleX(view, 1f)
            ViewCompat.setScaleY(view, 1f)
            dispatchMoveFinished(item.holder)
            mPendingMoves.removeAt(i)
        }
        count = mPendingRemovals.size
        for (i in count - 1 downTo 0) {
            val item = mPendingRemovals.get(i)
            dispatchRemoveFinished(item)
            mPendingRemovals.removeAt(i)
        }
        count = mPendingAdditions.size
        for (i in count - 1 downTo 0) {
            val item = mPendingAdditions.get(i)
            val view = item.itemView
            ViewCompat.setAlpha(view, 1f)
            dispatchAddFinished(item)
            mPendingAdditions.removeAt(i)
        }
        count = mPendingChanges.size
        for (i in count - 1 downTo 0) {
            endChangeAnimationIfNecessary(mPendingChanges.get(i))
        }
        mPendingChanges.clear()
        if (!isRunning) {
            return
        }

        var listCount = mMovesList.size
        for (i in listCount - 1 downTo 0) {
            val moves = mMovesList.get(i)
            count = moves.size
            for (j in count - 1 downTo 0) {
                val moveInfo = moves.get(j)
                val item = moveInfo.holder
                val view = item.itemView
                ViewCompat.setTranslationY(view, 0f)
                ViewCompat.setTranslationX(view, 0f)
                ViewCompat.setScaleX(view, 1f)
                ViewCompat.setScaleY(view, 1f)
                dispatchMoveFinished(moveInfo.holder)
                moves.removeAt(j)
                if (moves.isEmpty()) {
                    mMovesList.remove(moves)
                }
            }
        }
        listCount = mAdditionsList.size
        for (i in listCount - 1 downTo 0) {
            val additions = mAdditionsList.get(i)
            count = additions.size
            for (j in count - 1 downTo 0) {
                val item = additions.get(j)
                val view = item.itemView
                ViewCompat.setAlpha(view, 1f)
                dispatchAddFinished(item)
                additions.removeAt(j)
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions)
                }
            }
        }
        listCount = mChangesList.size
        for (i in listCount - 1 downTo 0) {
            val changes = mChangesList.get(i)
            count = changes.size
            for (j in count - 1 downTo 0) {
                endChangeAnimationIfNecessary(changes.get(j))
                if (changes.isEmpty()) {
                    mChangesList.remove(changes)
                }
            }
        }

        cancelAll(mRemoveAnimations)
        cancelAll(mMoveAnimations)
        cancelAll(mAddAnimations)
        cancelAll(mChangeAnimations)

        dispatchAnimationsFinished()
    }

    fun cancelAll(viewHolders: List<RecyclerView.ViewHolder>) {
        for (i in viewHolders.indices.reversed()) {
            ViewCompat.animate(viewHolders[i].itemView).cancel()
        }
    }

    private open class VpaListenerAdapter : ViewPropertyAnimatorListener {
        override fun onAnimationStart(view: View) {}

        override fun onAnimationEnd(view: View) {}

        override fun onAnimationCancel(view: View) {}
    }

}
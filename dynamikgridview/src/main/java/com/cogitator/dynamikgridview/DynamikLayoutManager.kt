package com.cogitator.dynamikgridview

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import java.util.*


/**
 * @author Ankit Kumar (ankitdroiddeveloper@gmail.com) on 24/05/2018 (MM/DD/YYYY)
 */
class DynamikLayoutManager(spanCount: Int) : RecyclerView.LayoutManager() {
    /* Default spanCount. */
    private val DEFAULT_SPAN_COUNT = 4
    /* Current spanCount. */
    private var mSpanCount = DEFAULT_SPAN_COUNT

    /* Store the bottom of each span. */
    private var spanBottom: IntArray? = null
    /* The minimum  of the spanBottom. */
    private var spanBottomMin: Int = 0
    /* The maximum of the spanBottom. */
    private var spanBottomMax: Int = 0
    /* The top of each span. */
    private var spanTop: IntArray? = null
    /* The minimum of the spanTop. */
    private var spanTopMin: Int = 0
    /* The maximum of the spanTop. */
    private var spanTopMax: Int = 0

    /* Store the first span index where the first and the second span are both empty. */
    private var firstTwoEmptyBottomSpanIndex: Int = 0
    /* The index of the first empty span. */
    private var firstOneEmptyBottomSpanIndex: Int = 0

    /* The top border of the area to be filled. */
    private var topBorder: Int = 0
    /* The bottom border. */
    private var bottomBorder: Int = 0

    /**
     * Store the left and right borders for each span according to the span count.
     * This is the same as the mCachedBorders in the GridLayoutManager.
     */
    private var spanWidthBorders: IntArray? = null
    /* Size for the smallest span. */
    private var sizePerSpan: Int = 0
    /* The current position we need to lay out. */
    private var mCurrentPosition: Int = 0
    /* The first and the last position of attached items. */
    private var firstAttachedItemPosition: Int = 0
    private var lastAttachedItemPosition: Int = 0

    /**
     * Store the layout widthNum and heightNum for each item.
     * The layout width is about widthNum * sizePerSpan.
     * The layout height is heightNum * sizePerSpan.
     */
    private var itemLayoutWidthCache: SparseIntArray? = null
    private var itemLayoutHeightCache: SparseIntArray? = null
    /* The first span index the item occupied. */
    private var itemOccupiedStartSpan: SparseIntArray? = null

    /* The scroll offset. */
    private var scrollOffset: Int = 0

    /* The first item which is removed with notifyItemRemoved(). */
    private var firstChangedPosition: Int = 0
    /* The number of removed items except for the items out of the bottom border. */
    private var removedTopAndBoundPositionCount: Int = 0
    /**
     * If it is true, we need to update some parameters,
     * i.e., firstChangedPosition, removedTopAndBoundPositionCount.
     */
    private var isBeforePreLayout: Boolean = false

    /* A disappearing view cache with descending order. */
    private var disappearingViewCache: TreeMap<Int, DisappearingViewParams>? = null

    /* Determine whether onLayoutChildren() is triggered with notifyDataSetChanged(). */
    private var isNotifyDataSetChanged: Boolean = false

    /* The Rect for the items to be laid out. */
    val mDecorInsets = Rect()

    /**
     * The following params is calculated during the pre-layout phase
     * and is used for the real layout.
     */
    private var fakeSpanBottomMin: Int = 0
    private var fakeSpanBottomMax: Int = 0
    private var fakeCurrentPosition: Int = 0
    private var fakeFirstAttachedItemPosition: Int = 0
    private var fakeSpanTop: IntArray? = null
    private var fakeSpanBottom: IntArray? = null
    private var fakeFirstTwoEmptyBottomSpanIndex: Int = 0
    private var fakeFirstOneEmptyBottomSpanIndex: Int = 0
    private var fakeItemLayoutWidthCache: SparseIntArray? = null
    private var fakeItemLayoutHeightCache: SparseIntArray? = null
    private var fakeItemOccupiedStartSpan: SparseIntArray? = null


    init {
        setSpanCount(spanCount)
    }


    /**
     * set the number of span, it should be at least 2.
     * @param spanCount The number of spans.
     */
    private fun setSpanCount(spanCount: Int) {
        if (spanCount == mSpanCount)
            return
        if (spanCount < 2)
            throw IllegalArgumentException("Span count should be at least 2. Provided $spanCount")
        mSpanCount = spanCount
    }

    /**
     *
     * @return sizePerSpan
     */
    fun getSizePerSpan(): Int {
        return sizePerSpan
    }

    /**
     * If you want to customize the animation, it should return true.
     * @return
     */
    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    override fun onAdapterChanged(oldAdapter: RecyclerView.Adapter<*>?, newAdapter: RecyclerView.Adapter<*>?) {
        removeAllViews()
    }

    /**
     * This method is triggered with notifyDataSetChanged().
     * We need the label the isNotifyDataSetChanged before onLayoutChildren.
     * @param recyclerView
     */
    override fun onItemsChanged(recyclerView: RecyclerView?) {
        isNotifyDataSetChanged = true
    }

    /**
     * Triggered with notifyItemRemoved().
     * This method will be triggered before the pre-layout for invisible items (out of bounds)
     * and after the pre-layout for visible items.
     * If there are items removed out of the top border, we update the firstChangedPosition
     * and removedTopAndBoundPositionCount.
     * @param recyclerView
     * @param positionStart The start position of removed items.
     * @param itemCount The number of removed items from the start position.
     */
    override fun onItemsRemoved(recyclerView: RecyclerView?, positionStart: Int, itemCount: Int) {
        if (isBeforePreLayout) {
            if (firstChangedPosition > positionStart || firstChangedPosition == -1)
                firstChangedPosition = positionStart
            if (firstChangedPosition < firstAttachedItemPosition)
                removedTopAndBoundPositionCount += itemCount
        }
    }

    /**
     * Called when it is initial layout, or the data set is changed.
     * If supportsPredictiveItemAnimations() returns true, it will be called twice,
     * i.e., the pre-layout and the real layout.
     * @param recycler
     * @param state
     */
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {

        // Nothing to be laid out, just clear attached views and return.
        if (state!!.itemCount == 0) {
            detachAndScrapAttachedViews(recycler)
            return
        }
        // For the pre-layout, we need to layout current attached views and appearing views.
        if (state.isPreLayout) {
            // If nothing is attached, just return.
            if (childCount == 0)
                return
            // For the current attached views, find the views removed and update
            // removedTopAndBoundPositionCount and firstChangedPosition.
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                val lp = child.layoutParams as RecyclerView.LayoutParams
                if (lp.isItemRemoved) {
                    removedTopAndBoundPositionCount++
                    if (firstChangedPosition == -1 || firstAttachedItemPosition + i < firstChangedPosition) {
                        firstChangedPosition = firstAttachedItemPosition + i
                    }
                }
            }
            // If removedTopAndBoundPositionCount = 0, items changes out of the bottom border,
            // So we have nothing to do during the pre-layout.
            // Otherwise we need to lay out current attached views and appearing views.
            if (removedTopAndBoundPositionCount != 0) {
                layoutAttachedAndAppearingViews(recycler, state)
            }
            // Reset isBeforePreLayout after the pre-layout ends.
            isBeforePreLayout = false
            return
        }

        // The real layout.
        // First or empty layout, initialize layout parameters and fill.
        if (childCount == 0) {
            initializeLayoutParameters()
            fillGrid(recycler, state, true)
            return
        }
        // If it is triggered with notifyDataSetChanged(),
        // we just clear attached views and layout from the beginning.
        if (isNotifyDataSetChanged) {
            detachAndScrapAttachedViews(recycler)
            initializeLayoutParameters()
            fillGrid(recycler, state, true)
            isNotifyDataSetChanged = false
            return
        }

        // Adapter data set changes.
        if (firstChangedPosition == -1) { // No item is removed
            // reset parameters.
            mCurrentPosition = firstAttachedItemPosition
            lastAttachedItemPosition = firstAttachedItemPosition
            topBorder = paddingTop
            bottomBorder = height - paddingBottom
            spanBottom = Arrays.copyOf(spanTop, mSpanCount)
            updateSpanBottomParameters()
            // Fill the area.
            detachAndScrapAttachedViews(recycler)
            fillGrid(recycler, state, true)

            // Reset isBeforePreLayout.
            isBeforePreLayout = true
            //            firstChangedPosition = -1;
            //            removedTopAndBoundPositionCount = 0;
            return
        }
        // There are removed items.
        // Clear the cache from the firstChangedPosition
        for (i in firstChangedPosition until state.itemCount) {
            if (itemLayoutWidthCache?.get(i, 0) != 0) {
                itemLayoutWidthCache?.delete(i)
                itemLayoutHeightCache?.delete(i)
                itemOccupiedStartSpan?.delete(i)
            }
            if (fakeItemLayoutWidthCache?.get(i, 0) != 0) {
                fakeItemLayoutWidthCache?.get(i)?.let { itemLayoutWidthCache?.put(i, it) }
                fakeItemLayoutHeightCache?.get(i)?.let { itemLayoutHeightCache?.put(i, it) }
                fakeItemOccupiedStartSpan?.get(i)?.let { itemOccupiedStartSpan?.put(i, it) }
            }
        }
        fakeItemLayoutWidthCache?.clear()
        fakeItemLayoutHeightCache?.clear()
        fakeItemOccupiedStartSpan?.clear()

        detachAndScrapAttachedViews(recycler)

        // There are removed items out of the upper bound.
        if (firstChangedPosition < firstAttachedItemPosition) {
            mCurrentPosition = firstAttachedItemPosition
            lastAttachedItemPosition = firstAttachedItemPosition
            topBorder = paddingTop
            bottomBorder = height - paddingBottom
            spanBottom = Arrays.copyOf(spanTop, mSpanCount)
            updateSpanBottomParameters()
            fillGrid(recycler, state, true)
            // If it cannot fill until the bottomBorder, call  scrollBy() to fill.
            if (spanBottomMax < bottomBorder) {
                scrollBy(spanBottomMax - bottomBorder, recycler, state)
            }
            // Finally, we layout disappearing views.
            layoutDisappearingViews(recycler, state)
        } else { // There are no removed items out of the upper bound.
            // Just set layout parameters and fill the visible area.
            mCurrentPosition = firstAttachedItemPosition
            lastAttachedItemPosition = firstAttachedItemPosition
            topBorder = paddingTop
            bottomBorder = height - paddingBottom
            spanBottom = Arrays.copyOf(spanTop, mSpanCount)
            updateSpanBottomParameters()
            fillGrid(recycler, state, true)
            // The number of items is too small, call scrollBy() to fill.
            if (spanBottomMax - bottomBorder < 0) {
                scrollBy(spanBottomMax - bottomBorder, recycler, state)
            }
        }
        // After the real layout, we need to clear some parameters.
        isBeforePreLayout = true
        firstChangedPosition = -1
        removedTopAndBoundPositionCount = 0
        disappearingViewCache?.clear()
    }

    /**
     * Return true to indicate that it supports scrolling vertically.
     * @return
     */
    override fun canScrollVertically(): Boolean {
        return true
    }

    /**
     * We need to fill some extra space and offset children in this method.
     * @param dy The distance scrolled.
     * @param recycler
     * @param state
     * @return
     */
    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // Nothing to do when there are no attached items or dy = 0.
        return if (childCount == 0 || dy == 0) {
            0
        } else scrollBy(dy, recycler, state)

    }

    /**
     * The real logic for scroll.
     * @param dy The distance scrolled.
     * @param recycler
     * @param state
     * @return
     */
    private fun scrollBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {

        var delta = 0
        // When scroll down, layout from left to right, top to bottom
        // Scroll down, update bottomBorder and fill.
        if (dy > 0) {
            topBorder = paddingTop
            bottomBorder += dy
            mCurrentPosition = lastAttachedItemPosition + 1
            fillGrid(recycler, state, true)
            // Offset child views.
            if (spanBottomMin >= bottomBorder) {
                delta = dy
                bottomBorder -= dy
            } else { // There are no more items we need to lay out.
                bottomBorder = height - paddingBottom
                if (spanBottomMax - bottomBorder >= dy) {
                    delta = dy
                } else {
                    delta = Math.max(0, spanBottomMax - bottomBorder)
                }
            }
            offsetChildrenVertical(-delta)
            // After offset children, we need to update parameters.
            for (i in 0 until mSpanCount) {
                spanTop?.set(i, delta)
                spanBottom?.set(i, delta)
            }
            spanTopMin -= delta
            spanTopMax -= delta
            spanBottomMin -= delta
            spanBottomMax -= delta
            // Recycle views out of the topBorder
            recycleTopInvisibleViews(recycler)
        } else { // dy < 0
            // Scroll up, update topBorder and fill.
            topBorder += dy
            bottomBorder = height - paddingBottom
            // Happens when we delete too much items,
            // and the item for firstAttachedItemPosition is null.
            if (firstAttachedItemPosition == -1 || firstAttachedItemPosition >= state!!.itemCount) {
                firstAttachedItemPosition = state!!.itemCount - 1
                lastAttachedItemPosition = firstAttachedItemPosition
                mCurrentPosition = firstAttachedItemPosition
            } else {
                mCurrentPosition = firstAttachedItemPosition - 1
            }
            fillGrid(recycler, state, false)
            // Offset child views.
            if (spanTopMax <= topBorder) {
                delta = dy
                topBorder -= dy
            } else { // There are no more items we need to lay out.
                topBorder = paddingTop
                if (spanTopMin - topBorder <= dy) {
                    delta = dy
                } else {
                    delta = -Math.max(0, topBorder - spanTopMin)
                }
            }
            offsetChildrenVertical(-delta)
            // After offset children, we need to update parameters.
            for (i in 0 until mSpanCount) {
                spanTop?.set(i, delta)
                spanBottom?.set(i, delta)
            }
            spanTopMin -= delta
            spanTopMax -= delta
            spanBottomMin -= delta
            spanBottomMax -= delta
            // Recycle views out of the bottomBorder.
            recycleBottomInvisibleViews(recycler)
        }
        // Update scrollOffset.
        scrollOffset += delta
        return delta
    }

    /**
     * Initialize necessary parameters.
     */
    private fun initializeLayoutParameters() {

        topBorder = paddingTop
        bottomBorder = height - paddingBottom
        spanTop = IntArray(mSpanCount)
        Arrays.fill(spanTop, paddingTop)
        spanBottom = IntArray(mSpanCount)
        Arrays.fill(spanBottom, paddingTop)
        spanTopMin = paddingTop
        spanTopMax = paddingTop
        spanBottomMin = paddingTop
        spanBottomMax = paddingTop
        firstOneEmptyBottomSpanIndex = 0
        firstTwoEmptyBottomSpanIndex = 0
        spanWidthBorders = IntArray(mSpanCount + 1)
        calculateSpanWidthBorders(width - paddingLeft - paddingRight)
        mCurrentPosition = 0
        firstAttachedItemPosition = 0
        lastAttachedItemPosition = 0
        itemLayoutWidthCache = SparseIntArray()
        itemLayoutHeightCache = SparseIntArray()
        itemOccupiedStartSpan = SparseIntArray()
        //isRandomSize = true;
        scrollOffset = 0
        isBeforePreLayout = true
        firstChangedPosition = -1
        removedTopAndBoundPositionCount = 0
        disappearingViewCache = TreeMap(Comparator<Int> { lhs, rhs -> rhs!!.compareTo(lhs) })
        isNotifyDataSetChanged = false

        fakeItemLayoutWidthCache = SparseIntArray()
        fakeItemLayoutHeightCache = SparseIntArray()
        fakeItemOccupiedStartSpan = SparseIntArray()
    }

    /**
     * Update spanBottomMin, spanBottomMax,
     * firstOneEmptyBottomSpanIndex and firstTwoEmptyBottomSpanIndex.
     */
    private fun updateSpanBottomParameters() {
        spanBottomMin = spanBottom?.get(0)!!
        spanBottomMax = spanBottom!![0]
        for (i in 1 until mSpanCount) {
            if (spanBottomMin > spanBottom!![i])
                spanBottomMin = spanBottom!![i]
            if (spanBottomMax < spanBottom!![i])
                spanBottomMax = spanBottom!![i]
        }
        for (i in 0 until mSpanCount) {
            if (spanBottom!![i] == spanBottomMin) {
                firstOneEmptyBottomSpanIndex = i
                break
            }
        }
        firstTwoEmptyBottomSpanIndex = -1
        for (i in firstOneEmptyBottomSpanIndex until mSpanCount - 1) {
            if (spanBottom!![i] == spanBottomMin && spanBottom!![i + 1] == spanBottomMin) {
                firstTwoEmptyBottomSpanIndex = i
                break
            }
        }
    }

    /**
     * Update fake params.
     */
    private fun updateFakeSpanBottomParameters() {
        fakeSpanBottomMin = fakeSpanBottom!![0]
        fakeSpanBottomMax = fakeSpanBottom!![0]
        for (i in 1 until mSpanCount) {
            if (fakeSpanBottomMin > fakeSpanBottom!![i])
                fakeSpanBottomMin = fakeSpanBottom!![i]
            if (fakeSpanBottomMax < fakeSpanBottom!![i])
                fakeSpanBottomMax = fakeSpanBottom!![i]
        }
        for (i in 0 until mSpanCount) {
            if (fakeSpanBottom!![i] == fakeSpanBottomMin) {
                fakeFirstOneEmptyBottomSpanIndex = i
                break
            }
        }
        fakeFirstTwoEmptyBottomSpanIndex = -1
        for (i in fakeFirstOneEmptyBottomSpanIndex until mSpanCount - 1) {
            if (fakeSpanBottom!![i] == fakeSpanBottomMin && fakeSpanBottom!![i + 1] == fakeSpanBottomMin) {
                fakeFirstTwoEmptyBottomSpanIndex = i
                break
            }
        }
    }

    /**
     * Update spanTopMin and spanTopMax.
     */
    private fun updateSpanTopParameters() {
        spanTopMin = spanTop!![0]
        spanTopMax = spanTop!![0]
        for (i in 1 until mSpanCount) {
            if (spanTopMin > spanTop!![i])
                spanTopMin = spanTop!![i]
            if (spanTopMax < spanTop!![i])
                spanTopMax = spanTop!![i]
        }
    }

    /**
     * Calculate spanWidthBorders.
     * This is the same as calculateItemBorders(int totalSpace) in the GridLayoutManager.
     * @param totalSpace
     */
    private fun calculateSpanWidthBorders(totalSpace: Int) {
        if (spanWidthBorders == null || spanWidthBorders!!.size != mSpanCount + 1
                || spanWidthBorders!![spanWidthBorders!!.size - 1] != totalSpace) {
            spanWidthBorders = IntArray(mSpanCount + 1)
        }
        spanWidthBorders!![0] = 0
        sizePerSpan = totalSpace / mSpanCount
        val sizePerSpanRemainder = totalSpace % mSpanCount
        var consumedPixels = 0
        var additionalSize = 0
        for (i in 1..mSpanCount) {
            var itemSize = sizePerSpan
            additionalSize += sizePerSpanRemainder
            if (additionalSize > 0 && mSpanCount - additionalSize < sizePerSpanRemainder) {
                itemSize += 1
                additionalSize -= mSpanCount
            }
            consumedPixels += itemSize
            spanWidthBorders!![i] = consumedPixels
        }
    }


    /**
     * fill the area between the topBorder and the bottomBorder.
     * @param recycler
     * @param state
     * @param isFillBottom The direction, from the top to the bottom or the reverse.
     */
    private fun fillGrid(recycler: RecyclerView.Recycler?, state: RecyclerView.State?,
                         isFillBottom: Boolean) {
        while ((isFillBottom && spanBottomMin <= bottomBorder || !isFillBottom && spanTopMax >= topBorder)
                && mCurrentPosition >= 0 && mCurrentPosition < state!!.itemCount) {
            layoutChunk(recycler, state, isFillBottom)
        }
    }

    /**
     * Fill the area for pre-layout.
     * On the one hand, we layout current attached view and appearing views.
     * On the other hand, we calculate the layout params for the real layout.
     * And the stop criterion is calculated according the params.
     * @param recycler
     * @param state
     */
    private fun fillGridForPreLayout(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        while (fakeSpanBottomMin <= bottomBorder
                && mCurrentPosition >= 0 && mCurrentPosition < state.itemCount) {
            layoutChunk(recycler, state, true, true)
        }
    }


    /**
     * Here we lay out current attached views and appearing views.
     * Called when there are removed items out of the top border and in the visible area.
     * @param recycler
     * @param state
     */
    private fun layoutAttachedAndAppearingViews(recycler: RecyclerView.Recycler?,
                                                state: RecyclerView.State) {

        // There are no removed items out of the top border.
        // So we just layout from the firstAttachedItemPosition.
        // We do the pre layout and calculate the layout params for the real layout.
        if (firstChangedPosition >= firstAttachedItemPosition) {
            // Store the layout parameters.
            val firstAttachedItemPositionTemp = firstAttachedItemPosition
            val lastAttachedItemPositionTemp = lastAttachedItemPosition
            val spanTopTemp = Arrays.copyOf(spanTop, mSpanCount)
            val spanBottomTemp = Arrays.copyOf(spanBottom, mSpanCount)

            topBorder = paddingTop
            bottomBorder = height - paddingBottom
            spanBottom = Arrays.copyOf(spanTop, mSpanCount)
            updateSpanBottomParameters()

            detachAndScrapAttachedViews(recycler)

            mCurrentPosition = firstAttachedItemPosition
            lastAttachedItemPosition = firstAttachedItemPosition

            // Set the fake params.
            fakeSpanBottomMin = spanBottomMin
            fakeSpanBottomMax = spanBottomMax
            fakeCurrentPosition = mCurrentPosition
            fakeFirstAttachedItemPosition = firstAttachedItemPosition
            fakeFirstOneEmptyBottomSpanIndex = firstOneEmptyBottomSpanIndex
            fakeFirstTwoEmptyBottomSpanIndex = firstTwoEmptyBottomSpanIndex
            fakeSpanTop = Arrays.copyOf(spanTop, mSpanCount)
            fakeSpanBottom = Arrays.copyOf(spanBottom, mSpanCount)

            // Lay out current attached views and appearing views.
            fillGridForPreLayout(recycler, state)

            // Restore the layout parameters.
            firstAttachedItemPosition = firstAttachedItemPositionTemp
            lastAttachedItemPosition = lastAttachedItemPositionTemp
            spanTop = Arrays.copyOf(spanTopTemp, mSpanCount)
            spanBottom = Arrays.copyOf(spanBottomTemp, mSpanCount)
            updateSpanTopParameters()
            updateSpanBottomParameters()
        } else { // There are removed items out of the top border.

            // Calculate the spanTop begin with the firstChangedPosition
            // and update layout parameters.
            topBorder = paddingTop - scrollOffset
            Arrays.fill(spanTop, topBorder)
            for (i in 0 until firstChangedPosition) {
                for (j in 0 until itemLayoutWidthCache!!.get(i)) {
                    val spanIndex = itemOccupiedStartSpan!!.get(i) + j
                    spanTop!![spanIndex] += itemLayoutHeightCache!!.get(i) * sizePerSpan
                }
            }
            updateSpanTopParameters()
            bottomBorder = height - paddingBottom
            spanBottom = Arrays.copyOf(spanTop, mSpanCount)
            updateSpanBottomParameters()
            mCurrentPosition = firstChangedPosition
            // Fill from the spanTop until bottomBorder.
            // Note that we just lay out attached views and appearing views.
            // The firstAttachedItemPosition may change,
            // set it as -1 and update it during the layout
            firstAttachedItemPosition = -1
            lastAttachedItemPosition = -1

            detachAndScrapAttachedViews(recycler)

            // Set the fake params.
            fakeSpanBottomMin = spanBottomMin
            fakeSpanBottomMax = spanBottomMax
            fakeCurrentPosition = mCurrentPosition
            fakeFirstAttachedItemPosition = firstAttachedItemPosition
            fakeFirstOneEmptyBottomSpanIndex = firstOneEmptyBottomSpanIndex
            fakeFirstTwoEmptyBottomSpanIndex = firstTwoEmptyBottomSpanIndex
            fakeSpanTop = Arrays.copyOf(spanTop, mSpanCount)
            fakeSpanBottom = Arrays.copyOf(spanBottom, mSpanCount)

            // Lay out current attached views and appearing views.
            fillGridForPreLayout(recycler, state)

            // Restore the layout parameters.
            firstAttachedItemPosition = fakeFirstAttachedItemPosition
            spanTop = Arrays.copyOf(fakeSpanTop, mSpanCount)
            spanBottom = Arrays.copyOf(fakeSpanBottom, mSpanCount)
            updateSpanTopParameters()
            updateSpanBottomParameters()
        }

    }

    // Lay out disappearing views from the last one to the first one
    private fun layoutDisappearingViews(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        val iterator = disappearingViewCache!!.keys.iterator()
        while (iterator.hasNext()) {
            val position = iterator.next()
            val view = recycler!!.getViewForPosition(position)
            val params = disappearingViewCache!![position]
            addDisappearingView(view, 0)
            view.measure(params!!.widthSpec, params.heightSpec)
            layoutDecorated(view, params.left, params.top, params.right, params.bottom)
        }
    }

    /**
     * The layout process for each item.
     * @param recycler
     * @param state
     * @param isFillBottom
     */
    private fun layoutChunk(recycler: RecyclerView.Recycler?, state: RecyclerView.State,
                            isFillBottom: Boolean) {
        layoutChunk(recycler, state, isFillBottom, false)
    }

    /**
     * The layout process for each item.
     * If isPreLayout = true, we lay out attached views and appearing views.
     * Otherwise we lay out views between the topBorder and the bottomBorder.
     * @param recycler
     * @param state
     * @param isFillBottom
     * @param isPreLayout
     */
    private fun layoutChunk(recycler: RecyclerView.Recycler?, state: RecyclerView.State,
                            isFillBottom: Boolean, isPreLayout: Boolean) {

        var widthNum = 0
        var heightNum = 0
        var nextItemIndex = 0

        var fakeWidthNum = 0
        var fakeHeightNum = 0
        var fakeNextItemIndex = 0

        var view: View? = null
        var params: DisappearingViewParams? = null
        // If disappearingViewCache contains the params of the current view to be laid out,
        // get its params. This happens when too many items are removed,
        // and the fillGird() cannot fill to the bottom. Then scrollBy() is called.
        if (disappearingViewCache!!.containsKey(mCurrentPosition)) {
            params = disappearingViewCache!![mCurrentPosition]
        }
        // Get view from the recycler.
        view = recycler!!.getViewForPosition(mCurrentPosition)

        val lp = view!!.layoutParams as LayoutParams

        // Calculate the widthNum and the heightNum.
        // If the cache contains the widthNum and heightNum, get them from the cache.
        if (itemLayoutWidthCache!!.get(mCurrentPosition, 0) != 0) {
            widthNum = itemLayoutWidthCache!!.get(mCurrentPosition)
            heightNum = itemLayoutHeightCache!!.get(mCurrentPosition)
            nextItemIndex = itemOccupiedStartSpan!!.get(mCurrentPosition)
        } else {
            // Otherwise, if LayoutParams contains them, get them from the LayoutParams.
            if (lp.widthNum != 0) {
                widthNum = lp.widthNum
                heightNum = lp.heightNum
            } else {
                // Otherwise, calculate the widthNum and the heightNum
                // according to the size of the child view.
                widthNum = Math.min(2, Math.max(1, lp.width / sizePerSpan))
                heightNum = Math.min(2, Math.max(1, lp.height / sizePerSpan))
                lp.widthNum = widthNum
                lp.heightNum = heightNum
            }
            // If widthNum = 2 and there are no two sequential empty spans, just set widthNum as 1.
            if (isFillBottom && firstTwoEmptyBottomSpanIndex == -1) {
                widthNum = 1
            }
            // Store the layout widthNum and heightNum (different from the original one).
            itemLayoutWidthCache!!.put(mCurrentPosition, widthNum)
            itemLayoutHeightCache!!.put(mCurrentPosition, heightNum)
            // Calculate the index of the first occupied span.
            if (isFillBottom) {
                nextItemIndex = if (widthNum == 1)
                    firstOneEmptyBottomSpanIndex
                else
                    firstTwoEmptyBottomSpanIndex
            }
            // Store the index of the first occupied span, which is useful when scrolling up.
            itemOccupiedStartSpan!!.put(mCurrentPosition, nextItemIndex)
        }

        // Calculate fake params.
        if (isPreLayout && !lp.isItemRemoved) {
            fakeWidthNum = lp.widthNum
            fakeHeightNum = lp.heightNum
            if (fakeFirstTwoEmptyBottomSpanIndex == -1) {
                fakeWidthNum = 1
            }
            fakeNextItemIndex = if (fakeWidthNum == 1)
                fakeFirstOneEmptyBottomSpanIndex
            else
                fakeFirstTwoEmptyBottomSpanIndex
            fakeItemLayoutWidthCache!!.put(fakeCurrentPosition, fakeWidthNum)
            fakeItemLayoutHeightCache!!.put(fakeCurrentPosition, fakeHeightNum)
            fakeItemOccupiedStartSpan!!.put(fakeCurrentPosition, fakeNextItemIndex)
        }

        // Calculate the left, right, top and bottom of the view to be laid out.
        var left = 0
        var right = 0
        var top = 0
        var bottom = 0
        var fakeLeft = 0
        var fakeRight = 0
        var fakeTop = 0
        var fakeBottom = 0

        // We do not need to calculate decorations for views in the disappearingViewCache.
        if (params == null) {
            calculateItemDecorationsForChild(view, mDecorInsets)
        }
        left = paddingLeft + spanWidthBorders!![nextItemIndex] + lp.leftMargin
        right = paddingLeft + spanWidthBorders!![nextItemIndex + widthNum] - lp.rightMargin
        if (isFillBottom) {
            top = paddingTop + spanBottomMin + lp.topMargin
            bottom = paddingTop + spanBottomMin + sizePerSpan * heightNum - lp.bottomMargin
        } else {
            bottom = paddingTop + spanTop!![nextItemIndex] - lp.bottomMargin
            top = paddingTop + spanTop!![nextItemIndex] - sizePerSpan * heightNum + lp.topMargin
        }

        if (isPreLayout && !lp.isItemRemoved) {
            fakeLeft = paddingLeft + spanWidthBorders!![fakeNextItemIndex] + lp.leftMargin
            fakeRight = paddingLeft + spanWidthBorders!![fakeNextItemIndex + fakeWidthNum] - lp.rightMargin
            fakeTop = paddingTop + fakeSpanBottomMin + lp.topMargin
            fakeBottom = paddingTop + fakeSpanBottomMin + sizePerSpan * fakeHeightNum - lp.bottomMargin
        }

        // If we lay out the view to fill bottom, add the view to the end.
        if (isFillBottom) {

            if (!isPreLayout) {
                addView(view)
            } else if (bottom + lp.bottomMargin >= paddingTop || // Attached

                    firstAttachedItemPosition != -1 ||
                    fakeBottom + lp.bottomMargin >= paddingTop || // Appearing

                    fakeFirstAttachedItemPosition != -1) {
                // If it is pre-layout, we just lay out attached views and appearing views.
                if (lp.isItemRemoved) {
                    addDisappearingView(view)
                } else {
                    addView(view)
                }
            }
        } else if (!isFillBottom) { // Otherwise it is added to the beginning.
            addView(view, 0)
        }

        // Make measureSpec.
        val widthSpec: Int
        val heightSpec: Int
        var fakeWidthSpec = 0
        var fakeHeightSpec = 0
        if (params == null) {
            widthSpec = View.MeasureSpec.makeMeasureSpec(
                    right - left - mDecorInsets.left - mDecorInsets.right, View.MeasureSpec.EXACTLY)
            heightSpec = View.MeasureSpec.makeMeasureSpec(
                    bottom - top - mDecorInsets.top - mDecorInsets.bottom, View.MeasureSpec.EXACTLY)
        } else {
            // If disappearingViewCache contains the params,
            // get the widthSpec and the heightSpec from it.
            widthSpec = params.widthSpec
            heightSpec = params.heightSpec
        }

        if (isPreLayout && !lp.isItemRemoved) {
            fakeWidthSpec = View.MeasureSpec.makeMeasureSpec(
                    fakeRight - fakeLeft - mDecorInsets.left - mDecorInsets.right,
                    View.MeasureSpec.EXACTLY)
            fakeHeightSpec = View.MeasureSpec.makeMeasureSpec(
                    fakeBottom - fakeTop - mDecorInsets.top - mDecorInsets.bottom,
                    View.MeasureSpec.EXACTLY)
        }

        // Measure child.
        // If isPreLayout = true, we just measure and lay out attached views and appearing views.
        if (!isPreLayout || (isPreLayout && (bottom + lp.bottomMargin >= paddingTop || // Attached

                        firstAttachedItemPosition != -1 ||
                        fakeBottom + lp.bottomMargin >= paddingTop || // Appearing

                        fakeFirstAttachedItemPosition != -1)
                        && !lp.isItemRemoved)) {
            view.measure(widthSpec, heightSpec)
            layoutDecorated(view, left, top, right, bottom)
        }
        // If isPreLayout = true, for disappearing views, we put the params and position into cache.
        if (isPreLayout && (bottom + lp.bottomMargin >= paddingTop || // Currently visible
                        firstAttachedItemPosition != -1)
                && fakeBottom + lp.bottomMargin < paddingTop && // Invisible in real layout
                fakeFirstAttachedItemPosition == -1
                && !lp.isItemRemoved) {
            disappearingViewCache!![fakeCurrentPosition] = DisappearingViewParams(fakeWidthSpec, fakeHeightSpec,
                    fakeLeft, fakeTop, fakeRight, fakeBottom)
        }
        // For the normal layout,
        // if we lay out a disappearing view, it should be removed from the cache.
        if (!isPreLayout && params != null) {
            disappearingViewCache!!.remove(mCurrentPosition)
        }

        // update some parameters
        if (isFillBottom) {
            for (i in 0 until widthNum)
                spanBottom!![nextItemIndex + i] += sizePerSpan * heightNum
            updateSpanBottomParameters()
            if (!isPreLayout) {
                lastAttachedItemPosition = mCurrentPosition
            } else {
                // If isPreLayout = true.
                for (i in 0 until fakeWidthNum)
                    fakeSpanBottom!![fakeNextItemIndex + i] += sizePerSpan * fakeHeightNum
                updateFakeSpanBottomParameters()
                // we need to update fakeFirstAttachedItemPosition and firstAttachedItemPosition.
                if (fakeFirstAttachedItemPosition == -1 &&
                        !lp.isItemRemoved &&
                        fakeBottom + lp.bottomMargin >= paddingTop) {
                    fakeFirstAttachedItemPosition = fakeCurrentPosition
                }
                if (firstAttachedItemPosition == -1 && bottom + lp.bottomMargin >= paddingTop) {
                    firstAttachedItemPosition = mCurrentPosition
                }
            }
            mCurrentPosition++
            if (isPreLayout && !lp.isItemRemoved) {
                fakeCurrentPosition++
            }
            // Update fakeSpanTop and spanTop.
            if (isPreLayout && fakeFirstAttachedItemPosition == -1) {
                for (i in 0 until fakeWidthNum)
                    fakeSpanTop!![fakeNextItemIndex + i] += sizePerSpan * fakeHeightNum
            }
            if (isPreLayout && firstAttachedItemPosition == -1) {
                for (i in 0 until widthNum)
                    spanTop!![nextItemIndex + i] += sizePerSpan * heightNum
            }
        } else {
            for (i in 0 until widthNum)
                spanTop!![nextItemIndex + i] -= sizePerSpan * heightNum
            updateSpanTopParameters()
            firstAttachedItemPosition = mCurrentPosition
            mCurrentPosition--
        }

    }

    /**
     * Recycle views out of the top border.
     * @param recycler
     */
    private fun recycleTopInvisibleViews(recycler: RecyclerView.Recycler?) {
        val childCount = childCount
        for (i in 0..childCount) {
            val child = getChildAt(i)
            // Recycle views from here.
            if (getDecoratedEnd(child) > topBorder) {
                recycleChildren(recycler, 0, i - 1)
                firstAttachedItemPosition += i
                updateSpanTopParameters()
                return
            }
            // Update spanTop.
            val heightNum = itemLayoutHeightCache!!.get(firstAttachedItemPosition + i)
            for (j in 0 until itemLayoutWidthCache!!.get(firstAttachedItemPosition + i)) {
                val spanIndex = itemOccupiedStartSpan!!.get(firstAttachedItemPosition + i) + j
                spanTop!![spanIndex] += heightNum * sizePerSpan
            }
        }
    }

    /**
     * Recycle views out of the bottom border.
     * @param recycler
     */
    private fun recycleBottomInvisibleViews(recycler: RecyclerView.Recycler?) {
        val childCount = childCount
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            // Recycle views from here.
            if (getDecoratedStart(child) < bottomBorder) {
                recycleChildren(recycler, i + 1, childCount - 1)
                lastAttachedItemPosition -= childCount - 1 - i
                updateSpanBottomParameters()
                return
            }
            // Update spanBottom.
            val position = lastAttachedItemPosition - (childCount - 1 - i)
            val heightNum = itemLayoutHeightCache!!.get(position)
            for (j in 0 until itemLayoutWidthCache!!.get(position)) {
                val spanIndex = itemOccupiedStartSpan!!.get(position) + j
                spanBottom!![spanIndex] -= heightNum * sizePerSpan
            }
        }
    }

    /**
     * Recycle views from the endIndex to the startIndex.
     * @param startIndex inclusive
     * @param endIndex inclusive
     */
    private fun recycleChildren(recycler: RecyclerView.Recycler?, startIndex: Int, endIndex: Int) {
        if (startIndex > endIndex) {
            return
        }
        for (i in endIndex downTo startIndex) {
            removeAndRecycleViewAt(i, recycler!!)
        }
    }

    /**
     * Helper method to get the top of the view including the decoration and the margin.
     * @param view
     * @return
     */
    fun getDecoratedStart(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedTop(view) - params.topMargin
    }

    /**
     * Helper method to get the bottom of the view including the decoration and the margin.
     * @param view
     * @return
     */
    fun getDecoratedEnd(view: View): Int {
        val params = view.layoutParams as RecyclerView.LayoutParams
        return getDecoratedBottom(view) + params.bottomMargin
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams {
        return LayoutParams(c, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams {
        return if (lp is ViewGroup.MarginLayoutParams) {
            LayoutParams(lp)
        } else {
            LayoutParams(lp)
        }
    }

    override fun checkLayoutParams(lp: RecyclerView.LayoutParams?): Boolean {
        return lp is LayoutParams
    }

    class LayoutParams : RecyclerView.LayoutParams {

        //Original widthNum.
        var widthNum: Int = 0
        //Original heightNum.
        var heightNum: Int = 0

        constructor(c: Context, attrs: AttributeSet) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.MarginLayoutParams) : super(source)
        constructor(source: ViewGroup.LayoutParams) : super(source)
        constructor(source: RecyclerView.LayoutParams) : super(source)
    }

    class DisappearingViewParams internal constructor(internal var widthSpec: Int, internal var heightSpec: Int,
                                                      internal var left: Int, internal var top: Int, internal var right: Int, internal var bottom: Int)

}
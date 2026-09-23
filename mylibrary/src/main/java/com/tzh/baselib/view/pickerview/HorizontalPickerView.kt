package com.tzh.baselib.view.pickerview

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.tzh.baselib.R
import kotlin.math.abs
import kotlin.math.ceil

/** Center-snapping picker. Every public position is an index into the original data. */
class HorizontalPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    var normalTextColor = Color.GRAY
        set(value) { field = value; refreshStyles() }
    var selectedTextColor = Color.BLACK
        set(value) { field = value; refreshStyles() }
    /** Text sizes are pixels, matching XML dimension values. */
    var normalTextSize = sp(14f)
        set(value) { require(value > 0 && value.isFinite()); field = value; refreshStyles(); requestLayout() }
    var selectedTextSize = sp(18f)
        set(value) { require(value > 0 && value.isFinite()); field = value; refreshStyles(); requestLayout() }
    var itemWidth = dp(100)
        set(value) { require(value > 0); field = value; requestLayout() }
    var itemHeight = dp(48)
        set(value) { require(value > 0); field = value; requestLayout() }
    var isCircular = false
        set(value) {
            if (field == value) return
            field = value
            if (initialized) rebuild()
        }

    private var initialized = false
    private var items: List<String> = emptyList()
    private var selectedIndex = -1
    private var lastNotifiedIndex = -1
    private var dataVersion = 0
    private var restoredIndex: Int? = null
    private var selectedAdapterPosition = RecyclerView.NO_POSITION
    private var needsCompletion = false
    private var changingData = false
    private var effectiveWidth = itemWidth
    private var rowHeight = itemHeight
    private var listener: CurrentItemChangeListener? = null
    private val manager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    private val snap = LinearSnapHelper()
    private val pickerAdapter = PickerAdapter()
    private val recycler = RecyclerView(context).apply {
        layoutManager = manager
        adapter = pickerAdapter
        itemAnimator = null
        clipToPadding = false
        overScrollMode = OVER_SCROLL_NEVER
        isHorizontalScrollBarEnabled = false
        isSaveEnabled = false
    }
    private val settle = Runnable { settleSelection() }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalPickerView, defStyleAttr, 0)
        try {
            normalTextColor = a.getColor(R.styleable.HorizontalPickerView_normalTextColor, normalTextColor)
            selectedTextColor = a.getColor(R.styleable.HorizontalPickerView_selectedTextColor, selectedTextColor)
            normalTextSize = a.getDimension(R.styleable.HorizontalPickerView_normalTextSize, normalTextSize).coerceAtLeast(1f)
            selectedTextSize = a.getDimension(R.styleable.HorizontalPickerView_selectedTextSize, selectedTextSize).coerceAtLeast(1f)
            itemWidth = a.getDimensionPixelSize(R.styleable.HorizontalPickerView_itemWidth, itemWidth).coerceAtLeast(1)
            itemHeight = a.getDimensionPixelSize(R.styleable.HorizontalPickerView_itemHeight, itemHeight).coerceAtLeast(1)
            isCircular = a.getBoolean(R.styleable.HorizontalPickerView_isCircular, false)
        } finally { a.recycle() }
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        fun arrow(resource: Int) = ImageView(context).apply {
            setImageResource(resource)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        column.addView(arrow(R.drawable.icon_vector_1), LinearLayout.LayoutParams(dp(7), dp(6)))
        column.addView(recycler, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, itemHeight))
        column.addView(arrow(R.drawable.icon_vector_2), LinearLayout.LayoutParams(dp(7), dp(6)))
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        snap.attachToRecyclerView(recycler)
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                if (changingData) return
                updateSelection()
                if (view.scrollState == RecyclerView.SCROLL_STATE_IDLE) scheduleSettle()
            }
            override fun onScrollStateChanged(view: RecyclerView, state: Int) {
                if (changingData) return
                if (state == RecyclerView.SCROLL_STATE_DRAGGING) needsCompletion = true
                if (state == RecyclerView.SCROLL_STATE_IDLE) scheduleSettle()
            }
        })
        recycler.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> scheduleSettle() }
        initialized = true
    }

    /** Copies display text. Strings are shown verbatim; legacy weekday DTOs remain supported. */
    fun setData(data: List<*>?) {
        lastNotifiedIndex = -1
        items = data.orEmpty().map {
            when (it) {
                is PickerWeekDto -> it.getWeekText()
                null -> ""
                else -> it.toString()
            }
        }
        selectedIndex = if (items.isEmpty()) -1 else
            (restoredIndex ?: selectedIndex.coerceAtLeast(0)).coerceIn(items.indices)
        if (items.isNotEmpty()) restoredIndex = null
        rebuild()
    }

    /** Format arbitrary business objects without coupling the picker to their model. */
    fun <T> setData(data: List<T>, textProvider: (T) -> CharSequence) {
        setData(data.map { textProvider(it).toString() })
    }

    fun getCurrentItem(): Int = selectedIndex
    fun getSelectedItem(): String? = items.getOrNull(selectedIndex)
    fun setCurrentItemChangeListener(value: CurrentItemChangeListener?) { listener = value }

    @JvmOverloads
    fun setCurrentItem(position: Int, smoothScroll: Boolean = false) {
        if (items.isEmpty()) { restoredIndex = position.coerceAtLeast(0); return }
        dataVersion++
        changingData = true
        recycler.stopScroll()
        changingData = false
        val logical = position.coerceIn(items.indices)
        val target = if (isLooping()) {
            val current = selectedAdapterPosition.takeIf { it >= 0 } ?: middle(logical)
            val base = current - current % items.size + logical
            listOf(base.toLong(), base.toLong() - items.size, base.toLong() + items.size)
                .filter { it in 0 until pickerAdapter.itemCount.toLong() }
                .minByOrNull { kotlin.math.abs(it - current) }!!.toInt()
        } else logical
        needsCompletion = true
        if (smoothScroll && recycler.isLaidOut) recycler.smoothScrollToPosition(target)
        else {
            manager.scrollToPositionWithOffset(target, 0)
            selectedIndex = logical
            selectedAdapterPosition = target
            refreshStyles()
        }
        scheduleSettle()
    }

    /** Compatibility alias; accepts original data indices rather than repeated indices. */
    fun scrollto(position: Int) = setCurrentItem(position, true)
    fun notifyDataSetChanged() = setData(items)

    private fun isLooping() = isCircular && items.size > 1
    private fun middle(logical: Int): Int = if (isLooping())
        (Int.MAX_VALUE / 2 / items.size) * items.size + logical else logical

    private fun rebuild() {
        dataVersion++
        changingData = true
        recycler.stopScroll()
        removeCallbacks(settle)
        selectedAdapterPosition = if (selectedIndex >= 0) middle(selectedIndex) else RecyclerView.NO_POSITION
        pickerAdapter.notifyDataSetChanged()
        if (selectedIndex >= 0) manager.scrollToPositionWithOffset(selectedAdapterPosition, 0)
        needsCompletion = selectedIndex >= 0
        changingData = false
        scheduleSettle()
    }

    private fun scheduleSettle() {
        removeCallbacks(settle)
        if (isAttachedToWindow && !changingData) post(settle)
    }

    private fun updateSelection() {
        val view = snap.findSnapView(manager) ?: return
        val adapterPosition = recycler.getChildAdapterPosition(view)
        if (adapterPosition == RecyclerView.NO_POSITION || items.isEmpty()) return
        val logical = adapterPosition % items.size
        val oldAdapter = selectedAdapterPosition
        selectedAdapterPosition = adapterPosition
        val changed = logical != lastNotifiedIndex
        selectedIndex = logical
        if (oldAdapter != adapterPosition) {
            (manager.findViewByPosition(oldAdapter) as? TextView)?.let { style(it, false) }
            (view as? TextView)?.let { style(it, true) }
        }
        if (changed) {
            lastNotifiedIndex = logical
            listener?.onCurrentItemChanged(view, logical)
        }
    }

    private fun settleSelection() {
        if (changingData || recycler.isComputingLayout || recycler.isLayoutRequested
            || recycler.scrollState != RecyclerView.SCROLL_STATE_IDLE || items.isEmpty()) return
        val view = snap.findSnapView(manager) ?: return
        val distance = snap.calculateDistanceToFinalSnap(manager, view) ?: return
        if (abs(distance[0]) > 1) {
            recycler.smoothScrollBy(distance[0], 0)
            return
        }
        val version = dataVersion
        updateSelection()
        if (version != dataVersion) return
        if (needsCompletion) {
            needsCompletion = false
            listener?.onScrollChangedFinish(view, selectedIndex)
            if (version != dataVersion) return
        }
        // Recenter the virtual range only when settled, without changing the logical selection.
        if (isLooping() && (selectedAdapterPosition < 10000 || selectedAdapterPosition > Int.MAX_VALUE - 10000)) {
            selectedAdapterPosition = middle(selectedIndex)
            manager.scrollToPositionWithOffset(selectedAdapterPosition, 0)
        }
    }

    private fun style(view: TextView, selected: Boolean) {
        view.isSelected = selected
        view.setTextColor(if (selected) selectedTextColor else normalTextColor)
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, if (selected) selectedTextSize else normalTextSize)
    }

    private fun refreshStyles() {
        if (!initialized) return
        for (i in 0 until recycler.childCount) {
            val view = recycler.getChildAt(i) as? TextView ?: continue
            style(view, recycler.getChildAdapterPosition(view) == selectedAdapterPosition)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val paint = Paint().apply { textSize = maxOf(normalTextSize, selectedTextSize) }
        val metrics = paint.fontMetrics
        val desiredHeight = maxOf(itemHeight, ceil(metrics.bottom - metrics.top).toInt() + dp(12))
        val available = (MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight).coerceAtLeast(1)
        val desiredWidth = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) itemWidth else minOf(itemWidth, available)
        if (rowHeight != desiredHeight || effectiveWidth != desiredWidth) {
            rowHeight = desiredHeight
            effectiveWidth = desiredWidth
            recycler.layoutParams.height = rowHeight
            pickerAdapter.notifyDataSetChanged()
        }
        val space = ((available - effectiveWidth) / 2).coerceAtLeast(0)
        if (recycler.paddingLeft != space || recycler.paddingRight != space) {
            recycler.setPadding(space, 0, space, 0)
            if (selectedIndex >= 0) manager.scrollToPositionWithOffset(middle(selectedIndex), 0)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("super", super.onSaveInstanceState())
        putInt("selection", restoredIndex ?: selectedIndex)
        putBoolean("circular", isCircular)
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) { super.onRestoreInstanceState(state); return }
        @Suppress("DEPRECATION")
        super.onRestoreInstanceState(state.getParcelable("super"))
        restoredIndex = state.getInt("selection", 0).coerceAtLeast(0)
        isCircular = state.getBoolean("circular", isCircular)
        if (items.isNotEmpty()) setData(items)
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); scheduleSettle() }
    override fun onDetachedFromWindow() {
        changingData = true
        recycler.stopScroll()
        removeCallbacks(settle)
        changingData = false
        super.onDetachedFromWindow()
    }

    private inner class PickerAdapter : RecyclerView.Adapter<Holder>() {
        override fun getItemCount(): Int = if (isLooping()) Int.MAX_VALUE else items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(TextView(context).apply {
            gravity = Gravity.CENTER
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(4), 0, dp(4), 0)
        })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.text.layoutParams = RecyclerView.LayoutParams(effectiveWidth, rowHeight)
            holder.text.text = items[position % items.size]
            style(holder.text, position == selectedAdapterPosition)
            holder.text.setOnClickListener {
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) setCurrentItem(current % items.size, true)
            }
        }
    }
    private class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    interface CurrentItemChangeListener {
        fun onCurrentItemChanged(view: View?, position: Int)
        fun onScrollChangedFinish(view: View?, position: Int)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}

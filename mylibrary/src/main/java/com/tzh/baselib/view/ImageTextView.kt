package com.tzh.baselib.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import com.tzh.baselib.R
import com.tzh.baselib.shapeview.ShapeLinearLayout
import com.tzh.baselib.util.DpToUtil

/** 图片和文字组合控件，保留原有 XML 属性和资源 ID 设置接口。 */
class ImageTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ShapeLinearLayout(context, attrs, defStyleAttr) {
    companion object {
        const val TO_TEXT_TOP = 0
        const val TO_TEXT_BOTTOM = 1
        const val TO_TEXT_LEFT = 2
        const val TO_TEXT_RIGHT = 3
    }

    val mImageView by lazy { AppCompatImageView(context) }
    val mTextview by lazy {
        TextView(context).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
    }

    private var imageSelected: Drawable? = null
    private var imageDefault: Drawable? = null
    private var tintSelected: ColorStateList? = null
    private var tintDefault: ColorStateList? = null
    private var textSelected: ColorStateList? = null
    private var textDefault = ColorStateList.valueOf(android.graphics.Color.BLACK)
    private var spacePx = 0
    private var imageWidth = 0
    private var imageHeight = 0
    private var showLocal = -1
    private var autoToggle = true
    private var initialized = false
    private var appliedTint: Int? = null

    var onSelectChangeListener: OnSelectChangeListener? = null

    init {
        val values = context.obtainStyledAttributes(attrs, R.styleable.ImageTextView, defStyleAttr, 0)
        var location = TO_TEXT_RIGHT
        try {
            imageWidth = values.getDimensionPixelSize(R.styleable.ImageTextView_itvImgWidth, DpToUtil.dip2px(context, 24f))
            imageHeight = values.getDimensionPixelSize(R.styleable.ImageTextView_itvImgHeight, DpToUtil.dip2px(context, 24f))
            spacePx = values.getDimensionPixelSize(R.styleable.ImageTextView_itvSpace, DpToUtil.dip2px(context, 5f))
            imageDefault = values.getDrawable(R.styleable.ImageTextView_itvImgUnSelectSrc)
            imageSelected = values.getDrawable(R.styleable.ImageTextView_itvImgSelectSrc)
            tintDefault = values.getColorStateList(R.styleable.ImageTextView_itvImgUnSelectColor)
            tintSelected = values.getColorStateList(R.styleable.ImageTextView_itvImgSelectColor)
            textDefault = values.getColorStateList(R.styleable.ImageTextView_itvTextUnSelectColor)
                ?: AppCompatResources.getColorStateList(context, R.color.color_000)
            textSelected = values.getColorStateList(R.styleable.ImageTextView_itvTextSelectColor)
            mTextview.text = values.getText(R.styleable.ImageTextView_itvText)
            mTextview.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                values.getDimension(R.styleable.ImageTextView_itvTextSize,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)))
            mTextview.paint.isFakeBoldText = values.getBoolean(R.styleable.ImageTextView_itvTextIsBold, false)
            autoToggle = values.getBoolean(R.styleable.ImageTextView_itvIsClick, true)
            location = values.getInt(R.styleable.ImageTextView_itvShowLocal, TO_TEXT_RIGHT)
            gravity = when (values.getInt(R.styleable.ImageTextView_itvGravity, 4)) {
                0 -> Gravity.START or Gravity.CENTER_VERTICAL
                1 -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                2 -> Gravity.END or Gravity.CENTER_VERTICAL
                3 -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                else -> Gravity.CENTER
            }
        } finally {
            values.recycle()
        }
        mImageView.isDuplicateParentStateEnabled = true
        mTextview.isDuplicateParentStateEnabled = true
        isClickable = autoToggle || isClickable
        setShowLocal(location)
        initialized = true
        updateAppearance()
    }

    /** 相同方向不重复布局；非法方向在修改子 View 前抛出异常。左右跟随布局方向。 */
    fun setShowLocal(viewLocal: Int) {
        require(viewLocal in TO_TEXT_TOP..TO_TEXT_RIGHT) { "Unknown image location: $viewLocal" }
        if (showLocal == viewLocal) return
        orientation = if (viewLocal <= TO_TEXT_BOTTOM) VERTICAL else HORIZONTAL
        val imageFirst = viewLocal == TO_TEXT_TOP || viewLocal == TO_TEXT_LEFT
        if (mTextview.parent == null) {
            addView(mTextview, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }
        val imageIndex = if (imageFirst) 0 else 1
        if (indexOfChild(mImageView) != imageIndex) {
            if (mImageView.parent === this) removeView(mImageView)
            addView(mImageView, imageIndex, LayoutParams(imageWidth, imageHeight))
        }
        val params = mTextview.layoutParams as LayoutParams
        params.setMargins(0, 0, 0, 0)
        params.marginStart = 0
        params.marginEnd = 0
        when (viewLocal) {
            TO_TEXT_TOP -> params.topMargin = spacePx
            TO_TEXT_BOTTOM -> params.bottomMargin = spacePx
            TO_TEXT_LEFT -> params.marginStart = spacePx
            TO_TEXT_RIGHT -> params.marginEnd = spacePx
        }
        mTextview.layoutParams = params
        showLocal = viewLocal
    }

    /** 返回 true 的监听器可阻止变化；重复设置同一状态不会触发回调。 */
    fun setSelected(selected: Boolean, isTrigger: Boolean = true) {
        if (isSelected == selected) return
        if (isTrigger && onSelectChangeListener?.onSelect(selected) == true) return
        super.setSelected(selected)
        if (initialized) updateAppearance()
    }

    override fun setSelected(selected: Boolean) {
        setSelected(selected, true)
    }

    /** 自动切换先执行，外部点击监听随后读取最终状态（包括被拦截的状态）。 */
    override fun performClick(): Boolean {
        if (!isEnabled) return false
        if (autoToggle) isSelected = !isSelected
        val handled = super.performClick()
        return handled || autoToggle
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        // View 的构造过程也可能调用此方法。
        if (initialized) updateAppearance()
    }

    private fun updateAppearance() {
        val colors = if (isSelected) textSelected ?: textDefault else textDefault
        val color = colors.getColorForState(drawableState, colors.defaultColor)
        if (mTextview.currentTextColor != color) mTextview.setTextColor(color)
        val drawable = if (isSelected) imageSelected ?: imageDefault else imageDefault
        if (mImageView.drawable !== drawable) mImageView.setImageDrawable(drawable)
        val tint = if (isSelected) tintSelected ?: tintDefault else tintDefault
        val tintColor = tint?.getColorForState(drawableState, tint.defaultColor)
        if (appliedTint != tintColor) {
            mImageView.imageTintList = tintColor?.let(ColorStateList::valueOf)
            appliedTint = tintColor
        }
    }

    fun setText(str: String?) {
        mTextview.text = str.orEmpty()
    }

    /** 更新默认图片；显式配置的选中图片继续生效。0 表示清空默认图片。 */
    fun setImage(@DrawableRes img: Int) {
        imageDefault = if (img == 0) null else AppCompatResources.getDrawable(context, img)
        updateAppearance()
    }

    /** 兼容旧接口：参数仍然是颜色资源 ID。 */
    fun setTextColor(@ColorRes color: Int) = setTextColorRes(color)

    fun setTextColorRes(@ColorRes color: Int) {
        textDefault = AppCompatResources.getColorStateList(context, color)
        updateAppearance()
    }

    fun setTextColorInt(@ColorInt color: Int) {
        textDefault = ColorStateList.valueOf(color)
        updateAppearance()
    }

    /** selected 为 null 时，选中状态使用默认颜色（包括默认 selector）。 */
    @JvmOverloads
    fun setTextColors(default: ColorStateList, selected: ColorStateList? = null) {
        textDefault = default
        textSelected = selected
        updateAppearance()
    }

    interface OnSelectChangeListener {
        fun onSelect(selected: Boolean): Boolean
    }
}
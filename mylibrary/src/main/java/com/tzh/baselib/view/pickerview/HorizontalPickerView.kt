package com.tzh.baselib.view.pickerview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.tzh.baselib.R
import com.tzh.baselib.databinding.LayoutHorizontalPickerViewBinding
import com.tzh.baselib.util.LogUtils
import com.tzh.baselib.util.bindingInflateLayout

class HorizontalPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 自定义属性
    private var normalTextColor = Color.GRAY
    private var selectedTextColor = Color.BLACK
    private var normalTextSize = 14f
    private var selectedTextSize = 18f

    var binding: LayoutHorizontalPickerViewBinding? = null

    init {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        // 解析自定义属性
        context.obtainStyledAttributes(attrs, R.styleable.HorizontalPickerView).apply {
            normalTextColor = getColor(
                R.styleable.HorizontalPickerView_normalTextColor,
                Color.GRAY
            )
            selectedTextColor = getColor(
                R.styleable.HorizontalPickerView_selectedTextColor,
                Color.BLACK
            )
            normalTextSize = getDimension(
                R.styleable.HorizontalPickerView_normalTextSize,
                14f
            )
            selectedTextSize = getDimension(
                R.styleable.HorizontalPickerView_selectedTextSize,
                18f
            )
            val isCircular = getBoolean(
                R.styleable.HorizontalPickerView_isCircular,
                false
            )
            recycle()
        }

        binding = bindingInflateLayout<LayoutHorizontalPickerViewBinding>(R.layout.layout_horizontal_picker_view).apply {
            pickView.setData(
                mutableListOf<PickerWeekDto>().apply {
                    add(PickerWeekDto(1,1))
                    add(PickerWeekDto(2,1))
                    add(PickerWeekDto(3,1))
                    add(PickerWeekDto(4,1))
                    add(PickerWeekDto(5,1))
                    add(PickerWeekDto(6,1))
                    add(PickerWeekDto(0,1))
                    add(PickerWeekDto(7,1))
                }
            )

            pickView.setCurrentItemChangeListener(object : Horizontal2PickerView.CurrentItemChangeListener{
                override fun onCurrentItemChanged(view: View?, position: Int) {

                }

                override fun onScrollChangedFinish(view: View?, position: Int) {
                    LogUtils.e("=======",position.toString())
                }
            })
        }

    }
}
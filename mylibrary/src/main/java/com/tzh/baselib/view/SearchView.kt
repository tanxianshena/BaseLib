package com.tzh.baselib.view

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.tzh.baselib.R
import com.tzh.baselib.databinding.LayoutSearchViewBinding
import com.tzh.baselib.util.DpToUtil
import com.tzh.baselib.util.KeyBoardUtils
import com.tzh.baselib.util.bindingInflateLayout

/** 搜索框：提交时校验，search 返回 true 时关闭键盘。 */
class SearchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    // Binding 和子 View 的生命周期一致，不允许外部替换或清空。
    val binding: LayoutSearchViewBinding = bindingInflateLayout(R.layout.layout_search_view)
    var mListener: SvSearchListener? = null
    private var maxInputLength = 50
    private var inputLengthHint = ""
    private var emptyInputHint = ""
    private var hadText = false
    private var keyboardRequested = false
    private val showKeyboard = Runnable {
        if (keyboardRequested && isAttachedToWindow && hasWindowFocus() && isShown && isEnabled) {
            if (binding.etText.requestFocus()) {
                keyboardRequested = false
                KeyBoardUtils.openKeyboard(binding.etText, context)
            }
        }
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.SearchView, defStyleAttr, 0) {
            binding.etText.setTextColor(getColorStateList(R.styleable.SearchView_sv_text_color)
                ?: ContextCompat.getColorStateList(context, R.color.color_333))
            binding.etText.setHintTextColor(getColorStateList(R.styleable.SearchView_sv_hint_color)
                ?: ContextCompat.getColorStateList(context, R.color.color_bbbbbb))
            binding.layout.setShapeBackgroundColor(getColor(R.styleable.SearchView_sv_back_color,
                ContextCompat.getColor(context, R.color.color_f7f7f7)))
            // getDimension 返回 px，直接设置 Drawable，避免再次按 dp 换算。
            binding.layout.xBaseShape.gradientDrawable.cornerRadius =
                getDimension(R.styleable.SearchView_sv_corners, dp(99).toFloat())
            binding.etText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getDimension(R.styleable.SearchView_sv_text_size,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics)))
            binding.etText.hint = getText(R.styleable.SearchView_sv_hint)
                ?: context.getString(R.string.baselib_search_hint)
            maxInputLength = getInt(R.styleable.SearchView_sv_input_max_length, 50)
            inputLengthHint = getString(R.styleable.SearchView_sv_input_hint_text)
                ?: context.getString(R.string.baselib_search_too_long, maxInputLength)
            emptyInputHint = getString(R.styleable.SearchView_sv_no_input_hint_text).orEmpty()
            val imageSize = getDimensionPixelSize(R.styleable.SearchView_sv_image_width, dp(14)).coerceAtLeast(0)
            binding.ivSearch.layoutParams = binding.ivSearch.layoutParams.apply {
                width = imageSize
                height = imageSize
            }
            // 视觉图标大小与点击区域分离，清空按钮至少 48dp。
            val targetSize = maxOf(dp(48), imageSize + dp(10))
            binding.ivClear.layoutParams = binding.ivClear.layoutParams.apply {
                width = targetSize
                height = targetSize
            }
            val padding = (targetSize - imageSize) / 2
            binding.ivClear.setPadding(padding, padding, targetSize - imageSize - padding, targetSize - imageSize - padding)
            binding.etText.layoutParams = (binding.etText.layoutParams as LayoutParams).apply {
                marginStart = imageSize + dp(18)
                marginEnd = targetSize + dp(8)
            }
            minimumHeight = maxOf(minimumHeight, targetSize)
        }
        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                val becameEmpty = hadText && !hasText
                hadText = hasText
                binding.ivClear.visibility = if (hasText) VISIBLE else GONE
                if (becameEmpty) mListener?.clear()
            }
        })
        binding.ivClear.setOnClickListener { setText("") }
        binding.etText.setOnEditorActionListener { _, actionId, event ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER || event?.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            when {
                enter -> {
                    // 只在释放按键时提交一次，消费按下和长按重复事件。
                    if (event?.action == KeyEvent.ACTION_UP && !event.isCanceled) submitSearch()
                    true
                }
                actionId == EditorInfo.IME_ACTION_SEARCH -> {
                    submitSearch()
                    true
                }
                else -> false
            }
        }
    }

    private fun dp(value: Int) = DpToUtil.dip2px(context, value.toFloat())

    /** 校验和提交共用入口。保留空搜索开关，不裁剪调用方输入。 */
    fun submitSearch() {
        val query = getText()
        when {
            emptyInputHint.isNotEmpty() && query.isEmpty() ->
                Toast.makeText(context, emptyInputHint, Toast.LENGTH_LONG).show()
            maxInputLength > 0 && query.length > maxInputLength ->
                Toast.makeText(context, inputLengthHint, Toast.LENGTH_LONG).show()
            else -> {
                if (mListener?.search(query) != false) {
                    cancelKeyboardRequest()
                    KeyBoardUtils.closeKeyboard(binding.etText, context)
                }
            }
        }
    }

    fun setText(text: String) {
        if (getText() == text) return
        binding.etText.setText(text)
        binding.etText.setSelection(binding.etText.length())
    }

    fun setHintText(text: String) {
        binding.etText.hint = text
    }

    fun getHintText(): String = binding.etText.hint?.toString().orEmpty()
    fun getText(): String = binding.etText.text?.toString().orEmpty()

    /** 保留旧方法名；等待附着和窗口焦点后显示，不切换键盘状态。 */
    fun showKeyBord() {
        keyboardRequested = true
        scheduleKeyboard()
    }

    private fun scheduleKeyboard() {
        if (keyboardRequested && isAttachedToWindow && hasWindowFocus()) {
            removeCallbacks(showKeyboard)
            post(showKeyboard)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scheduleKeyboard()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) scheduleKeyboard()
    }

    override fun onDetachedFromWindow() {
        cancelKeyboardRequest()
        super.onDetachedFromWindow()
    }

    private fun cancelKeyboardRequest() {
        keyboardRequested = false
        removeCallbacks(showKeyboard)
    }

    fun setSvSearchListener(listener: SvSearchListener) {
        mListener = listener
    }

    interface SvSearchListener {
        /** true 关闭软键盘，false 保持键盘。 */
        fun search(text: String): Boolean
        /** 仅在文本由非空变为空时调用。 */
        fun clear()
    }
}
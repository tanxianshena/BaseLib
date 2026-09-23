package com.tzh.baselib.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.tzh.baselib.R
import com.tzh.baselib.util.dpToPx

abstract class BaseBindingDialog<B : ViewDataBinding> @JvmOverloads constructor(
    context: Context,
    @LayoutRes private val LayoutId: Int,
    @StyleRes themeResId: Int = R.style.dialog_float_translucent
) : Dialog(context, themeResId) {
    companion object {
        /**
         * 内置动画 透明浮现动画
         */
        val ANIM_TRANSLUCENT = android.R.style.Animation_Translucent

        /**
         * 内置动画——》底部出现动画
         */
        val ANIM_BOTTOM = R.style.BottomAnimation
    }

    /**
     * 顶部view 高度
     */
    protected var topViewHeight = 120f

    /**
     * dialog 宽
     */
    private var explicitWindowWidth: Int? = null
    var windowWidth: Int
        get() = explicitWindowWidth ?: (availableWindowWidth() * 0.85f).toInt()
        set(value) {
            explicitWindowWidth = value
        }

    /** Restore the automatic 85% width after assigning a custom width. */
    fun resetWindowWidth() {
        explicitWindowWidth = null
        applyWindowConfiguration()
    }

    /**
     * dialog 高
     */
    var windowHeight = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * dialog 位置
     */
    var windowGravity = Gravity.CENTER

    /**
     * dialog 动画
     */
    var windowAnim: Int = ANIM_TRANSLUCENT

    /**
     * 是否需要输入法
     */
    var isNeedInput: Boolean = false

    /**
     * 点击外部关闭弹框
     */
    var isCanceledOnTouchOutsideDialog = true

    /**
     * 返回键关闭弹框
     */
    var isCancelableDialog = true

    /**
     * 高度是否占满屏幕
     */
    var isMatchHeight = false

    /**
     * 从底部出现的dialog 的配置
     */
    @JvmOverloads
    fun initBottomDialog(matchHeight: Boolean = false) {
        bottomMode = true
        isMatchHeight = matchHeight
        windowWidth = ViewGroup.LayoutParams.MATCH_PARENT
        windowHeight =
            if (matchHeight) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        windowGravity = Gravity.BOTTOM
        windowAnim = ANIM_BOTTOM
    }

    /** Bottom-mode downward dismissal, independent of outside touch; disabled by default.
     * Requires isCancelableDialog. Set before or after show().
     */
    var isDragDismissEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) dragContainer?.reset()
        }

    private var dragContainer: BottomDragLayout? = null
    private var bottomMode = false

    private class BindingOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }

    private var bindingOwner: BindingOwner? = null
    private var cachedBinding: B? = null

    /** Lazy to preserve show(content) methods that populate views before show(). */
    protected var binding: B
        get() = cachedBinding ?: DataBindingUtil.inflate<B>(layoutInflater, LayoutId, null, false)
            .also {
                cachedBinding = it
                it.lifecycleOwner = bindingOwner
            }
        set(value) {
            check(cachedBinding == null) { "Cannot replace an initialized dialog binding" }
            cachedBinding = value
            value.lifecycleOwner = bindingOwner
        }

    private val resizeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        if (explicitWindowWidth == null) {
            window?.let {
                if (it.attributes.width != windowWidth) it.setLayout(
                    windowWidth,
                    windowHeight
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        applyWindowConfiguration()
        val owner = BindingOwner()
        bindingOwner = owner
        owner.registry.currentState = Lifecycle.State.CREATED
        binding.lifecycleOwner = owner
        owner.registry.currentState = Lifecycle.State.RESUMED
        binding.root.addOnLayoutChangeListener(resizeListener)
        binding.executePendingBindings()
    }

    override fun onStop() {
        dragContainer?.reset()
        cachedBinding?.root?.removeOnLayoutChangeListener(resizeListener)
        bindingOwner?.registry?.currentState = Lifecycle.State.DESTROYED
        cachedBinding?.lifecycleOwner = null
        bindingOwner = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        super.onCreate(savedInstanceState)

        //如果从底部出来的 dialog，需要至少距离顶部 120的高度
        if (bottomMode || windowAnim == ANIM_BOTTOM) {
            val draggableContent = BottomDragLayout(context, object : BottomDragLayout.Callback {
                override fun canDrag() = isDragDismissEnabled && isCancelableDialog
                override fun onDismiss() {
                    cancel()
                }
            }).also {
                it.addView(
                    binding.root, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, windowHeight
                    )
                )
                dragContainer = it
            }
            setContentView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    if (isMatchHeight) LinearLayout.LayoutParams.MATCH_PARENT else LinearLayout.LayoutParams.WRAP_CONTENT
                )
                addView(View(context).also {
                    it.setOnClickListener {
                        if (isCanceledOnTouchOutsideDialog) {
                            dismiss()
                        }
                    }
                }, ViewGroup.LayoutParams.MATCH_PARENT, context.dpToPx(topViewHeight))
                if (isNeedInput) {
                    addView(draggableContent, windowWidth, LinearLayout.LayoutParams.MATCH_PARENT)
                } else {
                    addView(draggableContent, windowWidth, windowHeight)
                }
            })
        } else {
            setContentView(binding.root)
        }

        // Let initialization failures propagate: Dialog.show() must not display partial UI.
        init()
    }

    private fun applyWindowConfiguration() {
        setCanceledOnTouchOutside(isCanceledOnTouchOutsideDialog)
        setCancelable(isCancelableDialog)
        window?.run {
            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            )
            if (isNeedInput) clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setLayout(windowWidth, windowHeight)
            setGravity(windowGravity)
            setWindowAnimations(windowAnim)
        }
    }

    open fun init() {
        initView()
        initData()
    }

    protected abstract fun initView()

    protected abstract fun initData()

    private fun availableWindowWidth(): Int {
        var host: Context = context
        while (host is ContextWrapper && host !is Activity) {
            val next = host.baseContext
            if (next === host) break
            host = next
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val manager = host.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = manager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout()
            )
            return (metrics.bounds.width() - insets.left - insets.right).coerceAtLeast(1)
        }
        val resources = host.resources
        return (resources.configuration.screenWidthDp * resources.displayMetrics.density)
            .toInt().coerceAtLeast(1)
    }
}

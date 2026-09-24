package com.tzh.baselib.activity

import android.app.Activity
import android.app.ActivityOptions
import android.app.SharedElementCallback
import android.graphics.drawable.Drawable
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition as GlideTransition
import java.util.UUID
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.tzh.baselib.R
import com.tzh.baselib.adapter.BannerImageAdapter
import com.tzh.baselib.base.XBaseBindingActivity
import com.tzh.baselib.databinding.ActivityPhotoViewBinding
import com.tzh.baselib.util.OnPermissionCallBackListener
import com.tzh.baselib.util.PermissionXUtil
import com.tzh.baselib.util.setOnClickNoDouble
import com.youth.banner.listener.OnPageChangeListener
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class PhotoViewActivity :
    XBaseBindingActivity<ActivityPhotoViewBinding>(R.layout.activity_photo_view) {
    companion object {
        private const val IMAGES = "imgUrlList"
        private const val POSITION = "imgPosition"
        private const val SHARED_IMAGE = "photo_shared_image"
        private const val SAVED_POSITION = "photo_current_position"

        /** The source Activity theme must enable android:windowActivityTransitions.
         * Keep the source view attached and its transitionName stable until return.
         */
        @JvmStatic
        fun start(activity: AppCompatActivity, sourceView: ImageView, url: String) =
            start(activity, sourceView, mutableListOf(url), 0)

        @JvmStatic
        @JvmOverloads
        fun start(activity: AppCompatActivity, sourceView: ImageView, imageList: MutableList<String>, position: Int = 0) {
            if (activity.isFinishing || activity.isDestroyed) return
            if (imageList.isEmpty() || !sourceView.isAttachedToWindow || !sourceView.isShown
                || sourceView.width == 0 || sourceView.height == 0 || sourceView.drawable == null
                || !activity.window.hasFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)) {
                start(activity, imageList, position)
                return
            }
            val name = sourceView.transitionName ?: "baselib_photo_${UUID.randomUUID()}".also {
                sourceView.transitionName = it
            }
            val intent = Intent(activity, PhotoViewActivity::class.java).apply {
                putStringArrayListExtra(IMAGES, ArrayList(imageList))
                putExtra(POSITION, position.coerceIn(imageList.indices))
                putExtra(SHARED_IMAGE, name)
            }
            activity.startActivity(intent,
                ActivityOptions.makeSceneTransitionAnimation(activity, sourceView, name).toBundle())
        }
        @JvmStatic
        fun start(context: Context, url: String) = start(context, arrayListOf(url), 0)

        @JvmStatic
        @JvmOverloads
        fun start(context: Context, imageList: MutableList<String>, position: Int? = null) {
            var host = context
            while (host is ContextWrapper && host !is Activity) {
                val next = host.baseContext
                if (host === next) break
                host = next
            }
            context.startActivity(Intent(context, PhotoViewActivity::class.java).apply {
                putStringArrayListExtra(IMAGES, ArrayList(imageList))
                putExtra(POSITION, position ?: 0)
                if (host !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    private var currentPosition = 0
    private var restoredPosition: Int? = null
    private var saving = false
    private var stopped = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var saveTask: Future<*>? = null

    @Volatile
    private var downloadTarget: FutureTarget<File>? = null

    val mList: List<String> by lazy { intent.getStringArrayListExtra(IMAGES)?.toList().orEmpty() }
    val mAdapter by lazy { BannerImageAdapter(mList) { closeFromImageTap() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.BaseLibPhotoPreviewTheme)
        restoredPosition = savedInstanceState?.getInt(SAVED_POSITION)
        if (intent.hasExtra(SHARED_IMAGE)) {
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            configureTransitions()
            postponeEnterTransition()
            mainHandler.postDelayed(enterTimeout, 2500)
        }
        super.onCreate(savedInstanceState)
        applyImmersiveMode()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { closePreview() }
        })
    }

    override fun initView() {
        if (mList.isEmpty()) {
            binding.tvNum.text = "0/0"
            binding.ivSaveImagePhoto.isEnabled = false
            Toast.makeText(this, "没有可预览的图片", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val initial = (restoredPosition ?: intent.getIntExtra(POSITION, 0)).coerceIn(mList.indices)
        currentPosition = initial
        binding.banner.setAdapter(mAdapter)
        binding.banner.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) = Unit

            override fun onPageScrollStateChanged(state: Int) {
                pagerMoving = state != 0
            }
            override fun onPageSelected(position: Int) {
                if (position in mList.indices) {
                    currentPosition = position
                    updatePosition()
                }
            }
        })
        binding.banner.setCurrentItem(initial, false)
        currentPosition = initial
        updatePosition()
        binding.ivSaveImagePhoto.setOnClickNoDouble { download(mList.getOrNull(currentPosition)) }
        if (intent.hasExtra(SHARED_IMAGE)) prepareSharedImage()
    }

    private var pagerMoving = false
    private var dispatchingTouch = false
    private var gestureMoved = false
    private var downX = 0f
    private var downY = 0f
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop.toFloat() }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                gestureMoved = false
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> gestureMoved = true
        }
        if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
            fun moved(x: Float, y: Float) =
                kotlin.math.abs(x - downX) > touchSlop || kotlin.math.abs(y - downY) > touchSlop
            if (moved(event.x, event.y)) gestureMoved = true
            for (i in 0 until event.historySize) {
                if (moved(event.getHistoricalX(i), event.getHistoricalY(i))) gestureMoved = true
            }
            if (event.eventTime - event.downTime > ViewConfiguration.getLongPressTimeout()) gestureMoved = true
        }
        dispatchingTouch = true
        return try { super.dispatchTouchEvent(event) } finally { dispatchingTouch = false }
    }

    private fun closeFromImageTap() {
        if (pagerMoving || (dispatchingTouch && gestureMoved)) return
        closePreview()
    }

    private fun applyImmersiveMode() {
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !closing) applyImmersiveMode()
    }
    private var sharedImage: ImageView? = null
    private var sharedReady = false
    private var entering = true
    private var closing = false
    private var returnStarted = false
    private val enterTimeout = Runnable { releaseEnter(false) }
    private val enterCleanup = Runnable { finishEntering() }

    private fun imageTransition() = TransitionSet().apply {
        ordering = TransitionSet.ORDERING_TOGETHER
        addTransition(ChangeBounds())
        addTransition(ChangeTransform())
        addTransition(ChangeImageTransform())
        duration = 280
    }

    private fun configureTransitions() {
        window.sharedElementEnterTransition = imageTransition().addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition) { finishEntering() }
            override fun onTransitionCancel(transition: Transition) { finishEntering() }
            override fun onTransitionStart(transition: Transition) = Unit
            override fun onTransitionPause(transition: Transition) = Unit
            override fun onTransitionResume(transition: Transition) = Unit
        })
        window.sharedElementReturnTransition = imageTransition()
        window.enterTransition = Fade().apply { duration = 180 }
        window.returnTransition = Fade().apply { duration = 180 }
        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                val name = intent.getStringExtra(SHARED_IMAGE) ?: return
                val image = sharedImage
                sharedElements.clear()
                if (sharedReady && image != null && image.isAttachedToWindow && image.isShown) {
                    sharedElements[name] = image
                } else {
                    names.clear()
                }
            }
        })
    }

    private fun prepareSharedImage() {
        // Dedicated overlay keeps one stable transition target as Banner recycles pages.
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            transitionName = intent.getStringExtra(SHARED_IMAGE)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            // Transition decoration must never consume the pager's swipe gestures.
            isClickable = false
            isFocusable = false
        }
        sharedImage = image
        (binding.banner.parent as ViewGroup).addView(image, 1,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val origin = intent.getIntExtra(POSITION, 0).coerceIn(mList.indices)
        Glide.with(this).load(mList[origin]).dontAnimate().into(object : DrawableImageViewTarget(image) {
            override fun onResourceReady(resource: Drawable, transition: GlideTransition<in Drawable>?) {
                super.onResourceReady(resource, transition)
                image.doOnPreDraw { releaseEnter(true) }
            }
            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                image.doOnPreDraw { releaseEnter(false) }
            }
        })
    }

    private fun releaseEnter(ready: Boolean) {
        if (stopped || isFinishing || isDestroyed) return
        sharedReady = ready
        if (!entering) return
        mainHandler.removeCallbacks(enterTimeout)
        if (!ready) sharedImage?.visibility = View.INVISIBLE
        startPostponedEnterTransition()
        // Missing source views or disabled system animations may skip transition callbacks.
        mainHandler.removeCallbacks(enterCleanup)
        mainHandler.postDelayed(enterCleanup, 700)
    }

    private fun finishEntering() {
        if (!entering) return
        entering = false
        mainHandler.removeCallbacks(enterCleanup)
        sharedImage?.visibility = View.INVISIBLE
        if (closing) closeWithTransition()
    }

    private fun closePreview() {
        if (closing || isFinishing || isDestroyed) return
        closing = true
        if (!intent.hasExtra(SHARED_IMAGE)) {
            finish()
        } else if (entering) {
            releaseEnter(sharedReady)
        } else {
            closeWithTransition()
        }
    }

    private fun closeWithTransition() {
        if (returnStarted || isFinishing || isDestroyed) return
        returnStarted = true
        val image = sharedImage
        if (sharedReady && image != null) {
            // This first version always shrinks back to the originally clicked image.
            binding.banner.visibility = View.INVISIBLE
            image.visibility = View.VISIBLE
            image.doOnPreDraw { finishAfterTransition() }
        } else {
            finishAfterTransition()
        }
    }
    private fun updatePosition() {
        binding.tvNum.text = "${currentPosition + 1}/${mList.size}"
    }

    override fun initData() = Unit

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SAVED_POSITION, currentPosition)
        super.onSaveInstanceState(outState)
    }

    /** Saves this exact source, even if the user changes pages during permission approval. */
    fun download(url: String?) {
        check(Looper.myLooper() == Looper.getMainLooper()) { "download must be called on the main thread" }
        if (saving || stopped || isFinishing || isDestroyed) return
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "图片地址无效", Toast.LENGTH_SHORT).show()
            return
        }
        saving = true
        binding.ivSaveImagePhoto.isEnabled = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveSource(url)
        } else {
            try {
                PermissionXUtil.requestStoragePermission(
                    this,
                    object : OnPermissionCallBackListener {
                        override fun onAgree() {
                            saveSource(url)
                        }

                        override fun onDisAgree() {
                            completeSave("未授予保存图片权限")
                        }
                    })
            } catch (error: Exception) {
                completeSave("无法申请保存图片权限")
            }
        }
    }

    private fun saveSource(url: String) {
        if (stopped || isFinishing || isDestroyed) return
        val app = applicationContext
        saveTask = worker.submit {
            val requests = Glide.with(app)
            var target: FutureTarget<File>? = null
            try {
                if (Thread.currentThread().isInterrupted) return@submit
                val pending = requests.downloadOnly().load(url).submit()
                target = pending
                downloadTarget = pending
                val file = pending.get(60, TimeUnit.SECONDS)
                PhotoImageSaver.save(app, file)
                mainHandler.post { completeSave("图片已保存到相册 Pictures/biubiu") }
            } catch (error: Exception) {
                if (!Thread.currentThread().isInterrupted) {
                    mainHandler.post { completeSave("保存失败，请检查图片或存储空间后重试") }
                }
            } finally {
                target?.let { requests.clear(it) }
                downloadTarget = null
            }
        }
    }

    private fun completeSave(message: String) {
        if (stopped || isFinishing || isDestroyed) return
        saving = false
        binding.ivSaveImagePhoto.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        stopped = true
        downloadTarget?.cancel(true)
        saveTask?.cancel(true)
        worker.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}

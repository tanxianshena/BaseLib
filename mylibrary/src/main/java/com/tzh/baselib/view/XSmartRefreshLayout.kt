package com.tzh.baselib.view

import android.content.Context
import android.util.AttributeSet
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.scwang.smart.refresh.layout.listener.ScrollBoundaryDecider
import com.scwang.smart.refresh.layout.simple.SimpleBoundaryDecider
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import java.lang.ref.WeakReference

/** 分页业务状态独立于刷新动画；所有状态操作应在主线程执行。 */
class XSmartRefreshLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : SmartRefreshLayout(context, attrs) {
    var pageIndex = 1
    var isRefresh = true
    /** 0（及负值）表示未知总页数；空结果请调用 loadNoData。 */
    var pageCount = 0
    var hasMore = true
        private set
    val isRequestInProgress: Boolean get() = currentRequestId != null
    /** 在请求发起时保存，响应时配合 completeRequest/failRequest 过滤过期响应。 */
    var currentRequestId: Long? = null
        private set

    private var sequence = 0L
    private var committedPage = 1
    private var previousPage = 1
    private var previousPageCount = 0
    private var previousHasMore = true
    private var previousIsRefresh = true
    private var adapterRef: WeakReference<XRvBindingPureDataAdapter<*>>? = null

    fun onRefresh() { mRefreshListener?.onRefresh(this) }
    fun onLoadMore() { mLoadMoreListener?.onLoadMore(this) }

    fun setOnRefreshLoadMoreListener(block: ((layout: RefreshLayout) -> Unit?)? = null): RefreshLayout {
        return setOnRefreshLoadMoreListener(if (block == null) null else object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) { block(refreshLayout) }
            override fun onLoadMore(refreshLayout: RefreshLayout) { block(refreshLayout) }
        })
    }

    override fun setOnRefreshLoadMoreListener(listener: OnRefreshLoadMoreListener?): RefreshLayout {
        setOnRefreshListener(listener)
        setOnLoadMoreListener(listener)
        return this
    }

    override fun setOnRefreshListener(listener: OnRefreshListener?): RefreshLayout {
        return super.setOnRefreshListener(if (listener == null) null else OnRefreshListener { layout ->
            dispatchRequest(true) { listener.onRefresh(layout) }
        })
    }

    override fun setOnLoadMoreListener(listener: OnLoadMoreListener?): RefreshLayout {
        return super.setOnLoadMoreListener(if (listener == null) null else OnLoadMoreListener { layout ->
            dispatchRequest(false) { listener.onLoadMore(layout) }
        })
    }

    private fun dispatchRequest(refresh: Boolean, callback: () -> Unit) {
        if (isRequestInProgress) return
        if (mEnablePureScrollMode || (refresh && !mEnableRefresh) || (!refresh && !mEnableLoadMore)) {
            finishAnimations(false)
            return
        }
        if (!refresh && (!hasMore || (pageCount > 0 && pageIndex >= pageCount))) {
            loadNoData()
            return
        }
        previousPage = pageIndex
        previousPageCount = pageCount
        previousHasMore = hasMore
        previousIsRefresh = isRefresh
        currentRequestId = ++sequence
        isRefresh = refresh
        if (refresh) {
            pageIndex = 1
            // 保留已配置的总页数；服务端返回新总数时由调用方更新。
            onRefreshStatus()
        } else {
            pageIndex++
        }
        val requestId = currentRequestId
        try {
            callback()
        } catch (error: Exception) {
            if (currentRequestId == requestId) loadError()
            throw error
        }
    }

    /** 默认按总页数判断；未知总页数默认继续加载，显式 true 表示末页。 */
    fun loadSuccess(
        adapter: XRvBindingPureDataAdapter<*>? = null,
        isShowNoData: Boolean = pageCount > 0 && pageIndex >= pageCount
    ) {
        completeSuccess(!isShowNoData, adapter)
    }

    fun loadAutoSuccess(adapter: XRvBindingPureDataAdapter<*>? = null) {
        completeSuccess(pageCount <= 0 || pageIndex < pageCount, adapter)
    }

    /** 不返回总页数的接口使用 hasMore 显式控制下一页。 */
    fun loadPageSuccess(hasMore: Boolean, adapter: XRvBindingPureDataAdapter<*>? = null) {
        completeSuccess(hasMore, adapter)
    }

    /** 先验证 ID，再执行列表更新，防止旧响应覆盖当前列表。返回 false 表示已过期。 */
    fun completeRequest(
        requestId: Long,
        hasMore: Boolean,
        adapter: XRvBindingPureDataAdapter<*>? = null,
        updateData: () -> Unit = {}
    ): Boolean {
        if (currentRequestId != requestId) return false
        try {
            updateData()
        } catch (error: Exception) {
            failRequest(requestId)
            throw error
        }
        if (currentRequestId != requestId) return false
        completeSuccess(hasMore, adapter)
        return true
    }

    fun failRequest(requestId: Long): Boolean {
        if (currentRequestId != requestId) return false
        loadError()
        return true
    }

    private fun completeSuccess(more: Boolean, adapter: XRvBindingPureDataAdapter<*>?) {
        committedPage = pageIndex
        currentRequestId = null
        updateNoMoreData(!more, adapter)
        finishAnimations(true)
    }

    fun loadError() {
        restorePage()
        currentRequestId = null
        updateNoMoreData(!hasMore, null)
        finishAnimations(false)
    }

    fun loadNoData(adapter: XRvBindingPureDataAdapter<*>? = null) {
        completeSuccess(false, adapter)
    }

    /** 重置本轮分页结束状态，不改变调用方设置的加载更多功能开关。 */
    fun onRefreshStatus(adapter: XRvBindingPureDataAdapter<*>? = null) {
        updateNoMoreData(false, adapter)
    }

    private fun updateNoMoreData(noMoreData: Boolean, adapter: XRvBindingPureDataAdapter<*>?) {
        if (adapter != null) adapterRef = WeakReference(adapter)
        setNoMoreData(noMoreData)
    }

    override fun setNoMoreData(noMoreData: Boolean): RefreshLayout {
        hasMore = !noMoreData
        adapterRef?.get()?.showNoMoreData(noMoreData)
        return super.setNoMoreData(noMoreData)
    }

    private fun restorePage() {
        if (isRequestInProgress) {
            pageIndex = previousPage
            pageCount = previousPageCount
            hasMore = previousHasMore
            isRefresh = previousIsRefresh
        } else {
            pageIndex = committedPage
        }
    }

    private fun finishAnimations(success: Boolean, refreshDelay: Int = 0, loadDelay: Int = 0) {
        // 同时覆盖刷新、加载及尚未进入动画的手动请求；保持相同的末页状态。
        finishRefresh(refreshDelay, success, !hasMore)
        finishLoadMore(loadDelay, success, !hasMore)
    }

    /** 取消业务请求并回滚页码，默认无额外等待；控件自身收起动画仍会执行。 */
    fun stopRefreshLoad(refreshDelayed: Int = 0, loadDelayed: Int = 0) {
        if (isRequestInProgress) restorePage()
        currentRequestId = null
        updateNoMoreData(!hasMore, null)
        finishAnimations(false, refreshDelayed.coerceAtLeast(0), loadDelayed.coerceAtLeast(0))
    }

    override fun onDetachedFromWindow() {
        if (isRequestInProgress) restorePage()
        currentRequestId = null
        super.onDetachedFromWindow()
        updateNoMoreData(!hasMore, null)
    }

    @JvmOverloads
    fun dampingStyle(enable: Boolean = false) {
        setEnableLoadMore(enable)
        setEnableRefresh(enable)
        setEnableOverScrollBounce(!enable)
        setEnableOverScrollDrag(!enable)
        setEnablePureScrollMode(!enable)
    }

    fun dampingRefreshStyle() {
        setEnableLoadMore(false)
        setEnableRefresh(true)
        setEnableOverScrollBounce(true)
        setEnableOverScrollDrag(true)
        setEnablePureScrollMode(false)
    }

    open class ScrollBoundaryDeciderAdapter : SimpleBoundaryDecider(), ScrollBoundaryDecider
}
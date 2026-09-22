package com.tzh.baselib

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import com.tzh.baselib.view.XSmartRefreshLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XSmartRefreshLayoutTest {
    private fun onMain(block: (Context) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            block(ContextThemeWrapper(instrumentation.context, androidx.appcompat.R.style.Theme_AppCompat))
        }
    }

    private fun flag(view: XSmartRefreshLayout, name: String): Boolean {
        val field = SmartRefreshLayout::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.getBoolean(view)
    }

    @Test fun unknownTotalAllowsNextPageAndExplicitEndBlocksFurtherRequests() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        val pages = mutableListOf<Int>()
        view.setOnRefreshLoadMoreListener { pages.add(view.pageIndex); Unit }
        view.onRefresh()
        view.loadSuccess()
        assertTrue(view.hasMore)
        view.onLoadMore()
        assertEquals(listOf(1, 2), pages)
        view.loadPageSuccess(false)
        assertFalse(view.hasMore)
        assertTrue(flag(view, "mFooterNoMoreData"))
        view.onLoadMore()
        assertEquals(listOf(1, 2), pages)
    }

    @Test fun knownLastPageWorksWithoutAdapter() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        view.pageCount = 1
        view.setOnRefreshLoadMoreListener { Unit }
        view.onRefresh()
        view.loadAutoSuccess()
        assertFalse(view.hasMore)
        assertFalse(view.isRequestInProgress)
        assertTrue(flag(view, "mFooterNoMoreData"))
    }

    @Test fun failedLoadRetriesSamePageAndRefreshFailureRestoresPreviousState() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        view.setOnRefreshLoadMoreListener { Unit }
        view.onRefresh()
        view.loadPageSuccess(true)
        view.onLoadMore()
        assertEquals(2, view.pageIndex)
        view.loadError()
        assertEquals(1, view.pageIndex)
        view.onLoadMore()
        assertEquals(2, view.pageIndex)
        view.loadPageSuccess(false)
        view.onRefresh()
        assertEquals(1, view.pageIndex)
        assertTrue(view.hasMore)
        view.loadError()
        assertEquals(2, view.pageIndex)
        assertFalse(view.hasMore)
    }

    @Test fun duplicateRequestsAreIgnoredAndCancelledResponsesCannotUpdateData() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        var calls = 0
        view.setOnRefreshLoadMoreListener { calls++; Unit }
        view.onRefresh()
        val oldId = checkNotNull(view.currentRequestId)
        view.onRefresh()
        view.onLoadMore()
        assertEquals(1, calls)
        view.stopRefreshLoad()
        view.onRefresh()
        val newId = checkNotNull(view.currentRequestId)
        var updates = 0
        assertFalse(view.completeRequest(oldId, false) { updates++ })
        assertFalse(view.failRequest(oldId))
        assertEquals(newId, view.currentRequestId)
        assertTrue(view.completeRequest(newId, true) { updates++ })
        assertFalse(view.completeRequest(newId, false) { updates++ })
        assertEquals(1, updates)
        assertTrue(view.hasMore)
    }

    @Test fun separateListenersUseSamePaginationAndDisabledLoadMoreStaysDisabled() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        var refreshes = 0
        var loads = 0
        view.setOnRefreshListener(OnRefreshListener { refreshes++ })
        view.setOnLoadMoreListener(OnLoadMoreListener { loads++ })
        view.setEnableLoadMore(false)
        view.onRefresh()
        assertEquals(1, refreshes)
        view.loadPageSuccess(true)
        assertFalse(flag(view, "mEnableLoadMore"))
        view.onLoadMore()
        assertEquals(0, loads)
        view.setEnableLoadMore(true)
        view.onLoadMore()
        assertEquals(1, loads)
        assertEquals(2, view.pageIndex)
        view.loadSuccess()
    }

    @Test fun refreshOnlyModeKeepsRefreshAndDisablesPureScrolling() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        var calls = 0
        view.setOnRefreshLoadMoreListener { calls++; Unit }
        view.dampingRefreshStyle()
        view.onRefresh()
        assertEquals(1, calls)
        assertFalse(flag(view, "mEnablePureScrollMode"))
        view.loadNoData()
        view.onRefresh()
        view.loadSuccess()
        assertFalse(flag(view, "mEnableLoadMore"))
    }

    @Test fun footerIsResetOnRefreshAndRestoredOnFailure() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        val adapter = object : XRvBindingPureDataAdapter<String>() {
            override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: String) {}
        }
        view.setOnRefreshLoadMoreListener { Unit }
        view.loadNoData(adapter)
        assertTrue(adapter.getShowNoMoreData())
        view.onRefresh()
        assertFalse(adapter.getShowNoMoreData())
        view.loadError()
        assertTrue(adapter.getShowNoMoreData())
    }

    @Test fun callbackExceptionReleasesRequestAndRollsBackPage() = onMain { context ->
        val view = XSmartRefreshLayout(context)
        view.setOnLoadMoreListener(OnLoadMoreListener { throw IllegalStateException("test") })
        try {
            view.onLoadMore()
            fail("Expected callback failure")
        } catch (_: IllegalStateException) {
            assertFalse(view.isRequestInProgress)
            assertEquals(1, view.pageIndex)
        }
    }
}
package com.tzh.baselib

import androidx.recyclerview.widget.RecyclerView
import com.tzh.baselib.adapter.XRvBindingHolder
import com.tzh.baselib.adapter.XRvBindingPureDataAdapter
import org.junit.Assert.*
import org.junit.Test

class AdapterNotificationsTest {
    private class Adapter : XRvBindingPureDataAdapter<String>() {
        override fun onBindViewHolder(holder: XRvBindingHolder, position: Int, data: String) {}
    }

    @Test fun appendingReportsActualInsertionPositionWithAndWithoutFooter() {
        for (footer in listOf(false, true)) {
            val adapter = Adapter()
            adapter.showNoMoreData(footer)
            val inserts = mutableListOf<Int>()
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    inserts.add(start)
                    assertTrue(start >= 0 && start + count <= adapter.itemCount)
                }
                override fun onItemRangeChanged(start: Int, count: Int) {
                    assertTrue(start >= 0 && start + count <= adapter.itemCount)
                }
            })
            adapter.addData("a")
            adapter.addData("b")
            assertEquals(listOf(0, 1), inserts)
        }
    }

    @Test fun removalNeverChangesPositionsOutsideRemainingRows() {
        for (footer in listOf(false, true)) {
            val adapter = Adapter()
            adapter.setDatas(mutableListOf("a", "b", "c"), false)
            adapter.showNoMoreData(footer)
            adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeChanged(start: Int, count: Int) {
                    assertTrue(count > 0 && start >= 0 && start + count <= adapter.itemCount)
                }
            })
            assertTrue(adapter.removeData(1))
            assertTrue(adapter.removeData(1))
            assertTrue(adapter.removeData(0))
            assertFalse(adapter.removeData(0))
        }
    }

    @Test fun indexedInsertionUsesFullRebindInsteadOfIntegerPayload() {
        val adapter = Adapter()
        adapter.setDatas(mutableListOf("a", "c"), false)
        var changed = false
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeChanged(start: Int, count: Int, payload: Any?) {
                changed = true
                assertNull(payload)
                assertTrue(start + count <= adapter.itemCount)
            }
        })
        adapter.addData(1, "b")
        assertTrue(changed)
        assertEquals(listOf("a", "b", "c"), adapter.listData)
    }
}
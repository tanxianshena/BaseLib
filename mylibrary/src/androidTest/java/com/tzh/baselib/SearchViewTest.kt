package com.tzh.baselib

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tzh.baselib.view.SearchView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchViewTest {
    private class Listener : SearchView.SvSearchListener {
        val queries = mutableListOf<String>()
        var clears = 0
        override fun search(text: String): Boolean { queries.add(text); return false }
        override fun clear() { clears++ }
    }

    private fun onMain(block: (Context) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            block(ContextThemeWrapper(instrumentation.context, androidx.appcompat.R.style.Theme_AppCompat))
        }
    }

    private fun fixture(context: Context): SearchView {
        val id = context.resources.getIdentifier("search_view_test", "layout", context.packageName)
        return LayoutInflater.from(context).inflate(id, null) as SearchView
    }

    @Test fun programmaticConstructionWorksAndGettersNeverReturnNullText() = onMain { context ->
        val view = SearchView(context)
        assertEquals(1, view.childCount)
        assertEquals("", view.getText())
        view.binding.etText.hint = null
        assertEquals("", view.getHintText())
        view.setHintText("New hint")
        assertEquals("New hint", view.getHintText())
        view.setText("abc")
        assertEquals("abc", view.getText())
        assertEquals(3, view.binding.etText.selectionStart)
    }

    @Test fun dimensionsAreAppliedAsPixelsAndClearTargetIsAccessible() = onMain { context ->
        val view = fixture(context)
        assertEquals(18f, view.binding.etText.textSize, 0.01f)
        assertEquals(7f, view.binding.layout.xBaseShape.gradientDrawable.cornerRadius, 0.01f)
        assertEquals(20, view.binding.ivSearch.layoutParams.width)
        assertEquals(20, view.binding.ivSearch.layoutParams.height)
        val clear = view.binding.ivClear
        assertTrue(clear.layoutParams.width >= (48 * context.resources.displayMetrics.density).toInt())
        assertEquals(20, clear.layoutParams.width - clear.paddingLeft - clear.paddingRight)
        assertFalse(clear.contentDescription.isNullOrEmpty())
    }

    @Test fun clearCallbackOnlyFiresOnNonEmptyToEmptyTransition() = onMain { context ->
        val view = SearchView(context)
        val listener = Listener()
        view.setSvSearchListener(listener)
        view.setText("")
        view.binding.etText.setText("")
        assertEquals(0, listener.clears)
        view.setText("abc")
        assertEquals(View.VISIBLE, view.binding.ivClear.visibility)
        view.binding.ivClear.performClick()
        view.setText("")
        view.binding.etText.setText("")
        assertEquals(1, listener.clears)
        assertEquals(View.GONE, view.binding.ivClear.visibility)
        view.setText("def")
        view.binding.etText.text?.clear()
        assertEquals(2, listener.clears)
    }

    @Test fun changingHintCannotBypassValidationAndLengthBoundaryIsAccepted() = onMain { context ->
        val view = fixture(context)
        val listener = Listener()
        view.setSvSearchListener(listener)
        view.setHintText("Changed hint")
        view.submitSearch()
        view.setText("abcd")
        view.submitSearch()
        assertTrue(listener.queries.isEmpty())
        view.setText("abc")
        view.submitSearch()
        assertEquals(listOf("abc"), listener.queries)
    }

    @Test fun unconfiguredEmptySearchRemainsAllowedAndTextIsNotTrimmed() = onMain { context ->
        val view = SearchView(context)
        val listener = Listener()
        view.setSvSearchListener(listener)
        view.submitSearch()
        view.setText(" a ")
        view.submitSearch()
        assertEquals(listOf("", " a "), listener.queries)
    }

    @Test fun imeSearchAndHardwareEnterSubmitOncePerAction() = onMain { context ->
        val view = SearchView(context)
        val listener = Listener()
        view.setSvSearchListener(listener)
        view.setText("abc")
        view.binding.etText.onEditorAction(EditorInfo.IME_ACTION_SEARCH)
        assertEquals(1, listener.queries.size)
        view.binding.etText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        view.binding.etText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        assertEquals(2, listener.queries.size)
    }

    @Test fun reentrantClearListenerCanRestoreTextWithoutStaleButtonState() = onMain { context ->
        val view = SearchView(context)
        var clears = 0
        view.setSvSearchListener(object : SearchView.SvSearchListener {
            override fun search(text: String) = false
            override fun clear() { clears++; view.setText("restored") }
        })
        view.setText("initial")
        view.setText("")
        assertEquals(1, clears)
        assertEquals("restored", view.getText())
        assertEquals(View.VISIBLE, view.binding.ivClear.visibility)
    }
}
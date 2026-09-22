package com.tzh.baselib

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tzh.baselib.view.ImageTextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageTextViewTest {
    private fun onMain(block: (Context) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            block(ContextThemeWrapper(instrumentation.context, androidx.appcompat.R.style.Theme_AppCompat))
        }
    }

    private fun fixture(context: Context): ImageTextView {
        val id = context.resources.getIdentifier("image_text_view_test", "layout", context.packageName)
        return LayoutInflater.from(context).inflate(id, null) as ImageTextView
    }

    @Test fun programmaticConstructionHasChildrenAndDpDefaults() = onMain { context ->
        val view = ImageTextView(context)
        assertEquals(2, view.childCount)
        assertEquals(Gravity.CENTER, view.gravity)
        assertEquals((24 * context.resources.displayMetrics.density + 0.5f).toInt(), view.mImageView.layoutParams.width)
        view.setText("Created in code")
        assertEquals("Created in code", view.mTextview.text.toString())
        assertSame(view, view.mTextview.parent)
    }

    @Test fun xmlSupportsLiteralColorsGravityAndExactPixelSpacing() = onMain { context ->
        val view = fixture(context)
        assertEquals(Gravity.START or Gravity.CENTER_VERTICAL, view.gravity)
        assertEquals(Color.GREEN, view.mTextview.currentTextColor)
        assertEquals(Color.BLUE, view.mImageView.imageTintList!!.defaultColor)
        assertEquals(7, (view.mTextview.layoutParams as LinearLayout.LayoutParams).topMargin)
        view.isSelected = true
        assertEquals(Color.RED, view.mTextview.currentTextColor)
        assertNotNull(view.mImageView.drawable)
        view.isSelected = false
        assertNull(view.mImageView.drawable)
    }

    @Test fun defaultImageSurvivesStateChangesAndZeroClearsIt() = onMain { context ->
        val view = ImageTextView(context)
        view.setImage(android.R.drawable.btn_star_big_off)
        val image = view.mImageView.drawable
        assertNotNull(image)
        view.isSelected = true
        assertSame(image, view.mImageView.drawable)
        view.isSelected = false
        assertSame(image, view.mImageView.drawable)
        view.setImage(0)
        assertNull(view.mImageView.drawable)
    }

    @Test fun explicitSelectedStyleSurvivesUpdatingDefaultStyle() = onMain { context ->
        val view = fixture(context)
        view.isSelected = true
        val selectedImage = view.mImageView.drawable
        view.setImage(android.R.drawable.btn_star_big_off)
        view.setTextColorInt(Color.BLUE)
        assertSame(selectedImage, view.mImageView.drawable)
        assertEquals(Color.RED, view.mTextview.currentTextColor)
        view.isSelected = false
        assertNotNull(view.mImageView.drawable)
        assertNotSame(selectedImage, view.mImageView.drawable)
        assertEquals(Color.BLUE, view.mTextview.currentTextColor)
    }

    @Test fun selectorsFollowParentSelectionAndEnabledState() = onMain { context ->
        val view = ImageTextView(context)
        val id = context.resources.getIdentifier("image_text_selector_test", "color", context.packageName)
        view.setTextColorRes(id)
        assertEquals(Color.GREEN, view.mTextview.currentTextColor)
        view.isSelected = true
        assertEquals(Color.RED, view.mTextview.currentTextColor)
        view.isEnabled = false
        assertEquals(0xff888888.toInt(), view.mTextview.currentTextColor)
        view.isEnabled = true
        view.setTextColors(ColorStateList.valueOf(Color.BLUE))
        assertEquals(Color.BLUE, view.mTextview.currentTextColor)
        view.setTextColor(android.R.color.white)
        assertEquals(Color.WHITE, view.mTextview.currentTextColor)
    }

    @Test fun changingDirectionsResetsMarginsAndSameDirectionKeepsParams() = onMain { context ->
        val view = fixture(context)
        for (direction in 0..3) {
            view.setShowLocal(direction)
            val params = view.mTextview.layoutParams as LinearLayout.LayoutParams
            assertEquals(if (direction == 0) 7 else 0, params.topMargin)
            assertEquals(if (direction == 1) 7 else 0, params.bottomMargin)
            assertEquals(if (direction == 2) 7 else 0, params.marginStart)
            assertEquals(if (direction == 3) 7 else 0, params.marginEnd)
            assertEquals(if (direction < 2) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL, view.orientation)
            assertSame(if (direction == 0 || direction == 2) view.mImageView else view.mTextview, view.getChildAt(0))
            view.setShowLocal(direction)
            assertSame(params, view.mTextview.layoutParams)
        }
        val first = view.getChildAt(0)
        try {
            view.setShowLocal(99)
            fail("Invalid direction must be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals(2, view.childCount)
            assertSame(first, view.getChildAt(0))
        }
    }

    @Test fun externalClickListenerSeesToggledStateAndVetoIsRespected() = onMain { context ->
        val view = ImageTextView(context)
        val states = mutableListOf<Boolean>()
        view.setOnClickListener { states.add(view.isSelected) }
        assertTrue(view.performClick())
        assertEquals(listOf(true), states)
        view.onSelectChangeListener = object : ImageTextView.OnSelectChangeListener {
            override fun onSelect(selected: Boolean) = true
        }
        view.performClick()
        assertEquals(listOf(true, true), states)
        view.setSelected(false, false)
        assertFalse(view.isSelected)
        view.isEnabled = false
        assertFalse(view.performClick())
        assertEquals(2, states.size)
    }

    @Test fun xmlCanDisableAutomaticToggleWithoutDisablingExternalClicks() = onMain { context ->
        val id = context.resources.getIdentifier("image_text_view_no_toggle_test", "layout", context.packageName)
        val view = LayoutInflater.from(context).inflate(id, null) as ImageTextView
        var clicks = 0
        view.setOnClickListener { clicks++ }
        assertTrue(view.performClick())
        assertEquals(1, clicks)
        assertFalse(view.isSelected)
        view.isSelected = true
        assertEquals(Color.RED, view.mTextview.currentTextColor)
    }

    @Test fun identicalSelectionDoesNotNotifyAgain() = onMain { context ->
        val view = ImageTextView(context)
        var changes = 0
        view.onSelectChangeListener = object : ImageTextView.OnSelectChangeListener {
            override fun onSelect(selected: Boolean): Boolean { changes++; return false }
        }
        view.isSelected = false
        view.isSelected = true
        view.isSelected = true
        assertEquals(1, changes)
    }
}
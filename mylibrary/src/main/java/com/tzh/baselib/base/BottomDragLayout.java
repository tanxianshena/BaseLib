package com.tzh.baselib.base;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Intercepts downward drags only after the touched scrollable child reaches its top. */
final class BottomDragLayout extends FrameLayout {
    interface Callback {
        boolean canDrag();
        void onDismiss();
    }

    private final Callback callback;
    private final int slop;
    private float downX, downY;
    private boolean tracking, dragging, horizontal;

    BottomDragLayout(Context context, Callback callback) {
        super(context);
        this.callback = callback;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!callback.canDrag()) { reset(); return false; }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                reset();
                downX = event.getX();
                downY = event.getY();
                tracking = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                tracking = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!tracking || horizontal) break;
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                    horizontal = true;
                    break;
                }
                if (canScrollUp(this, event.getX(), event.getY())) {
                    downX = event.getX();
                    downY = event.getY();
                    break;
                }
                if (dy > slop && dy > Math.abs(dx)) {
                    dragging = true;
                    downY += slop;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                tracking = false;
                break;
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!callback.canDrag()) { reset(); return false; }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) onInterceptTouchEvent(event);
                if (!dragging) return true;
                // Translate the content, leaving the hit-test coordinate system fixed.
                if (getChildCount() > 0) {
                    getChildAt(0).setTranslationY(Math.max(0, event.getY() - downY));
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean dismiss = dragging && getChildCount() > 0
                        && getChildAt(0).getTranslationY() >= threshold();
                tracking = dragging = false;
                if (dismiss) {
                    reset();
                    callback.onDismiss();
                } else {
                    settle();
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                tracking = dragging = false;
                settle();
                return true;
            default:
                return true;
        }
    }

    private float threshold() {
        float density = getResources().getDisplayMetrics().density;
        return Math.min(getHeight() * 0.5f, Math.max(48 * density,
                Math.min(96 * density, getHeight() * 0.25f)));
    }

    private void settle() {
        if (getChildCount() > 0) getChildAt(0).animate().translationY(0).setDuration(180).start();
    }

    void reset() {
        tracking = dragging = horizontal = false;
        if (getChildCount() > 0) {
            View content = getChildAt(0);
            content.animate().cancel();
            content.setTranslationY(0);
        }
    }

    private static boolean canScrollUp(View view, float x, float y) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (child.getVisibility() != VISIBLE) continue;
                float childX = x + group.getScrollX() - child.getX();
                float childY = y + group.getScrollY() - child.getY();
                if (childX >= 0 && childY >= 0 && childX < child.getWidth()
                        && childY < child.getHeight()
                        && canScrollUp(child, childX, childY)) return true;
            }
        }
        return view.canScrollVertically(-1);
    }

    @Override protected void onDetachedFromWindow() {
        reset();
        super.onDetachedFromWindow();
    }
}

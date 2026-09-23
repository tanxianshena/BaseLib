package com.tzh.baselib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import com.tzh.baselib.R;

/** A 3x3 gesture input. IDs run left-to-right, top-to-bottom from 1 through 9. */
public class GestureLockView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureLockPattern pattern = new GestureLockPattern();
    private float pointRadius;
    private float lineWidth;
    private int lockColor;
    private int unlockColor;
    private int linerColor;
    private int activePointer = MotionEvent.INVALID_POINTER_ID;
    private float currentX;
    private float currentY;
    private boolean legacyCoordinateFormat;
    private OnGestureLockListener listener;
    private int contentWidth = -1, contentHeight = -1, contentLeft = -1, contentTop = -1;

    public GestureLockView(Context context) { this(context, null); }
    public GestureLockView(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public GestureLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public GestureLockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GestureLockView,
                defStyleAttr, defStyleRes);
        try {
            lockColor = a.getColor(R.styleable.GestureLockView_gv_lock_color, 0xff9ecca4);
            unlockColor = a.getColor(R.styleable.GestureLockView_gv_unlock_color, 0xff666666);
            linerColor = a.getColor(R.styleable.GestureLockView_gv_liner_color, 0xff9ecca4);
            pointRadius = readSize(a, R.styleable.GestureLockView_gv_point_radius, dp(20));
            lineWidth = readSize(a, R.styleable.GestureLockView_gv_line_width, dp(4));
        } finally {
            a.recycle();
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }

    // Integer XML values retain their historical pixel meaning; dimensions support dp.
    private static float readSize(TypedArray a, int index, float fallback) {
        TypedValue value = a.peekValue(index);
        if (value == null) return fallback;
        float size = value.type == TypedValue.TYPE_DIMENSION
                ? a.getDimension(index, fallback) : a.getInt(index, (int) fallback);
        return size > 0 && !Float.isInfinite(size) && !Float.isNaN(size) ? size : fallback;
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int desired = (int) Math.ceil(Math.max(dp(240), pointRadius * 9));
        setMeasuredDimension(resolveSize(Math.max(getSuggestedMinimumWidth(),
                        desired + getPaddingLeft() + getPaddingRight()), widthSpec),
                resolveSize(Math.max(getSuggestedMinimumHeight(),
                        desired + getPaddingTop() + getPaddingBottom()), heightSpec));
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateGeometry();
    }

    private void updateGeometry() {
        int width = Math.max(0, getWidth() - getPaddingLeft() - getPaddingRight());
        int height = Math.max(0, getHeight() - getPaddingTop() - getPaddingBottom());
        if (width == contentWidth && height == contentHeight
                && getPaddingLeft() == contentLeft && getPaddingTop() == contentTop) return;
        resetGesture();
        contentWidth = width;
        contentHeight = height;
        contentLeft = getPaddingLeft();
        contentTop = getPaddingTop();
        pattern.layout(width, height, contentLeft, contentTop, pointRadius);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pattern.radius <= 0) return;
        int save = canvas.save();
        canvas.clipRect(contentLeft, contentTop, contentLeft + contentWidth, contentTop + contentHeight);
        paint.setStrokeWidth(Math.min(lineWidth, pattern.radius * 2));
        paint.setColor(linerColor);
        for (int i = 1; i < pattern.count; i++) {
            int from = pattern.order[i - 1], to = pattern.order[i];
            canvas.drawLine(pattern.x[from], pattern.y[from], pattern.x[to], pattern.y[to], paint);
        }
        if (activePointer != MotionEvent.INVALID_POINTER_ID && pattern.count > 0) {
            int last = pattern.order[pattern.count - 1];
            canvas.drawLine(pattern.x[last], pattern.y[last], currentX, currentY, paint);
        }
        for (int i = 0; i < 9; i++) {
            paint.setColor(pattern.selected[i] ? lockColor : unlockColor);
            canvas.drawCircle(pattern.x[i], pattern.y[i], pattern.radius, paint);
        }
        canvas.restoreToCount(save);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) { resetGesture(); return false; }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            updateGeometry();
            resetGesture();
            if (pattern.radius <= 0) return false;
            activePointer = event.getPointerId(0);
            currentX = event.getX(0);
            currentY = event.getY(0);
            pattern.trace(currentX, currentY, currentX, currentY);
            disallowIntercept(pattern.count > 0);
        } else {
            if (activePointer == MotionEvent.INVALID_POINTER_ID) return false;
            if (action == MotionEvent.ACTION_CANCEL
                    || (action == MotionEvent.ACTION_POINTER_UP
                    && event.getPointerId(event.getActionIndex()) == activePointer)) {
                resetGesture();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
                int index = event.findPointerIndex(activePointer);
                if (index < 0) { resetGesture(); return true; }
                for (int h = 0; h < event.getHistorySize(); h++) {
                    traceTo(event.getHistoricalX(index, h), event.getHistoricalY(index, h));
                }
                traceTo(event.getX(index), event.getY(index));
                if (action == MotionEvent.ACTION_UP) {
                    String result = pattern.serialize(legacyCoordinateFormat);
                    OnGestureLockListener callback = listener;
                    resetGesture();
                    if (!result.isEmpty()) {
                        performClick();
                        if (callback != null) callback.onGestureComplete(result);
                    }
                    return true;
                }
            }
        }
        postInvalidateOnAnimation();
        return true;
    }

    private void traceTo(float x, float y) {
        pattern.trace(currentX, currentY, x, y);
        currentX = x;
        currentY = y;
        if (pattern.count > 0) disallowIntercept(true);
    }

    private void disallowIntercept(boolean disallow) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(disallow);
    }

    /** Cancels the current gesture without delivering a completion callback. */
    public void resetGesture() {
        activePointer = MotionEvent.INVALID_POINTER_ID;
        if (pattern != null) pattern.clear();
        disallowIntercept(false);
        invalidate();
    }

    @Override public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) resetGesture();
    }

    @Override protected void onDetachedFromWindow() {
        resetGesture();
        super.onDetachedFromWindow();
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Default false: results use stable IDs such as "1-2-3-6-9".
     * True retains the old "x,y;" encoding only; coordinates still depend on layout.
     * Existing stored coordinate patterns must be migrated or enrolled again.
     */
    public void setLegacyCoordinateFormat(boolean enabled) {
        resetGesture();
        legacyCoordinateFormat = enabled;
    }

    public void setOnGestureLockListener(OnGestureLockListener listener) { this.listener = listener; }

    public interface OnGestureLockListener {
        void onGestureComplete(String gesture);
    }
}

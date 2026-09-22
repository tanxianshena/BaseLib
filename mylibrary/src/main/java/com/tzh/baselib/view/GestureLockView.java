package com.tzh.baselib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.tzh.baselib.R;

import java.util.ArrayList;
import java.util.List;

public class GestureLockView extends View {

    private static final int POINT_COUNT = 3; // 3x3的点阵
    private static int POINT_RADIUS = 60; // 点的半径
    private static int LINE_WIDTH = 16; // 连接线的宽度
    private static final int ANIMATION_DURATION = 150; // 动画时长

    private Paint paint;
    private Point[][] points; // 所有点的坐标
    private List<Point> selectedPoints; // 已选中的点
    private Point currentPoint; // 当前手指位置
    private OnGestureLockListener listener;

    private final int color = Color.parseColor("#9ECCA4");

    int lockColor;
    int unlockColor;
    int linerColor;

    public GestureLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.GestureLockView);
        //解锁的点颜色
        lockColor = attributes.getColor(R.styleable.GestureLockView_gv_lock_color,color);
        //未解锁的点颜色
        unlockColor = attributes.getColor(R.styleable.GestureLockView_gv_unlock_color,context.getColor(R.color.color_666));
        //连接线颜色
        linerColor = attributes.getColor(R.styleable.GestureLockView_gv_liner_color,color);
        //点的半径
        POINT_RADIUS = attributes.getInt(R.styleable.GestureLockView_gv_point_radius,60);
        //连接线的宽度
        LINE_WIDTH = attributes.getInt(R.styleable.GestureLockView_gv_line_width,16);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(LINE_WIDTH);
        selectedPoints = new ArrayList<>();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initPoints();
    }

    // 初始化点的位置
    private void initPoints() {
        points = new Point[POINT_COUNT][POINT_COUNT];
        int width = getWidth();
        int height = getHeight();
        int stepX = width / (POINT_COUNT + 1);
        int stepY = height / (POINT_COUNT + 1);

        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                points[i][j] = new Point(stepX * (i + 1), stepY * (j + 1));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawPoints(canvas);
        drawLines(canvas);
    }

    // 绘制所有点
    private void drawPoints(Canvas canvas) {
        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                Point point = points[i][j];
                paint.setColor(selectedPoints.contains(point) ? lockColor : unlockColor);
                canvas.drawCircle(point.x, point.y, POINT_RADIUS, paint);
            }
        }
    }

    // 绘制连接线
    private void drawLines(Canvas canvas) {
        if (selectedPoints.size() > 1) {
            for (int i = 1; i < selectedPoints.size(); i++) {
                Point start = selectedPoints.get(i - 1);
                Point end = selectedPoints.get(i);
                paint.setColor(linerColor);
                canvas.drawLine(start.x, start.y, end.x, end.y, paint);
            }
        }

        // 绘制当前手指位置的连接线
        if (currentPoint != null && !selectedPoints.isEmpty()) {
            Point lastPoint = selectedPoints.get(selectedPoints.size() - 1);
            paint.setColor(linerColor);
            canvas.drawLine(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                selectedPoints.clear();
                checkPoint(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                checkPoint(x, y);
                currentPoint = new Point(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (listener != null) {
                    listener.onGestureComplete(getGesture());
                }
//                animatePoints();
                selectedPoints.clear();
                currentPoint = null;
                break;
        }
        invalidate();
        return true;
    }

    // 检查手指是否触碰到点
    private void checkPoint(int x, int y) {
        for (int i = 0; i < POINT_COUNT; i++) {
            for (int j = 0; j < POINT_COUNT; j++) {
                Point point = points[i][j];
                if (Math.abs(x - point.x) < POINT_RADIUS && Math.abs(y - point.y) < POINT_RADIUS) {
                    if (!selectedPoints.contains(point)) {
                        selectedPoints.add(point);
//                        animatePoint(point);
                    }
                }
            }
        }
    }

    // 获取手势路径
    private String getGesture() {
        StringBuilder gesture = new StringBuilder();
        for (Point point : selectedPoints) {
            gesture.append(point.x).append(",").append(point.y).append(";");
        }
        return gesture.toString();
    }

    // 点的缩放动画
    private void animatePoint(Point point) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(ANIMATION_DURATION);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        startAnimation(scaleAnimation);
    }

    // 所有点的缩放动画
    private void animatePoints() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1f, 0.8f, 1f, 0.8f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(ANIMATION_DURATION);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        startAnimation(scaleAnimation);
    }

    public void setOnGestureLockListener(OnGestureLockListener listener) {
        this.listener = listener;
    }

    public interface OnGestureLockListener {
        void onGestureComplete(String gesture);
    }
}
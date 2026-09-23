package com.tzh.baselib.view;

import java.util.Arrays;

/** Geometry and selection state, independent of Android event delivery. */
final class GestureLockPattern {
    final float[] x = new float[9];
    final float[] y = new float[9];
    final boolean[] selected = new boolean[9];
    final int[] order = new int[9];
    int count;
    float radius;

    void clear() {
        Arrays.fill(selected, false);
        count = 0;
    }

    void layout(float width, float height, float left, float top, float requestedRadius) {
        clear();
        float stepX = Math.max(0, width) / 4;
        float stepY = Math.max(0, height) / 4;
        radius = Math.min(requestedRadius, Math.min(stepX, stepY) * 0.45f);
        for (int i = 0; i < 9; i++) {
            x[i] = left + stepX * (i % 3 + 1);
            y[i] = top + stepY * (i / 3 + 1);
        }
    }

    // Visit every intersected circle in travel order, including between event samples.
    void trace(float fromX, float fromY, float toX, float toY) {
        if (radius <= 0) return;
        while (count < 9) {
            int next = -1;
            double earliest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < 9; i++) {
                if (selected[i]) continue;
                double entry = intersection(fromX, fromY, toX, toY, i);
                if (entry < earliest) {
                    earliest = entry;
                    next = i;
                }
            }
            if (next < 0) return;
            add(next);
        }
    }

    private double intersection(float ax, float ay, float bx, float by, int index) {
        double dx = bx - ax, dy = by - ay;
        double ox = ax - x[index], oy = ay - y[index];
        double c = ox * ox + oy * oy - radius * radius;
        if (c <= 0) return 0;
        double a = dx * dx + dy * dy;
        if (a == 0) return Double.POSITIVE_INFINITY;
        double b = ox * dx + oy * dy;
        double discriminant = b * b - a * c;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        double t = (-b - Math.sqrt(discriminant)) / a;
        return t >= 0 && t <= 1 ? t : Double.POSITIVE_INFINITY;
    }

    private void add(int index) {
        if (count > 0) {
            int previous = order[count - 1];
            int rowSum = previous / 3 + index / 3;
            int colSum = previous % 3 + index % 3;
            // Android-style rule: insert an unselected midpoint on a grid jump.
            if (rowSum % 2 == 0 && colSum % 2 == 0) {
                int middle = rowSum / 2 * 3 + colSum / 2;
                if (!selected[middle] && middle != index) append(middle);
            }
        }
        if (!selected[index]) append(index);
    }

    private void append(int index) {
        selected[index] = true;
        order[count++] = index;
    }

    String serialize(boolean legacyCoordinates) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            int index = order[i];
            if (legacyCoordinates) {
                result.append((int) x[index]).append(',').append((int) y[index]).append(';');
            } else {
                if (i > 0) result.append('-');
                result.append(index + 1);
            }
        }
        return result.toString();
    }
}

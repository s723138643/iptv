package com.orion.player.ui;

public class Rect {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public Rect() {
        left = 0;
        top  = 0;
        right = 0;
        bottom = 0;
    }

    public Rect(int width, int height) {
        left = 0;
        top = 0;
        right = width;
        bottom = height;
    }

    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void reset(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void inset(Rect r) {
        left += r.left;
        top += r.top;
        right += r.right;
        bottom += r.bottom;
    }

    public void inset(float dx, float dy) {
        inset((int) dx, (int) dy);
    }

    public void inset(int dx, int dy) {
        left += dx;
        top += dy;
        right -= dx;
        bottom -= dy;
    }

    public void inset(int left, int top, int right, int bottom) {
        this.left += left;
        this.top += top;
        this.right += right;
        this.bottom += bottom;
    }

    public boolean in(float x, float y) {
        return in((int) x, (int) y);
    }

    public boolean in(int x, int y) {
        return x >= left
                && x <= right
                && y >= top
                && y <= bottom;
    }
}
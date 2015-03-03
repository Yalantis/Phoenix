package com.yalantis.pulltorefresh.library.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

import com.yalantis.pulltorefresh.library.PullToRefreshView;
import com.yalantis.pulltorefresh.library.R;
import com.yalantis.pulltorefresh.library.util.Utils;

/**
 * Created by Apisov on 02/03/2015.
 * https://dribbble.com/shots/1623131-Pull-to-Refresh
 */
public class JetRefreshView extends BaseRefreshView implements Animatable {

    private static final float SCALE_START_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 1000;

    private final static float SKY_RATIO = 0.65f;
    private static final float SKY_INITIAL_SCALE = 1.05f;

    private final static float CENTER_CLOUDS_RATIO = 0.22f;
    private static final float CENTER_CLOUDS_INITIAL_SCALE = 0.5f;
    private static final float CENTER_CLOUDS_FINAL_SCALE = 1.05f;

    private static final float SUN_FINAL_SCALE = 0.75f;
    private static final float SUN_INITIAL_ROTATE_GROWTH = 1.2f;
    private static final float SUN_FINAL_ROTATE_GROWTH = 1.5f;

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;

    private int mSkyHeight;
    private float mSkyTopOffset;
    private float mSkyMoveOffset;

    private int mCenterCloudsHeight;
    private float mCenterCloudsInitialTopOffset;
    private float mCenterCloudsFinalTopOffset;
    private float mCenterCloudsMoveOffset;

    private int mJetSize = 200;
    private float mJetLeftOffset;
    private float mJetTopOffset;

    private float mPercent = 0.0f;
    private float mRotate = 0.0f;

    private Bitmap mJet;
    private Bitmap mFrontClouds;
    private Bitmap mLeftClouds;
    private Bitmap mRightClouds;

    private boolean isRefreshing = false;

    public JetRefreshView(Context context, PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();

        initiateDimens();
        createBitmaps();
        setupAnimations();
    }

    private void initiateDimens() {
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mSkyHeight = (int) (SKY_RATIO * mScreenWidth);
        mSkyTopOffset = (mSkyHeight * 0.38f);
        mSkyMoveOffset = Utils.convertDpToPixel(getContext(), 15);

        mCenterCloudsHeight = (int) (CENTER_CLOUDS_RATIO * mScreenWidth);
        mCenterCloudsInitialTopOffset = (mParent.getTotalDragDistance() - mCenterCloudsHeight * CENTER_CLOUDS_INITIAL_SCALE);
        mCenterCloudsFinalTopOffset = (mParent.getTotalDragDistance() - mCenterCloudsHeight * CENTER_CLOUDS_FINAL_SCALE);
        mCenterCloudsMoveOffset = Utils.convertDpToPixel(getContext(), 10);

        mJetLeftOffset = 0.1f * (float) mScreenWidth;
        mJetTopOffset = (mParent.getTotalDragDistance() * 0.5f);

        mTop = -mParent.getTotalDragDistance();
    }

    private void createBitmaps() {
        mLeftClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_left);
        mLeftClouds = Bitmap.createScaledBitmap(mLeftClouds, mScreenWidth, mSkyHeight, true);

        mRightClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_right);
        mRightClouds = Bitmap.createScaledBitmap(mRightClouds, mScreenWidth, mSkyHeight, true);

        mFrontClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_center);
        mFrontClouds = Bitmap.createScaledBitmap(mFrontClouds, mScreenWidth, (int) (mScreenWidth * CENTER_CLOUDS_RATIO), true);

        mJet = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.airplane);
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
        if (invalidate) setRotate(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        final int saveCount = canvas.save();
        canvas.translate(0, mTop);

        drawJet(canvas);
        drawSideClouds(canvas);
        drawFrontClouds(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawSideClouds(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float skyScale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            /** Change skyScale between {@link #SKY_INITIAL_SCALE} and 1.0f depending on {@link #mPercent} */
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            skyScale = SKY_INITIAL_SCALE - (SKY_INITIAL_SCALE - 1.0f) * scalePercent;
        } else {
            skyScale = SKY_INITIAL_SCALE;
        }

        float offsetX = -(mScreenWidth * skyScale - mScreenWidth) / 2.0f;
        float offsetY = (1.0f - dragPercent) * mParent.getTotalDragDistance() - mSkyTopOffset // Offset canvas moving
                - mSkyHeight * (skyScale - 1.0f) / 2 // Offset sky scaling
                + mSkyMoveOffset * dragPercent; // Give it a little move top -> bottom

        matrix.postScale(skyScale, skyScale);
        matrix.postTranslate(offsetX, offsetY);
        canvas.drawBitmap(mLeftClouds, matrix, null);
    }

    private void drawFrontClouds(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float townScale;
        float townTopOffset;
        float townMoveOffset;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            /**
             * Change townScale between {@link #CENTER_CLOUDS_INITIAL_SCALE} and {@link #CENTER_CLOUDS_FINAL_SCALE} depending on {@link #mPercent}
             * Change townTopOffset between {@link #mCenterCloudsInitialTopOffset} and {@link #mCenterCloudsFinalTopOffset} depending on {@link #mPercent}
             */
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            townScale = CENTER_CLOUDS_INITIAL_SCALE + (CENTER_CLOUDS_FINAL_SCALE - CENTER_CLOUDS_INITIAL_SCALE) * scalePercent;
            townTopOffset = mCenterCloudsInitialTopOffset - (mCenterCloudsFinalTopOffset - mCenterCloudsInitialTopOffset) * scalePercent;
            townMoveOffset = mCenterCloudsMoveOffset * (1.0f - scalePercent);
        } else {
            float scalePercent = dragPercent / SCALE_START_PERCENT;
            townScale = CENTER_CLOUDS_INITIAL_SCALE;
            townTopOffset = mCenterCloudsInitialTopOffset;
            townMoveOffset = mCenterCloudsMoveOffset * scalePercent;
        }

        float offsetX = -(mScreenWidth * townScale - mScreenWidth) / 2.0f;
        float offsetY = (1.0f - dragPercent) * mParent.getTotalDragDistance() // Offset canvas moving
                + townTopOffset
                - mCenterCloudsHeight * (townScale - 1.0f) / 2 // Offset town scaling
                + townMoveOffset; // Give it a little move

        matrix.postScale(townScale, townScale);
        matrix.postTranslate(offsetX, offsetY);

        canvas.drawBitmap(mFrontClouds, matrix, null);
    }

    private void drawJet(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        if (dragPercent > 1.0f) { // Slow down if pulling over set height
            dragPercent = (dragPercent + 9.0f) / 10;
        }

        float offsetX = mJetLeftOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f + dragPercent); // Move the jet right
        float offsetY = mJetTopOffset
                + (mParent.getTotalDragDistance() / 2) * (1.0f - dragPercent) // Move the jet up
                - mTop; // Depending on Canvas position

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            matrix.preTranslate(offsetX, offsetY);
        } else {
            matrix.postTranslate(offsetX, offsetY);
        }

        canvas.drawBitmap(mJet, matrix, null);
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void setRotate(float rotate) {
        mRotate = rotate;
        invalidateSelf();
    }

    public void resetOriginals() {
        setPercent(0);
        setRotate(0);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, mSkyHeight + top);
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        isRefreshing = true;
        mParent.startAnimation(mAnimation);
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        isRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setRotate(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.RESTART);
        mAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

}

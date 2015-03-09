package com.yalantis.pulltorefresh.library.refresh_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import com.yalantis.pulltorefresh.library.PullToRefreshView;
import com.yalantis.pulltorefresh.library.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Apisov on 02/03/2015.
 * https://dribbble.com/shots/1623131-Pull-to-Refresh
 */
public class JetRefreshView extends BaseRefreshView implements Animatable {

    private static final float SCALE_START_PERCENT = 0.5f;
    private static final int ANIMATION_DURATION = 1000;

    private static final float SIDE_CLOUDS_INITIAL_SCALE = 1.05f;
    private static final float SIDE_CLOUDS_FINAL_SCALE = 1.55f;

    private static final float CENTER_CLOUDS_INITIAL_SCALE = 0.8f;
    private static final float CENTER_CLOUDS_FINAL_SCALE = 1.30f;

    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    public static final int LOADING_ANIMATION_COEFFICIENT = 80;
    public static final int SLOW_DOWN_ANIMATION_COEFFICIENT = 6;

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Matrix mAdditionalMatrix;
    private Animation mAnimation;
    private Paint mWinterPaint;

    private int mTop;
    private int mScreenWidth;
    private boolean mInverseDirection;

    //KEY: Y position, Value: X position
    private Map<Float, Float> mWinters;

    private int mJetWidthCenter;
    private int mJetHeightCenter;
    private float mJetTopOffset;

    private float mPercent = 0.0f;

    private Bitmap mJet;
    private Bitmap mFrontClouds;
    private Bitmap mLeftClouds;
    private Bitmap mRightClouds;

    private boolean isRefreshing = false;
    private boolean mEndOfRefreshing;
    private float mLoadingAnimationTime;
    private int mFrontCloudHeightCenter;
    private int mFrontCloudWidthCenter;
    private float mLineWidth;
    private float mLastAnimationTime;

    public JetRefreshView(Context context, PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();
        mAdditionalMatrix = new Matrix();
        mWinters = new HashMap<>();

        initiateDimens();
        createBitmaps();
        setupAnimations();
        mWinterPaint = new Paint();
        mWinterPaint.setColor(getContext().getResources().getColor(android.R.color.darker_gray));
        mWinterPaint.setStrokeWidth(10);
        mWinterPaint.setAlpha(125);
    }

    private void initiateDimens() {
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mJetTopOffset = mParent.getTotalDragDistance() * 0.5f;
        mTop = -mParent.getTotalDragDistance();
        mLineWidth = (float) (Math.random() * 100);
    }

    private void createBitmaps() {
        mLeftClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_left);
        mRightClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_right);
        mFrontClouds = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.clouds_center);

        mJet = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.airplane);
        mJetWidthCenter = mJet.getWidth() / 2;
        mJetHeightCenter = mJet.getHeight() / 2;
        mFrontCloudWidthCenter = mFrontClouds.getWidth() / 2;
        mFrontCloudHeightCenter = mFrontClouds.getHeight() / 2;
    }

    @Override
    public void setPercent(float percent, boolean invalidate) {
        setPercent(percent);
    }

    @Override
    public void offsetTopAndBottom(int offset) {
        mTop += offset;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        final int saveCount = canvas.save();

        // DRAW BACKGROUND.
        canvas.drawColor(getContext().getResources().getColor(R.color.jet_sky_background));

        if (isRefreshing) {
            while (mWinters.size() < 5) {
                float y = (float) (mParent.getTotalDragDistance() / (Math.random() * 10));
                float x = (float) (Math.random() * 500);
                mWinters.put(y, x);
                drawWinter(canvas, y, x);
            }
            if (mWinters.size() >= 5) {
                for (Map.Entry<Float, Float> winter : mWinters.entrySet()) {
                    drawWinter(canvas, winter.getKey(), winter.getValue());
                }
            }
            mLastAnimationTime = mLoadingAnimationTime;
        }
        drawJet(canvas);
        drawSideClouds(canvas);
        drawCenterClouds(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawWinter(Canvas canvas, float y, float xOffset) {
        float coef = mScreenWidth / (LOADING_ANIMATION_COEFFICIENT / SLOW_DOWN_ANIMATION_COEFFICIENT);
        float time = mLoadingAnimationTime;

        //HORRIBLE HACK FOR REVERS ANIMATION THAT SHOULD WORK LIKE RESTART ANIMATION
        if (mLastAnimationTime - mLoadingAnimationTime > 0) {
            mInverseDirection = true;
            time = LOADING_ANIMATION_COEFFICIENT - mLoadingAnimationTime;
//            mWinters.remove(y);
            return;
        }

        float x = (mScreenWidth - (time * coef)) + xOffset;
        float xEnd = x + mLineWidth;
        canvas.drawLine(x, y, xEnd, y, mWinterPaint);
    }

    private void drawSideClouds(Canvas canvas) {
        Matrix matrixLeftClouds = mMatrix;
        Matrix matrixRightClouds = mAdditionalMatrix;
        matrixLeftClouds.reset();
        matrixRightClouds.reset();

        float dragPercent = Math.min(1f, Math.abs(mPercent));

        boolean overdrag = false;

        if (mPercent > 1.0f) {
            overdrag = true;
        }

        float scale;
        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            scale = SIDE_CLOUDS_INITIAL_SCALE + (SIDE_CLOUDS_FINAL_SCALE - SIDE_CLOUDS_INITIAL_SCALE) * scalePercent;
        } else {
            scale = SIDE_CLOUDS_INITIAL_SCALE;
        }

        float dragYOffset = mParent.getTotalDragDistance() * (1.0f - dragPercent);
        int startParallaxHeight = mParent.getTotalDragDistance() / 2 - mLeftClouds.getHeight() / 2;
        boolean parallax = false;
        if (dragYOffset < startParallaxHeight) {
            parallax = true;
        }

        float offsetRightX = mScreenWidth - mRightClouds.getWidth();
        float offsetRightY = (parallax
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);

        float offsetLeftX = 0;
        float offsetLeftY = (parallax
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);


        if (isRefreshing) {
            if (isFirstLoadingAnimationPart()) {
                offsetLeftY += mLoadingAnimationTime / 4;
                offsetRightX -= mLoadingAnimationTime / 2;
            } else if (isSecondLoadingAnimationPart()) {
                offsetLeftY += getSecondPartAnimationValue() / 4;
                offsetRightX -= getSecondPartAnimationValue() / 2;
            } else if (isThirdLoadingAnimationPart()) {
                offsetLeftY -= getThirdAnimationPartValue() / 4;
                offsetRightX += getThirdAnimationPartValue() / 2;
            } else if (isFourthLoadingAnimationPart()) {
                offsetLeftY -= getFourthAnimationPartValue() / 2;
                offsetRightX += getFourthAnimationPartValue() / 4;
            }
        }

        matrixRightClouds.postScale(scale, scale, mRightClouds.getWidth() / 2, mRightClouds.getHeight() / 2);
        matrixRightClouds.postTranslate(offsetRightX, offsetRightY);

        matrixLeftClouds.postScale(scale, scale, mLeftClouds.getWidth() / 2, mLeftClouds.getHeight() / 2);
        matrixLeftClouds.postTranslate(offsetLeftX, offsetLeftY);

        canvas.drawBitmap(mLeftClouds, matrixLeftClouds, null);
        canvas.drawBitmap(mRightClouds, matrixRightClouds, null);
    }

    private void drawCenterClouds(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        float scale;
        float overdragPercent = 0;
        boolean overdrag = false;

        if (mPercent > 1.0f) {
            overdrag = true;
            overdragPercent = Math.abs(1.0f - mPercent);
        }

        float scalePercentDelta = dragPercent - SCALE_START_PERCENT;
        if (scalePercentDelta > 0) {
            float scalePercent = scalePercentDelta / (1.0f - SCALE_START_PERCENT);
            scale = CENTER_CLOUDS_INITIAL_SCALE + (CENTER_CLOUDS_FINAL_SCALE - CENTER_CLOUDS_INITIAL_SCALE) * scalePercent;
        } else {
            scale = CENTER_CLOUDS_INITIAL_SCALE;
        }

        float parallaxPercent = 0;
        boolean parallax = false;
        float dragYOffset = mParent.getTotalDragDistance() * dragPercent;
        int startParallaxHeight = mParent.getTotalDragDistance() - mFrontCloudHeightCenter;

        if (dragYOffset > startParallaxHeight) {
            parallax = true;
            parallaxPercent = dragYOffset - startParallaxHeight;
        }

        float offsetX = (mScreenWidth / 2) - mFrontCloudWidthCenter;
        float offsetY = dragYOffset
                - (parallax ? mFrontCloudHeightCenter + parallaxPercent : mFrontCloudHeightCenter)
                + (overdrag ? mTop : 0);

        float sx = overdrag ? scale + overdragPercent / 4 : scale;
        float sy = overdrag ? scale + overdragPercent / 2 : scale;

        if (isRefreshing && !overdrag) {
            if (isFirstLoadingAnimationPart()) {
                sx = scale - (mLoadingAnimationTime / LOADING_ANIMATION_COEFFICIENT) / 8;
                sy = sx;
            } else if (isSecondLoadingAnimationPart()) {
                sx = scale - (getSecondPartAnimationValue() / LOADING_ANIMATION_COEFFICIENT) / 8;
                sy = sx;
            } else if (isThirdLoadingAnimationPart()) {
                sx = scale + (getThirdAnimationPartValue() / LOADING_ANIMATION_COEFFICIENT) / 6;
                sy = sx;
            } else if (isFourthLoadingAnimationPart()) {
                sx = scale + (getFourthAnimationPartValue() / LOADING_ANIMATION_COEFFICIENT) / 6;
                sy = sx;
            }
        }
        matrix.postScale(sx, sy, mFrontCloudWidthCenter, mFrontCloudHeightCenter);
        matrix.postTranslate(offsetX, offsetY);

        canvas.drawBitmap(mFrontClouds, matrix, null);
    }

    private void drawJet(Canvas canvas) {
        Matrix matrix = mMatrix;
        matrix.reset();

        float dragPercent = mPercent;
        float rotateAngle = 0;

        // Check overdrag
        if (dragPercent > 1.0f && !mEndOfRefreshing) {
            rotateAngle = dragPercent % 1 * 10;
            dragPercent = 1.0f;
        }

        float offsetX = ((mScreenWidth * dragPercent) / 2) - mJetWidthCenter;

        float offsetY = mJetTopOffset
                + (mParent.getTotalDragDistance() / 2)
                * (1.0f - dragPercent)
                - mJetHeightCenter;

        if (isRefreshing) {
            if (isFirstLoadingAnimationPart()) {
                offsetY -= mLoadingAnimationTime;
            } else if (isSecondLoadingAnimationPart()) {
                offsetY -= getSecondPartAnimationValue();
            } else if (isThirdLoadingAnimationPart()) {
                offsetY += getThirdAnimationPartValue();
            } else if (isFourthLoadingAnimationPart()) {
                offsetY += getFourthAnimationPartValue();
            }
        }

        matrix.setTranslate(offsetX, offsetY);

        if (dragPercent == 1.0f) {
            matrix.preRotate(rotateAngle, mJetWidthCenter, mJetHeightCenter);
        }

        canvas.drawBitmap(mJet, matrix, null);
    }

    private float getFourthAnimationPartValue() {
        return getThirdTimeAnimationPart() - (mLoadingAnimationTime - getFourthTimeAnimationPart());
    }

    private float getThirdAnimationPartValue() {
        return mLoadingAnimationTime - getSecondTimeAnimationPart();
    }

    private int getSecondTimeAnimationPart() {
        return LOADING_ANIMATION_COEFFICIENT / 2;
    }

    private int getThirdTimeAnimationPart() {
        return getFourthTimeAnimationPart() * 3;
    }

    private boolean isFourthLoadingAnimationPart() {
        return mLoadingAnimationTime > getThirdTimeAnimationPart();
    }

    private boolean isThirdLoadingAnimationPart() {
        return mLoadingAnimationTime < getThirdTimeAnimationPart();
    }

    private boolean isSecondLoadingAnimationPart() {
        return mLoadingAnimationTime < getSecondTimeAnimationPart();
    }

    private float getSecondPartAnimationValue() {
        return getFourthTimeAnimationPart() - (mLoadingAnimationTime - getFourthTimeAnimationPart());
    }

    private boolean isFirstLoadingAnimationPart() {
        return mLoadingAnimationTime < getFourthTimeAnimationPart();
    }

    private int getFourthTimeAnimationPart() {
        return LOADING_ANIMATION_COEFFICIENT / 4;
    }

    public void setEndOfRefreshing(boolean endOfRefreshing) {
        mEndOfRefreshing = endOfRefreshing;
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void resetOriginals() {
        setPercent(0);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
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
        mEndOfRefreshing = false;
        resetOriginals();
    }

    private void setupAnimations() {
        mAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setLoadingAnimationTime(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(DECELERATE_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    private void setLoadingAnimationTime(float loadingAnimationTime) {
        /**SLOW DOWN ANIMATION IN {@link #SLOW_DOWN_ANIMATION_COEFFICIENT} time */
        mLoadingAnimationTime = LOADING_ANIMATION_COEFFICIENT * (loadingAnimationTime / SLOW_DOWN_ANIMATION_COEFFICIENT);
        invalidateSelf();
    }

}

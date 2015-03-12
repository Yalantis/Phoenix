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
import android.support.annotation.NonNull;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;

import com.yalantis.pulltorefresh.library.PullToRefreshView;
import com.yalantis.pulltorefresh.library.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    private static final Interpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    // Multiply with this animation interpolator time
    public static final int LOADING_ANIMATION_COEFFICIENT = 80;
    public static final int SLOW_DOWN_ANIMATION_COEFFICIENT = 6;
    // Amount of lines when is going lading animation
    public static final int WIND_SET_AMOUNT = 10;
    public static final int Y_SIDE_CLOUDS_SLOW_DOWN_COF = 4;
    public static final int X_SIDE_CLOUDS_SLOW_DOWN_COF = 2;
    public static final int MIN_WIND_LINE_WIDTH = 50;
    public static final int MAX_WIND_LINE_WIDTH = 300;
    public static final int MIN_WIND_X_OFFSET = 1000;
    public static final int MAX_WIND_X_OFFSET = 2000;
    public static final int RANDOM_Y_COEFFICIENT = 5;

    private PullToRefreshView mParent;
    private Matrix mMatrix;
    private Matrix mAdditionalMatrix;
    private Animation mAnimation;

    private int mTop;
    private int mScreenWidth;
    private boolean mInverseDirection;

    //KEY: Y position, Value: X offset of wind
    private Map<Float, Float> mWinds;
    private Paint mWindPaint;
    private float mWindLineWidth;
    private boolean mNewWindSet;

    private int mJetWidthCenter;
    private int mJetHeightCenter;
    private float mJetTopOffset;
    private int mFrontCloudHeightCenter;
    private int mFrontCloudWidthCenter;
    private int mRightCloudsWidthCenter;
    private int mRightCloudsHeightCenter;
    private int mLeftCloudsWidthCenter;
    private int mLeftCloudsHeightCenter;

    private float mPercent = 0.0f;

    private Bitmap mJet;
    private Bitmap mFrontClouds;
    private Bitmap mLeftClouds;
    private Bitmap mRightClouds;

    private boolean isRefreshing = false;
    private boolean mEndOfRefreshing;
    private float mLoadingAnimationTime;
    private float mLastAnimationTime;

    private Random mRandom;

    public JetRefreshView(Context context, PullToRefreshView parent) {
        super(context, parent);
        mParent = parent;
        mMatrix = new Matrix();
        mAdditionalMatrix = new Matrix();
        mWinds = new HashMap<>();
        mRandom = new Random();

        mWindPaint = new Paint();
        mWindPaint.setColor(getContext().getResources().getColor(android.R.color.white));
        mWindPaint.setStrokeWidth(3);
        mWindPaint.setAlpha(50);

        initiateDimens();
        createBitmaps();
        setupAnimations();
    }

    private void initiateDimens() {
        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        mJetTopOffset = mParent.getTotalDragDistance() * 0.5f;
        mTop = -mParent.getTotalDragDistance();
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

        mRightCloudsWidthCenter = mRightClouds.getWidth() / 2;
        mRightCloudsHeightCenter = mRightClouds.getHeight() / 2;
        mLeftCloudsWidthCenter = mLeftClouds.getWidth() / 2;
        mLeftCloudsHeightCenter = mLeftClouds.getHeight() / 2;
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
    public void draw(@NonNull Canvas canvas) {
        final int saveCount = canvas.save();

        // DRAW BACKGROUND.
        canvas.drawColor(getContext().getResources().getColor(R.color.jet_sky_background));

        if (isRefreshing) {
            // Set up new set of wind
            while (mWinds.size() < WIND_SET_AMOUNT) {
                float y = (float) (mParent.getTotalDragDistance() / (Math.random() * RANDOM_Y_COEFFICIENT));
                float x = random(MIN_WIND_X_OFFSET, MAX_WIND_X_OFFSET);

                // Magic with checking interval between winds
                if (mWinds.size() > 1) {
                    y = 0;
                    while (y == 0) {
                        float tmp = (float) (mParent.getTotalDragDistance() / (Math.random() * RANDOM_Y_COEFFICIENT));

                        for (Map.Entry<Float, Float> wind : mWinds.entrySet()) {
                            // We want that interval will be greater than fifth part of draggable distance
                            if (Math.abs(wind.getKey() - tmp) > mParent.getTotalDragDistance() / RANDOM_Y_COEFFICIENT) {
                                y = tmp;
                            } else {
                                y = 0;
                                break;
                            }
                        }
                    }
                }

                mWinds.put(y, x);
                drawWind(canvas, y, x);
            }

            // Draw current set of wind
            if (mWinds.size() >= WIND_SET_AMOUNT) {
                for (Map.Entry<Float, Float> wind : mWinds.entrySet()) {
                    drawWind(canvas, wind.getKey(), wind.getValue());
                }
            }

            // We should to create new set of winds
            if (mInverseDirection && mNewWindSet) {
                mWinds.clear();
                mNewWindSet = false;
                mWindLineWidth = random(MIN_WIND_LINE_WIDTH, MAX_WIND_LINE_WIDTH);
            }

            // needed for checking direction
            mLastAnimationTime = mLoadingAnimationTime;
        }

        drawJet(canvas);
        drawSideClouds(canvas);
        drawCenterClouds(canvas);

        canvas.restoreToCount(saveCount);
    }

    /**
     * Draw wind on loading animation
     *
     * @param canvas  - area where we will draw
     * @param y       - y position fot one of lines
     * @param xOffset - x offset for on of lines
     */
    private void drawWind(Canvas canvas, float y, float xOffset) {
        /* We should multiply current animation time with this coefficient for taking all screen width in time
        Removing slowing of animation with dividing on {@LINK #SLOW_DOWN_ANIMATION_COEFFICIENT}
        And we should don't forget about distance that should "fly" line that depend on screen of device and x offset
        */
        float cof = (mScreenWidth + xOffset) / (LOADING_ANIMATION_COEFFICIENT / SLOW_DOWN_ANIMATION_COEFFICIENT);
        float time = mLoadingAnimationTime;

        // HORRIBLE HACK FOR REVERS ANIMATION THAT SHOULD WORK LIKE RESTART ANIMATION
        if (mLastAnimationTime - mLoadingAnimationTime > 0) {
            mInverseDirection = true;
            // take time from 0 to end of animation time
            time = (LOADING_ANIMATION_COEFFICIENT / SLOW_DOWN_ANIMATION_COEFFICIENT) - mLoadingAnimationTime;
        } else {
            mNewWindSet = true;
            mInverseDirection = false;
        }

        // Taking current x position of drawing wind
        // For fully disappearing of line we should subtract wind line width
        float x = (mScreenWidth - (time * cof)) + xOffset - mWindLineWidth;
        float xEnd = x + mWindLineWidth;

        canvas.drawLine(x, y, xEnd, y, mWindPaint);
    }

    private void drawSideClouds(Canvas canvas) {
        Matrix matrixLeftClouds = mMatrix;
        Matrix matrixRightClouds = mAdditionalMatrix;
        matrixLeftClouds.reset();
        matrixRightClouds.reset();

        // Drag percent will newer get more then 1 here
        float dragPercent = Math.min(1f, Math.abs(mPercent));

        boolean overdrag = false;

        // But we check here for overdrag
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

        // Current y position of clouds
        float dragYOffset = mParent.getTotalDragDistance() * (1.0f - dragPercent);

        // Position where clouds fully visible on screen and we should drag them with content of listView
        int cloudsVisiblePosition = mParent.getTotalDragDistance() / 2 - mLeftCloudsHeightCenter;

        boolean needMoveCloudsWithContent = false;
        if (dragYOffset < cloudsVisiblePosition) {
            needMoveCloudsWithContent = true;
        }

        float offsetRightX = mScreenWidth - mRightClouds.getWidth();
        float offsetRightY = (needMoveCloudsWithContent
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);

        float offsetLeftX = 0;
        float offsetLeftY = (needMoveCloudsWithContent
                ? mParent.getTotalDragDistance() * dragPercent - mLeftClouds.getHeight()
                : dragYOffset)
                + (overdrag ? mTop : 0);

        // Magic with animation on loading process
        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.FIRST) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.FIRST) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                offsetLeftY += getAnimationPartValue(AnimationPart.SECOND) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX -= getAnimationPartValue(AnimationPart.SECOND) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.THIRD) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.THIRD) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                offsetLeftY -= getAnimationPartValue(AnimationPart.FOURTH) / X_SIDE_CLOUDS_SLOW_DOWN_COF;
                offsetRightX += getAnimationPartValue(AnimationPart.FOURTH) / Y_SIDE_CLOUDS_SLOW_DOWN_COF;
            }
        }

        matrixRightClouds.postScale(scale, scale, mRightCloudsWidthCenter, mRightCloudsHeightCenter);
        matrixRightClouds.postTranslate(offsetRightX, offsetRightY);

        matrixLeftClouds.postScale(scale, scale, mLeftCloudsWidthCenter, mLeftCloudsHeightCenter);
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
            // Here we want know about how mach percent of over drag we done
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
        // Current y position of clouds
        float dragYOffset = mParent.getTotalDragDistance() * dragPercent;
        // Position when should start parallax scrolling
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
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                sx = scale - (getAnimationPartValue(AnimationPart.FIRST) / LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                sx = scale - (getAnimationPartValue(AnimationPart.SECOND) / LOADING_ANIMATION_COEFFICIENT) / 8;
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                sx = scale + (getAnimationPartValue(AnimationPart.THIRD) / LOADING_ANIMATION_COEFFICIENT) / 6;
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                sx = scale + (getAnimationPartValue(AnimationPart.FOURTH) / LOADING_ANIMATION_COEFFICIENT) / 6;
            }
            sy = sx;
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
            rotateAngle = (dragPercent % 1) * 10;
            dragPercent = 1.0f;
        }

        float offsetX = ((mScreenWidth * dragPercent) / 2) - mJetWidthCenter;

        float offsetY = mJetTopOffset
                + (mParent.getTotalDragDistance() / 2)
                * (1.0f - dragPercent)
                - mJetHeightCenter;

        if (isRefreshing) {
            if (checkCurrentAnimationPart(AnimationPart.FIRST)) {
                offsetY -= getAnimationPartValue(AnimationPart.FIRST);
            } else if (checkCurrentAnimationPart(AnimationPart.SECOND)) {
                offsetY -= getAnimationPartValue(AnimationPart.SECOND);
            } else if (checkCurrentAnimationPart(AnimationPart.THIRD)) {
                offsetY += getAnimationPartValue(AnimationPart.THIRD);
            } else if (checkCurrentAnimationPart(AnimationPart.FOURTH)) {
                offsetY += getAnimationPartValue(AnimationPart.FOURTH);
            }
        }

        matrix.setTranslate(offsetX, offsetY);

        if (dragPercent == 1.0f) {
            matrix.preRotate(rotateAngle, mJetWidthCenter, mJetHeightCenter);
        }

        canvas.drawBitmap(mJet, matrix, null);
    }

    public float random(int min, int max) {

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return mRandom.nextInt((max - min) + 1) + min;
    }

    /**
     * We need a special value for different part of animation
     *
     * @param part - needed part
     * @return - value for needed part
     */
    private float getAnimationPartValue(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime;
            }
            case SECOND: {
                return getAnimationTimePart(AnimationPart.FOURTH) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            case THIRD: {
                return mLoadingAnimationTime - getAnimationTimePart(AnimationPart.SECOND);
            }
            case FOURTH: {
                return getAnimationTimePart(AnimationPart.THIRD) - (mLoadingAnimationTime - getAnimationTimePart(AnimationPart.FOURTH));
            }
            default:
                return 0;
        }
    }

    /**
     * On drawing we should check current part of animation
     *
     * @param part - needed part of animation
     * @return - return true if current part
     */
    private boolean checkCurrentAnimationPart(AnimationPart part) {
        switch (part) {
            case FIRST: {
                return mLoadingAnimationTime < getAnimationTimePart(AnimationPart.FOURTH);
            }
            case SECOND:
            case THIRD: {
                return mLoadingAnimationTime < getAnimationTimePart(part);
            }
            case FOURTH: {
                return mLoadingAnimationTime > getAnimationTimePart(AnimationPart.THIRD);
            }
            default:
                return false;
        }
    }

    /**
     * Get part of animation duration
     *
     * @param part - needed part of time
     * @return - interval of time
     */
    private int getAnimationTimePart(AnimationPart part) {
        switch (part) {
            case SECOND: {
                return LOADING_ANIMATION_COEFFICIENT / 2;
            }
            case THIRD: {
                return getAnimationTimePart(AnimationPart.FOURTH) * 3;
            }
            case FOURTH: {
                return LOADING_ANIMATION_COEFFICIENT / 4;
            }
            default:
                return 0;
        }
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public void resetOriginals() {
        setPercent(0);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
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
        mLastAnimationTime = 0;
        mWinds.clear();
        mWindLineWidth = random(MIN_WIND_LINE_WIDTH, MAX_WIND_LINE_WIDTH);
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
            public void applyTransformation(float interpolatedTime, @NonNull Transformation t) {
                setLoadingAnimationTime(interpolatedTime);
            }
        };
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);
        mAnimation.setDuration(ANIMATION_DURATION);
    }

    private void setLoadingAnimationTime(float loadingAnimationTime) {
        /**SLOW DOWN ANIMATION IN {@link #SLOW_DOWN_ANIMATION_COEFFICIENT} time */
        mLoadingAnimationTime = LOADING_ANIMATION_COEFFICIENT * (loadingAnimationTime / SLOW_DOWN_ANIMATION_COEFFICIENT);
        invalidateSelf();
    }

}

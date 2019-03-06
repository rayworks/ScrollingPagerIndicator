package ru.tinkoff.scrollingpagerindicator;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

public class SimpleDotsIndicator extends IndicatorView {
    private static final int ANIMATION_DURATION = 250;
    private int total;
    private int currentPosition = -1;
    private int previousPosition;

    private int selectedColor = Color.RED;
    private int normalColor = Color.CYAN;

    private int dotSpacing = 4;

    // in DP
    private int normalDotRadius = 2;
    private int selectedDotRadius = 4;
    private boolean selectedItemAnimMode;
    private Paint paint;

    private float currentRadiusUpdating;
    private float previousRadiusUpdating;

    private boolean dotsAnimated;
    private Paint highlightPaint;

    public SimpleDotsIndicator(Context context) {
        this(context, null);
    }

    public SimpleDotsIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleDotsIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public SimpleDotsIndicator setDotsAnimated(boolean dotsAnimated) {
        this.dotsAnimated = dotsAnimated;
        return this;
    }

    private void init(Context context, AttributeSet attrs) {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        highlightPaint = new Paint();
        highlightPaint.setAntiAlias(true);
        highlightPaint.setStyle(Paint.Style.FILL);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.SimpleDotsIndicator);

        normalDotRadius = attributes.getDimensionPixelSize(R.styleable.SimpleDotsIndicator_dotRadius,
                dpToPx(getContext(), 3));
        selectedDotRadius = attributes.getDimensionPixelSize(
                R.styleable.SimpleDotsIndicator_dotSelectedRadius,
                dpToPx(getContext(), 5));
        dotSpacing = attributes.getDimensionPixelSize(R.styleable.SimpleDotsIndicator_dotSpacing,
                dpToPx(getContext(), 3));

        selectedColor = attributes.getColor(R.styleable.SimpleDotsIndicator_dotSelectedColor, Color.BLACK);
        normalColor = attributes.getColor(R.styleable.SimpleDotsIndicator_dotColor, Color.BLACK);

        attributes.recycle();
    }

    @Override
    public void setDotCount(int count) {
        total = count;
        requestLayout();
    }

    @Override
    public void setCurrentPosition(int position) {
        if (position != currentPosition) {
            previousPosition = currentPosition;
            currentPosition = position;

            if (dotsAnimated) {
                animateActiveDot();
                animateInActiveDot();
            } else {
                invalidate();
            }
        }
    }

    @Override
    public void onPageScrolled(int page, float offset) {
        setCurrentPosition(page);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveWidth(widthMeasureSpec), resolveHeight(heightMeasureSpec));
    }

    // simple version
    public int resolveWidth(int measureSpec) {
        int width = 0;
        if (total > 0) {
            width = (total - 1) * normalDotRadius * 2 + selectedDotRadius * 2 +
                    total * 2 * dotSpacing;
        }

        return width;
    }

    private int resolveHeight(int measureSpec) {
        int padding = getPaddingTop(); // respect padding top
        int height = selectedDotRadius * 2 + padding * 2;
        return height;
    }

    private void animateActiveDot() {
        ValueAnimator animator = ValueAnimator.ofObject(new FloatEvaluator(), normalDotRadius,
                selectedDotRadius);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                currentRadiusUpdating = (Float) animation.getAnimatedValue();

                invalidate();
            }
        });
        animator.start();
    }

    private void animateInActiveDot() {
        if (previousPosition >= 0) {
            ValueAnimator animator = ValueAnimator.ofObject(new FloatEvaluator(), selectedDotRadius,
                    normalDotRadius);
            animator.setDuration(ANIMATION_DURATION);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    previousRadiusUpdating = (Float) animation.getAnimatedValue();

                    invalidate();
                }
            });
            animator.start();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int offsetX = 0;

        int width = getWidth();
        int paddingTop = getPaddingTop();

        for (int i = 0; i < total; i++) {
            offsetX += dotSpacing; // left padding

            int color = normalColor;
            int radius = normalDotRadius;

            if (i == currentPosition) {
                color = selectedColor;
                radius = selectedDotRadius;

                highlightPaint.setColor(color);
                if (dotsAnimated) {
                    canvas.drawCircle(offsetX + radius, selectedDotRadius + paddingTop,
                            currentRadiusUpdating, highlightPaint);
                } else {
                    canvas.drawCircle(offsetX + radius, selectedDotRadius + paddingTop,
                            selectedDotRadius, highlightPaint);
                }
            } else if (previousPosition == i) {
                paint.setColor(color);
                if (dotsAnimated) {
                    canvas.drawCircle(offsetX + radius, selectedDotRadius + paddingTop,
                            previousRadiusUpdating, paint);
                } else {
                    canvas.drawCircle(offsetX + radius, selectedDotRadius + paddingTop,
                            radius, paint);
                }
            } else {
                paint.setColor(color);
                canvas.drawCircle(offsetX + radius, selectedDotRadius + paddingTop, radius, paint);
            }

            offsetX += 2 * radius;
            offsetX += dotSpacing; // right padding
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        SavedState savedState = new SavedState(parcelable);
        savedState.currentIndex = currentPosition;
        savedState.size = total;

        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        currentPosition = savedState.currentIndex;
        total = savedState.size;

        requestLayout();
        invalidate();
    }

    static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        int currentIndex;
        int size;

        public SavedState(Parcelable source) {
            super(source);
        }

        public SavedState(Parcel source) {
            super(source);

            currentIndex = source.readInt();
            size = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(currentIndex);
            out.writeInt(size);
        }
    }
}

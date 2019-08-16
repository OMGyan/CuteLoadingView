package com.yx.cuteloadingview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

/**
 * Author by YX, Date on 2019/8/14.
 */
public class CuteLoadingView extends SurfaceView implements SurfaceHolder.Callback,Runnable{

    private enum LoadingState{ DOWN,UP,FREE }
    private LoadingState loadingState = LoadingState.DOWN;
    
    private Canvas canvas;
    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Path path;

    private int ballColor;
    private int lineColor;
    private int lineWidth;
    private int strokeWidth;

    private float downDistance;//下移距离
    private float upDistance;
    private float freeDownDistance;//自由落体偏移

    private ValueAnimator downControl;
    private ValueAnimator upControl;
    private ValueAnimator freeDownControl;
    private AnimatorSet animatorSet;
    private boolean isRunning;
    private boolean isAnimationShowing;

    public CuteLoadingView(Context context) {
        this(context,null);
    }

    public CuteLoadingView(Context context, AttributeSet attrs) {
        this(context,attrs,0);
    }

    public CuteLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        //初始化自定义属性
        initAttrs(context,attrs);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        path = new Path();
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        initControl();
    }

    private void initControl() {
        downControl = ValueAnimator.ofFloat(0,1);
        downControl.setDuration(450);
        downControl.setInterpolator(new DecelerateInterpolator());
        downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                downDistance = 70 * (float)valueAnimator.getAnimatedValue();
            }
        });
        downControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.DOWN;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        upControl = ValueAnimator.ofFloat(0,1);
        upControl.setDuration(450);
        upControl.setInterpolator(new ShockInterpolator());
        upControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                 upDistance = 70 * (float)valueAnimator.getAnimatedValue();
                if(upDistance>=70 && !freeDownControl.isStarted() && !freeDownControl.isRunning()){
                    freeDownControl.start();
                }
            }
        });
        upControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.UP;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        freeDownControl = ValueAnimator.ofFloat(0, (float) (2 * Math.sqrt(10)));
        freeDownControl.setDuration(550);
        freeDownControl.setInterpolator(new LinearInterpolator());
        freeDownControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float t = (float) valueAnimator.getAnimatedValue();
                freeDownDistance = (float) (10 * Math.sqrt(10)*t - 5*t*t);
            }
        });
        freeDownControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.FREE;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimationShowing = false;
                startAllAnimation();
            }
        });

        animatorSet = new AnimatorSet();
        animatorSet.play(downControl).before(upControl);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CuteLoadingView);
        ballColor = typedArray.getColor(R.styleable.CuteLoadingView_ball_color, Color.BLUE);
        lineColor = typedArray.getColor(R.styleable.CuteLoadingView_line_color,Color.BLUE);
        lineWidth = typedArray.getDimensionPixelOffset(R.styleable.CuteLoadingView_line_width,200);
        strokeWidth = typedArray.getDimensionPixelOffset(R.styleable.CuteLoadingView_stroke_width,2);
        typedArray.recycle();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        isRunning = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
       isRunning = false;
    }

    @Override
    public void run() {
       while (isRunning){
           drawView();
           try {
               Thread.sleep(20);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
    }

    private void drawView() {

        try {
           if(surfaceHolder!=null){
               canvas = surfaceHolder.lockCanvas();
               canvas.drawColor(Color.WHITE);

               paint.setColor(lineColor);
               path.reset();
               path.moveTo(getWidth()/2-lineWidth/2,getHeight()/2);
               if(loadingState == LoadingState.DOWN){
                   path.rQuadTo(lineWidth/2,2*downDistance,lineWidth,0);
                   paint.setColor(lineColor);
                   paint.setStyle(Paint.Style.STROKE);
                   canvas.drawPath(path,paint);

                   paint.setColor(ballColor);
                   paint.setStyle(Paint.Style.FILL);
                   canvas.drawCircle(getWidth()/2,getHeight()/2+downDistance-15-strokeWidth/2,15,paint);
               }else {
                   path.rQuadTo(lineWidth/2,2*(70-upDistance),lineWidth,0);
                   paint.setColor(lineColor);
                   paint.setStyle(Paint.Style.STROKE);
                   canvas.drawPath(path,paint);

                   paint.setColor(ballColor);
                   paint.setStyle(Paint.Style.FILL);
                   if(loadingState == LoadingState.FREE){
                       canvas.drawCircle(getWidth()/2,getHeight()/2-freeDownDistance-15-strokeWidth/2,15,paint);
                   }else {
                       canvas.drawCircle(getWidth()/2,getHeight()/2+(50-upDistance)-15-strokeWidth/2,15,paint);
                   }
               }
               paint.setColor(ballColor);
               paint.setStyle(Paint.Style.FILL);
               canvas.drawCircle(getWidth()/2-lineWidth/2,getHeight()/2,15,paint);
               canvas.drawCircle(getWidth()/2+lineWidth/2,getHeight()/2,15,paint);
           }
        }catch (Exception e){

        }finally {
            if(canvas!=null){
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

    }

    class ShockInterpolator implements Interpolator{
        @Override
        public float getInterpolation(float v) {
            return (float) (1-Math.exp(-3*v)*Math.cos(10*v));
        }
    }

    public void startAllAnimation() {
        if(isAnimationShowing){
            return;
        }
        if(animatorSet.isRunning()){
            animatorSet.end();
            animatorSet.cancel();
        }
        animatorSet.start();
    }
}

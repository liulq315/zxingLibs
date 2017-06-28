package org.zxing.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.google.zxing.ResultPoint;

import org.zxing.camera.CameraManager;
import org.lqos.zxings.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 该视图是覆盖在相机的预览视图之上的一层视图。扫描区构成原理，其实是在预览视图上画四块遮罩层，
 * 中间留下的部分保持透明，并画上一条激光线，实际上该线条就是展示而已，与扫描功能没有任何关系。
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends AbViewfinderView {


    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192,
            128, 64};
    /**
     * 刷新界面的时间
     */
    private static final long ANIMATION_DELAY = 5L;
    private static final int OPAQUE = 0xFF;

    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private int i = 0;// 添加的
    private int scannerAlpha;

    /**
     * 画笔对象的引用
     */
    private Paint paint;


    private static final int MAX_RESULT_POINTS = 20;

    private Bitmap resultBitmap;
//    private Drawable lineDrawable;// 采用图片作为扫描线
    /**
     * 遮掩层的颜色
     */
    private final int maskColor;

    private final int resultPointColor;
    private List<ResultPoint> possibleResultPoints;

    private List<ResultPoint> lastPossibleResultPoints;

    /**
     * 第一次绘制控件
     */
    boolean isFirst = true;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); // 开启反锯齿
        scannerAlpha = 0;
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask); // 遮掩层颜色
        resultPointColor = resources.getColor(R.color.possible_result_points);
        possibleResultPoints = new ArrayList<ResultPoint>(5);
        lastPossibleResultPoints = null;

    }


    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }

        // 绘制遮掩层
        drawCover(canvas, frame);
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // 画扫描框外部的暗色背景
        // 设置蒙板颜色
        paint.setColor(maskColor);
        // 头部
        canvas.drawRect(0, 0, width, frame.top, paint);
        // 左边
        canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
        // 右边
        canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
        // 底部
        canvas.drawRect(0, frame.bottom, width, height, paint);

        if (resultBitmap != null) {
            // 在扫描框中画出预览图
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            drawRectEdges(canvas, frame);
            drawScanningLine(canvas, frame);

            List<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top
                            + point.getY(), 3.0f, paint);
                }
            }

            // 只刷新扫描框的内容，其他地方不刷新
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
                    frame.right, frame.bottom);

        }
    }

    /**
     * 绘制扫描线
     *
     * @param canvas
     * @param frame  扫描框
     */
    private void drawScanningLine(Canvas canvas, Rect frame) {

        // 初始化中间线滑动的最上边和最下边
        if (isFirst) {
            isFirst = false;
        }

        // 在扫描框中画出模拟扫描的线条
        paint.setColor(getResources().getColor(R.color.viewfinder_laser));
        // 设置绿色线条的透明值
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        // 透明度变化
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        paint.setStrokeWidth((dip2px(getContext(), 2)));
        // 将扫描线修改为上下走的线
        if ((i += 3) < frame.bottom - frame.top) {
            canvas.drawLine(frame.left + 3, frame.top + i - 6, frame.left + frame.width() - 3,
                    frame.top + i - 6, paint);
            invalidate();
        } else {
            i = 0;
        }
    }

    /**
     * 绘制遮掩层
     */
    private void drawCover(Canvas canvas, Rect frame) {

        // 获取屏幕的宽和高
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);

        // 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        // 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 描绘方形的四个角
     *
     * @param canvas
     * @param frame
     */
    private void drawRectEdges(Canvas canvas, Rect frame) {
        int length = 25;
        int crap = 5;

        // 画出四个角
        paint.setColor(getResources().getColor(R.color.viewfinder_laser));
        // 左上角
        canvas.drawRect(frame.left, frame.top, frame.left + length,
                frame.top + crap, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + crap,
                frame.top + length, paint);
        // 右上角
        canvas.drawRect(frame.right - length, frame.top, frame.right,
                frame.top + crap, paint);
        canvas.drawRect(frame.right - crap, frame.top, frame.right,
                frame.top + length, paint);
        // 左下角
        canvas.drawRect(frame.left, frame.bottom - crap, frame.left + length,
                frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - length, frame.left + crap,
                frame.bottom, paint);
        // 右下角
        canvas.drawRect(frame.right - length, frame.bottom - crap, frame.right,
                frame.bottom, paint);
        canvas.drawRect(frame.right - crap, frame.bottom - length, frame.right,
                frame.bottom, paint);
    }

    @Override
    public void drawViewfinder() {
        super.drawViewfinder();
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }


    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    @Override
    public void drawResultBitmap(Bitmap barcode) {
        super.drawResultBitmap(barcode);
        resultBitmap = barcode;
        invalidate();
    }


    @Override
    public void addPossibleResultPoint(ResultPoint point) {
        super.addPossibleResultPoint(point);
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}

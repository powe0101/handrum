package org.bitproject3.hansei.handrum;


import android.app.Activity;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

import org.bitproject3.hansei.handrum.Chord.Instrument;
import org.bitproject3.hansei.handrum.Resource.Resource;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColorDetector implements Runnable {

    // Lower and Upper bounds for range checking in HSV color space

    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25, 50, 50, 0);
    private Mat mSpectrum = new Mat();
    //private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
    private boolean findButton; //버튼을 찾으면 true

    Mat rgba = new Mat(); //연산할 소스
    int selectColor; //색을 선택.
    Instrument[] instruments;
    SoundPool soundPool; //빨강용
    SoundPool soundPool2; //파랑용
    // Cache
    private Mat mHsvMat = new Mat();
    private Mat mMask = new Mat();
    private Mat mDilatedMask = new Mat();
    private Mat mHierarchy = new Mat();
    private int streamId;
    private Scalar multiplyScalar = new Scalar(4, 4);

    public void setMainActivity(Activity mainActivity) {
        MainActivity = mainActivity;
    }

    private Activity MainActivity;
    private Button btn;

    @Override
    public void run() {
        while (true) //TODO 추후 Switch문으로 변경 예정 - G
        { //스레드가 처음 실행되면 무조건 무한 반복 하되.
            if (Resource.isPlay == Resource.THREAD_WAITING) {
                continue;
            }
            if (Resource.isPlay == Resource.THREAD_PROCESS) //isPlay가 트루일때만 재생
            {
                // Log.e("스레드", Thread.currentThread().getId()+"스레드 동작중");
                //  Log.e("스레드", String.valueOf(Thread.activeCount())+"                              ");
                process(rgba);
                Resource.isPlay = Resource.THREAD_WAITING;
            }
            if (Resource.isPlay == Resource.THREAD_EXIT) {
                Log.e("스레드", "스레드 종료");
                return;
            }
        }
    }

    public void setHsvColor(Scalar hsvColor) {

        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0] - mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0] + mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int) (maxH - minH), CvType.CV_8UC3);

        for ( int j = 0; j < maxH - minH; j++ ) {
            byte[] tmp = {(byte) (minH + j), (byte) 255, (byte) 255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
    } //입력받은 RGBA 값을 HSV로 변경

    public void MatrixTime(int delayTime) {
        long saveTime = System.currentTimeMillis();
        long currTime = 0;
        while (currTime - saveTime < delayTime) {
            currTime = System.currentTimeMillis();
        }
    }

    public void process(Mat rgbaImage) {
        Imgproc.cvtColor(rgbaImage, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL); //RGB를 HSV로 변경
        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);//HSV 범위 설정
        Imgproc.dilate(mMask, mDilatedMask, new Mat()); // 해당 변경된 이미지를 분석을 위해 팽창시킴

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>(); //이미지에서 찾은 관심색을 표시하기 위한 리스트

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        //mContours.clear();
        each = contours.iterator();
        double totalArea = mMinContourArea * maxArea;
        int sensitive = 0;

        while (each.hasNext()) {
            MatOfPoint contour = each.next();

            if (Imgproc.contourArea(contour) > totalArea) {
                Core.multiply(contour, multiplyScalar, contour);

                if (Resource.Sensitivity > contour.toArray().length)
                    sensitive = contour.toArray().length;
                else
                    sensitive = Resource.Sensitivity;

                for ( int j = 0; j < sensitive; ++ j ) {
                    Log.e("감도", sensitive + "");

                    for ( int i = 0; i < instruments.length; ++ i ) {
                        findButton = instruments[i].FindButton(contour.toArray()[j]);
                        if (! contours.isEmpty() && findButton&&!Resource.rubbishOn) {
                            btn = instruments[i].SetButton();
                            if (selectColor == Resource.COLOR_RED)
                                soundPool.play(instruments[i].uriRed, Resource.instrumentVolume[i], Resource.instrumentVolume[i], 0, 0, 1);
                            else
                                soundPool2.play(instruments[i].uriBlue, Resource.instrumentVolume[i], Resource.instrumentVolume[i], 0, 0, 1);

                            long downTime = SystemClock.uptimeMillis();
                            long eventTime = SystemClock.uptimeMillis();
                            Log.e("타임", downTime + "][" + eventTime + "][");
                            MotionEvent down_event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, 0, 0, 0);
                            MotionEvent up_event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, 0, 0, 0);

                            btn.dispatchTouchEvent(down_event);
                            try {
                                Thread.sleep(200);
                            } catch (Exception e) {
                            }
                            btn.dispatchTouchEvent(up_event);

                            instruments[i].isFind = false;
                            Resource.buttonCount = -1;
                            return;
                        }
                    }
                }
            }
        }
    }
}
package org.bitproject3.hansei.handrum;


import android.util.Log;

import org.bitproject3.hansei.handrum.Resource.Resource;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by G on 2016-01-21.
 */
public class ImageProcessing implements Runnable {
    public Mat temp = new Mat(); //원본 프레임을 임시로 저장하는 temp Mat
    private Mat calc = new Mat();//계산과정에서 사용하는 mat
    public Mat result = new Mat(); //계산이 끝난 결과 mat. results는 디텍터 객체로 보내진다.
    @Override
    public void run() {
        while(true)
        {
            if(Resource.isProcess == Resource.THREAD_WAITING)
            {
                continue;
            }
            if(Resource.isProcess == Resource.THREAD_PROCESS)
            {
             //   Log.e("스레드", Thread.currentThread().getId() + "스레드 동작중");
                Processing();
            }
            if(Resource.isProcess == Resource.THREAD_EXIT)
            {
                Log.e("스레드","스레드 종료");
                return;
            }
        }
    }

    public void Processing(){
        Imgproc.pyrDown(temp, calc);
        Imgproc.pyrDown(calc, calc);
        Imgproc.pyrDown(calc, calc);
        result = calc.clone();

        Resource.isSend = Resource.THREAD_PROCESS_END;//작업이 끝나면 Send의 Flag를 변경해서 디텍터객체에게 작업을 하도록 알림.
        Resource.isProcess = Resource.THREAD_WAITING;//작업이 끝났으므로 연산스레드를 대기상태로.
    }
}

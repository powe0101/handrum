package org.bitproject3.hansei.handrum;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.bitproject3.hansei.handrum.Activity.DrumThemeActivity;
import org.bitproject3.hansei.handrum.Activity.GuideActivity;
import org.bitproject3.hansei.handrum.Activity.MenuActivity;
import org.bitproject3.hansei.handrum.Activity.MetronomeActivity;
import org.bitproject3.hansei.handrum.Activity.layoutSettingActivity;
import org.bitproject3.hansei.handrum.Chord.Instrument;
import org.bitproject3.hansei.handrum.Chord.NoteCreate;
import org.bitproject3.hansei.handrum.Chord.NotePlay;
import org.bitproject3.hansei.handrum.Chord.NotePlayGuide;
import org.bitproject3.hansei.handrum.R.id;
import org.bitproject3.hansei.handrum.R.raw;
import org.bitproject3.hansei.handrum.Resource.RecycleUtils;
import org.bitproject3.hansei.handrum.Resource.Resource;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private String selectMode;//0111 근영프리모드 가이드모드 구분하기위한 string

    private int buttonID[] = new int[]{R.id.bassdrumBtn, R.id.snaredrumBtn, R.id.hightomBtn, R.id.lowtomBtn, R.id.hihatcymbalBtn, R.id.crashcymbalBtn, R.id.ridecymbalBtn};
    private int imageViewID[] = new int[]{R.id.bass, R.id.snare, R.id.high, R.id.low, R.id.hihat, R.id.crash, R.id.ride};


    private ColorDetector[] mDetectorArray = new ColorDetector[Resource.COLOR_COUNT]; //디텍터 객체 2개로 생성
    private Thread threads[] = new Thread[Resource.COLOR_COUNT]; //색감지를 위한 스레드 2개
    private ImageProcessing pc; // 이미지 축소 연산을 위한 스레드 클래스

    private Mat mRgba; //카메라의 현재 프레임을 RGB로 가져올 객체
    private CameraBridgeViewBase mOpenCvCameraView; // 카메라 기본 객체
    private Scalar[] colorHsvArray = {new Scalar(109, 160, 119, 255), new Scalar(173, 122, 253, 0)}; // 검출할 색 코드 등록

    private TextView bpmValue;//bpm설정에서의 textview
    private SeekBar tSeekBar;//bpm설정에서의 seekbar
    private int bpmTemp = Resource.SEEK_BAR_INITIAL_VALUE;

    //private LinearLayout scoreLinear;//노래목록 나오는 리니어
    private LinearLayout menuLinear;//기능 나오는 리니어

    private TextView changeThemeName;//테마 알려주는 텍스트

    private int clickDummyUri;//더미 사운드풀 uri
    private int[] clickUri = new int[Resource.BUTTON_MAX_COUNT];//0107근영 버튼클릭사운드풀uri
    public SoundPool[] pools = new SoundPool[Resource.COLOR_COUNT]; //private 확인
    public SoundPool[] clickPool = new SoundPool[Resource.BUTTON_MAX_COUNT];//0107근영 버튼클릭사운드풀
    public SoundPool clickDummyPool;//더미 사운드풀

    private NotePlayGuide GuideScore;//가이드 악보
    private NotePlay Score;//NotePlayClass 변수
    private NotePlay loopMusic;//NotePlayClass 변수

    private Button ScorePlayButton;//가이드 재생 중지 버튼
    private Button ScoreRepeatButton;//가이드 반복 버튼
    private Button playButton; //루프 가이드 재생 버튼
    private int requestCode = 1;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(Resource.TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMaxFrameSize(Resource.WIDTH, Resource.HEIGHT); //영상 이미지 가로 세로 지정.
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    //OpenCV 로드 콜백 함수

    public MainActivity() {
        Log.i(Resource.TAG, "Instantiated new " + this.getClass());
    } //디버깅용 코어 객체가 불러와졌는지 확인.

    /**
     * 엑티비티가 불러와지면 처음 호출되는 함수 (안드로이드 라이프사이클 참조)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        selectMode = intent.getStringExtra("MODE");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //항상 켜짐

        //프리모드이면 해당 레이아웃 호출.
        if (selectMode.equals("MODE_FREE")) {
            MatchLayoutForCameraView(R.layout.color_blob_detection_surface_view_freemode, R.id.color_blob_detection_activity_surface_view_freemode);
            FreeMode freeMode = new FreeMode(this);
        } else if (selectMode.equals("MODE_GUIDE")) {
            MatchLayoutForCameraView(R.layout.color_blob_detection_surface_view, R.id.color_blob_detection_activity_surface_view);
        }
        MakeLinearView();//리니어생성
        MakeTextView();//텍스트생성
        MakeButton();//버튼생성

        for ( int i = 0; i < Resource.BUTTON_MAX_COUNT; ++ i ) {
            clickPool[i] = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            clickUri[i] = clickPool[i].load(this, DrumThemeActivity.buttonSoundUri[i], 1);
        }
        //TODO Deprecated 확인
        loopMusic = NotePlay.Create("RockMusic");//악보 클래스 객체 생성-루프용
        // 16.1.14 현우 가이드라인 악보 등록
        if (selectMode.equals("MODE_GUIDE")) {

            Score = NotePlay.Create("Score");//악보 클래스 객체 생성-악보용
            GuideScore = NotePlayGuide.Create("Guide Score", this);
        }
        clickDummyPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);//더미 파일 재생하기 위한...
        clickDummyUri = clickDummyPool.load(String.valueOf(MainActivity.this), raw.dumy);

        Resource.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);//0107근영 진동 변수 생성자? 개념
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView(); // 화면을 끄면 뷰를 비활성화
        if (mDetectorArray[Resource.COLOR_RED] != null && mDetectorArray[Resource.COLOR_BLUE] != null)
            ReleaseThreads(); //객체가 존재하고 있으면. 엑티비티 종료이므로 스레드를 종료시킨다.
    }

    public void ReleaseThreads() {
        Resource.CheckScoreButton = false;
        Resource.isScore = false;
        Resource.isLoop = false;
        Resource.isPlay = Resource.THREAD_EXIT;
        Resource.isProcess = Resource.THREAD_EXIT;
        Resource.isSend = Resource.THREAD_EXIT;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (! OpenCVLoader.initDebug()) {
            Log.d(Resource.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(Resource.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        Resource.isPlay = Resource.THREAD_WAITING;
        Resource.isProcess = Resource.THREAD_WAITING;
    }

    public void onDestroy() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        Resource.rubbishOn=false;
        ReleaseThreads();
        ReleaseMetronome(); //가이드 모드 나가면 메트로놈 꺼짐.
        //TODO G - 메모리 초기화 코드
        RecycleUtils.recursiveRecycle(getWindow().getDecorView()); //현재 뷰를 가져와서 리사이클 호출
        System.gc(); // 시스템 gc 를 호출. 메모리 초기화
        super.onDestroy();
    }

    private void ReleaseMetronome() {
        if (MetronomeActivity.getMetroPool() != null) {
            MetronomeActivity.getMetroPool().stop(MetronomeActivity.getStreamID());
            MetronomeActivity.setIsMetroRun(false);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        for ( int i = 0; i < Resource.COLOR_COUNT; ++ i ) {
            mDetectorArray[i] = new ColorDetector(); // 색 검출 디텍터 객체
            mDetectorArray[i].setHsvColor(colorHsvArray[i]); //검출 할 색 HSV 정보 디텍터에 등록
            pools[i] = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }
        ///리사이클용 뷰
        mDetectorArray[Resource.COLOR_BLUE].setMainActivity(this);
        mDetectorArray[Resource.COLOR_RED].setMainActivity(this);


        for ( int i = 0; i < Resource.BUTTON_MAX_COUNT; ++ i ) {
            Resource.mInstrumentClass[i] = new Instrument((Button) findViewById(buttonID[i]),
                    pools[Resource.COLOR_RED].load(this, DrumThemeActivity.buttonSoundUri[i], 1),
                    pools[Resource.COLOR_BLUE].load(this, DrumThemeActivity.buttonSoundUri[i], 1), (ImageView) findViewById(imageViewID[i]));
            Resource.mInstrumentClass[i].SetButton().setOnTouchListener(mTouch);
            if (selectMode.equals("MODE_GUIDE"))//가이드 모드시 항상 소리나야하기 때문에
                Resource.mInstrumentClass[i].SetButton().setVisibility(View.VISIBLE);
        }
        if (! layoutSettingActivity.isCameraOn()) {
            mOpenCvCameraView.disableView();
            return;
        }//메서드로 빼면 스레드부분이 작동하기 때문에 일단 여기에 배치함.
        //TODO 수정 필요. 코드 함수화 진행할때 수정할 예정. -G
        //악기등록
        for ( int i = 0; i < Resource.COLOR_COUNT; ++ i ) {
            mDetectorArray[i].instruments = Resource.mInstrumentClass;
        }

        mDetectorArray[Resource.COLOR_RED].soundPool = pools[Resource.COLOR_RED];
        mDetectorArray[Resource.COLOR_BLUE].soundPool2 = pools[Resource.COLOR_BLUE];

        mDetectorArray[Resource.COLOR_RED].selectColor = Resource.COLOR_RED;
        mDetectorArray[Resource.COLOR_BLUE].selectColor = Resource.COLOR_BLUE;

        RunDetectingThread(Resource.COLOR_RED); //스레드를 실행하는 메서드
        RunDetectingThread(Resource.COLOR_BLUE);
        RunProcessingThread();

        mDetectorArray[Resource.COLOR_BLUE].setMainActivity(this);
    }

    public void RunDetectingThread(int SELECT_COLOR) {
        Runnable r = mDetectorArray[SELECT_COLOR];//스레드를 사용하기 위해 러너블 인터페이스 구현 해당 인터페이스는 색감지 객체를 가지고 있음.
        threads[SELECT_COLOR] = new Thread(r); //스레드 객체에 러너블 인터페이스를 넣어줌.
        threads[SELECT_COLOR].start(); //스레드 실행.
    }

    public void RunProcessingThread() {
        pc = new ImageProcessing();
        Runnable r = pc;
        Thread thread = new Thread(r);
        thread.start();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    } //카메라 종료시

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba(); //Mat
        pc.temp = mRgba;
        Resource.isProcess = Resource.THREAD_PROCESS; //이미지가 들어오면 연산을 진행한다.
        //연산과정에서 isSend가 1이면 연산이 끝났다는 의미.
        if (Resource.isSend == Resource.THREAD_PROCESS_END) {
            mDetectorArray[Resource.COLOR_BLUE].rgba = mDetectorArray[Resource.COLOR_RED].rgba = pc.result;
            Resource.isPlay = Resource.THREAD_PROCESS;
        }//isPlay를 여기서 해주는 이유는 수신된 이미지가 있을때만 스레드가 작동할 수 있도록.
        return mRgba; //return 을 해서 콜백으로 수신된 프레임 이미지를 뷰에 띄워줌.
    }

    View.OnTouchListener mTouch = new View.OnTouchListener() {//0112 근영 버튼 터치했을시 소리,진동
        private int buttonCount = - 1; //버튼 클릭 변수

        //TODO 변수명 바꿔야됌.
        @Override
        public synchronized boolean onTouch(View v, MotionEvent event) {

            for ( int i = 0; i < Resource.BUTTON_MAX_COUNT; ++ i ) {
                if (Resource.mInstrumentClass[i].GetButtonID() == v.getId()) {
                    buttonCount = i;
                    break;
                }
            }
            if (MotionEvent.ACTION_DOWN == event.getAction()) {
                if(Resource.rubbishOn) {
                    Resource.mInstrumentClass[buttonCount].SetImageView().setVisibility(View.INVISIBLE);
                    Resource.mInstrumentClass[buttonCount].SetButton().setVisibility(View.INVISIBLE);
                    return false;
                }
                Log.e("빛"," 들어옴");

                //TODO make by yun 프리모드 조건문 투명도 0.3인 상태에서 클릭하면 사라짐 -0113


                if (!Resource.mInstrumentClass[buttonCount].isFind)
                    clickPool[buttonCount].play(clickUri[buttonCount], Resource.instrumentVolume[buttonCount], Resource.instrumentVolume[buttonCount], 0, 0, 1);

                Resource.vibrator.vibrate(Resource.VIBRATE);//0107 근영 진동//
                Resource.mInstrumentClass[buttonCount].SetButton().setAlpha((float) 0.4);
                //TODO 근영 투명도 조절
                //Resource.mInstrumentClass[buttonCount].SetImageView().setAlpha((float) 0.2);
            } else if (MotionEvent.ACTION_UP == event.getAction()) {
                if(Resource.rubbishOn) {
                    return false;
                }
                Resource.mInstrumentClass[buttonCount].SetButton().setAlpha(0);
                Resource.buttonCount = buttonCount;//16-01.29 추가 어떤 악기를 손으로 클릭했는지 정보 담기
                //Resource.mInstrumentClass[buttonCount].SetImageView().setAlpha((float) 0.6);
            }

            System.gc();
            return false;
        }
    };

    public void MatchLayoutForCameraView(int layout, int id) { //Match Layout for Layout surface
        setContentView(layout); //불러올 XML 레이아웃 설정
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(id);
        mOpenCvCameraView.enableFpsMeter(); //FPS 표시 설정
        mOpenCvCameraView.setCvCameraViewListener(this); // 리스너 등록
    }

    //ToDo 근영 노래 시작 버튼
    public void onClickPlayMusic(View view) {

        if (Resource.LoopCheckActivity) {//재생할 노래가 있을 시만 노래 재생... 없으면 노래 재생 하지 않는다.
            if (! Resource.isLoop) //TODO 0128 Modify 근영 버튼 하나로 통합 loop 재생
            {
                playButton.setBackgroundResource(R.drawable.stop);
                Resource.isLoop = true;//루프 무한 재생

                loopMusic.Delete();//이전 노래 삭제
                loopMusic.SetBpm(Resource.bpm);//bpm설정
                NoteCreateLoop(loopMusic);//악보 생성
                Runnable r = loopMusic;
                Thread musicThread = new Thread(r);//새로운 쓰레드 생성
                musicThread.start();//시작.
            } else if (Resource.isLoop) //TODO 0128 Modify 근영 버튼 하나로 통합 loop 중지
            {
                playButton.setBackgroundResource(R.drawable.play);
                Resource.isLoop = false;//노래 중지변수

            }
        }
    }

    //16.01.27 현우 악보 반복버튼 클릭시 발생하는 이벤트 - 악보로 생성된 노래 무한재생
    public void onClickRepeatMusicScore(View view) {
        if (Resource.CheckScoreButton) {//재생할 노래가 있을 시만 노래 재생... 없으면 노래 재생 하지 않는다.
            if (! Resource.isScore) //TODO 0128 Modify 근영 버튼 하나로 통합 Score반복 재생
            {
                view.setVisibility(View.INVISIBLE);
                ScorePlayButton.setBackgroundResource(R.drawable.stop);
                Resource.isScore = true;
                Score.Delete();//이전 노래 삭제
                Score.SetBpm(Resource.bpm);//bpm받아옴
                NoteCreateScore(Score);//악보생성
                Runnable r = Score;
                Thread musicThread = new Thread(r);//쓰레드 생성
                musicThread.start();//쓰레드 시작
            } else if (Resource.isScore)//TODO 0128 Modify 근영 버튼 하나로 통합 Score반복 중지
            {
                ScorePlayButton.setVisibility(View.VISIBLE);
                view.setBackgroundResource(R.drawable.repeat);
                Resource.isScore = false;
            }
        }
    }

    //16.01.27 현우악보 재생버튼 클릭시 발생하는 이벤트 - 악보로 생성된 노래 한번 재생후 사용자가 드럼채로 올바르게 칠때마다 넘어감
    public void onClickPlayMusicScore(View view) {
        if (Resource.CheckScoreButton) {//재생할 노래가 있을 시만 노래 재생... 없으면 노래 재생 하지 않는다.
            Resource.guidePlay = true;
            if (! Resource.isScore) //TODO 0128 Modify 근영 버튼 하나로 통합 Score 가이드 재생
            {
                ScoreRepeatButton.setVisibility(View.INVISIBLE);
                view.setBackgroundResource(R.drawable.stop);
                Resource.isScore = true;
                GuideScore.Delete();//이전 노래 삭제
                GuideScore.SetBpm(Resource.bpm);//bpm받아옴
                NoteCreateScore(GuideScore);//악보생성
                Runnable r = GuideScore;
                Thread musicThread = new Thread(r);//쓰레드 생성
                musicThread.start();//쓰레드 시작
            } else if (Resource.isScore)//TODO 0128 Modify 근영 버튼 하나로 통합 Score 가이드 중지
            {
                ScoreRepeatButton.setVisibility(View.VISIBLE);
                view.setBackgroundResource(R.drawable.play);
                Resource.isScore = false;
            }
        }
    }

    public void onClickMenubtn(View view) {//오른쪽하단 메뉴버튼 클릭시
        if (menuLinear.getVisibility() == View.GONE) {
            menuLinear.setVisibility(View.VISIBLE);
            changeThemeName.setVisibility(View.VISIBLE);
            return;
        }
        if (menuLinear.getVisibility() == View.VISIBLE) {
            menuLinear.setVisibility(View.GONE);
            changeThemeName.setVisibility(View.GONE);
            if ((selectMode.equals("MODE_GUIDE"))) {
                //scoreLinear.setVisibility(View.GONE);
            }
        }
    }

    public void onClickMusic(View view) {
        playButton.setVisibility(View.GONE); // 루프 재생버튼 숨김
        Intent newIntent = new Intent(getApplicationContext(), GuideActivity.class);
        startActivityForResult(newIntent, requestCode);//intend 실행
    }

    // 현재 액티비티의 onActivityResult 이벤트 핸들러
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Resource.isPlay = Resource.THREAD_WAITING;
        Resource.isProcess = Resource.THREAD_WAITING;
        if (requestCode == 1) // 액티비티 식별코드
        {
            if (resultCode == RESULT_CANCELED) { // 취소 일떄
                return;
            } else {
                if (data.getAction() == "1") { // 악보를 선택했을때
                    ScoreRepeatButton.setVisibility(View.VISIBLE);
                    ScorePlayButton.setVisibility(View.VISIBLE);
                    Resource.guidePlay = false;
                    if (ScorePlayButton.getVisibility() == View.VISIBLE) {
                        ScorePlayButton.setBackgroundResource(R.drawable.play);
                    }
                } else
                    return;
            }
        }
    }

    public void onClickMetronome(View view) { //메트로놈 선택시
        if ((selectMode.equals("MODE_GUIDE"))) {
            ScoreRepeatButton.setVisibility(View.GONE);
            ScorePlayButton.setVisibility(View.GONE);
        }
        playButton.setVisibility(View.GONE);

        Resource.isLoop = false;//노래중지
        Resource.LoopCheckActivity = false;//ColorBlobDetection 인지 아니면 루프인지 구분
        Intent newIntent = new Intent(getApplicationContext(), MetronomeActivity.class);
        startActivity(newIntent);
    }

    public void onClickBpm(View view) {//bpm선택시
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(this);
        final View bpmView = getLayoutInflater().inflate(R.layout.bpm, null);//bpmlayouy얻기 위한 객체
        bpmView.setBackgroundResource(R.drawable.background);
        alert_confirm.setView(bpmView); //위에서 inflater가 만든 dialogView 객체 세팅 (Customize)

        tSeekBar = (SeekBar) bpmView.findViewById(id.bpmseekbar1);//seekbar 아이디 매칭
        tSeekBar.setMax(Resource.SEEK_BAR_MAX);//SeekBar 최대값은 180;
        tSeekBar.setProgress(Resource.bpm - Resource.SEEK_BAR_MIN);//seekBar 최초값은 120;
        tSeekBar.setOnSeekBarChangeListener(mSeekBar); //SeekBar 변화 이벤트

        bpmValue = (TextView) bpmView.findViewById(id.bpmValue);// 텍스트 아이디매칭
        bpmValue.setText(Integer.toString(Resource.bpm));

        alert_confirm.setCancelable(false).setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Resource.bpm = bpmTemp;
                        Toast.makeText(MainActivity.this,  "확인 버튼을 누르셨습니다.", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "취소버튼을 누르셨습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }

    public void onClickLoop(View view) { //루프기능 선택시
        playButton.setBackgroundResource(R.drawable.play);
        if ((selectMode.equals("MODE_GUIDE"))) {
            ScoreRepeatButton.setVisibility(View.GONE);
            ScorePlayButton.setVisibility(View.GONE);
        }
        playButton.setVisibility(View.VISIBLE);
        Resource.isLoop = false;//노래중지
        Resource.isScore = false;
        Resource.LoopCheckActivity = false;//ColorBlobDetection 인지 아니면 루프인지 구분
        Intent newIntent = new Intent(getApplicationContext(), NoteCreate.class);
        startActivity(newIntent);
    }

    public void onClickBack(View view) {
        Intent newIntent = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(newIntent);
        Resource.isLoop = false;//노래 중지변수
        finish();
    }

    //bpm설정에서 seekbar이벤트
    SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            // EditText -> TextView Change
            bpmTemp = progress + Resource.SEEK_BAR_MIN;
            bpmValue.setText(Integer.toString(bpmTemp));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }//필요 없음 seekbar눌렀을시

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }//필요 없음 seekbar up했을시
    };

    //메뉴리니어와 가이드 리니어 xml연동시키는 메서드
    private void MakeLinearView() {
        menuLinear = (LinearLayout) findViewById(id.menuLinear);
        menuLinear.setVisibility(View.GONE);
        if ((selectMode.equals("MODE_GUIDE"))) {
            //scoreLinear = (LinearLayout) findViewById(id.scoreLinear);
        } else if (selectMode.equals("MODE_FREE")) {
            Button button = (Button) findViewById(id.music);
            button.setVisibility(View.GONE);
        }
    }

        //재생버튼 정지버튼등 버튼 만드는 메서드
    private void MakeButton() {
        ScoreRepeatButton = (Button) findViewById(id.scorerepeatbutton);//악보 반복버튼
        ScorePlayButton = (Button) findViewById(id.scoreplaybutton);//악보 재생버튼

        playButton = (Button) findViewById(id.playButton);
    }

    //메뉴 클릭 시 테마 이름이 보이도록 텍스트뷰 만드는 메서드 2016-01-27 _KwangHyun
    private void MakeTextView() {
        changeThemeName = (TextView) findViewById(id.themeName);
        changeThemeName.setText(Resource.THEME_NAME);
    }

    //16.01.16 현우 루프 생성하는 메서드
    private void NoteCreateLoop(NotePlay Score) { //RockMusicScore 에서 변경
        for ( int i = 0; i < Resource.loopScore.length; ++ i ) {
            if (Resource.loopScore[i] == - 1) {//빈공간 즉 쉼표 같은 개념?일때 더미파일 재생
                Score.Add(8, clickDummyPool, clickDummyUri);
            } else {//악기 재생할때
                Score.Add(8, clickPool[Resource.loopScore[i]], clickUri[Resource.loopScore[i]]);
            }
        }
    }

    //16.01.27 현우 악보 생성하는 메서드
    private void NoteCreateScore(NotePlay Score) { //RockMusicScore 에서 변경
        for ( int i = 0; i < Resource.scoreNote.length; ++ i ) {
            if (Resource.scoreNote[i] == - 1) {//빈공간 즉 쉼표 같은 개념?일때 더미파일 재생
                Score.Add(8, clickDummyPool, clickDummyUri);
            } else//악기 재생할때{
                Score.Add(8, clickPool[Resource.scoreNote[i]], clickUri[Resource.scoreNote[i]]);
        }
    }

    // 하드웨어 뒤로가기버튼 이벤트 설정.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            //하드웨어 뒤로가기 버튼에 따른 이벤트 설정
            case KeyEvent.KEYCODE_BACK:

                Toast.makeText(this, "뒤로가기버튼 눌림", Toast.LENGTH_SHORT).show();

                new AlertDialog.Builder(this)
                        .setTitle("프로그램 종료")
                        .setMessage("프로그램을 종료 하시겠습니까?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 프로세스 종료.
                                finish();
                            }
                        })
                        .setNegativeButton("아니오", null)
                        .show();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://org.bitproject3.hansei.handrum/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://org.bitproject3.hansei.handrum/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

}
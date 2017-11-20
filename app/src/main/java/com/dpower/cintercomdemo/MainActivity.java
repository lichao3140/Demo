package com.dpower.cintercomdemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dpower.dpsiplib.CallConfig;
import com.dpower.dpsiplib.SipClient;
import com.dpower.dpsiplib.SipUser;
import com.dpower.dpsiplib.message.CIMessageAdapter;
import com.dpower.utils.*;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.Intents;

import java.io.File;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{
    private TextView mTvLogin = null; // 登录（登出）
    private TextView mTvBind = null; // 绑定（解绑）
    private TextView mTvDeviceNote = null; // 设备备注
    private SurfaceView mSurfaceView = null; // 视频
    private Button mBtnAccept = null; // 接听（挂断）
    private Button mBtnSnapshot = null; // 截图
    private Button mBtnOpenLock = null; // 开锁
    private Button mBtnSpeaker = null; // 听筒（扬声器）
    private Button mBtnMicrophone = null; // 麦克风

    private boolean isAcceptMode = false; // 是否接听状态
    private boolean isSpeakerTurnOn = true; // 是否开启扬声器
    private boolean isMicrophoneTurnOn = true; // 是否开启麦克风

    private static final int NO_CALL = 0x1001; // 无通话
    private static final int CALL_OUT = 0x1002; // 呼叫
    private static final int CALL_IN = 0x1003; // 呼入
    private static final int CALLING = 0x1004; // 通话中

    public static final int REQUEST_CODE = 0x0ba7c0de;

    private SipClient mSipClient = null;
    private CallConfig mCallConfig = null;

    private String mUsername = null; // 账号
    private String mServerAddress = null; // 服务器地址
    private Device mDevice = null; // 设备
    private String mRoomNumber = null; // 房号
    private String mId = null; // 设备编号
    private String mNote = null; // 设备备注
    private String mAccount = null; // 设备账号

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mSipClient = SipClient.getInstance();
        mSipClient.setHandler(mHandler);
        mSipClient.init_sip(this);
        mUsername = MyUtil.getUsername(MainActivity.this);
        mServerAddress = SipClient.DP_SERVER_IP;
        initView();
    }

    private void initView() {
        mTvLogin = (TextView) findViewById(R.id.main_tv_login);
        mTvBind = (TextView) findViewById(R.id.main_tv_bind);
        mTvDeviceNote = (TextView) findViewById(R.id.main_tv_devicenote);
        mSurfaceView = (SurfaceView) findViewById(R.id.main_surface_video);
        mBtnAccept = (Button) findViewById(R.id.main_btn_accept);
        mBtnSnapshot = (Button) findViewById(R.id.main_btn_snapshot);
        mBtnOpenLock = (Button) findViewById(R.id.main_btn_openlock);
        mBtnSpeaker = (Button) findViewById(R.id.main_btn_speaker);
        mBtnMicrophone = (Button) findViewById(R.id.main_btn_microphone);

        mTvLogin.setOnClickListener(this);
        mTvBind.setOnClickListener(this);
        mBtnAccept.setOnClickListener(this);
        mBtnSnapshot.setOnClickListener(this);
        mBtnOpenLock.setOnClickListener(this);
        mBtnSpeaker.setOnClickListener(this);
        mBtnMicrophone.setOnClickListener(this);

        setViewStatus(NO_CALL);
        mCallConfig = new CallConfig(this);
        mCallConfig.setSurfaceView(mSurfaceView);

        // 设置SurfaceView大小
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        android.view.ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = dm.widthPixels;
        lp.height =  lp.width * 3 / 4;
        mSurfaceView.setLayoutParams(lp);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_tv_login: // 登录（登出）
                if (!mSipClient.isLogin()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String serverIP = MyUtil.getServerIP(mServerAddress);
                            if (mUsername != null && serverIP != null) {
                                mSipClient.login(new SipUser(mUsername, null, SipUser.TYPE_PHONE), serverIP);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Progress.showProgress(MainActivity.this);
                                    }
                                });
                            }
                        }
                    }).start();
                } else {
                    mSipClient.logout();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Progress.showProgress(MainActivity.this);
                        }
                    });
                }
                break;
            case R.id.main_tv_bind: // 绑定（解绑）
                if (!mSipClient.isLogin()) {
                    Toast.makeText(MainActivity.this, getString(R.string.main_no_login), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mDevice == null) {
                    Intent intentScan = new Intent(this, CaptureActivity.class);
                    intentScan.setAction(Intents.Scan.ACTION);
                    intentScan.addCategory(Intent.CATEGORY_DEFAULT);

                    intentScan.putExtra(Intents.Scan.CAMERA_ID, 0);
                    intentScan.putExtra(Intents.Scan.SAVE_HISTORY, false);
                    intentScan.putExtra(Intents.Scan.RESULT_DISPLAY_DURATION_MS, 0);
                    intentScan.putExtra(Intents.Scan.FORMATS, "QR_CODE,PDF_417");
                    intentScan.putExtra(Intents.Scan.PROMPT_MESSAGE, getString(R.string.main_qrcode_hint));

                    intentScan.setPackage(getApplicationContext().getPackageName());
                    startActivityForResult(intentScan, REQUEST_CODE);
                } else {
                    if (mSipClient.isLogin()) {
                        mSipClient.unbind(mDevice.getAccount(), mDevice.getRoomNumber());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Progress.showProgress(MainActivity.this);
                            }
                        });
                    }
                }
                break;
            case R.id.main_btn_accept: // 接听（挂断）
                if (!mSipClient.isLogin()) {
                    Toast.makeText(MainActivity.this, getString(R.string.main_no_login), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isAcceptMode) {
                    if (mSipClient.isOnRing()) {
                        mSipClient.accept(mCallConfig);
                    } else {
                        if (mDevice == null) {
                            Toast.makeText(MainActivity.this, getString(R.string.main_no_bind), Toast.LENGTH_SHORT).show();
                        } else {
                            mCallConfig.mRemoteUser = new SipUser(mDevice.getAccount(), null, SipUser.TYPE_PHONE);
                            mSipClient.callout(mCallConfig);
                        }
                    }
                } else {
                    mSipClient.hangup();
                }
                break;
            case R.id.main_btn_snapshot: // 截图
                String remoteAccount = null;
                if (mSipClient.isOnRing() && mSipClient.isOnCall()) {
                    remoteAccount = mSipClient.getRemoteAccount();
                } else {
                    remoteAccount = mCallConfig.mRemoteUser.getName();
                }
                String imagePath = MyUtil.getImageName(remoteAccount);
                if (mSipClient.requestCaptureOnVideoStream((imagePath))) {
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri uri = Uri.fromFile(new File(imagePath));
                    intent.setData(uri);
                    sendBroadcast(intent);
                    Toast.makeText(MainActivity.this, getString(R.string.main_pic_save) + imagePath, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.main_btn_openlock: // 开锁
                if (mSipClient.isOnRing() && mSipClient.isOnCall()) {
                    remoteAccount = mSipClient.getRemoteAccount();
                } else {
                    remoteAccount = mCallConfig.mRemoteUser.getName();
                }
                mSipClient.openlock(remoteAccount);
                break;
            case R.id.main_btn_speaker: // 扬声器
                isSpeakerTurnOn = !isSpeakerTurnOn;
                setSpeakerTurnOn(isSpeakerTurnOn);
                mCallConfig.isSpeakerOn = isSpeakerTurnOn;
                mCallConfig.notifyDataSetChanged();
                break;
            case R.id.main_btn_microphone: // 麦克风
                isMicrophoneTurnOn = !isMicrophoneTurnOn;
                setMicrophoneTurnOn(isMicrophoneTurnOn);
                mCallConfig.isMicrophoneOn = isMicrophoneTurnOn;
                mCallConfig.notifyDataSetChanged();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) { // 扫描二维码返回结果
                String text = data.getStringExtra("SCAN_RESULT");
                List<DeviceInfoMod> deviceInfo = mSipClient.resolveQRCode(this, text);
                if (deviceInfo != null && deviceInfo.size() > 0) {
                    mRoomNumber = deviceInfo.get(0).getRoomNumber();
                    mId = deviceInfo.get(0).getDoorMachineNumber();
                    mNote = deviceInfo.get(0).getDevnote();
                    mAccount = deviceInfo.get(0).getDevacc();
                    Log.e("aa", "roomNumber=" + mRoomNumber + " id=" + mId + " account=" + mAccount);
                    if (mSipClient.isLogin()) {
                        if (mRoomNumber != null && mAccount != null) {
                            mSipClient.bind(mAccount, mRoomNumber);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Progress.showProgress(MainActivity.this);
                                }
                            });
                        }
                    }
                } else
                    Toast.makeText(MainActivity.this, getString(R.string.main_qrcode_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case SipClient.FLAG_STATE_ACCOUNT: // 登录状态
                    switch (msg.arg1) {
                        case SipClient.ACCOUNT_LOGIN_SUCCESS:
                            Log.e("aa", "FLAG_STATE_ACCOUNT ACCOUNT_LOGIN_SUCCESS " + msg.obj);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTvLogin.setText(R.string.main_logout);
                                    Toast.makeText(MainActivity.this, getString(R.string.main_login_success), Toast.LENGTH_SHORT).show();
                                    Progress.hideProgress();
                                }
                            });
                            break;
                        case SipClient.ACCOUNT_LOGIN_START:
                            Log.e("aa", "FLAG_STATE_ACCOUNT ACCOUNT_LOGIN_START " + msg.obj);
                            break;
                        case SipClient.ACCOUNT_LOGIN_FAIL:
                            Log.e("aa", "FLAG_STATE_ACCOUNT ACCOUNT_LOGIN_FAIL " + msg.obj);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTvLogin.setText(R.string.main_login);
                                    Toast.makeText(MainActivity.this, getString(R.string.main_login_fail), Toast.LENGTH_SHORT).show();
                                    Progress.hideProgress();
                                }
                            });
                            break;
                        case SipClient.ACCOUNT_LOGOUT:
                            Log.e("aa", "FLAG_STATE_ACCOUNT ACCOUNT_LOGOUT " + msg.obj);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTvLogin.setText(R.string.main_login);
                                    Progress.hideProgress();
                                }
                            });
                            break;
                    }
                    break;
                case SipClient.FLAG_STATE_CALL: // 呼叫状态
                    switch (msg.arg1) {
                        case SipClient.CALL_RING_CALLIN:
                            Log.e("aa", "FLAG_STATE_CALL CALL_RING CALL_RING_CALLIN");
                            setViewStatus(CALL_IN);
                            mTvDeviceNote.setText(getString(R.string.main_new_callin));

                            Intent intent = new Intent(getApplicationContext(),
                                    MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                            startActivity(intent);
                            break;
                        case SipClient.CALL_RING_CALLOUT:
                            Log.e("aa", "FLAG_STATE_CALL CALL_RING CALL_RING_CALLOUT");
                            setViewStatus(CALL_OUT);
                            break;
                        case SipClient.CALL_START:
                            Log.e("aa", "FLAG_STATE_CALL CALL_START");
                            isSpeakerTurnOn = true;
                            isMicrophoneTurnOn = true;
                            mCallConfig.isSpeakerOn = isSpeakerTurnOn;
                            mCallConfig.isMicrophoneOn = isMicrophoneTurnOn;
                            mCallConfig.notifyDataSetChanged(); // 初始化麦克风扬声器状态
                            setViewStatus(CALLING);

                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                            break;
                        case SipClient.CALL_BUSY:
                        case SipClient.CALL_FINISH:
                            Log.e("aa", "FLAG_STATE_CALL CALL_FINISH " + msg.obj);
                            setViewStatus(NO_CALL);
                            if (mDevice != null)
                                mTvDeviceNote.setText(mDevice.getNote());
                            else
                                mTvDeviceNote.setText("");

                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                            break;
                        case SipClient.CALL_UNKNOW:
                            Log.e("aa", "FLAG_STATE_CALL CALL_FINISH " + msg.obj);
                            break;
                    }
                    break;
                case CIMessageAdapter.FLAG_RESULT_BIND: // 绑定结果回调
                    Log.e("aa", "FLAG_RESULT_BIND " + msg.obj);
                    if (msg.arg2 == 1) {
                        mTvBind.setText(getString(R.string.main_unbind));
                        mTvDeviceNote.setText(mNote);
                        mDevice = new Device(mRoomNumber, mId, mNote, mAccount);
                        Toast.makeText(MainActivity.this, getString(R.string.main_bind_success), Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(MainActivity.this, getString(R.string.main_bind_fail) + msg.obj, Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Progress.hideProgress();
                        }
                    });
                    break;
                case CIMessageAdapter.FLAG_RESULT_UNBIND: // 解绑结果回调
                    Log.e("aa", "FLAG_RESULT_UNBIND");
                    if (msg.arg2 == 1) {
                        mTvBind.setText(getString(R.string.main_bind));
                        mTvDeviceNote.setText("");
                        mDevice = null;
                        Toast.makeText(MainActivity.this, getString(R.string.main_unbind_success), Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(MainActivity.this, getString(R.string.main_unbind_fail), Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Progress.hideProgress();
                        }
                    });
                    break;
                case CIMessageAdapter.FLAG_RESULT_OPENLOCK: // 开锁结果回调
                    Log.e("aa", "FLAG_RESULT_OPENLOCK");
                    if (msg.arg2 == 1) {
                        Toast.makeText(MainActivity.this, getString(R.string.main_openlock_success), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
            super.dispatchMessage(msg);
        }
    };

    /**
     * 设置界面状态
     * @param status
     */
    private void setViewStatus(int status) {
        switch(status) {
            case NO_CALL: // 无通话
                isAcceptMode = true;
                setAcceptMode(isAcceptMode);
                setSnapShotClickable(false);
                setOpenLockClickable(false);
                setSpeakerClickable(false);
                setMicrophoneClickable(false);
                mSurfaceView.setVisibility(View.GONE);
                mBtnAccept.setText(getString(R.string.main_callout));
                mTvLogin.setEnabled(true);
                mTvBind.setEnabled(true);
                break;
            case CALL_OUT: // 呼叫
                isAcceptMode = false;
                setAcceptMode(isAcceptMode);
                setSnapShotClickable(false);
                setOpenLockClickable(false);
                setSpeakerClickable(false);
                setMicrophoneClickable(false);
                mSurfaceView.setVisibility(View.GONE);
                mTvLogin.setEnabled(false);
                mTvBind.setEnabled(false);
                break;
            case CALL_IN: // 呼入
                isAcceptMode = true;
                setAcceptMode(isAcceptMode);
                setSnapShotClickable(false);
                setOpenLockClickable(false);
                setSpeakerClickable(false);
                setMicrophoneClickable(false);
                mSurfaceView.setVisibility(View.GONE);
                mTvLogin.setEnabled(false);
                mTvBind.setEnabled(false);
                break;
            case CALLING: // 通话中
                isAcceptMode = false;
                setAcceptMode(isAcceptMode);
                setSnapShotClickable(true);
                setOpenLockClickable(true);
                setSpeakerClickable(true);
                setMicrophoneClickable(true);
                mSurfaceView.setVisibility(View.VISIBLE);
                mTvLogin.setEnabled(false);
                mTvBind.setEnabled(false);
                break;
        }
    }

    /**
     * 设置接听按钮状态
     * @param isAcceptMode true 接听状态  false 挂断状态
     */
    private void setAcceptMode(boolean isAcceptMode) {
        this.isAcceptMode = isAcceptMode;
        if (isAcceptMode == true) {
            changeButtonIcon(mBtnAccept, R.mipmap.main_accept);
            mBtnAccept.setText(getString(R.string.main_accept));
        } else {
            changeButtonIcon(mBtnAccept, R.mipmap.main_hangup);
            mBtnAccept.setText(getString(R.string.main_hangup));
        }
    }

    /**
     * 设置截图按钮使能
     * @param clickable true 可用  false 不可用
     */
    private void setSnapShotClickable(boolean clickable) {
        mBtnSnapshot.setClickable(clickable);
        if (clickable) {
            changeButtonIcon(mBtnSnapshot, R.mipmap.main_snapshot);
        } else {
            changeButtonIcon(mBtnSnapshot, R.mipmap.main_snapshot_disabled);
        }
    }

    /**
     * 设置开锁按钮使能
     * @param clickable true 可用  false 不可用
     */
    private void setOpenLockClickable(boolean clickable) {
        mBtnOpenLock.setClickable(clickable);
        if (clickable) {
            changeButtonIcon(mBtnOpenLock, R.mipmap.main_openlock);
        } else {
            changeButtonIcon(mBtnOpenLock, R.mipmap.main_openlock_disabled);
        }
    }

    /**
     * 设置扬声器按钮使能
     * @param clickable true 可用  false 不可用
     */
    private void setSpeakerClickable(boolean clickable) {
        mBtnSpeaker.setClickable(clickable);
        if (clickable) {
            setSpeakerTurnOn(isSpeakerTurnOn);
        } else {
            changeButtonIcon(mBtnSpeaker, R.mipmap.main_speaker_disabled);
        }
    }

    /**
     * 设置麦克风按钮使能
     * @param clickable true 可用  false 不可用
     */
    private void setMicrophoneClickable(boolean clickable) {
        mBtnMicrophone.setClickable(clickable);
        if (clickable) {
            setMicrophoneTurnOn(isMicrophoneTurnOn);
        } else {
            changeButtonIcon(mBtnMicrophone, R.mipmap.main_microphone_disabled);
        }
    }

    /**
     * 设置扬声器
     * @param isSpeakerTurnOn true 开启  false 关闭
     */
    private void setSpeakerTurnOn(boolean isSpeakerTurnOn) {
        this.isSpeakerTurnOn = isSpeakerTurnOn;
        if (isSpeakerTurnOn) {
            changeButtonIcon(mBtnSpeaker, R.mipmap.main_speaker);
        } else {
            changeButtonIcon(mBtnSpeaker, R.mipmap.main_speaker_closed);
        }
    }

    /**
     * 设置麦克风
     * @param isMicrophoneTurnOn true 开启  false 关闭
     */
    private void setMicrophoneTurnOn(boolean isMicrophoneTurnOn) {
        this.isMicrophoneTurnOn = isMicrophoneTurnOn;
        if (isMicrophoneTurnOn) {
            changeButtonIcon(mBtnMicrophone, R.mipmap.main_microphone);
        } else {
            changeButtonIcon(mBtnMicrophone, R.mipmap.main_microphone_closed);
        }
    }

    /**
     * 改变按钮的图标
     * @param btn
     * @param drawableId
     */
    private void changeButtonIcon(Button btn, int drawableId) {
        Drawable drawable=this.getResources().getDrawable(drawableId);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        btn.setCompoundDrawables(null,drawable,null,null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSipClient.isOnCall())
            mSipClient.hangup();
    }
}
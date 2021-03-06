package com.microtechmd.pda.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.manager.ActivityPathManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentBase;
import com.microtechmd.pda.ui.activity.fragment.FragmentCalibration;
import com.microtechmd.pda.ui.activity.fragment.FragmentHome;
import com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer;
import com.microtechmd.pda.ui.widget.highlight.HighLight;

import java.util.Calendar;

import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_DATE_TIME;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.REALTIMEFLAG;


public class ActivityMain extends ActivityPDA {
    public static final String SETTING_STARTUP_TIME = "startup_time";
    public static final String FIRST_SETUP = "first_setup";

    public static final long STARTUP_DELAY = 1 * DateTime.SECOND_PER_MINUTE *
            DateTime.MILLISECOND_PER_SECOND / 10;

    private static History history = null;
    private FragmentHome mFragmentHome = null;
    private FragmentBase mFragmentCalibration = null;
    private FragmentBase mFragmentSettings = null;

    private Handler mHandlerTimer = null;
    private Handler mHandler = null;
    private Runnable mRunnableTimer = null;
    private long mStartupTime = 0;
    private RadioGroup radioGroup;
    private HighLight mHightLight;

    public static History getStatus() {
        return history;
    }


    public static void setStatus(History history) {
        if (history == null) {
            ActivityMain.history = null;
        } else {
            ActivityMain.history = new History(history.getByteArray());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
//            updateTimer();
        }
    }

    public void switchContent(FragmentBase to) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentHome).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentCalibration).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentSettings).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .show(to).commitAllowingStateLoss();
    }

    public void add(FragmentBase from) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.layout_fragment, from).commit();
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mHandler == null) {
            mHandler = new Handler();
        }
//        updateTimer();
        mFragmentHome = new FragmentHome();
        mFragmentCalibration = new FragmentCalibration();
        mFragmentSettings = new FragmentSettingContainer();

        add(mFragmentHome);
        add(mFragmentCalibration);
        add(mFragmentSettings);

        switchContent(mFragmentHome);

        radioGroup = (RadioGroup) findViewById(R.id.radio_group_tab);
        radioGroup
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    FragmentBase fragMent = null;


                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (group.getCheckedRadioButtonId()) {
                            case R.id.radio_button_tab_home:
                                dismissDialog();
                                if (mFragmentHome == null) {
                                    mFragmentHome = new FragmentHome();
                                }

                                fragMent = mFragmentHome;
                                switchContent(mFragmentHome);
                                mFragmentHome.setDateChange();

                                if (Calendar.getInstance()
                                        .get(Calendar.YEAR) >= YEAR_MIN) {
                                    changToMainSetting();
                                }
                                break;

                            case R.id.radio_button_tab_calibration:
                                dismissDialog();
                                if (mFragmentCalibration == null) {
                                    mFragmentCalibration = new FragmentCalibration();
                                }

                                fragMent = mFragmentCalibration;
                                switchContent(mFragmentCalibration);

                                if (Calendar.getInstance()
                                        .get(Calendar.YEAR) >= YEAR_MIN) {
                                    changToMainSetting();
                                }
                                break;

                            case R.id.radio_button_tab_settings:

                                if (mFragmentSettings == null) {
                                    mFragmentSettings = new FragmentSettingContainer();
                                }

                                fragMent = mFragmentSettings;
                                switchContent(mFragmentSettings);
//                                changToMainSetting();
                                break;

                            default:
                                break;
                        }

//                        if (fragMent != null) {
//                            ActivityMain.this.getSupportFragmentManager()
//                                    .beginTransaction()
//                                    .replace(R.id.layout_fragment, fragMent).commit();
//                        }
                    }
                });

        if (radioGroup.getCheckedRadioButtonId() < 0) {
            radioGroup.check(R.id.radio_button_tab_home);
        }

        ((ApplicationPDA)

                getApplication()).

                registerMessageListener(
                        ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        if (Calendar.getInstance()
                .get(Calendar.YEAR) >= YEAR_MIN) {
            showTimeSetting();
        }
    }

    private void dismissDialog() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.DISMISS_DIALOG,
                null));
    }

    private void changToMainSetting() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.SETTING_TYPE,
                new byte[]{(byte) TYPE_SETTING}));
    }


    @Override
    protected void onResume() {
        super.onResume();

        getStatusBar().setGlucose(true);

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < YEAR_MIN) {
            radioGroup.check(R.id.radio_button_tab_settings);
        }
    }

    private void showTimeSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle(getString(R.string.time_sure_title))
                .setMessage(getString(R.string.time_sure_content))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .setNegativeButton(R.string.setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        radioGroup.check(R.id.radio_button_tab_settings);
                        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_DATE_TIME}));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        radioGroup.check(R.id.radio_button_tab_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        new DataSetHistory(this).close();
    }


    @Override
    public void onBackPressed() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.SETTING_TYPE_BACK,
                null));
    }


    @Override
    protected void onHomePressed() {
        radioGroup.check(R.id.radio_button_tab_home);
    }

    @Override
    protected void handleEvent(EntityMessage message) {
        super.handleEvent(message);
        if ((message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) &&
                (message.getParameter() == ParameterGlucose.HOME_PRESS)) {
            radioGroup.check(R.id.radio_button_tab_home);
        }

    }

    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) &&
                (message.getParameter() == ParameterGlucose.PARAM_SIGNAL_PRESENT)) {
            if (getStatusBar().getGlucose()) {
                boolean realtimeFlag = (boolean) SPUtils.get(this, REALTIMEFLAG, true);
                if (realtimeFlag) {
                    Intent intent = new Intent(this, ActivityBgApply.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.history_mode_forbidden, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    @Override
    protected void setParameter(EntityMessage message) {
        super.setParameter(message);
        switch (message.getParameter()) {
            case ParameterMonitor.CAN_SEND_FAILD:
                if (radioGroup.getCheckedRadioButtonId() !=R.id.radio_button_tab_settings){
                    radioGroup.check(R.id.radio_button_tab_settings);
                }
                break;
            default:

                break;
        }

    }

    //    private void onScreenOn() {
//        mLog.Debug(getClass(), "Screen on");
//
//        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
//                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
//                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
//                {
//                        ParameterComm.BROADCAST_OFFSET_ALL
//                }));
//    }
//
    protected void onScreenOff() {
        mLog.Debug(getClass(), "Screen off");

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                {
                        ParameterComm.BROADCAST_OFFSET_STATUS
                }));
        radioGroup.check(R.id.radio_button_tab_home);

        Intent lockIntent = new Intent(this, ActivityUnlockNew.class);
        startActivity(lockIntent);
        ActivityPathManager.getInstance().registerSourceActivity(getAddTime());
    }

}

package com.tuya.smart.bizubundle.demo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.android.user.api.ILoginCallback;
import com.tuya.smart.android.user.api.ILogoutCallback;
import com.tuya.smart.android.user.bean.User;
import com.tuya.smart.api.service.MicroServiceManager;
import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;
import com.tuya.smart.demo_login.base.utils.LoginHelper;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.home.sdk.api.ITuyaHomeChangeListener;
import com.tuya.smart.home.sdk.bean.HomeBean;
import com.tuya.smart.home.sdk.callback.ITuyaGetHomeListCallback;
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuya.smart.sdk.bean.GroupBean;
import com.tuya.smart.utils.ProgressUtil;
import com.tuya.smart.utils.ToastUtil;
import com.tuya.smart.wrapper.api.TuyaWrapper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mCurrentFamilyName;
    private RouterPresenter routePresenter;
    private ITuyaHomeChangeListener mHomeChangeListener = new ITuyaHomeChangeListener() {
        @Override
        public void onHomeAdded(long homeId) {
            requestHomeDetail(homeId);
        }

        @Override
        public void onHomeInvite(long homeId, String homeName) {

        }

        @Override
        public void onHomeRemoved(long l) {

        }

        @Override
        public void onHomeInfoChanged(long l) {

        }

        @Override
        public void onSharedDeviceList(List<DeviceBean> list) {

        }

        @Override
        public void onSharedGroupList(List<GroupBean> list) {

        }

        @Override
        public void onServerConnectSuccess() {
        }
    };

    private void requestHomeDetail(long id) {
        TuyaHomeSdk.newHomeInstance(id).getHomeDetail(new ITuyaHomeResultCallback() {
            @Override
            public void onSuccess(HomeBean bean) {

            }

            @Override
            public void onError(String errorCode, String errorMsg) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //此处只是演示代码，集成时请在登录成功后调用
        //This method must be called after successful login
//        TuyaWrapper.onLogin();

        TuyaHomeSdk.getUserInstance()
                .loginOrRegisterWithUid(
                        "66",
                        "2023000389",
                        "2023000389",
                        new ILoginCallback() {
                            @Override
                            public void onSuccess(User user) {
                                Toast.makeText(getApplicationContext(), "Logged in with Username: " + TuyaHomeSdk.getUserInstance().getUser().getUsername(), Toast.LENGTH_SHORT).show();
                                // sample code
                                mCurrentFamilyName = findViewById(R.id.current_family_name);
                                mCurrentFamilyName.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        FamilyDialogFragment dialogFragment = FamilyDialogFragment.newInstance();
                                        dialogFragment.show(getSupportFragmentManager(), "FamilyDialogFragment");
                                    }
                                });
                                ProgressUtil.showLoading(MainActivity.this, "Loading...");
                                getHomeList();
                                TuyaHomeSdk.getHomeManagerInstance().registerTuyaHomeChangeListener(mHomeChangeListener);
                                findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        TuyaHomeSdk.getUserInstance().logout(new ILogoutCallback() {
                                            @Override
                                            public void onSuccess() {
                                                //演示代码
                                                //demo use only start
                                                LoginHelper.reLogin(MainActivity.this, false);
                                                //demo use only end

                                                //退出成功后必须调用此方法
                                                //This method must be called on exit.
//                        TuyaWrapper.onLogout(MainActivity.this);
                                            }

                                            @Override
                                            public void onError(String errorCode, String errorMsg) {
                                                L.e("tuya logout", errorCode + " " + errorMsg);
                                            }
                                        });

                                    }
                                });
                            }

                            @Override
                            public void onError(String code, String error) {
                                Toast.makeText(getApplicationContext(), code + " " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
    }

    private void getHomeList() {
        TuyaHomeSdk.getHomeManagerInstance().queryHomeList(new ITuyaGetHomeListCallback() {
            @Override
            public void onSuccess(List<HomeBean> list) {
                if (!list.isEmpty()) {
                    setCurrentFamily(list.get(0));
                } else {
                    ToastUtil.showToast(MainActivity.this, "home list is null,plz create home");
                }
                ProgressUtil.hideLoading();
                openUIBizBundle();
                for (HomeBean homeBean : list) {
                    requestHomeDetail(homeBean.getHomeId());
                }
            }

            @Override
            public void onError(String s, String s1) {
                ProgressUtil.hideLoading();
                Toast.makeText(MainActivity.this, s + "\n" + s1, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * 业务包接入后必须实现家庭服务(商城业务包可以不接入)
     * you should implementation AbsBizBundleFamilyService(mall bizbundle can not implementation)
     */
    public void setCurrentFamily(HomeBean homeBean) {
        mCurrentFamilyName.setText(homeBean.getName());
        AbsBizBundleFamilyService familyService = MicroServiceManager.getInstance().findServiceByInterface(AbsBizBundleFamilyService.class.getName());
        familyService.shiftCurrentFamily(homeBean.getHomeId(), homeBean.getName());
    }

    private void openUIBizBundle() {
        AbsBizBundleFamilyService familyService = MicroServiceManager.getInstance().findServiceByInterface(AbsBizBundleFamilyService.class.getName());
        if (familyService.getCurrentHomeId() == 0) {
            ToastUtil.showToast(this, "homeId is must not 0");
            return;
        }
        findViewById(R.id.panel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                String[] arrayList = {
//                        Manifest.permission.READ_EXTERNAL_STORAGE
//                };
//
//                ActivityCompat.requestPermissions(MainActivity.this, arrayList, 100);
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.panel.demo.PanelActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.mall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.mall.demo.MallActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.scene).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.scene.demo.SceneActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.ipc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.ipc.demo.IPCPanelActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.cloud_storage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.cloudstorage.demo.CloudStorageActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.activator).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.activator.demo.DeviceActivatorActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.message.demo.MessageActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.demo.FeedBackActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.ota).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.ota.demo.OtaActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.family).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.demo.family.FamilyManageActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.device_detail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.panelmore.demo.DeviceDetailActivity");
                startActivity(i);
            }
        });

        findViewById(R.id.location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.demo.location.LocationActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.groupmanager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.groupmanager.GroupManagerActivity");
                startActivity(i);
            }
        });

        findViewById(R.id.alexa_google_bind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.demo.socialbind.AlexaGoogleBindActivity");
                startActivity(i);
            }
        });

        findViewById(R.id.light_scene).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.appsdk.sample.lightscene.LightSceneManagerActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.share.ShareActivity");
                startActivity(i);
            }
        });
        findViewById(R.id.control).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizubundle.control.demo.ControlManagerActivity");
                startActivity(i);
            }
        });

        findViewById(R.id.speech).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClassName(MainActivity.this, "com.tuya.smart.bizbundle.demo.speech.SpeechDemoActivity");
                startActivity(i);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TuyaHomeSdk.getHomeManagerInstance().unRegisterTuyaHomeChangeListener(mHomeChangeListener);
        TuyaHomeSdk.getHomeManagerInstance().onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (schemeJump(intent)) {
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (routePresenter != null) {
            routePresenter.route(this);
            routePresenter = null;
        }
    }

    private boolean schemeJump(Intent intent) {
        routePresenter = RouterPresenter.parser(intent);
        return routePresenter != null;
    }
}
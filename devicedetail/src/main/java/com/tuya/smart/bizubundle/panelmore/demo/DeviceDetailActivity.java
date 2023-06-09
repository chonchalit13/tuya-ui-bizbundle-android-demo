package com.tuya.smart.bizubundle.panelmore.demo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tuya.smart.android.common.utils.L;
import com.tuya.smart.api.router.UrlBuilder;
import com.tuya.smart.api.router.UrlRouter;
import com.tuya.smart.api.service.MicroServiceManager;
import com.tuya.smart.commonbiz.bizbundle.family.api.AbsBizBundleFamilyService;
import com.tuya.smart.home.sdk.TuyaHomeSdk;
import com.tuya.smart.home.sdk.bean.HomeBean;
import com.tuya.smart.home.sdk.callback.ITuyaHomeResultCallback;
import com.tuya.smart.infraredsubdev_storage_api.OnInfraredSubDevDisplaySettingsListener;

import com.tuya.smart.panel.usecase.panelmore.service.PanelMoreItemClickService;
import com.tuya.smart.panel.usecase.panelmore.service.PanelMoreMenuService;
import com.tuya.smart.sdk.bean.DeviceBean;
import com.tuya.smart.sdk.bean.GroupBean;
import com.tuya.smart.tuyasmart_device_detail.api.IPluginInfraredSubDevDisplayService;
import com.tuya.smart.utils.ProgressUtil;
import com.tuya.smart.wrapper.api.TuyaWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * see GotoNewCell and tyarchercell_config.json ,a new way to add a new item.
 */
public class DeviceDetailActivity extends Activity {
    private EditText edt;
    private SimpleDevListAdapter mAdapter;
    private BottomSheetDialog bottomSheetDialog;
    private long groupId;
    private static final String TAG = "deviceDetailActivity";
    private SimpleDeviceBean currentSelectBean;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_more);
        TuyaWrapper.registerService(PanelMoreMenuService.class, new PanelMoreMenuServiceImpl());
        TuyaWrapper.registerService(PanelMoreItemClickService.class, new PanelMoreItemClickServiceImp());
        edt = findViewById(R.id.edit_id);
        mAdapter = new SimpleDevListAdapter();
        initClick();
        getCurrentHomeDetail();

        //      PanelMoreInfraredSubDevDisplayService is deprecated,please use IPluginInfraredSubDevDisplayService
//        PanelMoreInfraredSubDevDisplayService service = MicroServiceManager.getInstance().
//                findServiceByInterface(PanelMoreInfraredSubDevDisplayService.class.getName());
        IPluginInfraredSubDevDisplayService service = MicroServiceManager.getInstance().findServiceByInterface(IPluginInfraredSubDevDisplayService.class.getName());
        service.registerInfraredSubDevDisplaySettingsListener(new OnInfraredSubDevDisplaySettingsListener() {
            @Override
            public void onDisplaySettingsChanged(Long homeId, String gwId, Boolean shown) {
                // 更新设备列表
                // gwId 为红外设备id
                // devId 为红外子设备id
                //  service.getInfraredSubDevDisplaySettings(homeId,devId) 读取当前的红外子设备是否需要显示到首页

                Log.e(TAG, " changed " + "homeId: " + homeId + " devId" + gwId + " show" + shown);
                Toast.makeText(DeviceDetailActivity.this, "infrared Changed", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDisplaySettingsRemoved(Long homeId, String gwId) {
                Log.e(TAG, " removed " + "homeId: " + homeId + " devId" + gwId);
                Toast.makeText(DeviceDetailActivity.this, "infrared removed", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initClick() {
        findViewById(R.id.btn_dev_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDevsInHome();
            }
        });

        findViewById(R.id.btn_dev_panel_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UrlBuilder urlBuilder = new UrlBuilder(DeviceDetailActivity.this, "panelMore");
                String devId = edt.getText().toString().trim();
                if (TextUtils.isEmpty(devId)) {
                    Toast.makeText(DeviceDetailActivity.this, "input device id", Toast.LENGTH_LONG).show();
                    return;
                }
                DeviceBean deviceBean = TuyaHomeSdk.getDataInstance().getDeviceBean(devId);
                Bundle bundle = new Bundle();

                bundle.putString("extra_panel_name", currentSelectBean.getTitle());
                bundle.putLong("extra_panel_group_id", groupId);
                if (groupId > 0) {
                    bundle.putBoolean("extra_is_group", true);
                    bundle.putString("extra_panel_dev_id", groupId + "");
                } else {
                    bundle.putString("extra_panel_dev_id", devId);
                    bundle.putBoolean("extra_is_group", false);
                }
                urlBuilder.putExtras(bundle);
                UrlRouter.execute(urlBuilder);
            }
        });

    }

    private void showDevsInHome() {
        View view = View.inflate(this, R.layout.demo_dialog_dev_list, null);
        RecyclerView recyclerView = view.findViewById(R.id.demo_bottom_dialog_rv_dev);
        TextView textView = view.findViewById(R.id.demo_bottom_dialog_title);
        textView.setText("当前家庭下的所有设备");
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new SimpleDevListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SimpleDeviceBean bean, int position) {
                edt.setText(bean.getDevId());
                groupId = bean.getGroupId();
                bottomSheetDialog.dismiss();
                currentSelectBean = bean;
            }
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        final BottomSheetBehavior dialogBehavior = BottomSheetBehavior.from((View) view.getParent());
        dialogBehavior.setPeekHeight(500);
        dialogBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetDialog.dismiss();
                    dialogBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        bottomSheetDialog.show();
    }

    private void getCurrentHomeDetail() {
        ProgressUtil.showLoading(this, "Loading...");
        IPluginInfraredSubDevDisplayService service = MicroServiceManager.getInstance()
                .findServiceByInterface(IPluginInfraredSubDevDisplayService.class.getName());

        TuyaHomeSdk.newHomeInstance(getService().getCurrentHomeId()).getHomeDetail(new ITuyaHomeResultCallback() {
            @Override
            public void onSuccess(final HomeBean homeBean) {
                List<SimpleDeviceBean> beans = new ArrayList<>(8);
                for (GroupBean groupBean : homeBean.getGroupList()) {
                    beans.add(getItemBeanFromGroup(groupBean));
                }
                for (DeviceBean deviceBean : homeBean.getDeviceList()) {
                    beans.add(getItemBeanFromDevice(deviceBean));
                }
                for (SimpleDeviceBean bean : beans) {
                    boolean b = service.getInfraredSubDevDisplaySettings(getService().getCurrentHomeId(), bean.getDevId());
                    bean.setShow(b);
                }
                mAdapter.setData(beans);
                ProgressUtil.hideLoading();

            }

            @Override
            public void onError(String s, String s1) {
                ProgressUtil.hideLoading();
                Toast.makeText(DeviceDetailActivity.this, s + "\n" + s1, Toast.LENGTH_LONG).show();
            }
        });

    }

    /**
     * you must implementation AbsBizBundleFamilyService
     *
     * @return AbsBizBundleFamilyService
     */
    private AbsBizBundleFamilyService getService() {
        return MicroServiceManager.getInstance().findServiceByInterface(AbsBizBundleFamilyService.class.getName());
    }

    private SimpleDeviceBean getItemBeanFromGroup(GroupBean groupBean) {
        SimpleDeviceBean itemBean = new SimpleDeviceBean();
        itemBean.setGroupId(groupBean.getId());
        itemBean.setTitle(groupBean.getName());
        itemBean.setIconUrl(groupBean.getIconUrl());
        itemBean.setType(groupBean.getType());

        List<DeviceBean> deviceBeans = groupBean.getDeviceBeans();
        if (deviceBeans == null || deviceBeans.isEmpty()) {
            return null;
        } else {
            DeviceBean onlineDev = null;
            for (DeviceBean dev : deviceBeans) {
                if (dev != null) {
                    if (dev.getIsOnline()) {
                        onlineDev = dev;
                        break;
                    } else {
                        onlineDev = dev;
                    }
                }
            }
            itemBean.setDevId(onlineDev.getDevId());
            return itemBean;
        }
    }

    private SimpleDeviceBean getItemBeanFromDevice(DeviceBean deviceBean) {
        SimpleDeviceBean itemBean = new SimpleDeviceBean();
        itemBean.setDevId(deviceBean.getDevId());
        itemBean.setIconUrl(deviceBean.getIconUrl());
        itemBean.setTitle(deviceBean.getName());
        return itemBean;
    }

}


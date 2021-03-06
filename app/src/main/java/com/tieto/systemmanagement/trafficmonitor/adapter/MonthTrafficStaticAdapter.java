package com.tieto.systemmanagement.trafficmonitor.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tieto.systemmanagement.R;
import com.tieto.systemmanagement.authority.entity.AppWrapper;
import com.tieto.systemmanagement.trafficmonitor.entity.AppTrafficInfo;
import com.tieto.systemmanagement.trafficmonitor.entity.IptablesForDroidWall;
import com.tieto.systemmanagement.trafficmonitor.utils.CommonMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jane on 15-3-26.
 */
public class MonthTrafficStaticAdapter extends NetworkManageBasicAdapter {
    private List<AppTrafficInfo> mAppInfos;
    private LayoutInflater mInflater;

    public MonthTrafficStaticAdapter(Context context, List<AppTrafficInfo> appInfos) {
        if(appInfos == null) {
            appInfos = new ArrayList<AppTrafficInfo>();
        }
        this.mAppInfos = appInfos;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mAppInfos.size();
    }

    @Override
    public Object getItem(int i) {
        return mAppInfos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = null;
        if(view==null) {
            view = mInflater.inflate(R.layout.t_firewall_item_month_traffic_info,null);
            holder = new ViewHolder();
            holder.mAppIcon = (ImageView) view.findViewById(R.id.mon_app_icon);
            holder.mAppName = (TextView) view.findViewById(R.id.mon_app_name);
            holder.mSneakTrafficTip = (TextView) view.findViewById(R.id.mon_traffic_sneak_tip);
            holder.mAppUsedTraffic = (TextView) view.findViewById(R.id.mon_traffic_used);
            holder.mBackgroundUsedTraffic = (TextView) view.findViewById(R.id.mon_traffic_bg);
            holder.mSneakedTraffic = (TextView) view.findViewById(R.id.mon_traffic_sneak);
            holder.mFirewallType = (TextView) view.findViewById(R.id.mon_net_allowed_info);
            holder.mAllowNetwork = (ImageButton) view.findViewById(R.id.mon_allow_net);
            holder.mMonth_firewall_panel = (LinearLayout) view.findViewById(
                    R.id.month_firewall_panel);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }

        final AppTrafficInfo appInfo = mAppInfos.get(i);
        final AppWrapper appWrapper = appInfo.getmAppWrapper();

        holder.mAppIcon.setImageDrawable(appWrapper.loadIcon(context));
        holder.mAppName.setText(appWrapper.getName(context));
        holder.mAppUsedTraffic.setText("已用" + CommonMethod.formatString(
                appInfo.getmUsdTrafficInMonth(), false));
        holder.mBackgroundUsedTraffic.setText("后台" + CommonMethod.formatString(
                appInfo.getmBgUsedTrafficInMonth(), false));
        holder.mSneakedTraffic.setText("本月已偷跑" + appInfo.getmSneakedTrafficInMonth() + "M");
        holder.mFirewallType.setText(appInfo.getmFirewallType());

        final CallbackImpl impl = new CallbackImpl(holder.mFirewallType);
        final ViewHolder finalHolder = holder;
        holder.mMonth_firewall_panel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //popup window,and when  the item is clicked ,the info should be changed.
                //check the root permission first
                if (IptablesForDroidWall.hasRootAccess(context, true)) {
                    showWindow(view, finalHolder.mAllowNetwork ,appWrapper, impl);
                }
            }
        });
        return view;
    }

    private class ViewHolder {
        private ImageView mAppIcon;
        private TextView mAppName;
        private TextView mSneakTrafficTip;
        private TextView mAppUsedTraffic;
        private TextView mBackgroundUsedTraffic;
        private TextView mSneakedTraffic;
        private TextView mFirewallType;
        private ImageButton mAllowNetwork;
        private LinearLayout mMonth_firewall_panel;
    }
}

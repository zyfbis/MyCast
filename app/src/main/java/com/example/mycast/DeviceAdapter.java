package com.example.mycast;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 作者：leavesC
 * 时间：2019/11/23 11:56
 * 描述：
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<WifiP2pDevice> wifiP2pDeviceList;

    private OnClickListener clickListener;

    public interface OnClickListener {

        void onItemClick(int position);

    }

    public DeviceAdapter(List<WifiP2pDevice> wifiP2pDeviceList) {
        this.wifiP2pDeviceList = wifiP2pDeviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onItemClick((Integer) v.getTag());
                }
            }
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.tv_deviceName.setText(wifiP2pDeviceList.get(position).deviceName);
        holder.tv_deviceAddress.setText(wifiP2pDeviceList.get(position).deviceAddress);
        holder.tv_deviceDetails.setText(getDeviceStatus(wifiP2pDeviceList.get(position).status));
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return wifiP2pDeviceList.size();
    }

    public void setClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "可用的";
            case WifiP2pDevice.INVITED:
                return "邀请中";
            case WifiP2pDevice.CONNECTED:
                return "已连接";
            case WifiP2pDevice.FAILED:
                return "失败的";
            case WifiP2pDevice.UNAVAILABLE:
                return "不可用的";
            default:
                return "未知";
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tv_deviceName;

        private TextView tv_deviceAddress;

        private TextView tv_deviceDetails;

        ViewHolder(View itemView) {
            super(itemView);
            tv_deviceName = itemView.findViewById(R.id.tvDeviceName);
            tv_deviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            tv_deviceDetails = itemView.findViewById(R.id.tvDeviceDetails);
        }

    }

}

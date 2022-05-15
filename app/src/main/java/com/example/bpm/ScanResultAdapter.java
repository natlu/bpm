package com.example.bpm;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {
    private static final String TAG = "ScanResultAdapter";
    public Context context;
    private List<ScanResult> items;
    private View.OnClickListener onClickListener;

    ScanResultAdapter(Context context, List<ScanResult> scanResults, View.OnClickListener onClickListener) {
        this.context = context;
        this.items = scanResults;
        this.onClickListener = onClickListener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView device_name;
        private TextView mac_address;
        private TextView signal_strength;
        // private ScanResult result;

        public ViewHolder(View v, View.OnClickListener onClickListener) {
            super(v);
            Log.v(TAG, "constructor");
            // Define click listener for the ViewHolder's View.
            v.setOnClickListener(onClickListener);
            // v.setOnClickListener(new View.OnClickListener() {
            //     @Override
            //     public void onClick(View v) {
            //         // Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
            //         Log.d(TAG, "viewholder onclick");
            //     }
            // });
            TextView device_name = (TextView) v.findViewById(R.id.device_name);
            TextView mac_address = (TextView) v.findViewById(R.id.mac_address);
            TextView signal_strength = (TextView) v.findViewById(R.id.signal_strength);
            this.device_name = device_name;
            this.mac_address = mac_address;
            this.signal_strength = signal_strength;
        }

        public void bind(ScanResult result) {
            // this.result = result;
            Log.v(TAG, "bind");
            device_name.setText(result.getDevice().getName());
            mac_address.setText(result.getDevice().getAddress());
            signal_strength.setText(Integer.toString(result.getRssi()) + "dBm");
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Log.v(TAG, "onCreateViewHolder");
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.row_scan_result, viewGroup, false);
        return new ViewHolder(v, onClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // what happens when the recyclerview wants to replace old data with new ('binds' new data
        // to the old view;
        Log.v(TAG, "obBindViewHolder position is  " + position);

        ViewHolder itemHolder = (ViewHolder) viewHolder;
        Log.v(TAG, "obBindViewHolder item id is  " + itemHolder.getItemId());
        Log.v(TAG, "obBindViewHolder adapter pos is  " + itemHolder.getAdapterPosition());
        ScanResult result = items.get(position);
        viewHolder.bind(result);

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        // viewHolder.getTextView().setText(items[position]);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}

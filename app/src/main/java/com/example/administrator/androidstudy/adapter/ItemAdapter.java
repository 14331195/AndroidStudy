package com.example.administrator.androidstudy.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.administrator.androidstudy.ChannelActivity;
import com.example.administrator.androidstudy.MainActivity;
import com.example.administrator.androidstudy.NestedScrollActivity;
import com.example.administrator.androidstudy.PhotoViewActivity;
import com.example.administrator.androidstudy.R;
import com.example.administrator.androidstudy.RecyclerViewActivity;

import java.util.List;

/**
 * Created by ljm on 2018/3/27.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {
    private List<Item> list;
    private Context context;

    public ItemAdapter(Context context, List<Item> items) {
        this.context = context;
        this.list = items;
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View view  = LayoutInflater.from(context).inflate(R.layout.item_test, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(list.get(position).text);
        holder.textView.setOnClickListener((v)->{
            if (!(context instanceof MainActivity)) return;
            if (position == 0) {
                Intent intent = new Intent(context, PhotoViewActivity.class);
                context.startActivity(intent);
            } else if (position == 1) {
                context.startActivity(new Intent(context, RecyclerViewActivity.class));
            } else if (position == 2) {
                context.startActivity(new Intent(context, ChannelActivity.class));
            } else if (position == 3) {
                context.startActivity(new Intent(context, NestedScrollActivity.class));
            }
        });
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text);
        }
        public TextView textView;
    }

    public static class Item {
        public Item(int i){
            text = String.valueOf(i);
        }
        public String text;
    }
}

package com.example.administrator.androidstudy.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.androidstudy.R;

import java.util.Collections;
import java.util.List;

/**
 * Created by ljm on 2018/3/27.
 */
public class ITouchAdapter extends RecyclerView.Adapter<ITouchAdapter.ViewHolder>
        implements ItemTouchCallback.ItemCallback{
    private List<Item> list;
    private Context context;

    public ITouchAdapter(Context context, List<Item> items) {
        this.context = context;
        this.list = items;
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View view  = LayoutInflater.from(context).inflate(R.layout.item_touch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(list.get(position).text);
        holder.textView.setOnClickListener((v)->{
            Toast.makeText(context, position+"", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onItemMove(int pos, int target) {
        Collections.swap(list, pos, target);
        notifyItemMoved(pos, target);
    }

    @Override
    public void onItemDismiss(int pos) {
        list.remove(pos);
        notifyItemRemoved(pos);
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
        String text;
    }
}

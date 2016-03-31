package com.sally.imagepicker.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.sally.imagepicker.ImageLoader;
import com.sally.imagepicker.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sally on 16/3/30.
 */
public class GridViewAdapter extends BaseAdapter {

    private Context mContext;
    private List<String> mImgPaths;
    private String mDirPath;
    private LayoutInflater mInflater;
    private Set<String> mSelected = new HashSet<String>();

    public GridViewAdapter(Context context, List<String> datas, String dirPath) {
        this.mContext = context;
        this.mImgPaths = datas;
        this.mDirPath = dirPath;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder vh;
        if (convertView == null) {
            vh = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_grid_view, parent, false);
            vh.mImg = (ImageView) convertView.findViewById(R.id.item_image_view);
            vh.mImgBtn = (ImageButton) convertView.findViewById(R.id.item_select);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder) convertView.getTag();
        }

        // 重置状态
        vh.mImg.setColorFilter(null);
        vh.mImg.setImageResource(R.mipmap.ic_launcher);
        vh.mImgBtn.setImageResource(android.R.drawable.ic_menu_add);
        ImageLoader.getInstance().loadImage(mDirPath + "/" + mImgPaths.get(position), vh.mImg);

        final String filepath = mDirPath + "/" + mImgPaths.get(position);
        vh.mImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 已被选择的
                if (mSelected.contains(filepath)) {
                    mSelected.remove(filepath);
                    vh.mImgBtn.setImageResource(android.R.drawable.ic_menu_add);
                    vh.mImg.setColorFilter(null);
                } else {
                    // 未被选择的
                    mSelected.add(filepath);
                    vh.mImgBtn.setImageResource(android.R.drawable.ic_menu_more);
                    vh.mImg.setColorFilter(Color.parseColor("#55000000"));
                }
            }
        });
        if(mSelected.contains(filepath)) {
            vh.mImgBtn.setImageResource(android.R.drawable.ic_menu_more);
            vh.mImg.setColorFilter(Color.parseColor("#55000000"));
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView mImg;
        ImageButton mImgBtn;
    }
}

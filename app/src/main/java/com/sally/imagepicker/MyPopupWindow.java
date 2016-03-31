package com.sally.imagepicker;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sally.imagepicker.bean.FloderBean;

import java.util.List;

/**
 * Created by sally on 16/3/30.
 */
public class MyPopupWindow extends PopupWindow {

    private int mWidth;
    private int mHeight;
    private View mContentView;
    private ListView mListView;

    private List<FloderBean> mDatas;

    public interface OnMyListViewItemClickListener {
        void onSelected(FloderBean floderBean);
    }
    public OnMyListViewItemClickListener mListener;
    public void setOnMyListViewItemClickListener(OnMyListViewItemClickListener listener) {
        mListener = listener;
    }

    public MyPopupWindow(Context context, List<FloderBean> datas) {
        calWidthAndHeight(context);

        mContentView = LayoutInflater.from(context).inflate(R.layout.popup_window, null);
        mDatas = datas;

        // popupwindow一些常用的设置
        setContentView(mContentView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    return true;
                }
                return false;
            }
        });

        initView(context);
        initEvent();
    }

    private void initView(Context context) {
        mListView = (ListView) mContentView.findViewById(R.id.id_listview);
        mListView.setAdapter(new MyPopupWindowAdapter(context, mDatas));
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               if(mListener != null) {
                   mListener.onSelected(mDatas.get(position));
               }
            }
        });
    }

    /**
     * 计算popupwindow的宽高
     *
     * @param context
     */
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMateris = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMateris);
        mWidth = outMateris.widthPixels;
        mHeight = outMateris.heightPixels / 7 * 5;
    }

    private class MyPopupWindowAdapter extends BaseAdapter {

        private Context mContext;
        private List<FloderBean> mDatas;
        private LayoutInflater mInflater;

        public MyPopupWindowAdapter(Context context, List<FloderBean> datas) {
            this.mContext = context;
            this.mDatas = datas;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mDatas.size();
        }

        @Override
        public Object getItem(int position) {
            return mDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if(vh == null) {
                vh = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_window, parent, false);
                vh.mImageView = (ImageView) convertView.findViewById(R.id.item_dir_iv_popup);
                vh.mDirName = (TextView) convertView.findViewById(R.id.item_id_dir_name);
                vh.mDirCount = (TextView) convertView.findViewById(R.id.item_id_dir_count);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            // 重置imageview
            vh.mImageView.setImageResource(android.R.drawable.ic_menu_delete);

            FloderBean bean = (FloderBean) getItem(position);
            ImageLoader.getInstance().loadImage(bean.getFirstImgPath(), vh.mImageView);
            vh.mDirName.setText(bean.getName());
            vh.mDirCount.setText(bean.getCount() + "");

            return convertView;
        }

        class ViewHolder {
            ImageView mImageView;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}

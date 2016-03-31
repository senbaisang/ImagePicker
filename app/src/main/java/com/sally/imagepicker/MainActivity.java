package com.sally.imagepicker;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sally.imagepicker.adapter.GridViewAdapter;
import com.sally.imagepicker.bean.FloderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MyPopupWindow mMyPopupWindow;

    private GridView mGridView;
    private List<String> mImgs;
    private GridViewAdapter mGridViewAdapter;

    private RelativeLayout mBootmLayout;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FloderBean> mFloderBeans = new ArrayList<FloderBean>();

    private ProgressDialog mProgressDialog;

    private static final int SUCCESS = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    mProgressDialog.dismiss();
                    data2view();

                    // 初始化popupwindow
                    initPopupWindow();
                    break;
            }
        }
    };


    private void initPopupWindow() {
        mMyPopupWindow = new MyPopupWindow(this, mFloderBeans);
        mMyPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mMyPopupWindow.setOnMyListViewItemClickListener(new MyPopupWindow.OnMyListViewItemClickListener() {
            @Override
            public void onSelected(FloderBean floderBean) {
                mCurrentDir = new File(floderBean.getCurrentDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
                            return true;
                        }
                        return false;
                    }
                }));
                mGridViewAdapter = new GridViewAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(mGridViewAdapter);
                mDirName.setText(floderBean.getName());
                mDirCount.setText(mImgs.size() + "");
                mMyPopupWindow.dismiss();
            }
        });
    }

    /**
     * 内容区域变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }


    /**
     * 内容区域变暗
     */
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.6f;
        getWindow().setAttributes(lp);
    }

    /**
     *  绑定数据到view中
     */
    private void data2view() {
        if(mCurrentDir == null) {
            Toast.makeText(MainActivity.this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mGridViewAdapter = new GridViewAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mGridViewAdapter);
        mDirCount.setText(mMaxCount + "");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDatas();
        initEvent();
    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_gridview);
        mBootmLayout = (RelativeLayout) findViewById(R.id.id_bottom_relativelayout);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "当前存储卡不可用。。。", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载。。。");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                Set<String> mDirpaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentPath = new File(path).getParentFile();
                    if (parentPath == null) {
                        continue;
                    }
                    String dirPath = parentPath.getAbsolutePath();
                    FloderBean floderBean = null;
                    if (mDirpaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirpaths.add(dirPath);
                        floderBean = new FloderBean();
                        floderBean.setCurrentDir(dirPath);
                        floderBean.setFirstImgPath(path);
                    }
                    if (parentPath.list() == null) {
                        continue;
                    }
                    int picSize = parentPath.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    floderBean.setCount(picSize);
                    mFloderBeans.add(floderBean);


                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentPath;
                    }

                }
                cursor.close();
                // 通知handler图片扫描完成
                mHandler.sendEmptyMessage(SUCCESS);
            }
        }).start();
    }

    private void initEvent() {
        mBootmLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mMyPopupWindow.setAnimationStyle();
                mMyPopupWindow.showAsDropDown(mBootmLayout, 0, 0);
                lightOff();
            }
        });
    }
}

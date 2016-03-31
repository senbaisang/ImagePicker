package com.sally.imagepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * ImageLoader ： 图片加载核心类
 * Created by sally on 16/3/29.
 */
public class ImageLoader {

    private static ImageLoader mImageLoader;

    /*
    变量声明开始
     */

    // 图片缓存的核心对象
    private LruCache<String, Bitmap> mLruCache;

    // 线程池； 默认线程数； 线程池信号量，线程池中最多放3个任务，当任务不足3个时，再从任务队列中取任务。保证图片加载机制FIFO, LIFO的有效。信号量的功能类似线程的wait() notify()
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT = 3;
    private Semaphore mSemaphoreTnreadPool;

    // 任务队列
    private LinkedList<Runnable> mTaskQueue;

    // 后台轮询线程； 使用handler实现轮询； 后台轮询信号量，此处为了多线程安全着想，功能类似synchronized
    private Thread mBackLoopThread;
    private Handler mPoolThreadHandler;
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    // ui线程 和 耗时线程 之间通讯
    private Handler mUiThreadHandler;

    // 图片加载策略
    private enum Type {
        FIFO, LIFO;
    }

    private Type mType = Type.LIFO;

    /**
     * @param threadCount
     * @param type
     */
    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    /**
     * 初始化方法
     *
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {
        // 工作线程，后台轮询线程(任务队列， 线程池)
        mBackLoopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                // 通过线程池取一个任务并执行
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        // 从任务队列取任务执行
                        mThreadPool.execute(getTask());
                        try {
                            // 已经有3个方法在执行了，再请求第四个的时候，就需要请求信号量； 在执行完一个任务时，释放一个信号量
                            mSemaphoreTnreadPool.acquire();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                // mPoolThreadHandler初始化完成，释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        });
        mBackLoopThread.start();

        //  获取应用可用的最大内存
        int maxMemary = (int) Runtime.getRuntime().maxMemory();
        int cacheMemary = maxMemary / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemary) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        //  初始化线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mSemaphoreTnreadPool = new Semaphore(threadCount);

        // 初始化任务队列
        mTaskQueue = new LinkedList<Runnable>();

        mType = Type.LIFO;
    }

    public static ImageLoader getInstance() {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mImageLoader;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (mImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (mImageLoader == null) {
                    mImageLoader = new ImageLoader(threadCount, type);
                }
            }
        }
        return mImageLoader;
    }

    /**
     * 根据path 为 imageview 设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        imageView.setTag(path);

        // 显示图片
        if (mUiThreadHandler == null) {
            mUiThreadHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageBean bean = (ImageBean) msg.obj;
                    String imgPath = bean.path;
                    Bitmap bm = bean.bitmap;
                    ImageView iv = bean.imageView;
                    if (imgPath.equals(iv.getTag().toString())) {
                        iv.setImageBitmap(bm);
                    }
                }
            };
        }

        // 根据path从缓存中获取图片
        final Bitmap bm = getBitmapFromLruCache(path);

        if (bm != null) {
            // 显示图片
            showImage(path, imageView, bm);
        } else {
            // 创建任务获取图片
            addTask(new Runnable() {
                @Override
                public void run() {
                    // 1. 加载图片, 压缩图片
                    ImageSize imageSize = getImageViewSize(imageView);
                    Bitmap bmp = decodeSampleBitmapFromPath(path, imageSize.width, imageSize.height);

                    // 2. 将图片放入缓存
                    addBitmapToCache(path, bm);

                    // 3. 显示图片, 这里是工作线程，需要将图片传给主线程显示
                    showImage(path, imageView, bmp);

                    mSemaphoreTnreadPool.release();
                }
            });
        }
    }

    /**
     * 显示图片
     *
     * @param path
     * @param imageView
     * @param bm
     */
    private void showImage(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();
        ImageBean bean = new ImageBean();
        bean.path = path;
        bean.bitmap = bm;
        bean.imageView = imageView;
        message.obj = bean;
        mUiThreadHandler.sendMessage(message);
    }


    /**
     * 将图片加入缓存
     *
     * @param path
     * @param bm
     */
    private void addBitmapToCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == null) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    /**
     * 压缩图片
     *
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = caclulateInSampleSize(options, width, height);

        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        return bm;
    }

    /**
     * 根据需求的宽高 以及 图片真实宽高 计算 samplesize
     *
     * @param options
     * @param srcWidth
     * @param srcHeight
     * @return
     */
    private int caclulateInSampleSize(BitmapFactory.Options options, int srcWidth, int srcHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int sampleSize = 1;

        if (width > srcWidth || height > srcHeight) {
            int widthRadio = Math.round(width * 1.0f / srcWidth);
            int heightRadio = Math.round(height * 1.0f / srcHeight);

            sampleSize = Math.max(widthRadio, heightRadio);
        }
        return sampleSize;
    }

    /**
     * 根据imageview 获得适当的压缩图片的宽和高
     *
     * @param imageView
     * @return
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();

        // 获取iv的宽度 ： iv可能刚创建出来，还没有添加到容器中
        int width = imageView.getWidth();
        if (width <= 0) {
            width = lp.width;  // 获得iv再layout中的宽度 ： 可能时wrap_content match_parent
        }
        if (width <= 0) {
//            width = imageView.getMaxWidth();  // 获得iv的最大值
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {
            width = metrics.widthPixels;    // 设置为屏幕的宽度
        }

        int height = imageView.getHeight();
        if (height <= 0) {
            height = lp.height;
        }
        if (height <= 0) {
//            height = imageView.getMaxHeight();
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0) {
            height = metrics.heightPixels;
        }

        imageSize.width = width;
        imageSize.height = height;

        return imageSize;
    }

    /**
     * 为了兼容旧版本，getMaxWidth()方法就不能用了，通过反射获取
     *
     * @param fieldName
     * @return
     */
    private int getImageViewFieldValue(Object obj, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(obj);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return value;
    }

    /**
     * 向任务队列中添加获取图片的任务
     *
     * @param runnable
     */
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // mPoolThreadHandler声明在子线程,在使用之前应该先请求信号量；
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    /**
     * 从任务队列中获取任务
     *
     * @return
     */
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 根据 path 从缓存中获取图片
     *
     * @param path
     */
    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private class ImageSize {
        private int width;
        private int height;
    }

    private class ImageBean {
        Bitmap bitmap;
        String path;
        ImageView imageView;
    }
}

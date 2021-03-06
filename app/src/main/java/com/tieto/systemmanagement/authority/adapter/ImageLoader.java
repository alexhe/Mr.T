package com.tieto.systemmanagement.authority.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Jiang Ping
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class ImageLoader {

	protected Context mContext;
	
	/** 4MB memory cache. */
	private static final MemCache mMemCache = new MemCache(4 * 1024 * 1024);

	/** ThreadPool for 5 threads */
	private ExecutorService mExecutor = Executors.newFixedThreadPool(5);

	private static final int DRAWABLE_TRANS_TIME = 800;
	private int mTransitionTime = DRAWABLE_TRANS_TIME;
	private boolean mIsTransitionEnable = true;

    public ImageLoader(Context context) {
        mContext = context;
    }

	public void setTransitionTime(int time) {
		mTransitionTime = time;
	}
	
	public void setTransitionEnable(boolean enable) {
		mIsTransitionEnable = enable;
	}
	
	public void loadImage(ImageView imageView, Object data) {
		BitmapDrawable cached = mMemCache.getBitmap(data);
		if (cached != null) {
			imageView.setImageDrawable(cached);
		} else if (cancelRunningTask(imageView, data)) {
            ImageLoaderTask task = new ImageLoaderTask(imageView);
            AsyncDrawable drawable = new AsyncDrawable(mContext.getResources(), task);
            imageView.setImageDrawable(drawable);
            task.executeOnExecutor(mExecutor, data);
        }
	}
	
	public void loadImage(OnCompleteListener l, Object data) {
		BitmapDrawable cached = null;
		if (mMemCache != null) {
			cached = mMemCache.getBitmap(data);
		}
		if (cached != null && l != null) {
			l.onComplete(cached.getBitmap());
			return;
		}
		ImageLoaderTask task = new ImageLoaderTask(l);
		task.executeOnExecutor(mExecutor, data);
	}

	private boolean cancelRunningTask(ImageView imageView, Object data) {
		ImageLoaderTask task = getAttachedTask(imageView);
		if (task != null) {
			if (task.getData() == null || !task.getData().equals(data)) {
				task.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private ImageLoaderTask getAttachedTask(ImageView imageView) {
		if (imageView == null) return null;
		Drawable drawable = imageView.getDrawable();
		if (AsyncDrawable.class.isInstance(drawable)) {
			return ((AsyncDrawable) drawable).getLoaderTask();
		}
		return null;
	}

	private class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<ImageLoaderTask> mTaskReference;

		public AsyncDrawable(Resources res, ImageLoaderTask task) {
			super(res, (Bitmap)null);
			mTaskReference = new WeakReference<ImageLoaderTask>(task);
		}

		public ImageLoaderTask getLoaderTask() {
			return mTaskReference.get();
		}
	}

	private class ImageLoaderTask extends AsyncTask<Object, Void, BitmapDrawable> {
		private WeakReference<ImageView> mImageView;
		private Object mData = null;
		private OnCompleteListener mListener;
		
		private static final int MODE_IMAGEVIEW = 0;
		private static final int MODE_LISTENER = 1;
		private int mLoaderMode = MODE_IMAGEVIEW;

		public ImageLoaderTask(ImageView imageView) {
			mImageView = new WeakReference<ImageView>(imageView);
			mLoaderMode = MODE_IMAGEVIEW;
		}
		
		public ImageLoaderTask(OnCompleteListener l) {
			mListener = l;
			mLoaderMode = MODE_LISTENER;
		}

        public Object getData() {
            return mData;
        }

        @SuppressWarnings("deprecation")
        @Override
        protected BitmapDrawable doInBackground(Object... objects) {
            mData = objects[0];
            return new BitmapDrawable(onCreateBitmap(mData));
        }

        @Override
		protected void onPostExecute(BitmapDrawable result) {
			if (mLoaderMode == MODE_IMAGEVIEW) {
				ImageView imageView = getAttachedImageView();
				if (imageView != null && result != null) {
					if (mIsTransitionEnable) {
						final TransitionDrawable td = new TransitionDrawable(new Drawable[] {
								new ColorDrawable(android.R.color.transparent), result });
						imageView.setImageDrawable(new ColorDrawable(android.R.color.transparent));
						imageView.setImageDrawable(td);
						td.startTransition(mTransitionTime);
					} else {
						imageView.setImageDrawable(result);
					}
				}
			} else {
				if (mListener != null) {
					mListener.onComplete(result == null? null : result.getBitmap());
				}
			}
			super.onPostExecute(result);
		}

		private ImageView getAttachedImageView() {
			ImageView imageView = mImageView.get();
			if (this == getAttachedTask(imageView)) {
				return imageView;
			}
			return null;
		}
	}

    protected abstract Bitmap onCreateBitmap(Object data);

	public static interface OnCompleteListener {
		public void onComplete(Bitmap bitmap);
	}

    /**
     * The Image memory cache for image loader
     */
    private static class MemCache {
        private int mCapacity = 3 * 1024 * 1024; // 3MB Memory bitmap cache
        private LruCache<Object, BitmapDrawable> mCache;
        private final Object mLock = new Object();

        public MemCache(int capacity) {
            if (capacity > 0) {
                mCapacity = capacity;
            }
            mCache = new LruCache<Object, BitmapDrawable>(mCapacity) {
                protected int sizeOf(Object key, BitmapDrawable value) {
                    return value.getBitmap().getRowBytes() * value.getBitmap().getHeight();
                }

                @Override
                protected void entryRemoved(boolean evicted, Object key,
                                            BitmapDrawable oldValue, BitmapDrawable newValue) {
                    super.entryRemoved(evicted, key, oldValue, newValue);
                }
            };
        }

        public void addBitmap(String key, BitmapDrawable bitmap) {
            if (bitmap == null) {
                return;
            }
            synchronized (mCache) {
                mCache.put(key, bitmap);
            }
        }

        public BitmapDrawable getBitmap(Object key) {
            synchronized (mCache) {
                BitmapDrawable bitmap = mCache.get(key);
                // Because of the Transition Animation, the alpha in memory
                // may be set to smaller than 255.
                // Just restore the alpha value to 255.
                if (bitmap != null) {
                    bitmap.setAlpha(255);
                }
                return bitmap;
            }
        }

        public boolean containsKey(String key) {
            synchronized (mCache) {
                return mCache.snapshot().containsKey(key);
            }
        }

        public void clear() {
            synchronized (mCache) {
                mCache.evictAll();
                mCache = null;
            }
        }
    }
}

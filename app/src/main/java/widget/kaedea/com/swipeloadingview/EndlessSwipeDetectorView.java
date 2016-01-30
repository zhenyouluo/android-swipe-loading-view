package widget.kaedea.com.swipeloadingview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import widget.kaedea.com.swipeloadingview.util.LogUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created kaede on 1/26/16.
 */
public class EndlessSwipeDetectorView extends View {
	public static final String TAG = "SwipeDetectorLayout";

	ITouchEventProxy iTouchEventProxy;
	OnSwipeListener mOnSwipeListener;

	AtomicBoolean isCreated = new AtomicBoolean();
	View mLoadingView;
	float mSwipeRatio;
	private float mSwipeRatioThreshold;
	private int mDuration = 500;

	public EndlessSwipeDetectorView(Context context) {
		super(context);
		init();
	}

	public EndlessSwipeDetectorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public EndlessSwipeDetectorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public EndlessSwipeDetectorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}

	private void init() {
		isCreated.set(false);
		setInterceptTouchEvent(true);
		mSwipeRatioThreshold = 0.5f;
		iTouchEventProxy = new ITouchEventProxy() {
			int threshold = 100;

			@Override
			public int getThreshold() {
				LogUtil.i(TAG, "[getThreshold] threshold =" + threshold);
				return threshold;
			}

			@Override
			public void onPreTouch(int direction) {
				// start swipe
				LogUtil.i(TAG, "[onPreTouch] start swipe, direction = " + direction);
				if (direction == SwipeConstants.SWIPE_TO_UP) {
					resetLoadingViewPosition(SwipeConstants.MODE_BOTTOM);
				} else {
					resetLoadingViewPosition(SwipeConstants.MODE_ABOVE);
				}
			}

			@Override
			public void onTouchOffset(float offsetY, int direction) {
				float targetTranslationY = mLoadingView.getTranslationY() + offsetY;
				if (direction == SwipeConstants.SWIPE_TO_UP) {
					mSwipeRatio = 1f - mLoadingView.getTranslationY() / getTotalHeight();
					LogUtil.d(TAG, "[onTouchOffset] direction =" + direction);
					LogUtil.d(TAG, "[onTouchOffset] mSwipeRatio =" + mSwipeRatio);
					LogUtil.d(TAG, "[onTouchOffset] offsetY =" + offsetY + " mLoadingView.getTranslationY() = " + mLoadingView.getTranslationY() + " targetTranslationY=" + targetTranslationY);
					if (offsetY < 0f && targetTranslationY < 0f) {
						// can not continue swipe up
						LogUtil.i(TAG, "[onTouchOffset] can not continue swipe up!");
						resetLoadingViewPosition(SwipeConstants.MODE_CENTER);
						return;
					}
				} else {
					// swipe up to down
					mSwipeRatio = mLoadingView.getTranslationY() / getTotalHeight() + 1;
					LogUtil.d(TAG, "[onTouchOffset] direction =" + direction);
					LogUtil.d(TAG, "[onTouchOffset] mSwipeRatio =" + mSwipeRatio);
					LogUtil.d(TAG, "[onTouchOffset] offsetY =" + offsetY + " mLoadingView.getTranslationY() = " + mLoadingView.getTranslationY() + " targetTranslationY=" + targetTranslationY);
					if (offsetY > 0f && targetTranslationY > getTotalHeight()) {
						// can not continue swipe down
						LogUtil.i(TAG, "[onTouchOffset] can not continue swipe down!");
						resetLoadingViewPosition(SwipeConstants.MODE_CENTER);
						return;
					}
				}
				translateLoadingView(targetTranslationY);
				// Notify swipe event's progress.
				LogUtil.d(TAG, "[onTouchOffset] Notify swipe event's progress.");
				if (mOnSwipeListener != null)
					mOnSwipeListener.onSwiping(mSwipeRatio, mDirection);
			}

			@Override
			public void onPostTouch(int direction) {
				if (mLoadingView.getTranslationY() > getTotalHeight()) {
					// below the bottom end
					LogUtil.i(TAG, "[onPostTouch] below the bottom end");
					resetLoadingViewPosition(SwipeConstants.MODE_BOTTOM);
					if (mOnSwipeListener != null)
						mOnSwipeListener.onSwipeCanceled(mDirection);
				} else if (mLoadingView.getTranslationY() < -getTotalHeight()) {
					// above the top end
					LogUtil.i(TAG, "[onPostTouch] above the top end");
					resetLoadingViewPosition(SwipeConstants.MODE_ABOVE);
					if (mOnSwipeListener != null)
						mOnSwipeListener.onSwipeCanceled(mDirection);
				} else {
					if (mSwipeRatio <= mSwipeRatioThreshold) {
						// Can not reach the "Swipe Threshold", therefore taking it as Cancel;
						hideLoadingView(true, direction, new SwipeAnimatorListener() {

							@Override
							public void onAnimationEnd(Animator animation) {
								LogUtil.i(TAG, "[onPostTouch] Swipe Cancel");
								if (mOnSwipeListener != null)
									mOnSwipeListener.onSwipeCanceled(mDirection);
							}
						});

					} else {
						// Reach the "Swipe Threshold", therefore taking it as Finish;
						showLoadingView(true, direction, new SwipeAnimatorListener() {
							@Override
							public void onAnimationEnd(Animator animation) {
								LogUtil.i(TAG, "[onPostTouch] Swipe Finish");
								if (mOnSwipeListener != null)
									mOnSwipeListener.onSwipeFinished(mDirection);
							}
						});

					}
				}
			}
		};
	}

	private void translateLoadingView(float translate) {
		if (mLoadingView == null) {
			LogUtil.w(TAG, "[translateLoadingView] mLoadingView is null");
			return;
		}
		LogUtil.d(TAG, "[translateLoadingView] translate mLoadingView, translate= " + translate);
		ViewCompat.setTranslationY(mLoadingView, translate);
	}

	private void setInterceptTouchEvent(boolean isConsume) {
		setClickable(isConsume); // Pause or resume detect swipe event.
	}

	private boolean isInterceptTouchEvent() {
		return isClickable();
	}

	public void hideLoadingView(boolean isShowAnimation, int direction, SwipeAnimatorListener listener) {
		if (mLoadingView == null) {
			LogUtil.w(TAG, "[hideLoadingView] mLoadingView is null");
			return;
		}

		LogUtil.d(TAG, "[hideLoadingView] isShowAnimation = " + isShowAnimation + " direction= " + direction);

		if (direction == SwipeConstants.SWIPE_UNKNOWN) {
			setInterceptTouchEvent(false);
			if (listener != null) listener.onAnimationStart(null);
			resetLoadingViewPosition(SwipeConstants.MODE_BOTTOM);
			if (listener != null) listener.onAnimationEnd(null);
			setInterceptTouchEvent(true);
			return;
		}

		float targetTranslateY;
		if (direction == SwipeConstants.SWIPE_TO_UP) {
			if (!isShowAnimation) {
				setInterceptTouchEvent(false);
				if (listener != null) listener.onAnimationStart(null);
				resetLoadingViewPosition(SwipeConstants.MODE_BOTTOM);
				if (listener != null) listener.onAnimationEnd(null);
				setInterceptTouchEvent(true);
				return;
			}
			targetTranslateY = getTotalHeight();
		} else {
			if (!isShowAnimation) {
				setInterceptTouchEvent(false);
				if (listener != null) listener.onAnimationStart(null);
				resetLoadingViewPosition(SwipeConstants.MODE_ABOVE);
				if (listener != null) listener.onAnimationEnd(null);
				setInterceptTouchEvent(true);
				return;
			}
			targetTranslateY = -getTotalHeight();
		}
		// Execute animation job.
		ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mLoadingView, "translationY", mLoadingView.getTranslationY(), targetTranslateY);
		objectAnimator.setDuration(mDuration);
		objectAnimator.addListener(new SwipeAnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
				setInterceptTouchEvent(false);
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				setInterceptTouchEvent(true);
			}
		});
		if (listener != null) objectAnimator.addListener(listener);
		objectAnimator.start();
	}

	public void showLoadingView(boolean isShowAnimation, int direction, SwipeAnimatorListener listener) {
		if (mLoadingView == null) {
			LogUtil.w(TAG, "[showLoadingView] mLoadingView is null");
			return;
		}

		LogUtil.d(TAG, "[showLoadingView] isShowAnimation = " + isShowAnimation + " direction= " + direction);

		if (direction == SwipeConstants.SWIPE_UNKNOWN) {
			setInterceptTouchEvent(false);
			if (listener != null) listener.onAnimationStart(null);
			resetLoadingViewPosition(SwipeConstants.MODE_CENTER);
			if (listener != null) listener.onAnimationEnd(null);
		}

		if (!isShowAnimation) {
			setInterceptTouchEvent(false);
			if (listener != null) listener.onAnimationStart(null);
			resetLoadingViewPosition(SwipeConstants.MODE_CENTER);
			if (listener != null) listener.onAnimationEnd(null);
			return;
		}
		// Execute animation job.
		ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(mLoadingView, "translationY", mLoadingView.getTranslationY(), 0f);
		objectAnimator.setDuration(mDuration);
		objectAnimator.addListener(new SwipeAnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
				setInterceptTouchEvent(false);
			}
		});
		if (listener != null) objectAnimator.addListener(listener);
		objectAnimator.start();
	}


	private void resetLoadingViewPosition(int mode) {
		if (mLoadingView == null) {
			LogUtil.w(TAG, "[resetLoadingViewPosition] mLoadingView is null");
			return;
		}
		switch (mode) {
			case SwipeConstants.MODE_ABOVE:
				ViewCompat.setTranslationY(mLoadingView, -getTotalHeight());
				break;
			case SwipeConstants.MODE_CENTER:
				ViewCompat.setTranslationY(mLoadingView, 0f);
				break;
			case SwipeConstants.MODE_BOTTOM:
				ViewCompat.setTranslationY(mLoadingView, getTotalHeight());
				break;
		}
	}

	private int getTotalHeight() {
		return this.getMeasuredHeight();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Hide the loading view in the very beginning.
		if (!isCreated.get()) {
			hideLoadingView(false, SwipeConstants.SWIPE_UNKNOWN, null);
			isCreated.set(true);
		}

	}

	float y_pre = 0;
	float y_down = 0;
	int mDirection = SwipeConstants.SWIPE_UNKNOWN;
	boolean isBeginSwipe = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (iTouchEventProxy == null) return super.onTouchEvent(event);

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				logEventInfo("ACTION_DOWN", event);
				y_down = event.getY();
				y_pre = event.getY();
				mDirection = SwipeConstants.SWIPE_UNKNOWN;
				isBeginSwipe = false;
				break;
			case MotionEvent.ACTION_MOVE:
				logEventInfo("ACTION_MOVE", event);
				if (isBeginSwipe) {
					iTouchEventProxy.onTouchOffset(event.getY() - y_pre, mDirection);
				} else if (Math.abs(event.getY() - y_down) >= iTouchEventProxy.getThreshold()) {
					if (event.getY() <= y_down) {
						// down to up
						mDirection = SwipeConstants.SWIPE_TO_UP;
					} else {
						// up to down
						mDirection = SwipeConstants.SWIPE_TO_DOWN;
					}
					iTouchEventProxy.onPreTouch(mDirection);
					isBeginSwipe = true;
					iTouchEventProxy.onTouchOffset(event.getY() - y_pre, mDirection);
				}
				y_pre = event.getY();
				break;
			default:
				LogUtil.i(TAG, "Action = " + event.getAction());
				logEventInfo("ACTION_OTHERS", event);
				y_down = 0;
				y_pre = 0;
				if (isBeginSwipe) {
					isBeginSwipe = false;
					iTouchEventProxy.onPostTouch(mDirection);
				}
				break;
		}
		return isInterceptTouchEvent() || super.onTouchEvent(event);
	}

	private void logEventInfo(String type, MotionEvent event) {
		LogUtil.d(TAG, "[onTouchEvent][logEventInfo] " + type + " getY= " + event.getY() + "; getRawY=" + event.getRawY());
	}

	public void setLoadingView(View loadingView) {
		this.mLoadingView = loadingView;
	}

	public void setOnSwipeListener(OnSwipeListener onSwipeListener) {
		this.mOnSwipeListener = onSwipeListener;
	}

	public int getDirection() {
		return mDirection;
	}

	public void setAnimationDuration(int mDuration) {
		this.mDuration = mDuration;
	}

	public interface ITouchEventProxy {
		public int getThreshold();

		public void onPreTouch(int direction);

		public void onTouchOffset(float offsetY, int direction);

		public void onPostTouch(int direction);
	}


}
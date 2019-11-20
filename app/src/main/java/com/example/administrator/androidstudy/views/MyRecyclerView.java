package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.os.TraceCompat;
import android.support.v4.util.TimeUtils;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.widget.ChildHelper;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerViewAccessibilityDelegate;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
//import android.support.v7.widget.

/**
 * Created by ljm on 2018/6/1.
 */
public class MyRecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild {
    private static final String TAG = "MyRecyclerView";

    private static final boolean DEBUG = false;

    private static final int[] NESTED_SCROLLING_ATTRSS = {android.R.attr.nestedScrollingEnabled};
    private static final int[] CLIP_TO_PADDING_ATTR = {android.R.attr.clipToPadding};

    private static final boolean FORCE_INVALIDATE_DISPLAY_LIST = Build.VERSION.SDK_INT == 18
            || Build.VERSION.SDK_INT == 19 || Build.VERSION.SDK_INT == 20;
    private static final boolean ALLOW_SIZE_IN_UNSPECIFIED_SPEC = Build.VERSION.SDK_INT >= 23;
    private static final boolean POST_UPDATE_ON_ANIMATION = Build.VERSION.SDK_INT >= 16;
    private static final boolean ALLOW_PREFETCHING = Build.VERSION.SDK_INT >= 21;
    private static final boolean DISPATCH_TEMP_DETACH = false;
    public static final int HORIZONTAL = 0;
    public static final int VERITICAL = 1;

    public static final int NO_POSITION = -1;
    public static final int INVALID_TYPE = -1;
    public static final long NO_ID = -1;

    public static final int TOUCH_SLOP_DEFAULT = 0;
    public static final int TOUCH_SLOP_PAGING = 1;

    private static final int MAX_SCROLL_DURATION = 2000;
    private static final String TRACE_SCROLL_TAG = "RV scroll";
    private static final String TRACE_ON_LAYOUT_TAG = "RV OnLayout";
    private static final String TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG = "RV FullInvalidate";
    private static final String TRACE_HANDLE_ADAPTER_UPDATES_TAG = "RV PartialInvalidate";
    private static final String TRACE_BIND_VIEW_TAG = "Rv OnBindView";
    private static final String TRACE_PREFETCH_TAG = "Rv Prefetch";

    private static final String TRACE_CREATE_VIEW_TAG = "RV CreateView";
    private static final Class<?>[] LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE = new Class[]{
            Context.class, AttributeSet.class, int.class, int.class
    };

    private final RecyclerViewDataObserver mObserver = new RecyclerViewDataObserver();
    private final Recycler mRecycler = new Recycler();
    private SavedState mPendingSavedState;

    private AdapterHelper mAdapterHelper;
    private ChildHelper mChildHelper;

    private final ViewInfoStore mViewInfoStore = new ViewInfoStore();
    boolean mClipToPadding;

    final Runnable mUpdateChildViewsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mFirstLayoutComplete || isLayoutRequested()) {
                return;
            }
            if (!mIsAttached) {
                requestLayout();
                return;
            }
            if (mLayoutFrozen) {
                mLayoutRequestEaten = true;
                return;
            }
            consumePendingUpdateOperations();
        }
    };
    final Rect mTempRect = new Rect();
    private final Rect mTempRect2 = new Rect();
    final RectF mTempRectF = new RectF();

    MyRecyclerView.Adapter mAdapter;  //// TODO: 2018/6/1
    MyRecyclerView.LayoutManager mLayout;
    MyRecyclerView.RecyclerListener mRecyclerListener;
    final ArrayList<ItemDecoration> mItemDecorations = new ArrayList<>();
    private final ArrayList<RecyclerView.OnItemTouchListener> mOnItemTouchListeners = new ArrayList<>();
    private RecyclerView.OnItemTouchListener mActiveOnItemTouchListener;
    boolean mIsAttached;
    boolean mHasFixedSize;
    boolean mFirstLayoutComplete;

    private int mEatRequestLayout = 0;
    boolean mLayoutRequestEaten;
    boolean mLayoutFrozen;
    private boolean mIgnoreMotionEventTillDown;

    private int mEatenAccesibilityChangeFlags;
    boolean mAdapterUpdateDuringMeasure;
    private final AccessibilityManager mAccesibilityManager;
    private List<RecyclerView.OnChildAttachStateChangeListener> mOnChildAttachStateListeners;

    boolean mDataSetHasChangedAfterLayout = false;
    private int mLayoutOrScrollCounter = 0;
    private int mDispatchScrollCounter = 0;

    private EdgeEffectCompat mLeftGlow, mTopGlow, mRightGlow, mBottomGlow;
    MyRecyclerView.ItemAnimator mItemAnimator;

    private static final int INVALID_POINTER = -1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private VelocityTracker mVelocityTracker;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mLastTouchX;
    private int mLastTouchY;
    private int mTouchSlop;
    private RecyclerView.OnFlingListener mOnFlingListener;
    private final int mMaxFlingVelocity;
    private final int mMinFlingVelocity;
    private float mScrollFactor = Float.MIN_VALUE;
    private boolean mPreserveFocusAfterLayout = true;

    final ViewFlinger mViewFlinger = new ViewFlinger();

    private static final long MIN_PREFETCH_TIME_NANOS = TimeUnit.MILLISECONDS.toNanos(4);
    static long sFrameIntervalNanos = 0;
    ViewPrefetcher mViewPrefetcher = ALLOW_PREFETCHING ? new ViewPrefetcher() : null;

    final MyRecyclerView.State mState = new MyRecyclerView.State();

    private RecyclerView.OnScrollListener mOnScrollListener;
    private List<RecyclerView.OnScrollListener> mOnScrollListeners;

    //for use in item animations
    boolean mItemAddedOrRemoved = false;
    boolean mItemChanged = false;
    private RecyclerView.ItemAnimator.ItemAnimatorListener mItemAnimatorListener =
            new RecyclerView.ItemAnimator.ItemAnimatorListener();
    boolean mPostedAnimatorRunner = false;
    RecyclerViewAccessibilityDelegate mAccessibilityDelegate;
    private RecyclerView.ChildDrawingOrderCallback mChildDrawingOrderCallback;

    private int[] mMinMaxLayoutPositions = new int[2];
    private NestedScrollingChildHelper mScrollingChildHelper;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private final int[] mNestedOffsets = new int[2];

    private List<RecyclerView.ViewHolder> mPendingAccessibilityImportanceChange = new ArrayList<>();

    private Runnable mItemAnimatorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mItemAnimator != null) {
                mItemAnimator.runPendingAnimations();
            }
            mPostedAnimatorRunner = false;
        }
    };
    static final Interpolator mQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return (float) Math.pow(t, 5) + 1.0f;
        }
    };

    private final ViewInfoStore.ProcessCallback mViewInfoProcessCallback =
            new ViewInfoStore.ProcessCallback() {
                @Override
                public void processDisappeared(RecyclerView.ViewHolder viewHolder,
                                               RecyclerView.ItemAnimator.ItemHolderInfo info,
                                               RecyclerView.ItemAnimator.ItemHolderInfo postInfo) {
                    mRecycler.unscrapView(viewHolder);
                    animateDisappearance(viewHolder, info, postInfo);
                }
                @Override
                public void processAppeared(RecyclerView.ViewHolder viewHolder,
                                            RecyclerView.ItemAnimator.ItemHolderInfo preInfo,
                                            RecyclerView.ItemAnimator.ItemHolderInfo info) {
                    animateAppearance(viewHolder, preInfo, info);
                }
                @Override
                public void processPersistent(RecyclerView.ViewHolder viewHolder,
                                              RecyclerView.ItemAnimator.ItemHolderInfo preInfo,
                                              RecyclerView.ItemAnimator.ItemHolderInfo postInfo) {
                    viewHolder.setIsRecyclable(false);
                    if (mDataSetHasChangedAfterLayout) {
                        if (mItemAnimator.animateChange(viewHolder, viewHolder, preInfo, postInfo)) {
                            postAnimationRunner();
                        }
                    } else if (mItemAnimator.animatePersistence(viewHolder, preInfo, postInfo)) {
                        postAnimationRunner();
                    }
                }
                @Override
                public void unused(RecyclerView.ViewHolder viewHolder) {
                    mLayout.removeAndRecycleView(viewHolder.itemView, mRecycler);
                }
    };


    public MyRecyclerView(Context context) {
        this(context, null);
    }

    public MyRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyRecyclerView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        if (attr != null) {
            TypedArray a = context.obtainStyledAttributes(attr, CLIP_TO_PADDING_ATTR, defStyle, 0);
            mClipToPadding = a.getBoolean(0, true);
            a.recycle();
        } else {
            mClipToPadding = true;
        }
        setScrollContainer(true);
        setFocusableInTouchMode(true);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);
        mItemAnimator.setListener(mItemAnimatorListener);
        initAdapterManager();
        initChildrenHelper();
        if (ViewCompat.getImportantForAccessibility(this)==ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO){
            ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
        mAccesibilityManager = (AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        setAccessibilityDelegateCompat(new RecyclerViewAccessibilityDelegate(this));

        boolean nestedScrollingEnabled = true;

        if (attr != null) {
            int defStyleRes = 0;
            TypedArray a = context.obtainStyledAttributes(attr, R.styleable.RecyclerView, defStyle, defStyleRes);
            String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);
            int descendantFocusability = a.getInt(R.styleable.RRR, -1);
            if (descendantFocusability == -1) {
                setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
            a.recycle();
            createLayoutManager(context, layoutManagerName, attr, defStyle, defStyleRes);

            if (Build.VERSION.SDK_INT >= 21) {
                a = context.obtainStyledAttributes(attr, NESTED_SCROLLING_ATTRSS, defStyle, defStyleRes);
                nestedScrollingEnabled = a.getBoolean(0, true);
                a.recycle();
            }
        } else {
            setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }
        setNestedScrollingEnabled(nestedScrollingEnabled);
    }

    public RecyclerViewAccessibilityDelegate getCompatAccessibilityDelegate() {
        return mAccessibilityDelegate;
    }

    public void setAccessibilityDelegateCompat(RecyclerViewAccessibilityDelegate d) {
        mAccessibilityDelegate = d;
        ViewCompat.setAccessibilityDelegate(this, mAccessibilityDelegate);
    }

    private void createLayoutManager(Context context, String className, AttributeSet attrs,
                                     int defStyleAttr, int defStyleRes) {
        if (className != null) {
            className = className.trim();
            if (className.length() != 0) {  // Can't use isEmpty since it was added in API 9.
                className = getFullClassName(context, className);
                try {
                    ClassLoader classLoader;
                    if (isInEditMode()) {
                        // Stupid layoutlib cannot handle simple class loaders.
                        classLoader = this.getClass().getClassLoader();
                    } else {
                        classLoader = context.getClassLoader();
                    }
                    Class<? extends LayoutManager> layoutManagerClass =
                            classLoader.loadClass(className).asSubclass(LayoutManager.class);
                    Constructor<? extends LayoutManager> constructor;
                    Object[] constructorArgs = null;
                    try {
                        constructor = layoutManagerClass
                                .getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
                        constructorArgs = new Object[]{context, attrs, defStyleAttr, defStyleRes};
                    } catch (NoSuchMethodException e) {
                        try {
                            constructor = layoutManagerClass.getConstructor();
                        } catch (NoSuchMethodException e1) {
                            e1.initCause(e);
                            throw new IllegalStateException(attrs.getPositionDescription() +
                                    ": Error creating LayoutManager " + className, e1);
                        }
                    }
                    constructor.setAccessible(true);
                    setLayoutManager(constructor.newInstance(constructorArgs));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Unable to find LayoutManager " + className, e);
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (InstantiationException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Could not instantiate the LayoutManager: " + className, e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Cannot access non-public constructor " + className, e);
                } catch (ClassCastException e) {
                    throw new IllegalStateException(attrs.getPositionDescription()
                            + ": Class is not a LayoutManager " + className, e);
                }
            }
        }
    }

    private String getFullClassName(Context context, String className) {
        if (className.charAt(0) == '.') {
            return context.getPackageName() + className;
        }
        if (className.contains(".")) {
            return className;
        }
        return RecyclerView.class.getPackage().getName() + '.' + className;
    }

    private void initChildrenHelper() {
        mChildHelper = new ChildHelper(new ChildHelper.Callback() {
            @Override
            public int getChildCount() {
                return RecyclerView.this.getChildCount();
            }

            @Override
            public void addView(View child, int index) {
                RecyclerView.this.addView(child, index);
                dispatchChildAttached(child);
            }

            @Override
            public int indexOfChild(View view) {
                return RecyclerView.this.indexOfChild(view);
            }

            @Override
            public void removeViewAt(int index) {
                final View child = RecyclerView.this.getChildAt(index);
                if (child != null) {
                    dispatchChildDetached(child);
                }
                RecyclerView.this.removeViewAt(index);
            }

            @Override
            public View getChildAt(int offset) {
                return RecyclerView.this.getChildAt(offset);
            }

            @Override
            public void removeAllViews() {
                final int count = getChildCount();
                for (int i = 0; i < count; i ++) {
                    dispatchChildDetached(getChildAt(i));
                }
                RecyclerView.this.removeAllViews();
            }

            @Override
            public ViewHolder getChildViewHolder(View view) {
                return getChildViewHolderInt(view);
            }

            @Override
            public void attachViewToParent(View child, int index,
                                           ViewGroup.LayoutParams layoutParams) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    if (!vh.isTmpDetached() && !vh.shouldIgnore()) {
                        throw new IllegalArgumentException("Called attach on a child which is not"
                                + " detached: " + vh);
                    }
                    if (DEBUG) {
                        Log.d(TAG, "reAttach " + vh);
                    }
                    vh.clearTmpDetachFlag();
                }
                RecyclerView.this.attachViewToParent(child, index, layoutParams);
            }

            @Override
            public void detachViewFromParent(int offset) {
                final View view = getChildAt(offset);
                if (view != null) {
                    final ViewHolder vh = getChildViewHolderInt(view);
                    if (vh != null) {
                        if (vh.isTmpDetached() && !vh.shouldIgnore()) {
                            throw new IllegalArgumentException("called detach on an already"
                                    + " detached child " + vh);
                        }
                        if (DEBUG) {
                            Log.d(TAG, "tmpDetach " + vh);
                        }
                        vh.addFlags(ViewHolder.FLAG_TMP_DETACHED);
                    }
                }
                RecyclerView.this.detachViewFromParent(offset);
            }

            @Override
            public void onEnteredHiddenState(View child) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    vh.onEnteredHiddenState(RecyclerView.this);
                }
            }

            @Override
            public void onLeftHiddenState(View child) {
                final ViewHolder vh = getChildViewHolderInt(child);
                if (vh != null) {
                    vh.onLeftHiddenState(RecyclerView.this);
                }
            }
        });
    }

    void initAdapterManager() {
        mAdapterHelper = new AdapterHelper(new CallBack() {
            public ViewHolder findViewHolder(int position) {
                final ViewHolder vh = findViewHolderForPosition(position, true);
                if (vh == null) return null;
                if (mChildHelper.isHidden(vh.itemView)) {
                    return null;
                }
                return vh;
            }

            public void offsetPositionsForRemovingInvisible(int start, int count) {
                offsetPositionRecordsForRemove(start, count, true);
                mItemAddedOrRemoved = true;
                mState.mDeletedInvisibleItemCountSincePreviousLayout += count;
            }

            public void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount) {
                offsetPositionRecordsForRemove(positionStart, itemCount, false);
                mItemsAddedOrRemoved = true;
            }

            public void markViewHoldersUpdated(int positionStart, int itemCount, Object payload) {
                viewRangeUpdate(positionStart, itemCount, payload);
                mItemChanged = true;
            }

            public void onDispatchFirstPass(UpdateOp op) {
                dispatchUpdate(op);
            }

            void dispatchUpdate(UpdateOp op) {
                switch (op.cmd) {
                    case UpdateOp.ADD:
                        mLayout.onItemsAdded(RecyclerView.this, op.positionStart, op.itemCount);
                        break;
                    case UpdateOp.REMOVE:
                        mLayout.onItemsRemoved(RecyclerView.this, op.positionStart, op.itemCount);
                        break;
                    case UpdateOp.UPDATE:
                        mLayout.onItemsUpdated(RecyclerView.this, op.positionStart, op.itemCount,
                                op.payload);
                        break;
                    case UpdateOp.MOVE:
                        mLayout.onItemsMoved(RecyclerView.this, op.positionStart, op.itemCount, 1);
                        break;
                }
            }

            public void onDispatchSecondPass(UpdateOp op) {
                dispatchUpdate(op);
            }
            public void offsetPositionsForAdd(int positionStart, int itemCount) {
                offsetPositionRecordsForInsert(positionStart, itemCount);
                mItemsAddedOrRemoved = true;
            }

            public void offsetPositionsForMove(int from, int to) {
                offsetPositionRecordsForMove(from, to);
                // should we create mItemsMoved ?
                mItemsAddedOrRemoved = true;
            }
        });
    }

    public void setHasFixedSize(boolean hasFixSize){

    }

    public boolean hasFixSize() {
        return mHasFixedSize;
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (clipToPadding != mClipToPadding) {
            invalidateGlows();
        }
        mClipToPadding = clipToPadding;
        super.setClipToPadding(clipToPadding);
        if (mFirstLayoutComplete) {
            requestLayout();
        }
    }

    public boolean getClipToPadding() {
        return mClipToPadding;
    }

    public void setScrollingTouchSlop(int slopConstant) {
        final ViewConfiguration vc = ViewConfiguration.get(getContext());
        switch (slopConstant) {
            default:
                Log.w(TAG, "setScrollingTouchSlop(): bad argument constant "
                        + slopConstant + "; using default value");
                // fall-through
            case TOUCH_SLOP_DEFAULT:
                mTouchSlop = vc.getScaledTouchSlop();
                break;

            case TOUCH_SLOP_PAGING:
                mTouchSlop = vc.getScaledPagingTouchSlop();
                break;
        }
    }

    public void swapAdapter(Adapter adapter, boolean remove) {
        setLayoutFrozen(false);
        setAdapterInternal(adapter, true, remove);
        setDataSetChangedAfterLayout();
        requestLayout();
    }

    public void setAdapter(Adapter adapter) {
        setLayoutFrozen(false);
        setAdapterInternal(adapter, false, true);
        requestLayout();
    }

    private void setAdapterInternal(Adapter adapter, boolean compatibleWithPrevious, boolean remove) {
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mAdapter.onDetachedFromRecyclerView(this);
        }
        if (!compatibleWithPrevious || remove) {
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            if (mLayout != null) {
                mLayout.removeAndRecycleAllViews(mRecycler);
                mLayout.removeAndRecycleScrapInt(mRecycler);
            }
            mRecycler.clear();
        }
        mAdapterHelper.reset();
        final Adapter oldAdapter = mAdapter;
        mAdapter = adapter;
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
            adapter.onAttachedToRecyclerView(this);
        }
        if (mLayout != null) {
            mLayout.onAdapterChanged(oldAdapter, adapter);
        }
        mRecycler.onAdapterChanged(oldAdapter, adapter, compatibleWithPrevious);
        mState.mStructureChanged = true;
        markKnownViewsInvalid();
    }



    public Adapter getAdapter() {
        return mAdapter;
    }

    public void setRecyclerListener(RecyclerView.RecyclerListener listener) {
        mRecyclerListener = listener;
    }

    @Override
    public int getBaseline() {
        if (mLayout != null) {
            return mLayout.getBaseline();
        } else {
            return super.getBaseline();
        }
    }


    public void addOnChildAttachStateChangeListener(RecyclerView.OnChildAttachStateChangeListener l) {
        if (mOnChildAttachStateListeners == null) {
            mOnChildAttachStateListeners = new ArrayList<>();
        }
        mOnChildAttachStateListeners.add(listener);
    }

    public void removeOnChildAttachStateChangeListener(RecyclerView.OnChildAttachStateChangeListener l) {
        if (mOnChildAttachStateListeners == null) {
            return;
        }
        mOnChildAttachStateListeners.remove(listener);
    }

    public void clearOnChildAttachStateChangeListener(){
        if (mOnChildAttachStateListeners != null) {
            mOnChildAttachStateListeners.clear();
        }
    }

    public void setLayoutManner(MyRecyclerView.LayoutManager manager) {
        if(mLayout == manager) {
            return;
        }
        stopScroll();
        if (mLayout != null) {
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();

            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }

        mChildHelper.removeAllViewsUnfiltered();
        mLayout = manager;
        if (manager != null) {
            if(manager.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + manager +
                        " is already attached to a RecyclerView: " + manager.mRecyclerView);
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout();
    }



    public void setOnFlingListener(@Nullable OnFlingListener onFlingListener) {
        mOnFlingListener = onFlingListener;
    }


    public RecyclerView.OnFlingListener getOnFlingListener() {
        return mOnFlingListener;
    }

    @Override
    protected Parcelable onSaveInstanceState(){
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        if (mPendingSavedState != null) {
            savedState.copyFrom(mPendingSavedState);
        } else if (mLayout != null) {
            savedState.mLayoutState = mLayout.onSaveInstanceState();
        } else {
            savedState.mLayoutState = null;
        }
        return savedState;
    }



    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        mPendingSavedState = (SavedState) state;
        super.onRestoreInstanceState(mPendingSavedState.getSuperState());
        if (mLayout != null && mPendingSavedState.mLayoutState != null) {
            mLayout.onRestoreInstanceState(mPendingSavedState.mLayoutState);
        }
    }



    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }


    private void addAnimatingView(MyRecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        final boolean alreadyParented = view.getParent() == this;
        mRecycler.unscrapView(getChildViewHolder(view));
        if (holder.isTmpDetached()) {
            mChildHelper.attachViewToParent(view, -1, view.getLayoutParams(), true);
        } else if (!alreadyParented) {
            mChildHelper.addView(view, true);
        } else {
            mChildHelper.hide(view);
        }
    }

    boolean removeAnimatingView(View view) {
        eatRequestLayout();
        final boolean removed = mChildHelper.removeViewIfHidden(view);
        if (removed) {
            final ViewHolder viewHolder = getChildViewHolderInt(view);
            mRecycler.unscrapView(viewHolder);
            mRecycler.recycleViewHolderInternal(viewHolder);
            if (DEBUG) {
                Log.d(TAG, "after removing animated view: " + view + ", " + this);
            }
        }
        // only clear request eaten flag if we removed the view.
        resumeRequestLayout(!removed);
        return removed;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return mLayout;
    }

    public MyRecyclerView.RecycledViewPool getRecycledViewPool(){
        return mRecycler.getRecycledViewPool();
    }

    public void setRecycledViewPool(MyRecyclerView.RecycledViewPool pool) {
        mRecycler.setRecycledViewPool(pool);
    }

    public void setViewCacheExtension(MyRecyclerView.ViewCacheExtension ext) {
        mRecycler.setViewCacheExtension(ext);
    }

    public void setItemViewCacheSize(int size){
        mRecycler.setViewCachedSize(size);
    }

    public int getScrollState() {
        return mScrollState;
    }

    void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            stopScrollersInternal();
        }
        dispatchOnScrollStateChanged(state);
    }

    public void addItemDecoration(ItemDecoration decor, int index) {
        if (mLayout != null) {
            mLayout.assertInLayoutOrScroll("Cannot add item decoration during a scroll  or"
                    + " layout");
        }
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(false);
        }
        if (index < 0) {
            mItemDecorations.add(decor);
        } else {
            mItemDecorations.add(index, decor);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    public void addItemDecoration(ItemDecoration decor) {
        addItemDecoration(decor, -1);
    }


    public void removeItemDecoration(RecyclerView.ItemDecoration decor) {
        if (mLayout != null) {
            mLayout.assertInLayoutOrScroll("Cannot remove item decoration during a scroll  or"
                    + " layout");
        }
        mItemDecorations.remove(decor);
        if (mItemDecorations.isEmpty()) {
            setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);
        }
        markItemDecorInsetsDirty();
        requestLayout();
    }

    public void setChildDrawingOrderCallback(RecyclerView.ChildDrawingOrderCallback callback) {
        if (callback == mChildDrawingOrderCallback) {
            return;
        }
        mChildDrawingOrderCallback = callback;
        setChildrenDrawingOrderEnabled(mChildDrawingOrderCallback != null);
    }

    public void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mOnScrollListeners == null) {
            mOnScrollListeners = new ArrayList<>();
        }
        mOnScrollListeners.add(listener);
    }

    public void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mOnScrollListeners != null) {
            mOnScrollListeners.remove(listener);
        }
    }

    public void clearOnScrollListenr() {
        if (mOnScrollListeners != null) {
            mOnScrollListeners.clear();
        }
    }

    public void scrollToPosition(int position) {
        if (mLayoutFrozen) {
            return;
        }
        stopScroll();
        if (mLayout == null) {
            return;
        }
        mLayout.scrollToPosition(position);
        awakenScrollBars();
    }



    void jumpToPositionForSmoothScroller(int position) {
        if (mLayout == null) {
            return;
        }
        mLayout.scrollToPosition(position);
        awakenScrollBars();
    }

    public void smoothScrollToPosition(int position) {
        if (mLayoutFrozen) {
            return;
        }
        if (mLayout == null) {
            Log.e(TAG, "Cannot smooth scroll without a LayoutManager set. " +
                    "Call setLayoutManager with a non-null argument.");
            return;
        }
        mLayout.smoothScrollToPosition(this, mState, position);
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mLayout == null) {
            Log.e(TAG, "Cannot scroll without a LayoutManager set. " +
                    "Call setLayoutManager with a non-null argument.");
            return;
        }
        if (mLayoutFrozen) {
            return;
        }
        final boolean canScrollHorizontal = mLayout.canScrollHorizontally();
        final boolean canScrollVertical = mLayout.canScrollVertically();
        if (canScrollHorizontal || canScrollVertical) {
            scrollByInternal(canScrollHorizontal ? x : 0, canScrollVertical ? y : 0, null);
        }
    }

    void consumePendingUpdateOperations() {
        if (!mFirstLayoutComplete || mDataSetHasChangedAfterLayout) {
            TraceCompat.beginSection(TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG);
            dispatchLayout();
            TraceCompat.endSection();
            return;
        }
        if (!mAdapterHelper.hasPendingUpdates()) {
            return;
        }

        if (mAdapterHelper.hasAnyUpdateTypes(UpdateOp.UPDATE) && !mAdapterHelper
                .hasAnyUpdateTypes(UpdateOp.ADD | UpdateOp.REMOVE | UpdateOp.MOVE)) {
            TraceCompat.beginSection(TRACE_HANDLE_ADAPTER_UPDATES_TAG);
            eatRequestLayout();
            onEnterLayoutOrScroll();
            mAdapterHelper.preProcess();
            if (!mLayoutRequestEaten) {
                if (hasUpdatedView()) {
                    dispatchLayout();
                } else {
                    mAdapterHelper.consumePostponedUpdates();
                }
            }
            resumeRequestLayout(true);
            onExitLayoutOrScroll();
            TraceCompat.endSection();
        } else if (mAdapterHelper.hasPendingUpdates()){
            TraceCompat.beginSection(TRACE_ON_DATA_SET_CHANGE_LAYOUT_TAG);
            dispatchLayout();
            TraceCompat.endSection();
        }
    }

    private boolean hasUpdatedView() {
        final int childCount = mChildHelper.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            final ViewHolder holder = getChildViewHolderInt(mChildHelper.getChildAt(i));
            if (holder == null || holder.shouldIgnore()) {
                continue;
            }
            if (holder.isUpdated()) {
                return true;
            }
        }
        return false;
    }

    boolean scrollByInternal(int x, int y, MotionEvent ev) {

    }



    @Override
    public int  computeHorizontalScrollOffset() {

    }

    @Override
    public int computeHorizontalScrollExtent() {

    }

    @Override
    public int computeHorizontalScrollRange() {

    }

    @Override
    public int  computeVerticalScrollOffset() {

    }

    @Override
    public int computeVerticalScrollExtent() {

    }

    @Override
    public int computeVerticalScrollRange() {

    }



    void eatRequestLayout() {

    }

    void resumeRequestLayout(boolean performLayoutChildren) {

    }

    public void setLayoutFrozen(boolean frozen) {

    }

    public boolean isLayoutFrozen() {
        return mLayoutFrozen;
    }

    public void smoothScrollBy(int x, int y) {

    }

    public boolean fling(int velocityX, int velocityY) {

    }

    public void stopScroll() {

    }

    private void stopScrollersInternal() {

    }

    public int getMinFlingVelocity() {
        return mMinFlingVelocity;
    }

    public int getMaxFlingVelocity() {
        return mMaxFlingVelocity;
    }

    private void pullGlows(float x, float overscrollX, float y, float overscrollY) {

    }

    private void releaseGlows(){

    }

    void considerReleasingGlowsOnScroll(int dx, int dy) {

    }

    void absordGlows(int velocityX, int velocityY) {

    }

    void ensureLeftGlow(){

    }

    void ensurTopGlow() {

    }

    void ensurRightGlow() {

    }

    void ensureBottomGlow() {

    }

    void invalidateGlows(){

    }

    @Override
    public View forceSearch(View focused, int direction) {

    }

    private boolean isPreferredNextFocus(View focuesd, View next, int direction) {

    }

    @Override
    public void requestChildFocus(View Child, View focus) {

    }

    @Override
    public boolean requestChildRectangleOnScreen(View Child, Rect rect, boolean immediate){

    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {

    }

    @Override
    protected boolean onRequesetFocusInDecendants(int direction, Rect previouslyFocusedRect) {

    }

    @Override
    protected void onAttachedToWindow(){

    }

    @Override
    protected void onDetachedFromWindow(){

    }

    public boolean isAttachedToWindow() {

    }

    void assertInLayoutOnScroll(String message){

    }

    void assertNotInLayoutOrScroll(String message) {

    }

    public void addOnItemTouchListener(RecyclerView.OnItemTouchListener l) {

    }

    public void removeOnItemTouchListener(RecyclerView.OnItemTouchListener l) {
    }


    private boolean dispachOnItemTouchIntercept(MotionEvent e) {

    }

    private boolean disptachOnItemTouch(MotionEvent e) {

    }







    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {

    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallow) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

    }

    private void resetTouch() {

    }

    private void cancelTouch(){

    }

    private void onPointerUp(){

    }

    private boolean onGenericMotionEvent(MotionEvent e) {

    }



    private float getScrollFrator(){

    }


    @Override
    public void onMeasure(int widthSpec, int heightSpec) {

    }

    void defaultOnMeasure(int widthSpec, int heightSpec) {

    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {

    }



    public void setItemAnimator(RecyclerView.ItemAnimator animator) {

    }

    void onEnterLayoutOrScroll(){

    }

    void onExitLayoutOrScroll() {

    }

    boolean isAccessibilityEnabled() {

    }

    private void dispatchContentChangedIfNecessary(){

    }

    public boolean isComputingLayout() {

    }

    boolean shouldDeferAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {

    }

    public RecyclerView.ItemAnimator getItemAnimator() {

    }

    void postAnimationRunner() {

    }

    private boolean predictiveItemAnimationEnabled() {

    }

    private void processAdapterUpdatesAndSetAniamtionFlags() {

    }



    private void saveFocusInfo() {

    }
    private void resetFocusInfo() {

    }

    private void recoverFocusFromState() {

    }

    private int getDeepestFocusedViewWithId(View view) {

    }



    void dispatchLayout() {

    }

    private void dispatchLayoutStep1(){

    }
    private void dispatchLayoutStep2() {
    }
    private void dispatchLayoutStep3() {
    }


    private void handleMissingPreInfoForChangeError(long key, ViewHoder holder, ViewHolder oldHolder) {

    }

    void recordAnimationInfoIfBouncedHiddenView(ViewHolder holder, ItemAnimator.ItemHolderInfo info) {

    }

    private void findMinMaxChildLayoutPositions(int[] info) {

    }

    private boolean didChildRangeChange(int minPositionPreLayout, int maxPositionPreLayout) {

    }

    @Override
    public void removeDetachedView(View child, boolean animate) {

    }

    long getChangedHolderKey(ViewHolder holder) {

    }

    void animateAppearance(ViewHolder itemHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {

    }
    void animateDisappearance(ViewHolder itemHolder, ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {

    }
    void animateChange(ViewHolder oldHolder, Viewholder newHolder,
                       ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo,
                       boolean oldHolderDisappearing, boolean newHolderDisappearing) {

    }


    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    public void requestLayout() {

    }

    void markItemDecorInsetsDirty() {

    }

    @Override
    public void draw(Canvas c){

    }

    @Override
    public void onDraw(Canvas c) {

    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams params){

    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {

    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {

    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(LayoutParams p) {

    }



    public boolean isAnimating(){

    }

    void saveOldPosition() {

    }

    void clearOldPosition() {

    }

    void offsetPositionRecordsForMove(int from, int to) {

    }
    void offsetPositionRecordsForInsert (int positionStart, int itemCount) {

    }
    void offsetPositionRecordsForRemove(int positionStart, int itemCount, boolean applyToPerLayout) {

    }

    void viewRangeUpdate(int positionStart, int itemCount, Object payload){

    }

    boolean canReuseUpdatedViewHolder(ViewHolder holder) {

    }

    void setDataSetChangedAfterLayout(){

    }

    void markKnownViewsInvalid(){

    }

    public void invalidateItemDecorations(){

    }

    public boolean getPreserveFocusAfterLayout() {
        return mPreserveFocusAfterLayout;
    }

    public void setPreserveFocusAfterLayout(boolean b) {
        mPreserveFocusAfterLayout = b;
    }


    public ViewHolder getChildViewHolder(View child){

    }



    public View findContainingItemView(View view){

    }

    public ViewHolder findContainingViewHolder(View view) {

    }

    static ViewHolder getChildViewHolderInt(View child) {

    }

    public int getChildPosition(View child) {

    }

    public int getChildAdapterPosition(View view) {

    }

    public int getChildLayoutPosition(View view) {

    }

    public long getChildItemId(View child) {

    }

    public ViewHolder findViewHolderForPosition(int position) {

    }
    public ViewHolder findViewHolderForLayoutPosition(int position) {

    }

    public ViewHolder findViewHolderForAdapterPosition(int position) {
    }

    ViewHolder findViewHolderForPosition(int position, boolean checkNewPosition) {

    }

    public ViewHolder findViewHolderForItemId(long id) {
    }

    public View findChildViewUnder(float x, float y) {

    }

    public boolean drawChild(Canvas canvas, View child, long drawingTime) {

    }

    public void offsetChildrenVeritical(int dy) {

    }

    public void onChildAttachedToWindow(View child) {

    }

    public void onChildDetachedFromWindow(View child) {
    }

    public void offsetChildrenHorizontal(int dx){

    }

    public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {

    }

    static void getDecoratedBoundsWithMarginsInt(View view, Rect rect){

    }

    Rect getItemDecorInsetsForChild(View child) {

    }

    public void onScrolled(int dx, int dy) {

    }

    void dispatchOnScroll(int hresult, int vresult) {

    }

    public void onScrollSateChanged(int state) {

    }

    void dispatchOnScrollStateChanged(int state) {


    }

    public boolean hasPendingAdapterUpdates() {

    }



    class ViewPrefetcher implements Runnable{

    }

    private class ViewFlinger implements Runnable {

    }

    void repositionShadowingViews(){

    }


    void dispatchChildAttached(View child) {}
    void dispatchChildDetached(View child) {}
















    public abstract static class LayoutManager {
        ChildHelper mChildHelper;
        MyRecyclerView mRecyclerView;

        SmoothScroller mSmoothScroller;

        boolean mRequestedSimpleAnimations = false;

        boolean isAttachedToWindow = false;

        boolean mAutoMeasure = false;


        private boolean mMeasurementCacheEnabled = true;
        private boolean mItemPrefetchEnabled = true;

        private int mWidthMode, mHeightMode;
        private int mWidth, mHeight;

        void setRecyclerView(MyRecyclerView rv) {
            if (rv == null) {
                mRecyclerView = null;
                mChildHelper = null;
                mWidth = 0;
                mHeight = 0;
            } else {
                mRecyclerView = rv;
                mChildHelper = rv.mChildHelper;
                mWidth = rv.getWidth();
                mHeight = rv.getHeight();
            }
            mWidthMode = MeasureSpec.EXACTLY;
            mHeightMode = MeasureSpec.EXACTLY;
        }

        void setMeasureSpecs(int wSpec, int hSpec) {
            mWidth = MeasureSpec.getSize(wSpec);
            mWidthMode = MeasureSpec.getMode(wSpec);
            if (mWidthMode == MeasureSpec.UNSPECIFIED && !ALLOW_SIZE_IN_UNSPECIFIED_SPEC) {
                mWidth = 0;
            }
            mHeight = MeasureSpec.getSize(hSpec);
            mHeightMode = MeasureSpec.getMode(hSpec);
            if (mHeightMode == MeasureSpec.UNSPECIFIED && !ALLOW_SIZE_IN_UNSPECIFIED_SPEC) {
                mHeight = 0;
            }
        }

        void setMeasuredDimensionFromChildren(int widthSpec, int heightSpec) {
            final int count = getChildCount();
            if (count == 0) {
                mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
                return;
            }
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final Rect bounds = mRecyclerView.mTempRect;
                getDecoratedBoundsWithMargins(child, bounds);
                if (bounds.left < minX) {
                    minX = bounds.left;
                }
                if (bounds.right > maxX) {
                    maxX = bounds.right;
                }
                if (bounds.top < minY) {
                    minY = bounds.top;
                }
                if (bounds.bottom > maxY) {
                    maxY = bounds.bottom;
                }
            }
            mRecyclerView.mTempRect.set(minX, minY, maxX, maxY);
            setMeasuredDimension(mRecyclerView.mTempRect, widthSpec, heightSpec);
        }

        public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
            int usedWidth = childrenBounds.width() + getPaddingLeft() + getPaddingRight();
            int usedHeight = childrenBounds.height() + getPaddingTop() + getPaddingBottom();
            int width = chooseSize(wSpec, usedWidth, getMinimumWidth());
            int height = chooseSize(hSpec, usedHeight, getMinimumHeight());
            setMeasuredDimension(width, height);
        }

        public static int chooseSize(int spec, int desired, int min) {
            final int mode = MeasureSpec.getMode(spec);
            final int size = MeasureSpec.getSize(spec);
            switch (mode) {
                case MeasureSpec.EXACTLY:
                    return size;
                case MeasureSpec.AT_MOST:
                    return Math.min(size, Math.max(desired, min));
                case MeasureSpec.UNSPECIFIED:
                default:
                    return Math.max(desired, min);
            }
        }

        public void requestLayout() {
            if (mRecyclerView != null) {
                mRecyclerView.requestLayout();
            }
        }
        public void assertInLayoutOrScroll(String message) {
            if (mRecyclerView != null) {
                mRecyclerView.assertInLayoutOrScroll(message);
            }
        }

        public void setAutoMeasureEnabled(boolean enabled) {
            mAutoMeasure = enabled;
        }
        public boolean isAutoMeasureEnabled() {
            return mAutoMeasure;
        }
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }
        public final boolean isItemPrefetchEnabled() {
            return mItemPrefetchEnabled;
        }
        int getItemPrefetchCount() { return 0; }
        int gatherPrefetchIndices(int dx, int dy, State state, int[] outIndices) {
            return 0;
        }
        public boolean isAttachedToWindow() {
            return mIsAttachedToWindow;
        }


        public final void setItemPrefetchEnabled(boolean enabled) {
            if (enabled != mItemPrefetchEnabled) {
                mItemPrefetchEnabled = enabled;
                if (mRecyclerView != null) {
                    mRecyclerView.mRecycler.updateViewCacheSize();
                }
            }
        }

        void dispatchAttachedToWindow(MyRecyclerView view) {
            mIsAttachedToWindow = true;
            onAttachedToWindow(view);
        }

        void dispatchDetachedFromWindow(MyRecyclerView view, Recycler recycler) {
            mIsAttachedToWindow = false;
            onDetachedFromWindow(view, recycler);
        }

        /*postOnAnimation函数就是将runnable发到队列中执行，转调用postDelayed*/
        public void postOnAnimation(Runnable action) {
            if (mRecyclerView != null) {
                ViewCompat.postOnAnimation(mRecyclerView, action);
            }
        }
        public boolean removeCallbacks(Runnable action) {
            if (mRecyclerView != null) {
                return mRecyclerView.removeCallbacks(action);
            }
            return false;
        }

        @CallSuper
        public void onAttachedToWindow(RecyclerView view) {
        }

        /**
         * @deprecated
         * override {@link #onDetachedFromWindow(RecyclerView, Recycler)}
         */
        @Deprecated
        public void onDetachedFromWindow(RecyclerView view) {

        }
        @CallSuper
        public void onDetachedFromWindow(RecyclerView view, Recycler recycler) {
            onDetachedFromWindow(view);
        }

        public boolean getClipToPadding() {
            return mRecyclerView != null && mRecyclerView.mClipToPadding;
        }


        public void onLayoutChildren(Recycler recycler, State state) {
            Log.e(TAG, "You must override onLayoutChildren(Recycler recycler, State state) ");
        }

        public void onLayoutCompleted(State state) {
        }

        public abstract LayoutParams generateDefaultLayoutParams();
        public boolean checkLayoutParams(LayoutParams lp) {
            return lp != null;
        }
        public LayoutParams generateLayouParams(ViewGroup.LayoutParams lp) {
            if (lp instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) lp);
            } else if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams)lp);
            } else {
                return new LayoutParams(lp);
            }
        }
        public LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
            return new LayoutParams(c, attrs);
        }

        public int scrollHorizontallyBy(int dx, Recycler recycler, State state) {
            return 0;
        }
        public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
            return 0;
        }
        public boolean canScrollHorizontally() {
            return false;
        }
        public boolean canScrollVertically() {
            return false;
        }

        public void scrollToPosition(int position) {
            if (DEBUG) {
                Log.e(TAG, "You MUST implement scrollToPosition. It will soon become abstract");
            }
        }

        public void smoothScrollToPosition(MyRecyclerView recyclerView, State state,
                                           int position) {
            Log.e(TAG, "You must override smoothScrollToPosition to support smooth scrolling");
        }

        public void startSmoothScroll(SmoothScroller smoothScroller) {
            if (mSmoothScroller != null && smoothScroller != mSmoothScroller && mSmoothScroller.isRunning()){
                mSmoothScroller.stop();
            }
            mSmoothScroller = smoothScroller;
            mSmoothScroller.start(mRecyclerView, this);
        }

        public boolean isSmoothScrolling() {
            return mSmoothScroller != null && mSmoothScroller.isRunning();
        }

        public int getLayoutDirection() {
            return ViewCompat.getLayoutDirection(mRecyclerView);
        }

        public void endAniamtion(View view) {
            if (mRecyclerView.mItemAnimator != null) {
                mRecyclerView.mItemAnimator.endAnimation(getChildViewHolderInt(view));
            }
        }

        public void addDisappearingView(View child) {
            addDisappearingView(child, -1);
        }
        public void addDisappearingView(View child, int index) {
            addViewInt(child, index, true);
        }

        public void addView(View child) {
            addView(child, -1);
        }
        public void addView(View child, int index) {
            addViewInt(child, index, false);
        }
        private void addViewInt(View child, int index, boolean disappearing) {
            final ViewHolder holder = getChildViewHolderInt(child);
            if (disappearing || holder.isRemoved()) {
                mRecyclerView.mViewInfoStore.addToDisappearedInLayout(child);
            } else {
                mRecyclerView.mViewInfoStore.removeFromDisappearedInLayout(holder);
            }
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (holder.wasReturnedFromScrap() || holder.isScrap()) {
                if (holder.isScrap()) {
                    holder.unScrap();
                } else {
                    holder.clearReturnedFromScrapFlag();
                }
                mChildHelper.attachViewToParent(child, index, child.getLayoutParams(), false);
                if (DISPATCH_TEMP_DETACH) {
                    ViewCompat.dispatchFinishTemporaryDetach(child);
                }
            } else if (child.getParent() == mRecyclerView) {
                int currentIndex = mChildHelper.indexOfChild(child);
                if (index == -1) {
                    index = mChildHelper.getChildCount();
                }
                if (currentIndex == -1) {
                    throw new IllegalStateException("Added View has RecyclerView as parent but"
                            + " view is not a real child. Unfiltered index:"
                            + mRecyclerView.indexOfChild(child));
                }
                if (currentIndex != index) {
                    mRecyclerView.mLayout.moveView(currentIndex, index);
                }
            } else {
                mChildHelper.addView(child, index, false);
                lp.mInsetsDirty = true;
                if (mSmoothScroller != null && mSmoothScroller.isRunning()) {
                    mSmoothScroller.onChildAttachedToWindow(child);
                }
            }
            if (lp.mPendingInvalidate) {
                holder.itemView.invalidate();
                lp.mPendingInvalidate = false;
            }
        }

        public void removeView(View child) {
            mChildHelper.removeView(child);
        }
        public void removeViewAt(int index) {
            final View child = getChildAt(index);
            if (child != null) {
                mChildHelper.removeViewAt(index);
            }
        }

        public void removeAllViews() {
            // Only remove non-animating views
            final int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                mChildHelper.removeViewAt(i);
            }
        }

        public int getBaseline() {
            return -1;
        }

        public int getPosition(View view) {
            return ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
        }

        public int getItemViewType(View view) {
            return getChildViewHolderInt(view).getItemViewType();
        }

        public View findContainingItemView(View view) {
            if (mRecyclerView == null) {
                return null;
            }
            View found = mRecyclerView.findContainingItemView(view);
            if (found == null) {
                return null;
            }
            if (mChildHelper.isHidden(found)) {
                return null;
            }
            return found;
        }

        public View getViewByPosition(int position) {
            final int count = getChildCount();
            for (int i = 0; i < count; ++i) {
                View child = getChildAt(i);
                ViewHolder holder = getChildViewHolderInt(child);
                if (holder == null) continue;
                if (holder.getLayoutPosition() == position && !holder.shouldIgnore()
                        && (mRecyclerView.mState.isPreLayout() || !holder.isRemoved())){
                    return child;
                }
            }
            return null;
        }

        public void detachView(View child) {
            final int ind = mChildHelper.indexOfChild(child);
            if (ind >= 0) {
                detachViewInternal(ind, child);
            }
        }

        public void detachViewAt(int index) {
            detachViewInternal(index, getChildAt(index));
        }

        private void detachViewInternal(int index, View view) {
            if (DISPATCH_TEMP_DETACH) {
                ViewCompat.dispatchStartTemporaryDetach(view);
            }
            mChildHelper.detachViewFromParent(index);
        }

        public void attachView(View child, int index, LayoutParams lp) {
            ViewHolder holder = getChildViewHolderInt(child);
            if (holder.isRemoved()) {
                mRecyclerView.mViewInfoStore.addToDisappearedInLayout(vh);
            } else {
                mRecyclerView.mViewInfoStore.removeFromDisappearedInLayout(vh);
            }
            mChildHelper.attachViewToParent(child, index, lp, vh.isRemoved());
            if (DISPATCH_TEMP_DETACH)  {
                ViewCompat.dispatchFinishTemporaryDetach(child);
            }
        }
        public void attachView(View child, int index) {
            attachView(child, index, (LayoutParams) child.getLayoutParams());
        }
        public void attachView(View child) {
            attachView(child, -1);
        }

        public void removeDetachedView(View child) {
            mRecyclerView.removeDetachedView(child, false);
        }

        public void moveView(int fromIndex, int toIndex) {
            View view = getChildAt(fromIndex);
            if (view == null) {
                throw new IllegalArgumentException("Cannot move a child from non-existing index:"
                        + fromIndex);
            }
            detachViewAt(fromIndex);
            attachView(view, toIndex);
        }

        public void detachAndScrapView(View child, Recycler recycler) {
            int index = mChildHelper.indexOfChild(child);
            scrapOrRecycleView(recycler, index, child);
        }

        public void detachAndScrapViewAt(int index, Recycler recycler) {
            final View child = getChildAt(index);
            scrapOrRecycleView(recycler, index, child);
        }

        public void removeAndRecycleView(View child, Recycler recycler) {
            removeView(child);
            recycler.recycleView(child);
        }

        public void removeAndRecycleViewAt(int index, Recycler recycler) {
            final View view = getChildAt(index);
            removeViewAt(index);
            recycler.recycleView(view);
        }

        public int getChildCount() {
            return mChildHelper != null ? mChildHelper.getChildCount() : 0;
        }

        public View getChildAt(int index) {
            return mChildHelper != null ? mChildHelper.getChildAt(index) : null;
        }

        public int getWidthMode() {
            return mWidthMode;
        }
        public int getHeightMode() {
            return mHeightMode;
        }
        public int getWidth() {
            return mWidth;
        }
        public int getHeight() {
            return mHeight;
        }

        public int getPaddingLeft() {
            return mRecyclerView != null ? mRecyclerView.getPaddingLeft() : 0;
        }
        public int getPaddingTop() {
            return mRecyclerView != null ? mRecyclerView.getPaddingTop() : 0;
        }
        public int getPaddingRight() {
            return mRecyclerView != null ? mRecyclerView.getPaddingRight() : 0;
        }
        public int getPaddingBottom() {
            return mRecyclerView != null ? mRecyclerView.getPaddingBottom() : 0;
        }
        public int getPaddingStart() {
            return mRecyclerView != null ? ViewCompat.getPaddingStart(mRecyclerView) : 0;
        }
        public int getPaddingEnd() {
            return mRecyclerView != null ? ViewCompat.getPaddingEnd(mRecyclerView) : 0;
        }

        public boolean isFocused() {
            return mRecyclerView != null && mRecyclerView.isFocused();
        }
        public boolean hasFocus() {
            return mRecyclerView != null && mRecyclerView.hasFocus();
        }
        public View getFocusedChild() {
            if (mRecyclerView == null) {
                return null;
            }
            final View focused = mRecyclerView.getFocusedChild();
            if (focused == null || mChildHelper.isHidden(focused)) {
                return null;
            }
            return focused;
        }

        public int getItemCount() {
            final Adapter a = mRecyclerView != null ? mRecyclerView.getAdapter() : null;
            return a != null ? a.getItemCount() : 0;
        }

        public void offsetChildrenHorizontal(int dx) {
            if (mRecyclerView != null) {
                mRecyclerView.offsetChildrenHorizontal(dx);
            }
        }
        public void offsetChildrenVertical(int dy) {
            if (mRecyclerView != null) {
                mRecyclerView.offsetChildrenVeritical(dy);
            }
        }

        public void ignoreView(View view) {
            if (view.getParent() != mRecyclerView || mRecyclerView.indexOfChild(view) == -1) {
                // checking this because calling this method on a recycled or detached view may
                // cause loss of state.
                throw new IllegalArgumentException("View should be fully attached to be ignored");
            }
            final ViewHolder holder = getChildViewHolderInt(view);
            holder.addFlags(ViewHolder.FLAG_IGNORE);
            mRecyclerView.mViewInfoStore.removeViewHolder(vh);
        }

        public void stopIgnoringView(View view) {
            final ViewHolder holder = getChildViewHolderInt(view);
            holder.stopIgnoring();
            holder.resetInternal();
            holder.addFlags(ViewHolder.FLAG_INVALID);
        }

        public void detachAndScrapAttachedViews(Recycler recycler) {
            final int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                final View v = getChildAt(i);
                scrapOrRecycleView(recycler, i, v);
            }
        }

        private void scrapOrRecycleView(Recycler recycler, int index, View view) {
            final ViewHolder holder = getChildViewHolderInt(view);
            if (holder.shouldIgnore()) {
                if (DEBUG) {
                    Log.d(TAG, "ignoring view " + holder);
                }
                return;
            }
            if (holder.isInvalid() && !holder.isRemoved() && !mRecyclerView.mAdapter.hasStableIds()) {
                removeViewAt(index);
                recycler.recycleViewHolderInternal(holder);
            } else {
                detachViewAt(index);
                recycler.scrapView(view);
                mRecyclerView.mViewInfoStore.onViewDetached(holder);
            }
        }

        void removeAndRecycleScrapInt(Recycler recycler) {
            final int scrapCount = recycler.getScrapCount();
            for (int i = scrapCount - 1; i >= 0; --i) {
                final View scrap = recycler.getScrapViewAt(i);
                final ViewHolder holder = getChildViewHolderInt(scrap);
                if (holder.shouldIgnore()) continue;
                holder.setIsRecyclable(false);
                if (holder.isTmpDetached()) {
                    mRecyclerView.removeDetachedView(scrap, false);
                }
                if (mRecyclerView.mItemAnimator != null) {
                    mRecyclerView.mItemAnimator.endAnimation(holder);
                }
                holder.setIsRecyclable(true);
                recycler.quickRecycleScrapView(scrap);
            }
            recycler.clearScrap();
            if (scrapCount > 0) {
                mRecyclerView.invalidate();
            }
        }

        public static int getChildMeasureSpec(int parentSize, int parentMode, int padding,
                                              int childDimension, boolean canScroll) {
            int size = Math.max(0, parentSize - padding);
            int resultSize = 0;
            int resultMode = MeasureSpec.UNSPECIFIED;
            if (childDimension >= 0) {
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (canScroll) {
                if (childDimension == LayoutParams.MATCH_PARENT) {
                    switch (parentMode) {
                        case MeasureSpec.AT_MOST:
                        case MeasureSpec.EXACTLY:
                            resultSize = size;
                            resultMode = parentMode;
                            break;
                        case MeasureSpec.UNSPECIFIED:
                            resultSize = 0;
                            resultMode = MeasureSpec.UNSPECIFIED;
                            break;
                    }
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = 0;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
            } else {
                if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = size;
                    resultMode = parentMode;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = size;
                    if (parentMode == MeasureSpec.AT_MOST || parentMode == MeasureSpec.EXACTLY) {
                        resultMode = MeasureSpec.AT_MOST;
                    } else {
                        resultMode = MeasureSpec.UNSPECIFIED;
                    }
                }
            }
            return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
        }

        boolean shouldMeasureChild(View child, int widthSpec, int heightSpec, LayoutParams lp) {
            return child.isLayoutRequested()
                    || !mMeasurementCacheEnabled
                    || !isMeasurementUpToDate(child.getWidth(), widthSpec, lp.width)
                    || !isMeasurementUpToDate(child.getHeight(), heightSpec, lp.height);
        }
        boolean shouldReMeasureChild(View child, int widthSpec, int heightSpec, LayoutParams lp) {
            return !mMeasurementCacheEnabled
                    || !isMeasurementUpToDate(child.getMeasuredWidth(), widthSpec, lp.width)
                    || !isMeasurementUpToDate(child.getMeasuredHeight(), heightSpec, lp.height);
        }

        public void measureChild(View child, int widthUsed, int heightUsed) {
            final LayoutParams lp = (LayoutParams)child.getLayoutParams();
            final Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
            widthUsed += insets.left + insets.right;
            heightUsed += insets.top + insets.bottom;
            final int widthSpec = getChildMeasureSpec(getWidth(), getWidthMode(),
                    getPaddingLeft()+getPaddingRight()+widthUsed, lp.width, canScrollHorizontally());
            final int heightSpec = getChildMeasureSpec(getHeight(), getHeightMode(),
                    getPaddingTop()+getPaddingBottom()+heightUsed,lp.height, canScrollVertically());
            if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
                child.measure(widthSpec, heightSpec);
            }
        }

        public boolean isMeasurementCacheEnabled() {
            return mMeasurementCacheEnabled;
        }
        public void setMeasurementCacheEnabled(boolean measurementCacheEnabled) {
            mMeasurementCacheEnabled = measurementCacheEnabled;
        }

        private static boolean isMeasurementUpToDate(int childSize, int spec, int dimension) {
            final int specMode = MeasureSpec.getMode(spec);
            final int specSize = MeasureSpec.getSize(spec);
            if (dimension > 0 && childSize != dimension) {
                return false;
            }
            switch (specMode) {
                case MeasureSpec.UNSPECIFIED:
                    return true;
                case MeasureSpec.AT_MOST:
                    return specSize >= childSize;
                case MeasureSpec.EXACTLY:
                    return specSize == childSize;
            }
            return false;
        }

        public void measureChildWithMargins(View child, int widthUsed, int heightUsed) {
            final LayoutParams lp = (LayoutParams)child.getLayoutParams();
            final Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
            widthUsed += insets.left + insets.right;
            heightUsed += insets.top + insets.bottom;

            final int widthSpec = getChildMeasureSpec(getWidth(), getWidthMode(),
                    getPaddingLeft() + getPaddingRight() +
                            lp.leftMargin + lp.rightMargin + widthUsed, lp.width,
                    canScrollHorizontally());
            final int heightSpec = getChildMeasureSpec(getHeight(), getHeightMode(),
                    getPaddingTop() + getPaddingBottom() +
                            lp.topMargin + lp.bottomMargin + heightUsed, lp.height,
                    canScrollVertically());
            if (shouldMeasureChild(child, widthSpec, heightSpec, lp)) {
                child.measure(widthSpec, heightSpec);
            }
        }

        public int getDecoratedMeasuredWidth(View child) {
            final Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
            return child.getMeasuredWidth() + insets.left + insets.right;
        }
        public int getDecoratedMeasuredHeight(View child) {
            final Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
            return child.getMeasuredHeight() + insets.top + insets.bottom;
        }


        public void layoutDecorated(View child, int left, int top, int right, int bottom) {
            final Rect insets = ((LayoutParams)child.getLayoutParams()).mDecorInsets;
            child.layout(left + insets.left, top + insets.top, right - insets.right, bottom - insets.bottom);
        }
        public void layoutDecoratedWithMargins(View child, int left, int top, int right,
                                               int bottom) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final Rect insets = lp.mDecorInsets;
            child.layout(left + insets.left + lp.leftMargin, top + insets.top + lp.topMargin,
                    right - insets.right - lp.rightMargin,
                    bottom - insets.bottom - lp.bottomMargin);
        }

        public void getTransformedBoundingBox(View child, boolean includeDecorInsets, Rect out) {
            if (includeDecorInsets) {
                Rect insets = ((LayoutParams) child.getLayoutParams()).mDecorInsets;
                out.set(-insets.left, -insets.top,
                        child.getWidth() + insets.right, child.getHeight() + insets.bottom);
            } else {
                out.set(0, 0, child.getWidth(), child.getHeight());
            }

            if (mRecyclerView != null) {
                final Matrix childMatrix = ViewCompat.getMatrix(child);
                if (childMatrix != null && !childMatrix.isIdentity()) {
                    final RectF tempRectF = mRecyclerView.mTempRectF;
                    tempRectF.set(out);
                    childMatrix.mapRect(tempRectF);
                    out.set(
                            (int) Math.floor(tempRectF.left),
                            (int) Math.floor(tempRectF.top),
                            (int) Math.ceil(tempRectF.right),
                            (int) Math.ceil(tempRectF.bottom)
                    );
                }
            }
            out.offset(child.getLeft(), child.getTop());
        }

        public void getDecoratedBoundsWithMargins(View view, Rect outBounds) {
            MyRecyclerView.getDecoratedBoundsWithMarginsInt(view, outBounds);
        }

        public int getDecoratedLeft(View child) {
            return child.getLeft() - getLeftDecorationWidth(child);
        }
        public int getDecoratedTop(View child) {
            return child.getTop() - getTopDecorationHeight(child);
        }
        public int getDecoratedRight(View child) {
            return child.getRight() + getRightDecorationWidth(child);
        }
        public int getDecoratedBottom(View child) {
            return child.getBottom() + getBottomDecorationHeight(child);
        }

        public void calculateItemDecorationsForChild(View child, Rect outRect) {
            if (mRecyclerView == null) {
                outRect.set(0, 0, 0, 0);
                return;
            }
            Rect insets = mRecyclerView.getItemDecorInsetsForChild(child);
            outRect.set(insets);
        }

        public int getTopDecorationHeight(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.top;
        }
        public int getBottomDecorationHeight(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.bottom;
        }
        public int getLeftDecorationWidth(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.left;
        }
        public int getRightDecorationWidth(View child) {
            return ((LayoutParams) child.getLayoutParams()).mDecorInsets.right;
        }


        public View onFocusSearchFailed(View focused, int direction, Recycler recycler,
                                        State state) {
            return null;
        }
        public View onInterceptFocusSearch(View focused, int direction) {
            return null;
        }


        public boolean requestChildRectangleOnScreen(MyRecyclerView parent, View child, Rect rect,
                                                     boolean immediate) {
            final int parentLeft = getPaddingLeft();
            final int parentTop = getPaddingTop();
            final int parentRight = getWidth() - getPaddingRight();
            final int parentBottom = getHeight() - getPaddingBottom();
            final int childLeft = child.getLeft() + rect.left - child.getScrollX();
            final int childTop = child.getTop() + rect.top - child.getScrollY();
            final int childRight = childLeft + rect.width();
            final int childBottom = childTop + rect.height();

            final int offScreenLeft = Math.min(0, childLeft - parentLeft);
            final int offScreenTop = Math.min(0, childTop - parentTop);
            final int offScreenRight = Math.max(0, childRight - parentRight);
            final int offScreenBottom = Math.max(0, childBottom - parentBottom);

            final int dx;
            if (getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL) {
                dx = offScreenRight != 0 ? offScreenRight
                        : Math.max(offScreenLeft, childRight - parentRight);
            } else {
                dx = offScreenLeft != 0 ? offScreenLeft
                        : Math.min(childLeft - parentLeft, offScreenRight);
            }

            // Favor bringing the top into view over the bottom. If top is already visible and
            // we should scroll to make bottom visible, make sure top does not go out of bounds.
            final int dy = offScreenTop != 0 ? offScreenTop
                    : Math.min(childTop - parentTop, offScreenBottom);

            if (dx != 0 || dy != 0) {
                if (immediate) {
                    parent.scrollBy(dx, dy);
                } else {
                    parent.smoothScrollBy(dx, dy);
                }
                return true;
            }
            return false;
        }

        public boolean onRequestChildFocus(MyRecyclerView parent, View child, View focused) {
            // eat the request if we are in the middle of a scroll or layout
            return isSmoothScrolling() || parent.isComputingLayout();
        }
        public boolean onRequestChildFocus(MyRecyclerView parent, State state, View child,
                                           View focused) {
            return onRequestChildFocus(parent, child, focused);
        }


        public void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter) {
        }
        public boolean onAddFocusables(RecyclerView recyclerView, ArrayList<View> views,
                                       int direction, int focusableMode) {
            return false;
        }
        public void onItemsChanged(RecyclerView recyclerView) {
        }
        public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
        }
        public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        }
        public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        }
        public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount) {
        }
        public void onItemsUpdated(RecyclerView recyclerView, int positionStart, int itemCount,
                                   Object payload) {
            onItemsUpdated(recyclerView, positionStart, itemCount);
        }

        public int computeHorizontalScrollExtent(State state) {
            return 0;
        }
        public int computeHorizontalScrollOffset(State state) {
            return 0;
        }
        public int computeHorizontalScrollRange(State state) {
            return 0;
        }
        public int computeVerticalScrollExtent(State state) {
            return 0;
        }
        public int computeVerticalScrollOffset(State state) {
            return 0;
        }
        public int computeVerticalScrollRange(State state) {
            return 0;
        }

        public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
            mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
        }
        public void setMeasuredDimension(int widthSize, int heightSize) {
            mRecyclerView.setMeasuredDimension(widthSize, heightSize);
        }

        public int getMinimumWidth() {
            return ViewCompat.getMinimumWidth(mRecyclerView);
        }
        public int getMinimumHeight() {
            return ViewCompat.getMinimumHeight(mRecyclerView);
        }

        public Parcelable onSaveInstanceState() {
            return null;
        }
        public void onRestoreInstanceState(Parcelable state) {
        }

        void stopSmoothScroller() {
            if (mSmoothScroller != null) {
                mSmoothScroller.stop();
            }
        }
        private void onSmoothScrollerStopped(SmoothScroller smoothScroller) {
            if (mSmoothScroller == smoothScroller) {
                mSmoothScroller = null;
            }
        }
        public void onScrollStateChanged(int state) {
        }

        public void removeAndRecycleAllViews(Recycler recycler) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                final View view = getChildAt(i);
                if (!getChildViewHolderInt(view).shouldIgnore()) {
                    removeAndRecycleViewAt(i, recycler);
                }
            }
        }

        void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfoCompat info) {
            onInitializeAccessibilityNodeInfo(mRecyclerView.mRecycler, mRecyclerView.mState, info);
        }

        public void onInitializeAccessibilityNodeInfo(Recycler recycler, State state,
                                                      AccessibilityNodeInfoCompat info) {
            if (ViewCompat.canScrollVertically(mRecyclerView, -1) ||
                    ViewCompat.canScrollHorizontally(mRecyclerView, -1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
                info.setScrollable(true);
            }
            if (ViewCompat.canScrollVertically(mRecyclerView, 1) ||
                    ViewCompat.canScrollHorizontally(mRecyclerView, 1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                info.setScrollable(true);
            }
            final AccessibilityNodeInfoCompat.CollectionInfoCompat collectionInfo
                    = AccessibilityNodeInfoCompat.CollectionInfoCompat
                    .obtain(getRowCountForAccessibility(recycler, state),
                            getColumnCountForAccessibility(recycler, state),
                            isLayoutHierarchical(recycler, state),
                            getSelectionModeForAccessibility(recycler, state));
            info.setCollectionInfo(collectionInfo);
        }

        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            onInitializeAccessibilityEvent(mRecyclerView.mRecycler, mRecyclerView.mState, event);
        }
        public void onInitializeAccessibilityEvent(Recycler recycler, State state,
                                                   AccessibilityEvent event) {
            final AccessibilityRecordCompat record = AccessibilityEventCompat
                    .asRecord(event);
            if (mRecyclerView == null || record == null) {
                return;
            }
            record.setScrollable(ViewCompat.canScrollVertically(mRecyclerView, 1)
                    || ViewCompat.canScrollVertically(mRecyclerView, -1)
                    || ViewCompat.canScrollHorizontally(mRecyclerView, -1)
                    || ViewCompat.canScrollHorizontally(mRecyclerView, 1));

            if (mRecyclerView.mAdapter != null) {
                record.setItemCount(mRecyclerView.mAdapter.getItemCount());
            }
        }
        void onInitializeAccessibilityNodeInfoForItem(View host, AccessibilityNodeInfoCompat info) {
            final ViewHolder vh = getChildViewHolderInt(host);
            // avoid trying to create accessibility node info for removed children
            if (vh != null && !vh.isRemoved() && !mChildHelper.isHidden(vh.itemView)) {
                onInitializeAccessibilityNodeInfoForItem(mRecyclerView.mRecycler,
                        mRecyclerView.mState, host, info);
            }
        }
        public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler, State state,
                                                             View host, AccessibilityNodeInfoCompat info) {
            int rowIndexGuess = canScrollVertically() ? getPosition(host) : 0;
            int columnIndexGuess = canScrollHorizontally() ? getPosition(host) : 0;
            final AccessibilityNodeInfoCompat.CollectionItemInfoCompat itemInfo
                    = AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(rowIndexGuess, 1,
                    columnIndexGuess, 1, false, false);
            info.setCollectionItemInfo(itemInfo);
        }


        public void requestSimpleAnimationsInNextLayout() {
            mRequestedSimpleAnimations = true;
        }

        public int getSelectionModeForAccessibility(Recycler recycler, State state) {
            return AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE;
        }

        public int getRowCountForAccessibility(Recycler recycler, State state) {
            if (mRecyclerView == null || mRecyclerView.mAdapter == null) {
                return 1;
            }
            return canScrollVertically() ? mRecyclerView.mAdapter.getItemCount() : 1;
        }
        public int getColumnCountForAccessibility(Recycler recycler, State state) {
            if (mRecyclerView == null || mRecyclerView.mAdapter == null) {
                return 1;
            }
            return canScrollHorizontally() ? mRecyclerView.mAdapter.getItemCount() : 1;
        }

        public boolean isLayoutHierarchical(Recycler recycler, State state) {
            return false;
        }

        boolean performAccessibilityAction(int action, Bundle args) {
            return performAccessibilityAction(mRecyclerView.mRecycler, mRecyclerView.mState,
                    action, args);
        }
        public boolean performAccessibilityAction(Recycler recycler, State state, int action,
                                                  Bundle args) {
            if (mRecyclerView == null) {
                return false;
            }
            int vScroll = 0, hScroll = 0;
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                    if (ViewCompat.canScrollVertically(mRecyclerView, -1)) {
                        vScroll = -(getHeight() - getPaddingTop() - getPaddingBottom());
                    }
                    if (ViewCompat.canScrollHorizontally(mRecyclerView, -1)) {
                        hScroll = -(getWidth() - getPaddingLeft() - getPaddingRight());
                    }
                    break;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                    if (ViewCompat.canScrollVertically(mRecyclerView, 1)) {
                        vScroll = getHeight() - getPaddingTop() - getPaddingBottom();
                    }
                    if (ViewCompat.canScrollHorizontally(mRecyclerView, 1)) {
                        hScroll = getWidth() - getPaddingLeft() - getPaddingRight();
                    }
                    break;
            }
            if (vScroll == 0 && hScroll == 0) {
                return false;
            }
            mRecyclerView.scrollBy(hScroll, vScroll);
            return true;
        }
        boolean performAccessibilityActionForItem(View view, int action, Bundle args) {
            return performAccessibilityActionForItem(mRecyclerView.mRecycler, mRecyclerView.mState,
                    view, action, args);
        }
        public boolean performAccessibilityActionForItem(Recycler recycler, State state, View view,
                                                         int action, Bundle args) {
            return false;
        }

        public static RecyclerView.LayoutManager.Properties getProperties(Context context,
                                                                          AttributeSet attrs,
                                                              int defStyleAttr, int defStyleRes) {
            Properties properties = new Properties();
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView,
                    defStyleAttr, defStyleRes);
            properties.orientation = a.getInt(R.styleable.RecyclerView_android_orientation, VERTICAL);
            properties.spanCount = a.getInt(R.styleable.RecyclerView_spanCount, 1);
            properties.reverseLayout = a.getBoolean(R.styleable.RecyclerView_reverseLayout, false);
            properties.stackFromEnd = a.getBoolean(R.styleable.RecyclerView_stackFromEnd, false);
            a.recycle();
            return properties;
        }

        void setExactMeasureSpecsFrom(RecyclerView recyclerView) {
            setMeasureSpecs(
                    MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(recyclerView.getHeight(), MeasureSpec.EXACTLY)
            );
        }

        boolean shouldMeasureTwice() {
            return false;
        }

        boolean hasFlexibleChildInBothOrientations() {
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp.width < 0 && lp.height < 0) {
                    return true;
                }
            }
            return false;
        }

        public static class Properties {
            /** @attr ref android.support.v7.recyclerview.R.styleable#RecyclerView_android_orientation */
            public int orientation;
            /** @attr ref android.support.v7.recyclerview.R.styleable#RecyclerView_spanCount */
            public int spanCount;
            /** @attr ref android.support.v7.recyclerview.R.styleable#RecyclerView_reverseLayout */
            public boolean reverseLayout;
            /** @attr ref android.support.v7.recyclerview.R.styleable#RecyclerView_stackFromEnd */
            public boolean stackFromEnd;
        }



    }

    public static abstract class ItemDecoration {
        public void onDraw(Canvas c, MyRecyclerView parent, State state) {onDraw(c, parent);}
        @Deprecated
        public void onDraw(Canvas c, MyRecyclerView parent){
        }

        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            onDrawOver(c, parent);
        }
        @Deprecated
        public void onDrawOver(Canvas c, RecyclerView parent) {
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            getItemOffsets(outRect, ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition(),
                    parent);
        }
        @Deprecated
        public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
            outRect.set(0, 0, 0, 0);
        }

    }





    public abstract static class ViewCacheExtension {
        abstract public View getViewForPositionAndType(Recycler recycler, int position, int type);
    }

    public abstract static class Adapter<VH extends ViewHolder> {
        private final AdapterDataObservable mObservable = new AdapterDataObservable();
        private boolean hasStableIds = false;

        public abstract VH onCreateViewHolder(ViewGroup group, int viewType);
        public abstract void onBindViewHolder(ViewHolder holder, int position);

        public void onBindViewHolder(VH holder, int position, List<Object> payloads) {
            onBindViewHolder(holder, position);
        }

        public final VH createViewHolder(ViewGroup parent, int viewType) {
            TraceCompat.beginSection(TRACE_CREATE_VIEW_TAG);
            final VH holder = onCreateViewHolder(parent, viewType);
            holder.mItemViewType = viewType;
            TraceCompat.endSection();
            return holder;
        }

        public final void bindViewHolder(VH holder, int position) {
            holder.mPosition = position;
            if (hasStableIds()) {
                holder.mItemId = getItemId(position);
            }
            holder.setFlags(ViewHolder.FLAG_BOUND,
                    ViewHolder.FLAG_BOUND | ViewHolder.FLAG_INVALID | ViewHolder.FLAG_UPDATE
                            | ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN);

            TraceCompat.beginSection(TRACE_BIND_VIEW_TAG);
            onBindViewHolder(holder, position, holder.getUnmodifiedPayloads());
            holder.clearPayload();
            final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params instanceof MyRecyclerView.LayoutParams) {
                ((LayoutParams)params).mInsetsDirty = true;
            }
            TraceCompat.endSection();
        }

        public int getItemViewType(int position) {return 0;}

        public void setHasStableIds(boolean has){
            if (hasObservers()) {

            }
            hasStableIds = has;
        }

        public long getItemId(int pos) {return NO_ID;}

        public abstract int getItemCount();

        public final boolean hasStableIds() {return hasStableIds;}

        public void onViewRecycled(VH vh){

        }

        public boolean onFailedToRecycleView(VH holder) {
            return false;
        }

        public void onViewAttachedToWindow(VH holder) {

        }

        public void onViewDetachedFromWindow(VH holder) {
        }

        public boolean hasObservers() {return mObservable.hasObservers();}

        public void registerAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public void onAttachedToRecyclerView(MyRecyclerView myRecyclerView) {

        }

        public void onDetachedFromRecyclerView(MyRecyclerView myRecyclerView) {

        }


        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }
        public final void notifyItemChanged(int position) {
            mObservable.notifyItemRangeChanged(position, 1);
        }
        public final void notifyItemChanged(int position, Object payload) {
            mObservable.notifyItemRangeChanged(position, 1, payload);
        }
        public final void notifyItemRangeChanged(int position, int itemCount) {
            mObservable.notifyItemRangeChanged(position, itemCount);
        }
        public final void notifyItemRangeChanged(int position, int itemCount, Object payload) {
            mObservable.notifyItemRangeChanged(position, itemCount, payload);
        }
        public final void notifyItemInserted(int position) {
            mObservable.notifyItemRangeInserted(position, 1);
        }
        public final void notifyItemMoved(int fromPos, int toPos) {
            mObservable.notifyItemMoved(fromPos, toPos);
        }
        public final void notifyItemRangeInserted(int position, int itemCount) {
            mObservable.notifyItemRangeInserted(position, itemCount);
        }
        public final void notifyItemRemoved(int position) {
            mObservable.notifyItemRangeRemoved(position, 1);
        }
        public final void notifyItemRangeRemoved(int position, int itemCount) {
            mObservable.notifyItemRangeRemoved(position,itemCount);
        }

    }

    public static class State {
        static final int STEP_START = 1;
        static final int STEP_LAYOUT = 1 << 1;
        static final int STEP_ANIMATIONS = 1 << 2;

        void assertLayoutStep(int accepted) {
            if ((accepted & mLayoutStep) == 0) {
                throw new IllegalStateException("aa");
            }
        }

        @interface LayoutState {}

        private int mTargetPosition = MyRecyclerView.NO_POSITION;
        @State.LayoutState
        int mLayoutStep = STEP_START;

        private SparseArray<Object> mData;

        int mItemCount = 0;
        int mPreviousLayoutItemCount = 0;
        int mDeletedInvisibleItemCountSincePreviousLayout = 0;

        boolean mStructureChanged = false;
        boolean mInPreLayout = false;
        boolean mRunSimpleAnimations = false;
        boolean mRunPredictiveAnimations = false;
        boolean mTrackOldChangeHolders = false;
        boolean mIsMeasuring = false;

        int mFocusedItemPosition;
        long mFocusedItemId;
        int mFocusedSubChildId;

        State reset() {
            mTargetPosition = MyRecyclerView.NO_POSITION;
            if (mData != null) {
                mData.clear();
            }
            mItemCount = 0;
            mStructureChanged = false;
            mIsMeasuring = false;
            return this;
        }

        public boolean isMeasuring() {return mIsMeasuring;}
        public boolean isPreLayout() {return mInPreLayout;}
        public boolean willRunPredictiveAnimations() {return mRunPredictiveAnimations;}
        public boolean willRunSimpleAnimations() {return mRunSimpleAnimations;}

        public void remove(int resourceId) {
            if (mData == null) {
                return;
            }
            mData.remove(resourceId);
        }

        public <T> T get(int resourceId) {
            if (mData == null) {
                return null;
            }
            return (T) mData.get(resourceId);
        }

        public void put(int resoueceId, Object data) {
            if (mData == null) {
                mData = new SparseArray<>();
            }
            mData.put(resoueceId, data);
        }

        public int getTargetScrollPosition() {return mTargetPosition;}
        public boolean hasTargetScrollPosition() {return mTargetPosition != MyRecyclerView.NO_POSITION;}
        public boolean didStrucsturChange() {return mStructureChanged;}

        public int getItemCount() {
            return mInPreLayout ?
                    (mPreviousLayoutItemCount - mDeletedInvisibleItemCountSincePreviousLayout) :
                    mItemCount;
        }


    }

    public static class SavedState extends AbsSavedState {
        Parcelable mLayoutState;

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            mLayoutState = in.readParcelable(loader != null ?
                    loader : LayoutManager.class.getClassLoader());
        }
        SavedState(Parcelable state) {
            super(state);
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeParcelable(mLayoutState, 0);
        }

        void copyFrom(SavedState state) {mLayoutState = state.mLayoutState;}

        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                }
        )
    }

    public static class RecycledViewPool{
        private SparseArray<ArrayList<ViewHolder>> mScrap = new SparseArray<>();
        private SparseIntArray mMaxScrap = new SparseIntArray();
        private int mAttachCount = 0;
        private static final int DEFAULT_MAX_SCRAP = 5;

        public void clear() {mScrap.clear();}

        public void setMaxRecycledViews(int viewType, int max) {
            mMaxScrap.put(viewType, max);
            final ArrayList<ViewHolder> scrapHeap = mScrap.get(viewType);
            if (scrapHeap != null) {
                while (scrapHeap.size() > max) {
                    scrapHeap.remove(scrapHeap.size() - 1);
                }
            }
        }

        public ViewHolder getRecycledView(int viewType) {
            final ArrayList<ViewHolder> scrapHeap = mScrap.get(viewType);
            if (scrapHeap != null && !scrapHeap.isEmpty()) {
                final int index = scrapHeap.size() - 1;
                final ViewHolder scrap = scrapHeap.get(index);
                scrapHeap.remove(index);
                return scrap;
            }
            return null;
        }

        int size() {
            int count = 0;
            for (int i = 0; i < mScrap.size(); ++i) {
                ArrayList<ViewHolder> viewHolders = mScrap.valueAt(i);
                if (viewHolders != null) {
                    count += viewHolders.size();
                }
            }
            return count;
        }

        public void putRecycledView(ViewHolder scrap) {
            final int viewType = scrap.getItemViewType();
            final ArrayList<ViewHolder> scrapHeap = getScrapHeapForType(viewType);
            if (mMaxScrap.get(viewType) <= scrapHeap.size()) {
                return;
            }
            if (scrapHeap.contains(scrap)) {
                return;
            }
            scrap.resetInternal();
            scrapHeap.add(scrap);
        }

        void attach(Adapter adaper) {mAttachCount++;}

        void detach() {mAttachCount--;}

        void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter, boolean compatible) {
            if (oldAdapter!= null) {
                detach();
            }
            if (!compatible && mAttachCount == 0) {
                clear();
            }
            if (newAdapter != null) {
                attach(newAdapter);
            }
        }



        private ArrayList<ViewHolder> getScrapHeapForType(int viewType) {
            ArrayList<ViewHolder> scrapHeap = mScrap.get(viewType);
            if (scrapHeap == null) {
                scrapHeap = new ArrayList<>();
                mScrap.put(viewType, scrapHeap);
                if (mMaxScrap.indexOfKey(viewType) < 0) {
                    mMaxScrap.put(viewType, DEFAULT_MAX_SCRAP);
                }
            }
            return scrapHeap;
        }
    }

    public final class Recycler {
        final ArrayList<ViewHolder> mAttachedScrap = new ArrayList<>();
        ArrayList<ViewHolder> mChangedScrap = null;
        final ArrayList<ViewHolder> mCachedViews = new ArrayList<>();

        static final int DEFAULT_CACHE_SIZE = 2;

        private List<ViewHolder> mUnmodifiableAttachedScrap = Collections.unmodifiableList(mAttachedScrap);
        private int mRequestedCacheMax = DEFAULT_CACHE_SIZE;
        int mViewCacheMax = DEFAULT_CACHE_SIZE;

        private RecycledViewPool mRecyclerPool;
        private ViewCacheExtension mViewCacheExtension;

        public void clear() {
            mAttachedScrap.clear();
            recycleAndClearCachedViews();
        }

        public void setViewCachedSize(int size) {
            mRequestedCacheMax = size;
            updateViewCacheSize();
        }

        void updateViewCacheSize() {
            int extraCache = 0;
            if (mLayout != null && ALLOW_PREFETCHING) {
                extraCache = mLayout.isItemPrefetchEnabled() ? mLayout.getItemPrefetchCount() : 0;
            }
            mViewCacheMax = mRequestedCacheMax + extraCache;
            for (int i = mCachedViews.size() - 1; i >= 0 && mCachedViews.size() > mViewCacheMax; --i) {
                recycleCachedViewAt(i);
            }
        }

        public List<ViewHolder> getScrapList() {
            return mUnmodifiableAttachedScrap;
        }

        boolean validateViewHolderForOffsetPosition(ViewHolder holder) {
            if (holder.isRemoved()) {
                return mState.isPreLayout();
            }
            if (holder.mPosition < 0 || holder.mPosition >= mAdapter.getItemCount()) {
                throw new IndexOutOfBoundsException("Inconsistency detected. Invalid view holder "
                        + "adapter position" + holder);
            }
            if (!mState.isPreLayout()) {
                final int type = mAdapter.getItemViewType(holder.mPosition);
                if (type != holder.getItemViewType()) {
                    return false;
                }
            }
            if (mAdapter.hasStableIds()) {
                return holder.getItemId() == mAdapter.getItemId(holder.mPosition);
            }
            return true;
        }

        public void bindViewToPosition(View view, int position) {
            ViewHolder holder = getChildViewHolderInt(view);
            if (holder == null) {

            }
            final int offsetPosition = mAdapterHelper.findPositionOffset(position);
            if (offsetPosition < 0 || offsetPosition >= mAdapter.getItemCount()) {
                throw new IndexOutOfBoundsException("Inconsistency detected. Invalid item "
                        + "position " + position + "(offset:" + offsetPosition + ")."
                        + "state:" + mState.getItemCount());
            }
            holder.mOwnerRecyclerView = MyRecyclerView.this;
            mAdapter.bindViewHolder(holder, offsetPosition);
            attachAccessibilityDelegate(view);
            if (mState.isPreLayout()) {
                holder.mPreLayoutPosition = position;
            }
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            final LayoutParams params1;
            if (params == null) {
                params1 = (LayoutParams) generateDefaultLayoutParams();
                holder.itemView.setLayoutParams(params1);
            } else if (!checkLayoutParams(params)) {
                params1 = (LayoutParams) generateLayoutParams(params);
                holder.itemView.setLayoutParams(params1);
            } else {
                params1 = (LayoutParams) params;
            }
            params1.mInsetsDirty = true;
            params1.mViewHolder = holder;
            params1.mPendingInvalidate = holder.itemView.getParent() == null;
        }

        public int convertPreLayoutPositionToPostLayout(int position) {
            if (position < 0 || position >= mState.getItemCount()) {
                throw new IndexOutOfBoundsException("invalid position " + position + ". State "
                        + "item count is " + mState.getItemCount());
            }
            if (!mState.isPreLayout()) {
                return position;
            }
            return mAdapterHelper.findPositionOffset(position);
        }

        public View getViewForPosition(int position) {
            return getViewForPosition(position, false);
        }

        View getViewForPosition(int position, boolean dryRun) {
            if (position < 0 || position >= mState.getItemCount()) {
                throw new IndexOutOfBoundsException("Invalid item position " + position
                        + "(" + position + "). Item count:" + mState.getItemCount());
            }
            boolean fromScrap = false;
            ViewHolder holder = null;
            if (mState.isPreLayout()) {
                holder = getChangedScrapViewForPosition(position);
                fromScrap = holder != null;
            }
            if (holder == null) {
                holder = getScrapViewForPosition(position, INVALID_TYPE, dryRun);
                if (holder != null) {
                    if (!validateViewHolderForOffsetPosition(holder)) {
                        if (!dryRun) {
                            holder.addFlags(ViewHolder.FLAG_INVALID);
                            if (holder.isScrap()) {
                                removeDetachedView(holder.itemView, false);
                                holder.unScrap();
                            } else if (holder.wasReturnedFromScrap()) {
                                holder.clearReturnedFromScrapFlag();
                            }
                            recycleViewHolderInternal(holder);
                        }
                        holder = null;
                    } else {
                        fromScrap = true;
                    }
                }
            }
            if (holder == null) {
                final int offsetPosition = mAdapterHelper.findPositionOffset(position);
                if (offsetPosition < 0 || offsetPosition >= mAdapter.getItemCount()) {
                    throw new IndexOutOfBoundsException("Inconsistency detected. Invalid item "
                            + "position " + position + "(offset:" + offsetPosition + ")."
                            + "state:" + mState.getItemCount());
                }
                final int type = mAdapter.getItemViewType(offsetPosition);
                if (mAdapter.hasStableIds()) {
                    holder = getScrapViewForId(mAdapter.getItemId(offsetPosition), type, dryRun);
                    if (holder != null) {
                        holder.mPosition = offsetPosition;
                        fromScrap = true;
                    }
                }
                if (holder == null && mViewCacheExtension != null) {
                    final View view = mViewCacheExtension.getViewForPositionAndType(this, position, type);
                    if (view != null) {
                        holder = getChildViewHolder(view);
                        if (holder == null) {
                            throw new IllegalArgumentException("getViewForPositionAndType returned"
                                    + " a view which does not have a ViewHolder");
                        } else if (holder.shouldIgnore()) {
                            throw new IllegalArgumentException("getViewForPositionAndType returned"
                                    + " a view that is ignored. You must call stopIgnoring before"
                                    + " returning this view.");
                        }
                    }
                }
                if (holder == null) {
                    holder = getRecycledViewPool().getRecycledView(type);
                    if (holder != null) {
                        holder.resetInternal();
                        if (FORCE_INVALIDATE_DISPLAY_LIST) {
                            invalidateDisplayListInt(holder);
                        }
                    }
                }
                if (holder == null) {
                    holder = mAdapter.onCreateViewHolder(MyRecyclerView.this, type);
                }
            }

            if (fromScrap && !mState.isPreLayout() && holder.hasAnyOfTheFlags(
                    ViewHolder.FLAG_BOUNCED_FROM_HIIDEN_LIST)) {
                holder.setFlags(0, ViewHolder.FLAG_BOUNCED_FROM_HIIDEN_LIST);
                if (mState.mRunSimpleAnimations) {
                    int changedFlags = ItemAnimator.buildAdatperChangeFlagsForAnimations(holder);
                    changedFlags |= ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT;
                    final ItemAnimator.ItemHolderInfo info = mItemAnimator.recordPreLayoutInformation(
                            mState, holder, changedFlags, holder.getUnmodifiedPayloads());
                    recordAnimationInfoIfBouncedHiddenView(holder, info);
                }
            }

            boolean bound = false;
            if (mState.isPreLayout() && holder.isBound()) {
                holder.mPreLayoutPosition = position;
            } else if (!holder.isBound() || holder.needUpdate() || holder.isInvalid()) {
                if (holder.isRemoved()) {

                }
                final int offsetPosition = mAdapterHelper.findPositionOffset(position);
                holder.mOwnerRecyclerView = MyRecyclerView.this;
                mAdapter.bindViewHolder(holder, offsetPosition);
                attachAccessibilityDelegate(holder.itemView);
                bound = true;
                if (mState.isPreLayout()) {
                    holder.mPreLayoutPosition = position;
                }
            }

            final ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            final LayoutParams rvLayoutParams;
            if (lp == null) {
                rvLayoutParams = (LayoutParams) generateDefaultLayoutParams();
                holder.itemView.setLayoutParams(rvLayoutParams);
            } else if (!checkLayoutParams(lp)) {
                rvLayoutParams = (LayoutParams) generateLayoutParams(lp);
                holder.itemView.setLayoutParams(rvLayoutParams);
            } else {
                rvLayoutParams = (LayoutParams) lp;
            }
            rvLayoutParams.mViewHolder = holder;
            rvLayoutParams.mPendingInvalidate = fromScrap && bound;
            return holder.itemView;
        }

        private void attachAccessibilityDelegate(View itemView) {
            if (isAccessibilityEnabled()) {
                if (ViewCompat.getImportantForAccessibility(itemView) ==
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                    ViewCompat.setImportantForAccessibility(itemView,
                            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                }
                if (!ViewCompat.hasAccessibilityDelegate(itemView)) {
                    ViewCompat.setAccessibilityDelegate(itemView,
                            mAccessibilityDelegate.getItemDelegate());
                }
            }
        }

        private void invalidateDisplayListInt(ViewHolder holder) {
            if (holder.itemView instanceof ViewGroup) {
                invalidateDisplayListInt((ViewGroup) holder.itemView, false);
            }
        }

        private void invalidateDisplayListInt(ViewGroup viewGroup, boolean invalidateThis) {
            for (int i = viewGroup.getChildCount() - 1; i >= 0; --i) {
                final View view = viewGroup.getChildAt(i);
                if (view instanceof ViewGroup) {
                    invalidateDisplayListInt((ViewGroup) view, true);
                }
            }
            if (!invalidateThis) {
                return;
            }
            if (viewGroup.getVisibility() == View.INVISIBLE) {
                viewGroup.setVisibility(View.VISIBLE);
                viewGroup.setVisibility(View.INVISIBLE);
            } else {
                final int visibility = viewGroup.getVisibility();
                viewGroup.setVisibility(View.INVISIBLE);
                viewGroup.setVisibility(visibility);
            }
        }

        public void recycleView(View view) {
            ViewHolder holder = getChildViewHolderInt(view);
            if (holder.isTmpDetached()) {
                removeDetachedView(view, false);
            }
            if (holder.isScrap()) {
                holder.unScrap();
            } else if (holder.wasReturnedFromScrap()) {
                holder.clearReturnedFromScrapFlag();
            }
            recycleViewHolderInternal(holder);
        }

        void recycleViewInternal(View view) {
            recycleViewHolderInternal(getChildViewHolderInt(view));
        }

        void recycleViewHolderInternal(ViewHolder holder) {
            if (holder.isScrap() || holder.itemView.getParent() != null) {
                return;
            }
            if (holder.isTmpDetached()) {
                throw new IllegalArgumentException("Tmp detached view should be removed "
                        + "from RecyclerView before it can be recycled: " + holder);
            }
            if (holder.shouldIgnore()) {
                return;
            }
            final boolean transientStatePreventsRecycling = holder
                    .doesTransientStatePreventRecycling();
            final boolean forceRecycle = mAdapter != null
                    && transientStatePreventsRecycling
                    && mAdapter.onFailedToRecycleView(holder);
            boolean cached = false;
            boolean recycled = false;
            if (mCachedViews.contains(holder)) {
                return;
            }
            if (forceRecycle || holder.isRecyclable()) {
                if (mViewCacheMax > 0 && !holder.hasAnyOfTheFlags(ViewHolder.FLAG_REMOVED
                        | ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID)) {
                    int cacheViewSize = mCachedViews.size();
                    if (cacheViewSize >= mViewCacheMax && cacheViewSize > 0) {
                        recycleCachedViewAt(0);
                        cacheViewSize--;
                    }

                    int targetCacheIndex = cacheViewSize;
                    if (ALLOW_PREFETCHING && cacheViewSize > 0
                            && !mViewPrefetcher.lastPrefetchIncludedPosition(holder.mPosition)) {
                        int cacheIndex = cacheViewSize - 1;
                        while (cacheIndex >= 0) {
                            int cachedPos = mCachedViews.get(cacheIndex).mPosition;
                            if (!mViewPrefetcher.lastPrefetchIncludedPosition(cachedPos)) {
                                break;
                            }
                            cacheIndex--;
                        }
                        targetCacheIndex = cacheIndex + 1;
                    }
                    mCachedViews.add(targetCacheIndex, holder);
                    cached = true;
                }
                if (!cached) {
                    addViewHolderToRecycledViewPool(holder);
                    recycled = true;
                }
            }
            mViewInfoStore.removeViewHolder(holder);
            if (!cached && !recycled && transientStatePreventsRecycling) {
                holder.mOwnerRecyclerView = null;
            }
        }

        void recycleAndClearCachedViews() {
            final int count = mCachedViews.size();
            for (int i = count - 1; i >= 0; --i) {
                recycleCachedViewAt(i);
            }
            mCachedViews.clear();
            if (ALLOW_PREFETCHING) {
                mViewPrefetcher.clearPrefetchPositions();
            }
        }

        void recycleCachedViewAt(int cachedViewIndex) {
            ViewHolder holder = mCachedViews.get(cachedViewIndex);
            addViewHolderToRecycledViewPool(holder);
            mCachedViews.remove(cachedViewIndex);
        }

        void addViewHolderToRecycledViewPool(ViewHolder holder) {
            ViewCompat.setAccessibilityDelegate(holder.itemView, null);
            dispatchViewRecycled(holder);
            holder.mOwnerRecyclerView = null;
            getRecycledViewPool().putRecycledView(holder);
        }

        void quickRecycleScrapView(View view) {
            final ViewHolder holder = getChildViewHolderInt(view);
            holder.mScrapContainer = null;
            holder.mInChangeScarp = false;
            holder.clearReturnedFromScrapFlag();
            recycleViewHolderInternal(holder);
        }

        void scrapView(View view) {
            final ViewHolder holder = getChildViewHolderInt(view);
            if (holder.hasAnyOfTheFlags(ViewHolder.FLAG_REMOVED | ViewHolder.FLAG_INVALID)
                    || !holder.isUpdated() || canReuseUpdatedViewHolder(holder)) {
                if (holder.isInvalid() || holder.isRemoved() || !mAdapter.hasStableIds()) {
                    throw new IllegalArgumentException("Called scrap view with an invalid view."
                            + " Invalid views cannot be reused from scrap, they should rebound from"
                            + " recycler pool.");
                }
                holder.setScrapContainer(this, false);
                mAttachedScrap.add(holder);
            } else {
                if (mChangedScrap == null) {
                    mChangedScrap = new ArrayList<>();
                }
                holder.setScrapContainer(this, true);
                mChangedScrap.add(holder);
            }
        }

        void unscrapView(ViewHolder holder) {
            if (holder.mInChangeScarp) {
                mChangedScrap.remove(holder);
            } else {
                mAttachedScrap.remove(holder);
            }
            holder.mScrapContainer = null;
            holder.mInChangeScarp = false;
            holder.clearReturnedFromScrapFlag();
        }

        int getScrapCount() {
            return mAttachedScrap.size();
        }

        View getScrapViewAt(int index) {
            return mAttachedScrap.get(index).itemView;
        }

        void clearScrap() {
            mAttachedScrap.clear();
            if (mChangedScrap != null) {
                mChangedScrap.clear();
            }
        }

        ViewHolder getChangedScrapViewForPosition(int position) {
            final int changedScrapSize;
            if (mChangedScrap == null || (changedScrapSize = mChangedScrap.size()) == 0) {
                return null;
            }
            //find by position
            for (int i = 0; i < changedScrapSize; i++) {
                final ViewHolder holder = mChangedScrap.get(i);
                if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position) {
                    holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    return holder;
                }
            }
            //find by id
            if (mAdapter.hasStableIds()) {
                final int offsetPosition = mAdapterHelper.findPositionOffset(position);
                if (offsetPosition > 0 && offsetPosition < mAdapter.getItemCount()) {
                    final long id = mAdapter.getItemId(offsetPosition);
                    for (int i = 0; i < changedScrapSize; ++i) {
                        final ViewHolder holder = mChangedScrap.get(i);
                        if (!holder.wasReturnedFromScrap() && holder.getItemId() == id) {
                            holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                            return holder;
                        }
                    }
                }
            }
            return null;
        }

        ViewHolder getScrapViewForPosition(int position, int type, boolean dryRun) {
            final int scrapCount = mAttachedScrap.size();

            for (int i = 0; i < scrapCount; i++) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (!holder.wasReturnedFromScrap() && holder.getLayoutPosition() == position
                        && !holder.isInvalid() && (mState.mInPreLayout || !holder.isRemoved())) {
                    if (type != INVALID_TYPE && holder.getItemViewType() != type) {
                        break;
                    }
                    holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                    return holder;
                }
            }

            if (!dryRun) {
                View view = mChildHelper.findHiddenNonRemovedView(position, type);
                if (view != null) {
                    final ViewHolder holder = getChildViewHolderInt(view);
                    mChildHelper.unhide(view);
                    int layoutIndex = mChildHelper.indexOfChild(view);
                    if (layoutIndex == RecyclerView.NO_POSITION) {
                        throw new IllegalStateException("layout index should not be -1 after ");
                    }
                    mChildHelper.detachViewFromParent(layoutIndex);
                    scrapView(view);
                    holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP
                            | ViewHolder.FLAG_BOUNCED_FROM_HIIDEN_LIST);
                    return holder;
                }
            }

            // Search in our first-level recycled view cache.
            final int cacheSize = mCachedViews.size();
            for (int i = 0; i < cacheSize; ++i) {
                final ViewHolder holder = mCachedViews.get(i);
                if (!holder.isInvalid() && holder.getLayoutPosition() == position) {
                    if (!dryRun) {
                        mCachedViews.remove(i);
                    }
                    return holder;
                }
            }
            return null;
        }

        ViewHolder getScrapViewForId(long id, int type, boolean dryRun) {
            // Look in our attached views first
            final int count = mAttachedScrap.size();
            for (int i = count - 1; i >= 0; i--) {
                final ViewHolder holder = mAttachedScrap.get(i);
                if (holder.getItemId() == id && !holder.wasReturnedFromScrap()) {
                    if (type == holder.getItemViewType()) {
                        holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP);
                        if (holder.isRemoved()) {
                            // this might be valid in two cases:
                            // > item is removed but we are in pre-layout pass
                            // >> do nothing. return as is. make sure we don't rebind
                            // > item is removed then added to another position and we are in
                            // post layout.
                            // >> remove removed and invalid flags, add update flag to rebind
                            // because item was invisible to us and we don't know what happened in
                            // between.
                            if (!mState.isPreLayout()) {
                                holder.setFlags(ViewHolder.FLAG_UPDATE, ViewHolder.FLAG_UPDATE |
                                        ViewHolder.FLAG_INVALID | ViewHolder.FLAG_REMOVED);
                            }
                        }
                        return holder;
                    } else if (!dryRun) {
                        // if we are running animations, it is actually better to keep it in scrap
                        // but this would force layout manager to lay it out which would be bad.
                        // Recycle this scrap. Type mismatch.
                        mAttachedScrap.remove(i);
                        removeDetachedView(holder.itemView, false);
                        quickRecycleScrapView(holder.itemView);
                    }
                }
            }

            // Search the first-level cache
            final int cacheSize = mCachedViews.size();
            for (int i = cacheSize - 1; i >= 0; i--) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder.getItemId() == id) {
                    if (type == holder.getItemViewType()) {
                        if (!dryRun) {
                            mCachedViews.remove(i);
                        }
                        return holder;
                    } else if (!dryRun) {
                        recycleCachedViewAt(i);
                    }
                }
            }
            return null;
        }

        void dispatchViewRecycled(ViewHolder holder) {
            if (mRecyclerListener != null) {
                mRecyclerListener.onViewRecycled(holder);
            }
            if (mAdapter != null) {
                mAdapter.onViewRecycled(holder);
            }
            if (mState != null) {
                mViewInfoStore.removeViewHolder(holder);
            }
        }

        void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter, boolean compatible) {
            clear();
            getRecycledViewPool().onAdapterChanged(oldAdapter, newAdapter, compatible);
        }

        void offsetPositionRecordsForMove(int from, int to) {
            final int start, end, inBetweenOffset;
            if (from < to) {
                start = from;
                end = to;
                inBetweenOffset = -1;
            } else {
                start = to;
                end = from;
                inBetweenOffset = 1;
            }
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; ++i) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder == null || holder.mPosition < start || holder.mPosition > end) {
                    continue;
                }
                if (holder.mPosition == from) {
                    holder.offetPosition(to - from, false);
                } else {
                    holder.offetPosition(inBetweenOffset, false);
                }
            }
        }

        void offsetPositionRecordsForInsert(int insertedAt, int count) {
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; i++) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder != null && holder.mPosition >= insertedAt) {
                    holder.offetPosition(count, true);
                }
            }
        }

        void offsetPositionRecordsForRemove(int removedFrom, int count, boolean applyToPreLayout) {
            final int removedEnd = removedFrom + count;
            final int cachedCount = mCachedViews.size();
            for (int i = cachedCount - 1; i >= 0; i--) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder != null) {
                    if (holder.mPosition >= removedEnd) {
                        holder.offetPosition(-count, applyToPreLayout);
                    } else if (holder.mPosition >= removedFrom) {
                        holder.addFlags(ViewHolder.FLAG_REMOVED);
                        recycleCachedViewAt(i);
                    }
                }
            }
        }


        void setViewCacheExtension(ViewCacheExtension extension) {
            mViewCacheExtension = extension;
        }

        void setRecycledViewPool(RecycledViewPool pool) {
            if (mRecyclerPool != null) {
                mRecyclerPool.detach();
            }
            mRecyclerPool = pool;
            if (pool != null) {
                mRecyclerPool.attach(getAdapter());
            }
        }

        RecycledViewPool getRecycledViewPool() {
            if (mRecyclerPool == null) {
                mRecyclerPool = new RecycledViewPool();
            }
            return mRecyclerPool;
        }

        void viewRangeUpdate(int positionStart, int count) {
            final int positionEnd = positionStart + count;
            final int cachedCount = mCachedViews.size();
            for (int i = cachedCount - 1; i >= 0; --i) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder == null) {
                    continue;
                }
                final int pos = holder.getLayoutPosition();
                if (pos >= positionStart && pos < positionEnd) {
                    holder.addFlags(ViewHolder.FLAG_UPDATE);
                    recycleCachedViewAt(i);
                }
            }
        }

        void setAdapterPositionAsUnkown() {
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; ++i) {
                final ViewHolder holder = mCachedViews.get(i);
                if (holder != null) {
                    holder.addFlags(ViewHolder.FLAG_ADAPTER_POSITION_UNKNOWN);
                }
            }
        }

        void markKnownViewsInvalid() {
            if (mAdapter != null && mAdapter.hasStableIds()) {
                final int cachedCount = mCachedViews.size();
                for (int i = 0; i < cachedCount; i++) {
                    final ViewHolder holder = mCachedViews.get(i);
                    if (holder != null) {
                        holder.addFlags(ViewHolder.FLAG_UPDATE | ViewHolder.FLAG_INVALID);
                        holder.addChangePayload(null);
                    }
                }
            } else {
                // we cannot re-use cached views in this case. Recycle them all
                recycleAndClearCachedViews();
            }
        }

        void clearOldPositions() {
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; i++) {
                final ViewHolder holder = mCachedViews.get(i);
                holder.clearOldPosition();
            }
            final int scrapCount = mAttachedScrap.size();
            for (int i = 0; i < scrapCount; i++) {
                mAttachedScrap.get(i).clearOldPosition();
            }
            if (mChangedScrap != null) {
                final int changedScrapCount = mChangedScrap.size();
                for (int i = 0; i < changedScrapCount; i++) {
                    mChangedScrap.get(i).clearOldPosition();
                }
            }
        }

        void markItemDecorInsetsDirty() {
            final int cachedCount = mCachedViews.size();
            for (int i = 0; i < cachedCount; i++) {
                final ViewHolder holder = mCachedViews.get(i);
                LayoutParams layoutParams = (LayoutParams) holder.itemView.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.mInsetsDirty = true;
                }
            }
        }

        boolean isPrefetchPositionAttached(int position) {
            final int childCount = mChildHelper.getUnfilteredChildCount();
            for (int i = 0; i < childCount; i++) {
                View attachedView = mChildHelper.getUnfilteredChildAt(i);
                ViewHolder holder = getChildViewHolderInt(attachedView);
                if (holder.mPosition == position) {
                    return true;
                }
            }
            return false;
        }


        void prefetch(int[] itemPrefetchArray, int viewCount) {
            if (viewCount == 0) return;
            int childPosition = itemPrefetchArray[viewCount - 1];
            if (childPosition < 0) {
                throw new IllegalArgumentException("Recycler requested to prefetch invalid view "
                        + childPosition);
            }

            View prefetchView = null;
            if (!isPrefetchPositionAttached(childPosition)) {
                prefetchView = getViewForPosition(childPosition);
            }
            if (viewCount > 1) {
                prefetch(itemPrefetchArray, viewCount - 1);
            }
            if (prefetchView != null) {
                recycleView(prefetchView);
            }
        }
    }

    public static abstract class AdapterDataObserver {
        public void onChanged(){}
        public void onItemRangeChanged(int posStart, int itemCount) {}
        public void onItemRangeChanged(int posStart, int itemCount, Object payload) {
            onItemRangeChanged(posStart, itemCount);
        }
        public void onItemRangeInserted(int posStart, int itemCount) {}
        public void onItemRangeRemoved(int posStart, int itemCount) {}
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {}
    }

    private class RecyclerViewDataObserver extends AdapterDataObserver {
        RecyclerViewDataObserver() {

        }

        @Override
        public void onChanged() {
            assertInLayoutOnScroll(null);
            if (mAdapter.hasStableIds()) {
                mState.mStructureChanged = true;
                setDataSetChangedAfterLayout();
            } else {
                mState.mStructureChanged = true;
                setDataSetChangedAfterLayout();
            }
            if (!mAdapterHelper.hasPendingUpdates()) {
                requestLayout();
            }
        }
        @Override
        public void onItemRangeChanged(int posStart, int itemCount, Object payload) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeInserted(posStart, itemCount, payload)) {
                triggerUpdateProcessor();
            }
        }
        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeInserted(positionStart, itemCount)) {
                triggerUpdateProcessor();
            }
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeRemoved(positionStart, itemCount)) {
                triggerUpdateProcessor();
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            assertNotInLayoutOrScroll(null);
            if (mAdapterHelper.onItemRangeMoved(fromPosition, toPosition, itemCount)) {
                triggerUpdateProcessor();
            }
        }

        void triggerUpdateProcessor() {
            if (POST_UPDATE_ON_ANIMATION && mHasFixedSize && mIsAttached) {
                ViewCompat.postOnAnimation(MyRecyclerView.this, mUpdateChildViewsRunnable);
            } else {
                mAdapterUpdateDuringMeasure = true;
                requestLayout();
            }
        }
    }


    public static class SimpleOnItemTouchListener implements MyRecyclerView.OnItemTouchListener {
        @Override
        public boolean onInterceptTouchEvent(MyRecyclerView rv, MotionEvent e) {
            return false;
        }

        @Override
        public void onTouchEvent(MyRecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    public abstract static class OnScrollListener {
        public void onScrollStateChanged(MyRecyclerView rv, int newState) {}
        public void onScrolled(MyRecyclerView rv, int dx, int dy){}
    }

    public abstract class ViewHolder {
        public final View itemView;
        int mPosition = NO_POSITION;
        int mOldPosition = NO_POSITION;
        long mItemId = NO_ID;
        int mItemViewType = INVALID_TYPE;
        int mPreLayoutPosition = NO_POSITION;

        View mShadowedHolder = null;
        View mShadowingHolder = null;

        static final int FLAG_BOUND = 1 << 0;
        static final int FLAG_UPDATE = 1 << 1;
        static final int FLAG_INVALID = 1 << 2;
        static final int FLAG_REMOVED = 1 << 3;
        static final int FLAG_NOTRECYCLABLE = 1 << 4;
        static final int FLAG_RETURNED_FROM_SCRAP = 1 << 5;
        static final int FLAG_IGNORE = 1 << 7;
        static final int FLAG_TMP_DETACHED = 1 << 8;
        static final int FLAG_ADAPTER_POSITION_UNKNOWN = 1 << 9;
        static final int FLAG_ADAPTER_FULLUPDATE = 1 << 10;
        static final int FLAG_MOVED = 1 << 11;
        static final int FLAG_APPEARED_IN_PRE_LAYOUT = 1 << 12;
        static final int FLAG_BOUNCED_FROM_HIIDEN_LIST = 1 << 13;

        static final int PENDING_ACCESSIBILITY_STATE_NOT_SET = -1;

        private int mFlags;
        private static final List<Object> FULLUPDATE_PAYLOADS = Collections.EMPTY_LIST;
        List<Object> mPayloads = null;
        List<Object> mUnmodifiedPayloads = null;
        private int mIsRecyclableCount = 0;
        private Recycler mScrapContainer = null;
        private boolean mInChangeScarp = false;
        private int mWasImportantAccessibilityBeforeHidden = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
        private int mPendingAccessibilityState = PENDING_ACCESSIBILITY_STATE_NOT_SET;

        MyRecyclerView mOwnerRecyclerView;

        public ViewHolder(View view) {
            if (view == null) {

            }
            this.itemView = view;
        }

        void addFlags(int flag) {
            mFlags |= flag;
        }

        void flagRemovedAndOffsetPosition(int newPosition, int offset, boolean applyToPreLayout) {
            addFlags(ViewHolder.FLAG_REMOVED);
            offetPosition(offset, applyToPreLayout);
            mPosition = newPosition;
        }
        void offetPosition(int offset, boolean applyToPreLayout) {
            if (mOldPosition == NO_POSITION) {
                mOldPosition = mPosition;
            }
            if (mPreLayoutPosition == NO_POSITION ) {
                mPreLayoutPosition = mPosition;
            }
            if (applyToPreLayout) {
                mPreLayoutPosition += offset;
            }
            mPosition += offset;
            if (itemView.getLayoutParams() != null) {
                itemView.getLayoutParams().mInsetsDirty = true;
            }
        }

        void clearOldPosition() {
            mOldPosition = NO_POSITION;
            mPreLayoutPosition = NO_POSITION;
        }

        void saveOldPosition() {
            if (mOldPosition == NO_POSITION) {
                mOldPosition = mPosition;
            }
        }

        boolean shouldIgnore() {
            return (mFlags & FLAG_IGNORE) != 0;
        }

        public final int getPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }

        public final int getLayoutPosition() {
            return mPreLayoutPosition == NO_POSITION ? mPosition : mPreLayoutPosition;
        }

        public final int getAdapterPosition() {
            if (mOwnerRecyclerView == null) {
                return NO_POSITION;
            }
            return mOwnerRecyclerView.getAdapterPositionFor(this);
        }

        public final int getOldPosition() {return mOldPosition;}

        public final long getItemId() { return mItemId;}

        public final int getItemViewType() { return mItemViewType;}

        boolean isScrap() { return mScrapContainer != null;}

        void unScrap() {mScrollingChildHelper.unscrapView(this);}

        boolean wasReturnedFromScrap() {return (mFlags & FLAG_RETURNED_FROM_SCRAP) != 0;}

        void clearReturnedFromScrapFlag() {
            mFlags = mFlags & ~FLAG_RETURNED_FROM_SCRAP;
        }

        void stopIgnoring() { mFlags = mFlags & ~FLAG_IGNORE;}

        void setScrapContainer(Recycler recycler, boolean isChangeScrap) {
            mScrapContainer = recycler;
            mInChangeScarp = isChangeScrap;
        }

        boolean isInvalid() {return (mFlags & FLAG_INVALID) != 0;}
        boolean needUpdate() {return (mFlags & FLAG_UPDATE) != 0;}
        boolean isBound() {return (mFlags & FLAG_BOUND) != 0;}
        boolean isRemoved() {return (mFlags & FLAG_REMOVED) != 0;}
        boolean hasAnyOfTheFlags(int flags) {return (mFlags & flags) != 0;}
        boolean isTmpDetached() {return (mFlags & FLAG_TMP_DETACHED) != 0;}
        boolean isAdapterPositionUnknown() {
            return (mFlags & FLAG_ADAPTER_POSITION_UNKNOWN) != 0 || isInvalid();
        }
        void setFlags(int flags, int mask) {
            mFlags = (mFlags & ~mask) | (flags & mask);
        }
        void addChangePayload(Object payload) {
            if (payload == null) {
                addFlags(FLAG_ADAPTER_FULLUPDATE);
            } else if((mFlags & FLAG_ADAPTER_FULLUPDATE) == 0){
                createPayloadIfNeeded();
                mPayloads.add(payload);
            }
        }

        void createPayloadIfNeeded() {
            if (mPayloads == null) {
                mPayloads = new ArrayList<>();
                mUnmodifiedPayloads = Collections.unmodifiableList(mPayloads);
            }
        }

        void clearPayload() {
            if (mPayloads != null) {
                mPayloads.clear();
            }
            mFlags = mFlags & ~FLAG_ADAPTER_FULLUPDATE;
        }
        List<Object> getUnmodifiedPayloads() {
            if ((mFlags & FLAG_ADAPTER_FULLUPDATE) == 0) {
                if (mPayloads == null || mPayloads.size() == 0) {
                    return FULLUPDATE_PAYLOADS;
                }
                return mUnmodifiedPayloads;
            } else {
                return FULLUPDATE_PAYLOADS;
            }
        }

        void resetInternal() {
            mFlags = 0;
            mPosition = NO_POSITION;
            mOldPosition = NO_POSITION;
            mItemId = NO_ID;
            mPreLayoutPosition = NO_POSITION;
            mIsRecyclableCount = 0;
            mShadowedHolder = null;
            mShadowingHolder = null;
            clearPayload();
            mWasImportantAccessibilityBeforeHidden = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
            mPendingAccessibilityState = PENDING_ACCESSIBILITY_STATE_NOT_SET;
        }

        private void onEnteredHiddenState(MyRecyclerView parent) {
            mWasImportantAccessibilityBeforeHidden = ViewCompat.getImportantForAccessibility(itemView);
            parent.setChildImportantForAccessibilityInternal(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }

        public final void setIsRecyclable(boolean recyclable) {
            mIsRecyclableCount = recyclable ? mIsRecyclableCount - 1 : mIsRecyclableCount + 1;
            if (mIsRecyclableCount < 0) {
                mIsRecyclableCount = 0;
            } else if(!recyclable && mIsRecyclableCount == 1) {
                mFlags |= FLAG_NOTRECYCLABLE;
            } else if (recyclable && mIsRecyclableCount == 0) {
                mFlags &= ~FLAG_NOTRECYCLABLE;
            }
        }

        public final boolean isRecyclable() {
            return (mFlags & FLAG_NOTRECYCLABLE) == 0 && !ViewCompat.hasTransientState(itemView);
        }

        private boolean shouldBeKeptAsChild() {return (mFlags & FLAG_NOTRECYCLABLE) != 0;}

        private boolean doesTransientStatePreventRecycling() {
            return (mFlags & FLAG_NOTRECYCLABLE) == 0 && ViewCompat.hasTransientState(itemView);
        }
        boolean isUpdated() {return (mFlags & FLAG_UPDATE) != 0;}
    }


    public static interface OnItemTouchListener {
        public boolean onInterceptTouchEvent(MyRecyclerView rv, MotionEvent event);
        public void onTouchEvent(MyRecyclerView rv, MotionEvent event);
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept);
    }


    public interface RecyclerListener {
        public void onViewRecycled(ViewHolder h);
    }

    public static abstract class OnFlingListener {
        public abstract boolean onFling(int velocityX, int velocityY);
    }

    public static abstract class ItemAnimator {
        public static final int FLAG_CHANGED = ViewHolder.FLAG_UPDATE;
        public static final int FLAG_REMOVED = ViewHolder.FLAG_REMOVED;
        public static final int FLAG_MOVED = ViewHolder.FLAG_MOVED;
        public static final int FLAG_INVALIDATED = ViewHolder.FLAG_INVALID;
        public static final int FLAG_APPEARED_IN_PRE_LAYOUT= ViewHolder.FLAG_APPEARED_IN_PRE_LAYOUT;

        @IntDef(flag=true, value={
                FLAG_CHANGED, FLAG_REMOVED, FLAG_MOVED, FLAG_INVALIDATED,
                FLAG_APPEARED_IN_PRE_LAYOUT
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface AdapterChanges {}

        private ItemAnimatorListener mListener = null;
        private ArrayList<ItemAnimatorFinishedListener> mFinishedListeners = new ArrayList<>();

        private long mAddDuration = 120;
        private long mRemoveDuration = 120;
        private long mMoveDuration = 250;
        private long mChangeDuration = 250;

        public long getAddDuration() {return mAddDuration;}
        public long getRemoveDuration() {return mRemoveDuration;}
        public long getMoveDuration() {return mMoveDuration;}
        public long getChangeDuration() {return mChangeDuration;}

        public void setAddDuration(long d) {mAddDuration = d; }
        public void setRemoveDuration(long d) {mRemoveDuration = d; }
        public void setMoveDuration(long d) {mMoveDuration = d; }
        public void setChangeDuration(long d) {mChangeDuration = d; }

        void setListener(ItemAnimatorListener l) {
            mListener = l;
        }
        public @NonNull ItemHolderInfo recordPreLayoutInformation(@NonNull State state,
                                                                  @NonNull ViewHolder viewHolder,
                                                                  @AdapterChanges int changeFlags,
                                                                  @NonNull List<Object> payloads) {
            return obtainHolderInfo().setFrom(viewHolder);
        }
        public ItemHolderInfo recordPostLayoutInformation(State state, ViewHolder viewHolder) {
            return obtainHolderInfo().setFrom(viewHolder);
        }

        public ItemHolderInfo obtainHolderInfo() {return new ItemHolderInfo();}



        public abstract boolean animateDisappearance(ViewHolder viewHolder, ItemHolderInfo preInfo,
                                                     ItemHolderInfo postInfo);
        public abstract boolean animateAppearance(ViewHolder viewHolder, ItemHolderInfo preInfo,
                                                     ItemHolderInfo postInfo);
        public abstract boolean animatePersistence(ViewHolder viewHolder, ItemHolderInfo preInfo,
                                                     ItemHolderInfo postInfo);
        public abstract boolean animateChange(ViewHolder viewHolder, ViewHolder newHolder,
                                                     ItemHolderInfo preInfo,
                                                     ItemHolderInfo postInfo);


        static int buildAdapterChangeFlagsForAnimations(ViewHolder viewHolder) {
            int flags = viewHolder.mFlags & (FLAG_INVALIDATED | FLAG_CHANGED | FLAG_REMOVED);
            if (viewHolder.isInvalid()) {
                return FLAG_INVALIDATED;
            }
            if ((flags & FLAG_INVALIDATED) == 0) {
                final int oldPos = viewHolder.getOldPosition();
                final int pos = viewHolder.getAdapterPosition();
                if (oldPos != NO_POSITION && pos != NO_POSITION && pos != oldPos) {
                    flags |= FLAG_MOVED;
                }
            }
            return flags;
        }

        public abstract void runPendingAnimations();
        public abstract void endAnimation(ViewHolder holder);
        public abstract void endAnimations();
        public abstract boolean isRunning();

        public final void dispatchAnimationFinished(ViewHolder holder ){
            onAnimationFinished(holder);
            if (mListener != null) {
                mListener.onAnimationFinished(holder);
            }
        }
        public void onAnimationFinished(ViewHolder holder) {

        }

        public final void dispatchAnimationStarted(ViewHolder holder) {
            onAnimationStarted(holder);
        }
        public void onAnimationStarted(ViewHolder holder) {

        }

        public final boolean isRunning(ItemAnimatorFinishedListener l) {
            boolean running = isRunning();
            if (l != null) {
                if (!running) {
                    l.onAnimationsFinished();
                } else {
                    mFinishedListeners.add(l);
                }
            }
            return running;
        }

        public boolean canReuseUpdatedViewHolder(ViewHolder holder) {return true;}
        public boolean canReuseUpdatedViewHolder(ViewHolder holder, List<Object> payloads) {
            return canReuseUpdatedViewHolder(holder);
        }

        public final void dispatchAnimationsFinished() {
            final int count = mFinishedListeners.size();
            for (int i = 0; i < count; ++i) {
                mFinishedListeners.get(i).onAnimationsFinished();
            }
            mFinishedListeners.clear();
        }


        interface ItemAnimatorListener{
            void onAnimationFinished(ViewHolder holder);
        }
        interface ItemAnimatorFinishedListener{
            void onAnimationsFinished();
        }

        public static class ItemHolderInfo {
            public int left;
            public int top;
            public int right;
            public int bottom;

            public int changeFlags;

            public ItemHolderInfo(){}

            public ItemHolderInfo setFrom(ViewHolder viewHolder) {
                return setFrom(viewHolder, 0);
            }

            public ItemHolderInfo setFrom(ViewHolder viewHolder, int flags) {
                final View view = viewHolder.itemView;
                this.left = view.getLeft();
                this.top = view.getTop();
                this.right = view.getRight();
                this.bottom = view.getBottom();
                return this;
            }


        }
    }

    private class ItemAnimatorRestoreListener implements ItemAnimator.ItemAnimatorListener {
        ItemAnimatorRestoreListener() {}

        @Override
        public void onAnimationFinished(ViewHolder viewHolder) {
            viewHolder.setIsRecyclable(true);
            if (viewHolder.mShadowedHolder != null && viewHolder.mShadowingHolder == null) {
                viewHolder.mShadowedHolder = null;
            }
            viewHolder.mShadowingHolder = null;
            if (!viewHolder.shouldBeKeptAsChild()) {
                if (!removeAnimatingView(viewHolder.itemView) && viewHolder.isTmpDetached()) {
                    removeDetachedView(viewHolder.itemView, false);
                }
            }
        }
    }

    public interface OnChildAttachStateChangeListener {
        public void onChilViewAttachedToWindow(View view);
        public void onChildViewDetachedToWindow(View view);
    }





    boolean setChildImportantForAccessibilityInternal(ViewHolder holder, int important) {

    }

    void dispatchPendingImportantForAccessibilityChanges(){

    }

    int getAdapterPositionFor(ViewHolder holder) {

    }



    public static class  LayoutParams extends ViewGroup.MarginLayoutParams {
        ViewHolder mViewHolder;
        final Rect mDecorInsets = new Rect();
        boolean mInsetsDirty = true;
        boolean mPendingInvalidate = false;

        public LayoutParams(Context c, AttributeSet attributeSet) {super(c, attributeSet);}
        public LayoutParams(int width, int height) {super(width, height);}
        public LayoutParams(MarginLayoutParams s) {super(s);}
        public LayoutParams(ViewGroup.LayoutParams s) {super(s);}
        public LayoutParams(LayoutParams s) {super((ViewGroup.LayoutParams)s);}

        public boolean viewNeedsUpdate() {return mViewHolder.needUpdate();}
        public boolean isViewInvalid() {return mViewHolder.isInvalid();}
        public boolean isItemRemoved() {return mViewHolder.isRemoved();}
        public boolean isItemChanged() {return mViewHolder.isUpdated();}
        public int getViewPosition() {return mViewHolder.getPosition();}
        public int getViewLayoutPosition() {return mViewHolder.getLayoutPosition();}
        public int getViewAdapterPosition() {return mViewHolder.getAdapterPosition();}
    }

    public static abstract class SmoothScroller {

    }

    static class AdapterDataObservable extends android.database.Observable<AdapterDataObserver> {

        public boolean hasObservers() {return !mObservers.isEmpty();}

        public void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; --i ){
                mObservers.get(i).onChanged();
            }
        }
        public void notifyItemRangeChanged(int posStart, int itemCount) {
            notifyItemRangeChanged(posStart, itemCount, null);
        }
        public void notifyItemRangeChanged(int posStart, int itemCount, Object payload) {
            for (int i = mObservers.size() - 1; i >= 0; --i) {
                mObservers.get(i).onItemRangeChanged(posStart, itemCount, payload);
            }
        }

        public void notifyItemRangeInserted(int posStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; --i) {
                mObservers.get(i).onItemRangeInserted(posStart, itemCount);
            }
        }

        public void notifyItemRangeRemoved(int posStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; --i) {
                mObservers.get(i).onItemRangeRemoved(posStart, itemCount);
            }
        }

        public void notifyItemMoved(int fromPosition, int toPosition) {
            for (int i = mObservers.size() - 1; i >= 0; --i) {
                mObservers.get(i).onItemRangeMoved(fromPosition, toPosition, 1);
            }
        }
    }

    protected int getChildDrawingOrder(int childCount, int i) {

    }

    public interface ChildDrawingOrderCallback {
        int onGetChildDrawingOrder(int childCount, int i);
    }

    private NestedScrollingChildHelper getScrollingChildHelper() {
        if (mScrollingChildHelper == null) {
            mScrollingChildHelper = new NestedScrollingChildHelper(this);
        }
        return mScrollingChildHelper;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {

    }
    @Override
    public boolean isNestedingScrollingEnabled() {

    }
    @Override
    public boolean startNestedScroll(int axes) {

    }
    @Override
    public void stopNestedScroll(){

    }
    @Override
    public boolean hasNestedScrollingParent() {

    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
                                        int[] offsetInWindow) {

    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {

    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {

    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {

    }


}

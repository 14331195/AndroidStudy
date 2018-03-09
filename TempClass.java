package sg.bigo.live.community.mediashare.staggeredgridview;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.refresh.MaterialRefreshLayout;
import com.refresh.MaterialRefreshListener;
import com.yy.iheima.MyApplication;
import com.yy.iheima.image.avatar.ImageHelper;
import com.yy.iheima.outlets.Broadcast;
import com.yy.iheima.util.Log;
import com.yy.iheima.util.NetworkStatUtils;
import com.yy.iheima.util.OsUtil;
import com.yy.sdk.http.stat.HttpStatType;
import com.yy.sdk.module.videocommunity.data.VideoSimpleItem;
import com.yy.sdk.protocol.videocommunity.KKConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sg.bigo.live.R;
import sg.bigo.live.bigostat.BusinessStatisApi;
import sg.bigo.live.bigostat.info.shortvideo.BigoVideoList;
import sg.bigo.live.community.mediashare.CommunityMediaShareAdapter;
import sg.bigo.live.community.mediashare.TabBarController;
import sg.bigo.live.community.mediashare.VideoDetailActivity;
import sg.bigo.live.community.mediashare.VideoDetailActivityLauncher;
import sg.bigo.live.community.mediashare.stat.PageStayStatHelper;
import sg.bigo.live.community.mediashare.stat.VideoDetailPageStat;
import sg.bigo.live.community.mediashare.utils.KKPrefManager;
import sg.bigo.live.community.mediashare.utils.ScrollableBundleDataBridge;
import sg.bigo.live.community.mediashare.utils.UIUtils;
import sg.bigo.live.community.mediashare.utils.VideoSimpleItemBundleDataTransformer;
import sg.bigo.live.community.mediashare.utils.VlogDataManager;
import sg.bigo.live.databinding.LayoutCommunityMediashareFoundBinding;
import sg.bigo.live.list.HomePageBaseFragment;
import sg.bigo.live.list.OnToolBarChangeListener;
import sg.bigo.live.util.MainPageTracker;
import sg.bigo.sdk.network.extra.NetworkReceiver;
import sg.bigo.sdk.network.stat.httpstat.HttpStatManager;
import sg.bigo.svcapi.NetworkStateListener;

/**
 * Video-Hot页面
 */
public class TempClass extends HomePageBaseFragment implements View.OnClickListener,
        FragmentUserVisibleController.UserVisibleCallback, NetworkStateListener, VlogDataManager.PullVlogListener {
    private LayoutCommunityMediashareFoundBinding mDataBinding;
    private StaggeredGridLayoutManager mLayoutManager;
    private MediaShareStaggeredAdapter mAdapterOthers;
    private static final int LOAD_ITEM_COUNT = 20;//每次拉取20条数据
    private final static String TAG = "MediaShareFoundFragment";

    private TabBarController mTabController;
    private boolean mInitDone;

    private View mEmptyView;
    private boolean mHasPresented = false;

    private int mScreenHeight = 0;
    private FragmentUserVisibleController mUserVisibleController = new FragmentUserVisibleController(this, this);

    private HashSet<Long> mClickedPostIds = new HashSet<>();
    private int mMaxNumShow = 0;

    private PageStayStatHelper mPageStayStatHelper;
    private VlogDataManager mVlogDataManager;

    // 标记刷新前的大小，用于DetailActivity转场
    private int mPrevDataSize;

    private CommunityMediaShareAdapter.onRecyclerViewScrollStateChanged mRecycleViewScrollStateListener;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Broadcast.NOTIFY_KANKAN_VIDEO_DELETED.equals(action)) {
                long postId = intent.getLongExtra(Broadcast.KEY_VIDEO_ID, 0);
                if (mAdapterOthers != null) {
                    mAdapterOthers.removeVideo(postId);
                }
            } else if (Broadcast.NOTIFY_KANKAN_VIDEO_LIKE_CHANGED.equals(action)) {
                long postId = intent.getLongExtra(Broadcast.KEY_VIDEO_ID, 0);
                long likeId = intent.getLongExtra(Broadcast.KEY_LIKE_ID, 0);
                if (mAdapterOthers != null) {
                    mAdapterOthers.setVideoLikeId(postId, likeId);
                }
            } else if (Broadcast.NOTIFY_VIDEO_PLAYED.equals(action)) {
                long postId = intent.getLongExtra(Broadcast.KEY_VIDEO_ID, 0);
                if (mAdapterOthers != null) {
                    VideoSimpleItem item = mAdapterOthers.getVideoItem(postId);
                    if (item != null) {
                        item.play_count++;
                    }
                }
            }
        }
    };

    public void setRecycleViewScrollStateListener(CommunityMediaShareAdapter.onRecyclerViewScrollStateChanged listener) {
        mRecycleViewScrollStateListener = listener;
    }

    public static MediaShareFoundFragment getInstance() {
        MediaShareFoundFragment foundFragment = new MediaShareFoundFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_LAZY_LOAD, true);
        foundFragment.setArguments(args);
        return foundFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreenHeight = OsUtil.getScreenHeight();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Broadcast.NOTIFY_KANKAN_VIDEO_DELETED);
        filter.addAction(Broadcast.NOTIFY_KANKAN_VIDEO_LIKE_CHANGED);
        filter.addAction(Broadcast.NOTIFY_VIDEO_PLAYED);
        try {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mVlogDataManager = VlogDataManager.getInstance();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof TabBarController) {
            mTabController = (TabBarController) activity;
        }
    }

    private GestureDetector.OnGestureListener mGesListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mTabController != null) {
                if (velocityY > 0) {
                    mTabController.showTabBar();
                } else if (velocityY < 0) {
                    mTabController.hideTabBar();
                }
            }
            return false;
        }
    };

    @Override
    protected void onLazyCreateView(Bundle bundle) {
        Log.d(TAG, "onCreateView");
        mDataBinding = setBindingContentView(R.layout.layout_community_mediashare_found);
        initRefreshLayout();
        initRecyclerView();

        mVlogDataManager.addPullVlogListener(this);
        NetworkReceiver.getInstance().addNetworkStateListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUserVisibleController.activityCreated();
    }

    private void initRefreshLayout() {
        mDataBinding.freshLayout.setMaterialRefreshListener(new MaterialRefreshListener() {
            @Override
            public void onRefresh(MaterialRefreshLayout materialRefreshLayout) {
                if (!NetworkStatUtils.isNetworkAvailable(getContext())) {
                    mDataBinding.freshLayout.finishRefresh();
                    return;
                }
                mUIHandler.removeCallbacks(mMarkPageStayTask);
                mVlogDataManager.refreshList();
            }

            @Override
            public void onRefreshLoadMore(MaterialRefreshLayout materialRefreshLayout) {
                if (!NetworkStatUtils.isNetworkAvailable(getContext())) {
                    mDataBinding.freshLayout.finishRefreshLoadMore();
                    return;
                }
                mUIHandler.removeCallbacks(mMarkPageStayTask);

                // 标记刷新前的大小，用于DetailActivity转场
                mPrevDataSize = mAdapterOthers.getDataList().size();

                mVlogDataManager.loadMore();
            }
        });
    }

    @Override
    public void gotoTop() {
        if (mDataBinding != null && mDataBinding.foundList != null) {
            int[] lastVisibleIndex = mLayoutManager.findLastVisibleItemPositions(null);
            int offset = mDataBinding.foundList.computeVerticalScrollOffset();
            if (lastVisibleIndex[0] > 10) {
                // 这里是瀑布流layout， scrollToPosition会requestLayout
                mDataBinding.foundList.scrollBy(0, mScreenHeight - offset);
            }
            mDataBinding.foundList.smoothScrollToPosition(0);
        }
    }

    private void initRecyclerView() {
        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        //mLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        mDataBinding.foundList.addItemDecoration(new SpacesItemDecoration(2, OsUtil.dpToPx(2)));
        mDataBinding.foundList.setLayoutManager(mLayoutManager);
        mAdapterOthers = new MediaShareStaggeredAdapter(getContext(), KKConstant.KEY_TAB_FOUND, mVideoItemClickListener);
        mAdapterOthers.setRecycleView(mDataBinding.foundList);
        mDataBinding.foundList.setAdapter(mAdapterOthers);
        mPageStayStatHelper = new PageStayStatHelper(mDataBinding.foundList, mLayoutManager, mAdapterOthers, VideoDetailPageStat.POPULAR_LIST);
        mDataBinding.foundList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mRecycleViewScrollStateListener != null) {
                    mRecycleViewScrollStateListener.onScrollStateChanged(newState);
                }
                if (mPageStayStatHelper != null) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        mPageStayStatHelper.markPageStay();
                    } else {
                        mPageStayStatHelper.reportPageStay();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                prefetchCover();
                if (dy > 0 && isBottomShow()) {
                    mVlogDataManager.addPreloadAndCheck();
                    //addPreloadAndCheck();
                }
            }
        });
        loadVideoListStatData();
        mAdapterOthers.setDatas(mVlogDataManager.getVideoDatas());
        if (mAdapterOthers.getItemCount() > 0) {
            markPageStayDelay(200);
        }
    }


    @Override
    protected void loadData() {
        Log.d(TAG, "loadData");
        mInitDone = true;
        if (mVlogDataManager.getPreLoadVideoDatas().isEmpty()) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDataBinding.freshLayout.autoRefresh();
                }
            }, 200);
        }
    }


    private void showEmptyView() {
        if (mEmptyView == null) {
            ViewStub viewStub = (ViewStub) mDataBinding.getRoot().findViewById(R.id.empty_stub);
            mEmptyView = viewStub.inflate();
            TextView tvRefresh = (TextView) mEmptyView.findViewById(R.id.empty_refresh);
            tvRefresh.setOnClickListener(this);
        }
        mEmptyView.setVisibility(View.VISIBLE);
    }


    private boolean mIsFirstEnterLoadFinished = false;

    private void saveIsFirstEnterMediaShareFoundIfNeed() {
        saveIsFirstEnterMediaShareFoundIfNeed(false);
    }

    public void saveIsFirstEnterMediaShareFoundIfNeed(boolean forceCheck) {
        Log.i("GetPopularVideoPost", "saveIsFirstEnterMediaShareFoundIfNeed forceCheck(" + forceCheck + "), resume(" + isResumed() + "), visible("
                + getUserVisibleHint() + "), popular(" + (MainPageTracker.sCurrentTabIndex == KKPrefManager.MAIN_INDEX_POPULAR) + ")");
        boolean needCheck = forceCheck;
        if (!needCheck) {
            needCheck = isResumed() && getUserVisibleHint() && (MainPageTracker.sCurrentTabIndex == KKPrefManager.MAIN_INDEX_POPULAR);
        }
        if (needCheck) {
            if (KKPrefManager.loadIsFirstEnterMediaShareFound(MyApplication.getContext())) {
                KKPrefManager.saveIsFirstEnterMediaShareFound(MyApplication.getContext(), false);
            }
        }
    }

    /**
     * 添加内容
     *
     * @param pageIndex
     */

    private int mLastPrefetchPos = 0;
    private static final int REFETCH_RANGE = 4;

    private void prefetchCover() {
        if (mAdapterOthers == null || mDataBinding == null) {
            return;
        }
        if (!mHasPresented) {
            return;
        }
        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
        int[] lastPositions = new int[layoutManager.getSpanCount()];
        layoutManager.findLastVisibleItemPositions(lastPositions);
        int lastVisiblePos = Math.max(lastPositions[0], lastPositions[1]);
        int lastPos = mAdapterOthers.getItemCount() - 1;

        if (lastVisiblePos < 0 || (lastVisiblePos + 1) > lastPos) {
            return;
        }
        Context context = MyApplication.getContext();
        int ceil = Math.min(lastVisiblePos + REFETCH_RANGE, lastPos);
        int floor = Math.max(lastVisiblePos + 1, mLastPrefetchPos + 1);
        for (int i = floor; i <= ceil; i++) {
            VideoSimpleItem item = mAdapterOthers.getItem(i);
            if (item != null) {
                Log.d(TAG, "prefetchCover [" + i + "] " + item.cover_url);
                if (!TextUtils.isEmpty(item.cover_url)) {
                    HttpStatManager.getInstance().establishUrlToKey(item.cover_url, HttpStatType.STAT_FETCH_COVER);
                    ImageHelper.prefetchToDiskCache(context, item.cover_url);
                }
            }
            mLastPrefetchPos = i;
        }
    }

    private boolean isBottomShow() {
        StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
        int[] lastPositions = new int[layoutManager.getSpanCount()];
        layoutManager.findLastVisibleItemPositions(lastPositions);
        int lastVisibleItemPosition = Math.max(lastPositions[0], lastPositions[1]); //last show position
        if (lastVisibleItemPosition > mMaxNumShow) {
            mMaxNumShow = lastVisibleItemPosition;
        }
        int visibleItemCount = layoutManager.getChildCount();    //cur window show num
        int totalItemCount = layoutManager.getItemCount();       //all num of list
        //Log.d(TAG, "isBottomShow totalItemCount: " + totalItemCount + " visibleItemCount: " + visibleItemCount + " lastVisibleItemPosition: " + lastVisibleItemPosition);
        if (visibleItemCount > 0 && (totalItemCount - lastVisibleItemPosition < 4)) {
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isVisible()) {
            callSuperSetUserVisibleHint(true);
        }
        if (MainPageTracker.sCurrentTabIndex == MainPageTracker.MAIN_INDEX_POPULAR) {
            onPresentStateChanged(true);
        }
        mUserVisibleController.resume();
    }


    @Override
    public void onPause() {
        super.onPause();
        mUserVisibleController.pause();
        if (isVisible()) {
            callSuperSetUserVisibleHint(false);
        }
        if (MainPageTracker.sCurrentTabIndex == KKPrefManager.MAIN_INDEX_POPULAR) {
            onPresentStateChanged(false);
        }
    }

    Runnable mMarkPageStayTask = new Runnable() {
        @Override
        public void run() {
            if (mAdapterOthers != null && mAdapterOthers.getItemCount() > 0) {
                if (mPageStayStatHelper != null) {
                    mPageStayStatHelper.markPageStay();
                }
            }
        }
    };

    @Override
    public void onFragmentShown() {
//        if (!mHasPresented) {
//            mHasPresented = true;
//            onPresentStateChanged(true);
//        }
    }

    @Override
    public void setupToolbar(OnToolBarChangeListener listener) {

    }

    public void onPresentStateChanged(boolean present) {
        Log.d(TAG, "found onPresentStateChanged " + present);
        if (present) {
            if (!mHasPresented) {
                mHasPresented = true;
            }
            markPageStayDelay(100);
            saveIsFirstEnterMediaShareFoundIfNeed(true);
        } else {
            if (mPageStayStatHelper != null) {
                mPageStayStatHelper.reportPageStay();
            }
        }
    }

    public void markPageStayDelay(int delay) {
        mUIHandler.removeCallbacks(mMarkPageStayTask);
        mUIHandler.postDelayed(mMarkPageStayTask, delay);
    }


    @Override
    public void onStop() {
        super.onStop();
        saveVideoListStatData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        mUserVisibleController.setUserVisibleHint(isVisibleToUser);
        if (isResumed() && getUserVisibleHint()) {
            //FragmentTabs.mCurrentTabIndex = KKPrefManager.MAIN_INDEX_POPULAR;
            //KKPrefManager.setMainPageIndex(getContext(), KKPrefManager.MAIN_INDEX_POPULAR);
        }
        onPresentStateChanged(isVisibleToUser);
        markStart(isVisibleToUser);
    }

    @Override
    public void setWaitingShowToUser(boolean waitingShowToUser) {
        mUserVisibleController.setWaitingShowToUser(waitingShowToUser);
    }

    @Override
    public boolean isWaitingShowToUser() {
        return mUserVisibleController.isWaitingShowToUser();
    }

    @Override
    public void callSuperSetUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onVisibleToUserChanged(boolean isVisibleToUser, boolean invokeInResumeOrPause) {
        Log.i(TAG, "onVisibleToUserChanged:" + isVisibleToUser);
        if (isVisibleToUser) {
            saveIsFirstEnterMediaShareFoundIfNeed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVlogDataManager.removePullVlogListener(this);
        mVlogDataManager = null;
        NetworkReceiver.getInstance().removeNetworkStateListener(this);
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void gotoTopRefresh() {
        if (mDataBinding != null) {
            mDataBinding.freshLayout.autoRefresh();
            mDataBinding.foundList.scrollToPosition(0);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.empty_refresh:
                gotoTopRefresh();
                break;
            default:
                break;
        }
    }

    private String mDataBridgeTag;
    // 跳转到Preview页
    private AdapterView.OnItemClickListener mVideoItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
            //parent is null
            VideoSimpleItem item = mAdapterOthers.getItem(position);
            if (item != null && item.post_id != 0) {
                mClickedPostIds.add(item.post_id);
            }

            // VideoDetail支持切换
            mDataBridgeTag = ScrollableBundleDataBridge.PREFIX_FOUND_DATA_SOURCE + new Random().nextInt();
            ScrollableBundleDataBridge.pushNewInstance(mDataBridgeTag).attachSource(
                    mAdapterOthers.getDataList(),
                    new ScrollableBundleDataBridge.SourceListener() {
                        @Override
                        public void loadMore(int curOffset, int size) {
                            // 标记刷新前的大小，用于DetailActivity转场
                            mPrevDataSize = mAdapterOthers.getDataList().size();
                            mVlogDataManager.loadMore();
                        }
                    },
                    new VideoSimpleItemBundleDataTransformer(mAdapterOthers.getDataList())
            );

            int index = -1;
            if (mDataBinding.foundList != null) {
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
                int[] firstPositions = new int[layoutManager.getSpanCount()];
                layoutManager.findFirstVisibleItemPositions(firstPositions);
                if (firstPositions != null && firstPositions.length > 0) {
                    index = position - firstPositions[0] + 1;
                }
            }

            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            int screenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
            String positionStr = "";
            if (screenWidth > 0 && screenHeight > 0) {
                int pos[] = {-1, -1};
                view.getLocationOnScreen(pos);
                positionStr = (pos[0] * 100 / screenWidth) + "," + (pos[1] * 100 / screenHeight);
            }
            if (mAdapterOthers.getDataList().size() > position) {

                VideoDetailActivityLauncher.newInstance(UIUtils.getCurrentActivityContext(getContext()))
                        .withVideoSimpleItem(item)
                        .withView(view)
                        .withWhichTab(KKConstant.KEY_TAB_FOUND)
                        .withPosition(positionStr)
                        .withIndex(index)
                        .withJumpAnimation(true)
                        .withDataPosition(position)
                        .withFragment(MediaShareFoundFragment.this)
                        .startActivity();
            }
        }
    };

    @Override
    protected void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VideoDetailActivity.REQUEST_CODE_VIDEO_DETAIL && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                int pos = data.getIntExtra(VideoDetailActivity.REQUEST_EXTRA_VIDEO_DETAIL_CURRENT_POSITION, -1);
                if (pos != -1 && mDataBinding != null && mDataBinding.foundList != null) {
                    StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
                    layoutManager.scrollToPositionWithOffset(pos, 0);
                }
            }
        }
    }

    private void loadVideoListStatData() {
        if (mMaxNumShow == 0 && (mClickedPostIds == null || mClickedPostIds.isEmpty())) {
            mClickedPostIds = new HashSet<>();
            String list = KKPrefManager.getStatPopularVideoList(getContext());
            if (!TextUtils.isEmpty(list)) {
                try {
                    JSONObject jsonObject = new JSONObject(list);
                    mMaxNumShow = jsonObject.optInt("shownum");
                    JSONArray jsonArray = jsonObject.getJSONArray("list");
                    long postId;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        postId = jsonArray.getLong(i);
                        mClickedPostIds.add(postId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                reportVideoListStat();
            }
        }
    }

    private void saveVideoListStatData() {
        if (mDataBinding == null) {
            // debug选项中选中“不开启活动”时，这里的binding可能还没初始化
            return;
        }
        if (mMaxNumShow <= 0) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
            int[] lastPositions = new int[layoutManager.getSpanCount()];
            layoutManager.findLastVisibleItemPositions(lastPositions);
            mMaxNumShow = Math.max(lastPositions[0], lastPositions[1]); //last show position
        }
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (long postId : mClickedPostIds) {
            jsonArray.put(postId);
        }
        try {
            jsonObject.put("shownum", mMaxNumShow);
            jsonObject.put("list", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        KKPrefManager.setStatPopularVideoList(getContext(), jsonObject.toString());
    }

    private void reportVideoListStat() {
        if (mMaxNumShow <= 0) {
            /*if (mAdapterOthers == null || mAdapterOthers.getItemCount() <= 0) {
                return;
            }
            if (mClickedPostIds == null || mClickedPostIds.size() <= 0) { //没滑没点不上报
                return;
            }*/
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) mDataBinding.foundList.getLayoutManager();
            int[] lastPositions = new int[layoutManager.getSpanCount()];
            layoutManager.findLastVisibleItemPositions(lastPositions);
            mMaxNumShow = Math.max(lastPositions[0], lastPositions[1]); //last show position
            /*if (mMaxNumShow < 0 ) {
                return;
            }*/
        }
        int readNum = mClickedPostIds.size();

        BigoVideoList bigoVideoList = new BigoVideoList(BigoVideoList.PAGE_POPULAR);
        bigoVideoList.scan_num = (mMaxNumShow + 1);
        bigoVideoList.read_num = readNum;
        if (mAdapterOthers != null) {
            bigoVideoList.all_num = mAdapterOthers.getItemCount();
        }
        Log.d(TAG, "reportVideoListStat :" + bigoVideoList.toString());
        BusinessStatisApi.instance().reportBigoVideoList(getContext(), bigoVideoList);
        mMaxNumShow = 0;
        mClickedPostIds.clear();
    }

    @Override
    public void onNetworkStateChanged(boolean available) {
        if (available && mAdapterOthers != null && mAdapterOthers.getItemCount() > 0) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAdapterOthers.notifyDataSetChanged();
                }
            }, 500);
        }
    }

    @Override
    public void onVlogDataChange(List<VideoSimpleItem> videos, final int changeNum) {
        Log.i(TAG, "onVlogDataChange changeSize:" + changeNum);
        if (mDataBinding != null && mDataBinding.foundList != null) {
            mDataBinding.foundList.post(new Runnable() {
                @Override
                public void run() {
                    if (mVlogDataManager != null) {
                        mAdapterOthers.setDataAsAdd(mVlogDataManager.getVideoDatas(), changeNum);
                    }
                    markPageStayDelay(100);
                }
            });
        }
    }

    @Override
    public void onPullSucceed(boolean isReload, boolean isPreload, List<VideoSimpleItem> videolist) {
        Log.i(TAG, "onPullSucceed size:" + videolist.size() + " isReload:" + isReload + " isPreload:" + isPreload);
        if (getActivity() == null)
            return;

        if (getActivity().isFinishing()) {
            mDataBinding.freshLayout.finishRefresh();
            return;
        }

        if (!isPreload) {
            if (mPageStayStatHelper != null) {
                mPageStayStatHelper.reportPageStay();
            }
            if (mHasPresented) {
                markPageStayDelay(100);
            }

            mIsFirstEnterLoadFinished = true;
            saveIsFirstEnterMediaShareFoundIfNeed();
        }
        if (videolist != null && !videolist.isEmpty()) {
            if (mEmptyView != null && mEmptyView.getVisibility() == View.VISIBLE) {
                mEmptyView.setVisibility(View.GONE);
            }
            mDataBinding.freshLayout.setLoadMore(true);
        } else {
            if (mVlogDataManager.getVideoDatas() == null || mVlogDataManager.getVideoDatas().isEmpty()) {
                showEmptyView();
            }
            if (!isPreload) {
                mDataBinding.freshLayout.finishRefresh();
            }
            mDataBinding.freshLayout.finishRefreshLoadMore();
            mDataBinding.freshLayout.setLoadMore(false);
            return;
        }
        if (isReload) {
            reportVideoListStat();
            mLastPrefetchPos = 0;
            mPrevDataSize = 0;
            if (mPageStayStatHelper != null) {
                mPageStayStatHelper.resetReportedTopicInfo();
            }
            mDataBinding.freshLayout.finishRefresh();
        }
        if (!isPreload) {
            notifyScrollableDataChanged();

            mAdapterOthers.setDatas(mVlogDataManager.getVideoDatas());
            mDataBinding.freshLayout.finishRefresh();
            mDataBinding.freshLayout.finishRefreshLoadMore();
        }
    }

    private void notifyScrollableDataChanged() {
        // VideoDetail支持切换
        List<VideoSimpleItem> newDataList = mVlogDataManager.getVideoDatas();

        if (ScrollableBundleDataBridge.peekInstanceWithTag(mDataBridgeTag) != null) {
            ScrollableBundleDataBridge.peekInstanceWithTag(mDataBridgeTag).notifyDataSetChanged(
                    ScrollableBundleDataBridge.PREFIX_FOUND_DATA_SOURCE,
                    true,
                    mPrevDataSize,
                    new ArrayList<>(newDataList.subList(mPrevDataSize, newDataList.size())),
                    mVlogDataManager.isHasMore()
            );
        }
    }

    @Override
    public void onPullFailed(int errorCode, boolean isReload, boolean isPreload) {
        Log.i(TAG, "onPullFailed error:" + errorCode + " isReload:" + isReload + " isPreload:" + isPreload);
        if (mDataBinding == null) {
            return;
        }
        if (!isPreload) {
            mDataBinding.freshLayout.finishRefresh();
        }
        mDataBinding.freshLayout.finishRefreshLoadMore();
        mDataBinding.freshLayout.setLoadMore(true);//总是允许用户手动加载更多

        // VideoDetail支持切换
        if (ScrollableBundleDataBridge.peekInstanceWithTag(mDataBridgeTag) != null) {
            ScrollableBundleDataBridge.peekInstanceWithTag(mDataBridgeTag).notifyDataSetChanged(
                    ScrollableBundleDataBridge.PREFIX_FOUND_DATA_SOURCE,
                    false,
                    0,
                    null,
                    false
            );
        }

        if (mVlogDataManager.getVideoDatas() == null || mVlogDataManager.getVideoDatas().isEmpty()) {
            showEmptyView();
            return;
        }
        if (!isPreload) {
            Context context = getContext();
            if (context != null) {
                Toast.makeText(context, R.string.community_mediashare_no_network, Toast.LENGTH_SHORT).show();
            }
        }
    }
}

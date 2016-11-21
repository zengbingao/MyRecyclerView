package com.robin.myrecyclerview;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RRecyclerview extends RecyclerView {
    // -- footer view
    private CustomRecyclerFooterView mFooterView;
    private int footerHeight = -1;
    private boolean mIsEnablePullLoad;
    private boolean mIsPullLoading;
    private boolean isBottom;
    private boolean mIsFooterReady = false;
    private LoadMoreListener loadMoreListener;
    private OnClickListener footerClickListener;

    // -- header view
    private CustomDragHeaderView mHeaderView;
    private boolean mIsEnablePullRefresh = true;
    private boolean mIsRefreshing;
    private boolean isHeader;
    private boolean mIsHeaderReady = false;
    private float oldY;
    private OnRefreshListener refreshListener;
    private int maxPullHeight;//最多下拉高度的px值
    private static final int HEADER_HEIGHT = 68;//头部高度68dp
    private static final int MAX_PULL_LENGTH = 150;//最多下拉150dp
    Handler handler = new Handler();
    private Timer timer;

    private AlxDragRecyclerViewAdapter adapter;
    public LinearLayoutManager layoutManager;

    public RRecyclerview(Context context) {
        super(context);
        initView(context);
    }

    public RRecyclerview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RRecyclerview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public void setAdapter(AlxDragRecyclerViewAdapter adapter) {
        super.setAdapter(adapter);
        this.adapter = adapter;
    }

    public boolean ismIsPullLoading() {
        return mIsPullLoading;
    }

    public boolean ismIsRefreshing() {
        return mIsRefreshing;
    }

    /**
     * 滑动的时候更新footer的拉动高度（原理就是不停地设置BottomMargin）
     *
     * @param delta
     */
    private void updateFooterHeight(float delta) {
        if (mFooterView == null) return;
        int bottomMargin = mFooterView.getBottomMargin();
        if (delta > 50) delta = delta / 6;
        if (delta > 0) {//越往下滑越难滑
            if (bottomMargin > maxPullHeight) delta = delta * 0.65f;
            else if (bottomMargin > maxPullHeight * 0.83333f) delta = delta * 0.7f;
            else if (bottomMargin > maxPullHeight * 0.66667f) delta = delta * 0.75f;
            else if (bottomMargin > maxPullHeight >> 1) delta = delta * 0.8f;
            else if (bottomMargin > maxPullHeight * 0.33333f) delta = delta * 0.85f;
            else if (bottomMargin > maxPullHeight * 0.16667F && delta > 20)
                delta = delta * 0.2f;//如果是因为惯性向下迅速的俯冲
            else if (bottomMargin > maxPullHeight * 0.16667F) delta = delta * 0.9f;
        }

        int height = mFooterView.getBottomMargin() + (int) (delta + 0.5);

        if (mIsEnablePullLoad && !mIsPullLoading) {
            if (height > 1) {//立即刷新
                mFooterView.setState(CustomRecyclerFooterView.STATE_READY);
                mIsFooterReady = true;
            } else {
                mFooterView.setState(CustomRecyclerFooterView.STATE_NORMAL);
                mIsFooterReady = false;
            }
        }
        mFooterView.setBottomMargin(height);
    }

    /**
     * 重新设置footer的高度
     */
    private void resetFooterHeight() {
        int bottomMargin = mFooterView.getBottomMargin();
        if (bottomMargin > 20) {
            Log.i("robin", "准备重置高度,margin是" + bottomMargin + "自高是" + footerHeight);
            this.smoothScrollBy(0, -bottomMargin);
            //一松手就立即开始加载
            if (mIsFooterReady) {
                startLoadMore();
            }
        }
    }


    /**
     * 外部的监听事件，用了接口回调机制
     *
     * @param listener
     */
    public void setLoadMoreListener(LoadMoreListener listener) {
        this.loadMoreListener = listener;
    }

    /**
     * 初始化view主要做一些页面的初始化配置和滚动监听
     *
     * @param context
     */
    public void initView(Context context) {
        layoutManager = new LinearLayoutManager(context);//自带layoutManager，请勿设置
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        this.setLayoutManager(layoutManager);
        maxPullHeight = dp2px(getContext().getResources().getDisplayMetrics().density, MAX_PULL_LENGTH);//最多下拉150dp
        this.footerClickListener = new footerViewClickListener();
        this.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                        if (isBottom) resetFooterHeight();
                        break;
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        break;
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int lastItemPosition = layoutManager.findLastVisibleItemPosition();
                if (lastItemPosition == layoutManager.getItemCount() - 1 && mIsEnablePullLoad) {//如果到了最后一个
                    isBottom = true;
                    mFooterView = (CustomRecyclerFooterView) layoutManager.findViewByPosition(layoutManager.findLastVisibleItemPosition());//一开始还不能hide，因为hide得到最后一个可见的就不是footerview了
                    if (mFooterView != null) mFooterView.setOnClickListener(footerClickListener);
                    if (footerHeight == -1 && mFooterView != null) {
                        mFooterView.show();
                        mFooterView.setState(CustomRecyclerFooterView.STATE_NORMAL);
                        footerHeight = mFooterView.getMeasuredHeight();//这里的测量一般不会出问题
                    }
                    updateFooterHeight(dy);
                } else if (lastItemPosition == layoutManager.getItemCount() - 3 && mIsEnablePullLoad) {//如果到了倒数第二个
                    startLoadMore();//开始加载更多
                } else {
                    isBottom = false;
                }
            }
        });
    }

    /**
     * 设置是否可以刷新，这是只是一个状态并不是真正的去刷新
     *
     * @param enable
     */
    public void setPullLoadEnable(boolean enable) {
        mIsPullLoading = false;
        mIsEnablePullLoad = enable;
        if (adapter != null) adapter.setPullLoadMoreEnable(enable);//adapter和recyclerView要同时设置
        if (mFooterView == null) return;
        if (!mIsEnablePullLoad) {
//            this.smoothScrollBy(0,-footerHeight);
            mFooterView.hide();
            mFooterView.setOnClickListener(null);
            mFooterView.setBottomMargin(0);
            //make sure "pull up" don't show a line in bottom when listview with one page
        } else {
            mFooterView.show();
            mFooterView.setState(CustomRecyclerFooterView.STATE_NORMAL);
            mFooterView.setVisibility(VISIBLE);
            //make sure "pull up" don't show a line in bottom when listview with one page
            // both "pull up" and "click" will invoke load more.
            mFooterView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    startLoadMore();
                }
            });
        }
    }

    /**
     * 停止刷新
     */
    public void stopLoadMore() {
        if (mIsPullLoading) {
            mIsPullLoading = false;
            if (mFooterView == null) return;
            mFooterView.show();
            mFooterView.setState(CustomRecyclerFooterView.STATE_NORMAL);
        }
    }

    /**
     * 下拉加载更多时遇到了网络异常,此时要改变footer的显示
     */
    public void errorLoadMore() {
        if (mIsPullLoading) {
            mIsPullLoading = false;
            if (mFooterView == null) return;
            mFooterView.show();
            mFooterView.setState(CustomRecyclerFooterView.STATE_ERROR);
        }
    }

    /**
     * 开始刷新的方法，这个方法只在这个类里面用到，因此置成private，外部刷新用 setLoadMoreListener
     */
    private void startLoadMore() {
        if (mIsPullLoading) return;
        mIsPullLoading = true;
        if (mFooterView != null) mFooterView.setState(CustomRecyclerFooterView.STATE_LOADING);
        Log.i("robin", "现在开始加载");
        mIsFooterReady = false;
        if (loadMoreListener != null) {
            loadMoreListener.onLoadMore();
        }
    }

    /**
     * 在刷新时要执行的方法
     */
    public interface LoadMoreListener {
        public void onLoadMore();
    }

    /**
     * 点击loadMore后要执行的事件
     */
    class footerViewClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            startLoadMore();
        }
    }


    /**
     * 滑动的时候更新footer的拉动高度（原理就是不停地设置TopMargin）
     *
     * @param delta
     */
    private void updateHeaderHeight(float delta) {
        mHeaderView = (CustomDragHeaderView) layoutManager.findViewByPosition(0);
        if (delta > 0) {//如果是往下拉
            int topMargin = mHeaderView.getTopMargin();
            if (topMargin > maxPullHeight * 0.33333f) delta = delta * 0.5f;
            else if (topMargin > maxPullHeight * 0.16667F) delta = delta * 0.55f;
            else if (topMargin > 0) delta = delta * 0.6f;
            else if (topMargin < 0) delta = delta * 0.6f;//如果没有被完全拖出来
            mHeaderView.setTopMargin(mHeaderView.getTopMargin() + (int) delta);
        } else {//如果是推回去
            if (!mIsRefreshing || mHeaderView.getTopMargin() > 0) {//在刷新的时候不把margin设为负值以在惯性滑动的时候能滑回去
                this.scrollBy(0, (int) delta);//禁止既滚动，又同时减少触摸
                mHeaderView.setTopMargin(mHeaderView.getTopMargin() + (int) delta);
            }
        }
        if (mHeaderView.getTopMargin() > 0 && !mIsRefreshing) {
            mIsHeaderReady = true;
            mHeaderView.setState(CustomDragHeaderView.STATE_READY);
        }//设置为ready状态
        else if (!mIsRefreshing) {
            mIsHeaderReady = false;
            mHeaderView.setState(CustomDragHeaderView.STATE_NORMAL);
        }//设置为普通状态并且缩回去
    }

    /**
     * 滚动到指定高度，一般用作滚动到头部
     *
     * @param position
     * @param itemHeight
     */
    public void smoothScrollUpToPosition(final int position, int itemHeight) {
        int height = (layoutManager.getItemCount() - 2) * itemHeight;
        this.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                if (firstVisibleItem == position + 1) {
                    smoothScrollBy(0, 0);
                    removeOnScrollListener(this);
                }

            }
        });
        this.smoothScrollBy(0, -height);
    }

    /**
     * 平滑的滚动header
     */
    private void smoothShowHeader() {
        if (mHeaderView == null) return;
        if (timer != null) timer.cancel();
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mHeaderView == null) {
                    if (timer != null) timer.cancel();
                    return;
                }
                if (mHeaderView.getTopMargin() < 0) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsRefreshing) {//如果目前是ready状态或者正在刷新状态
                                mHeaderView.setTopMargin(mHeaderView.getTopMargin() + 2);
                            }
                        }
                    });
                } else if (timer != null) {//如果已经完全缩回去了，但是动画还没有结束，就结束掉动画
                    timer.cancel();
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 16);
    }

    /**
     * 重置header的高度
     */
    private void resetHeaderHeight() {
        if (mHeaderView == null)
            mHeaderView = (CustomDragHeaderView) layoutManager.findViewByPosition(0);
        if (layoutManager.findFirstVisibleItemPosition() != 0) {//如果刷新完毕的时候用户没有注视header
            mHeaderView.setTopMargin(-mHeaderView.getRealHeight());
            return;
        }
        if (timer != null) timer.cancel();
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mHeaderView == null) return;
                if (mHeaderView.getTopMargin() > -mHeaderView.getRealHeight()) {//如果header没有完全缩回去
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mIsHeaderReady || mIsRefreshing) {//如果目前是ready状态或者正在刷新状态
                                int delta = mHeaderView.getTopMargin() / 9;
                                if (delta < 5) delta = 5;
                                if (mHeaderView.getTopMargin() > 0)
                                    mHeaderView.setTopMargin(mHeaderView.getTopMargin() - delta);
                            } else {//如果是普通状态
                                mHeaderView.setTopMargin(mHeaderView.getTopMargin() - 5);
                            }
                        }
                    });
                } else if (timer != null) {//如果已经完全缩回去了，但是动画还没有结束，就结束掉动画
                    timer.cancel();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mHeaderView.setState(mHeaderView.STATE_FINISH);
                        }
                    });
                }
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 10);
    }


    /**
     * recyclerview 的事件处理
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (!mIsEnablePullRefresh) break;
                int delta = (int) (event.getY() - oldY);
                oldY = event.getY();
                if (layoutManager.findViewByPosition(0) instanceof CustomDragHeaderView) {
                    isHeader = true;
                    updateHeaderHeight(delta);//更新margin高度
                } else {
                    isHeader = false;
                    if (mHeaderView != null && !mIsRefreshing)
                        mHeaderView.setTopMargin(-mHeaderView.getRealHeight());
                }
                break;
//            case MotionEvent.ACTION_DOWN:
//                Log.i("robin", "touch down");
//                oldY = event.getY();
//                if(timer!=null)timer.cancel();
//                break;
            case MotionEvent.ACTION_UP:
//                Log.i("robin", "抬手啦！！！！ touch up ");
                if (mIsHeaderReady && !mIsRefreshing) startRefresh();
                if (isHeader) resetHeaderHeight();//抬手之后恢复高度
                break;
            case MotionEvent.ACTION_CANCEL:
//                Log.i("robin", "touch cancel");
                break;

        }
        return super.onTouchEvent(event);
    }

    /**
     * 因为设置了子元素的onclickListener之后，ontouch方法的down失效，所以要在分发前获取手指的位置
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                oldY = ev.getY();
                if (timer != null) timer.cancel();
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * header的下拉刷新
     *
     * @param listener
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    /**
     * 设置是否可以下拉刷新，这只是一个状态，并不执行动作。
     *
     * @param enable
     */
    public void setPullRefreshEnable(boolean enable) {
        mIsRefreshing = false;
        mIsEnablePullRefresh = enable;
        if (mHeaderView == null) return;
        if (!mIsEnablePullRefresh) {
            mHeaderView.setOnClickListener(null);
        } else {
            mHeaderView.setState(CustomRecyclerFooterView.STATE_NORMAL);
            mHeaderView.setVisibility(VISIBLE);
        }
    }

    /**
     * 停止下拉刷新
     */
    public void stopRefresh() {
        if (mIsRefreshing == true) {
            mIsRefreshing = false;
            mIsHeaderReady = false;
            if (mHeaderView == null) return;
            mHeaderView.setState(CustomRecyclerFooterView.STATE_NORMAL);
            resetHeaderHeight();
        }
    }

    /**
     * 强制下拉刷新
     */
    public void forceRefresh() {
        if (mHeaderView == null)
            mHeaderView = (CustomDragHeaderView) layoutManager.findViewByPosition(0);
        if (mHeaderView != null) mHeaderView.setState(CustomDragHeaderView.STATE_REFRESHING);
        mIsRefreshing = true;
        Log.i("robin", "现在开始强制刷新");
        mIsHeaderReady = false;
        smoothShowHeader();
        if (refreshListener != null) refreshListener.onRefresh();


    }

    /**
     * 开始下拉刷新
     */
    private void startRefresh() {
        mIsRefreshing = true;
        mHeaderView.setState(CustomDragHeaderView.STATE_REFRESHING);
        Log.i("robin", "现在开始加载");
        mIsHeaderReady = false;
        if (refreshListener != null) refreshListener.onRefresh();

    }

    /**
     * 下拉刷新的回调接口
     */
    public interface OnRefreshListener {
        public void onRefresh();
    }

    public static int dp2px(float density, int dp) {
        if (dp == 0) {
            return 0;
        }
        return (int) (dp * density + 0.5f);
    }

    /**
     * 适用于本recycler的header view下拉刷新
     */
    public static class CustomDragHeaderView extends LinearLayout {
        public final static int STATE_NORMAL = 0;
        public final static int STATE_READY = 1;
        public final static int STATE_REFRESHING = 2;
        public final static int STATE_FINISH = 3;

        public float screenDensity;
        private final int ROTATE_ANIM_DURATION = 180;
        private Context mContext;

        private View mContentView;
        private View mProgressBar;
        private ImageView mArrowImageView;
        private TextView mHintTextView;
        private Animation mRotateUpAnim;
        private Animation mRotateDownAnim;
        private int mState;
        private int realHeight;

        public CustomDragHeaderView(Context context) {
            super(context);
            initView(context);
        }

        public CustomDragHeaderView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initView(context);
        }

        /**
         * 通过state来判断header的显示信息
         *
         * @param state
         */
        public void setState(int state) {
            if (state == mState) return;
            if (state == STATE_REFRESHING) { // 显示进度
                mArrowImageView.clearAnimation();
                mArrowImageView.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
            } else { // 显示箭头图片
                mArrowImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            }

            switch (state) {
                case STATE_NORMAL:
                    if (mState == STATE_READY) {
                        mArrowImageView.startAnimation(mRotateDownAnim);
                        mHintTextView.setText("The drop-down refresh");
                    } else if (mState == STATE_REFRESHING) {//如果是从刷新状态过来
//                        mArrowImageView.clearAnimation();
                        mArrowImageView.setVisibility(INVISIBLE);
                        mHintTextView.setText("load completed");
                    }
                    break;
                case STATE_READY:
                    if (mState != STATE_READY) {
                        mArrowImageView.clearAnimation();
                        mArrowImageView.startAnimation(mRotateUpAnim);
                    }
                    mHintTextView.setText("load data");
                    break;
                case STATE_REFRESHING:
                    mHintTextView.setText("loading...");
                    break;
                case STATE_FINISH:
                    mArrowImageView.setVisibility(View.VISIBLE);
                    mHintTextView.setText("The drop-down refresh");
                    break;
                default:
            }

            mState = state;
        }

        /**
         * 设置TopMargin
         *
         * @param height
         */
        public void setTopMargin(int height) {
            if (mContentView == null) return;
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            lp.topMargin = height;
            mContentView.setLayoutParams(lp);
        }

        /**
         * 获取TopMargin
         */
        public int getTopMargin() {
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            return lp.topMargin;
        }

        /**
         * 设置 height
         *
         * @param height
         */
        public void setHeight(int height) {
            if (mContentView == null) return;
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            lp.height = height;
            mContentView.setLayoutParams(lp);
        }


        /**
         * 得到这个headerView真实的高度，而且这个高度是自己定的
         *
         * @return
         */
        public int getRealHeight() {
            return realHeight;
        }

        /**
         * 初始化header的基本信息
         *
         * @param context
         */
        private void initView(Context context) {
            mContext = context;
            this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));//recyclerView里不加这句话的话宽度就会比较窄
            LinearLayout moreView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_recyclerview_header, null);
            addView(moreView);
            moreView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            mContentView = moreView.findViewById(R.id.xlistview_header_content);
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            Log.i("robin", "初始height是" + mContentView.getHeight());
            screenDensity = getContext().getResources().getDisplayMetrics().density;//设置屏幕密度，用来px向dp转化
            lp.height = dp2px(screenDensity, HEADER_HEIGHT);//头部高度75dp
            realHeight = lp.height;
            lp.topMargin = -lp.height;
            mContentView.setLayoutParams(lp);
            mArrowImageView = (ImageView) findViewById(R.id.xlistview_header_arrow);
            mHintTextView = (TextView) findViewById(R.id.xlistview_header_hint_textview);
            mHintTextView.setPadding(0, dp2px(screenDensity, 3), 0, 0);//不知道为什么这个文字总会向上偏一下，所以要补回来
            mProgressBar = findViewById(R.id.xlistview_header_progressbar);

            mRotateUpAnim = new RotateAnimation(0.0f, -180.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mRotateUpAnim.setDuration(ROTATE_ANIM_DURATION);
            mRotateUpAnim.setFillAfter(true);
            mRotateDownAnim = new RotateAnimation(-180.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            mRotateDownAnim.setDuration(ROTATE_ANIM_DURATION);
            mRotateDownAnim.setFillAfter(true);
        }
    }

    /**
     * 适用于本recycler的footer view下拉刷新
     */
    public static class CustomRecyclerFooterView extends LinearLayout {
        public final static int STATE_NORMAL = 0;
        public final static int STATE_READY = 1;
        public final static int STATE_LOADING = 2;
        public final static int STATE_ERROR = 3;

        private Context mContext;

        private View mContentView;
        private View mProgressBar;
        private TextView mHintView;

        public CustomRecyclerFooterView(Context context) {
            super(context);
            initView(context);
        }

        public CustomRecyclerFooterView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initView(context);
        }


        /**
         * 设置footer的状态和相应的显示
         *
         * @param state
         */
        public void setState(int state) {
            mHintView.setVisibility(View.INVISIBLE);
            if (state == STATE_READY) {
                mHintView.setVisibility(View.VISIBLE);
            } else if (state == STATE_LOADING) {
                mProgressBar.setVisibility(View.VISIBLE);
            } else if (state == STATE_ERROR) {
                mProgressBar.setVisibility(GONE);
                mHintView.setVisibility(VISIBLE);
                mHintView.setText("Please check your internet then click here");
            }
        }

        /**
         * 设置bottomMargin
         *
         * @param height
         */
        public void setBottomMargin(int height) {
            if (height < 0) return;
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            lp.bottomMargin = height;
            mContentView.setLayoutParams(lp);
        }

        /**
         * 获得bottomMargin
         */
        public int getBottomMargin() {
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            return lp.bottomMargin;
        }


        /**
         * normal 状态下的view的状态
         */
        public void normal() {
            mHintView.setVisibility(View.VISIBLE);
//            mProgressBar.setVisibility(View.GONE);客户修改需求
        }


        /**
         * loading 状态下的view的状态
         */
        public void loading() {
            mHintView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        /**
         * 隐藏footer view
         */
        public void hide() {
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            lp.height = 1;//这里如果设为0那么layoutManger就会抓不到
            mContentView.setLayoutParams(lp);
            mContentView.setBackgroundColor(Color.WHITE);//这里的颜色要和自己的背景色一致
        }

        /**
         * 显示footer view
         */
        public void show() {
            LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
            lp.height = LayoutParams.WRAP_CONTENT;
            lp.width = LayoutParams.MATCH_PARENT;
            mContentView.setLayoutParams(lp);
            mContentView.setBackgroundColor(Color.WHITE);//这里的颜色要和自己的背景色一致
        }

        /**
         * 初始化footer view的一下基本信息
         *
         * @param context
         */
        private void initView(Context context) {
            mContext = context;
            this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            LinearLayout moreView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.layout_recyclerview_footer, null);
            addView(moreView);
            moreView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            mContentView = moreView.findViewById(R.id.rlContentView);
            mProgressBar = moreView.findViewById(R.id.pbContentView);
            mHintView = (TextView) moreView.findViewById(R.id.ctvContentView);
            //客户改设计后添加
            mHintView.setText("");//去掉loadmore
            mProgressBar.setVisibility(VISIBLE);//一直会显示转圈
        }
    }

    /**
     * 自定义recyclerview的适配器
     *
     * @param <T>
     */
    public static abstract class AlxDragRecyclerViewAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        static final int TYPE_HEADER = 436874;
        static final int TYPE_ITEM = 256478;
        static final int TYPE_FOOTER = 9621147;
        //布局文件的R值
        private int ITEM;

        private ViewHolder vhItem;
        boolean loadMore;

        private List<T> dataList;

        /**
         * 得到data list
         *
         * @return
         */
        public List<T> getDataList() {
            return dataList;
        }

        /**
         * 获取data list
         *
         * @param dataList
         */
        public void setDataList(List<T> dataList) {
            this.dataList = dataList;
        }

        /**
         * 对外的接口，实现这个adapter来展示数据
         *
         * @param dataList
         * @param itemLayout
         * @param pullEnable
         */
        public AlxDragRecyclerViewAdapter(List<T> dataList, int itemLayout, boolean pullEnable) {
            this.dataList = dataList;
            this.ITEM = itemLayout;
            this.loadMore = pullEnable;
        }

        /**
         * 返回这个position的类型
         *
         * @param position
         * @return
         */
        private T getObject(int position) {
            if (dataList != null && dataList.size() >= position)
                return dataList.get(position - 1);//如果有header
            return null;
        }

        /**
         * 是否需要显示footer，有些情况不需要显示footer，比如：第一次初始化显示的时候要不要显示footerView
         *
         * @param enable
         */
        public void setPullLoadMoreEnable(boolean enable) {
            this.loadMore = enable;
        }

        /**
         * 判断position是不是footer
         *
         * @param position
         * @return
         */
        boolean isPositonFooter(int position) {//这里的position从0算起
            if (dataList == null && position == 1) return true;//如果没有item
            return position == dataList.size() + 1;//如果有item(也许为0)
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(ITEM, null);
                this.vhItem = setItemViewHolder(itemView);
                return vhItem;
            } else if (viewType == TYPE_HEADER) {
                View headerView = new CustomDragHeaderView(parent.getContext());
                return new VHHeader(headerView);
            } else if (viewType == TYPE_FOOTER) {
                CustomRecyclerFooterView footerView = new CustomRecyclerFooterView(parent.getContext());
                return new VHFooter(footerView);
            }

            throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {//相当于getView
            Log.i("robin", "正在绑定" + position + "    " + holder.getClass());
            if (vhItem != null && holder.getClass() == vhItem.getClass()) {
                initItemView(holder, position, getObject(position));
            } else if (holder instanceof AlxDragRecyclerViewAdapter.VHHeader) {
                Log.i("robin", "正在绑定头");
            } else if (holder instanceof AlxDragRecyclerViewAdapter.VHFooter) {
                if (!loadMore) ((VHFooter) holder).footerView.hide();//第一次初始化显示的时候要不要显示footerView
            }
        }

        @Override
        public int getItemCount() {
            return (dataList == null || dataList.size() == 0) ? 1 : dataList.size() + 2;//如果有header,若list不存在或大小为0就没有footView，反之则有
        }//这里要考虑到头尾部，多以要加2

        /**
         * 根据位置判断这里该用哪个ViewHolder，position是从0开始的，但是一般计数是从1开始。如果一共20item（包括头尾），那么尾巴的position就是19
         *
         * @param position
         * @return TYPE_ITEM
         */
        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_HEADER;
            else if (isPositonFooter(position)) return TYPE_FOOTER;
            return TYPE_ITEM;
        }

        class VHHeader extends RecyclerView.ViewHolder {
            VHHeader(View headerView) {
                super(headerView);
            }
        }

        class VHFooter extends RecyclerView.ViewHolder {
            CustomRecyclerFooterView footerView;

            VHFooter(View itemView) {
                super(itemView);
                footerView = (CustomRecyclerFooterView) itemView;
            }
        }

        abstract void initItemView(ViewHolder itemHolder, int posion, T entity);

        abstract ViewHolder setItemViewHolder(View itemView);
    }
}


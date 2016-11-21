package com.robin.myrecyclerview;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    CustomRecyclerView crv;
    CustomRecyclerViewAdapter adapter;
    SwipeRefreshLayout sfl;
    private List<String> data = new ArrayList<>();
    int lastCompletelyVisibleItem;
    LinearLayoutManager linearLayoutManager;
    int footerHeight;
    int bottomMargin;
    boolean isBottom = false;
    int maxPullHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < 20; i++) {
            data.add("这是第" + i + "个");
        }
        crv = (CustomRecyclerView) findViewById(R.id.crv);
        sfl = (SwipeRefreshLayout) findViewById(R.id.srl);
        linearLayoutManager = new LinearLayoutManager(this);
        crv.setLayoutManager(linearLayoutManager);
        adapter = new CustomRecyclerViewAdapter(this, data);
        crv.setAdapter(adapter);
        maxPullHeight = dp2px(150);
        adapter.setOnRecyclerViewItemLongClickListener(new CustomRecyclerViewAdapter.OnRecyclerViewItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, String data) {
                Toast.makeText(MainActivity.this, "长按了" + data, Toast.LENGTH_SHORT).show();
            }
        });
        adapter.setmOnRecyclerViewItemClickListener(new CustomRecyclerViewAdapter.OnRecyclerViewItemClickListener() {
            @Override
            public void onItemClick(View view, String data) {
                Toast.makeText(MainActivity.this, "点击了" + data, Toast.LENGTH_SHORT).show();
            }
        });
        sfl.setProgressBackgroundColorSchemeColor(Color.CYAN);
        sfl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                new myAsync().execute();

            }
        });
        crv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.i("robin", "newState==" + newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && lastCompletelyVisibleItem + 1 == adapter.getItemCount()) {
                    if (isBottom) {
                        if (bottomMargin > 10) {
                            Log.i("robin", "准备重置高度,margin是" + bottomMargin + "自高是" + footerHeight);
                            crv.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    crv.smoothScrollBy(0, -bottomMargin);
                                }
                            },500);
                            adapter.setBottomMargin(0);
                        }
                    }
                    adapter.changeMoreStatus(CustomRecyclerViewAdapter.LOADING_MORE);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            adapter.changeMoreStatus(CustomRecyclerViewAdapter.PULLUP_LOAD_MORE);
                            for (int i = 9; i >= 0; i--) {
                                data.add(lastCompletelyVisibleItem, "这是第" + i + "个上拉加载出来的");
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }, 2000);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                dy = (int) (dy * 0.2);
                Log.i("robin", "dx==" + dx + ",dy==" + dy + ",FirstVisibleItemPosition==" + linearLayoutManager.findFirstVisibleItemPosition() + ",LastVisibleItemPosition==" + linearLayoutManager.findLastVisibleItemPosition());
                Log.i("robin", "FirstCompletelyVisibleItemPosition==" + linearLayoutManager.findFirstCompletelyVisibleItemPosition());
                Log.i("robin", "LastCompletelyVisibleItemPosition==" + linearLayoutManager.findLastCompletelyVisibleItemPosition());
                lastCompletelyVisibleItem = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (linearLayoutManager.findLastVisibleItemPosition() == linearLayoutManager.getItemCount() - 1) {
                    bottomMargin = adapter.getBottomMargin();
                    Log.i("robin", "现在在不停地设置高度bottomMargin==" + bottomMargin);
                    isBottom = true;
                    Log.i("robin", "到了最后一个，它已经出现");
                    LinearLayout footerView = (LinearLayout) linearLayoutManager.findViewByPosition(linearLayoutManager.findLastVisibleItemPosition());
                    Log.i("robin", "footerView==" + footerView);
                    if (footerView != null) {
                        if (dy == 0) {
                            dy = 1;
                        }
                        int height = bottomMargin + (int) (dy + 0.5);
                        Log.i("robin", "准备重置高度,margin是-->" + bottomMargin + "现在在不停地设置高度，height==" + height);
//                        layoutParams.bottomMargin = height;
//                        footerView.setLayoutParams(layoutParams);
                        adapter.setBottomMargin(height);
                    }
                } else {
                    isBottom = false;
                }
            }
        });
    }

    public int dp2px(int dp) {
        if (dp == 0) {
            return 0;
        }
        return (int) (dp * (MainActivity.this.getResources().getDisplayMetrics().density) + 0.5f);
    }

    class myAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            data.add(0, "这是下拉刷新的");
            adapter.notifyDataSetChanged();
            sfl.setRefreshing(false);
        }
    }
}

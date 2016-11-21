package com.robin.myrecyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * a
 * Created by robin on 2016/11/15.
 */

public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.BaseViewHolder> implements View.OnClickListener, View.OnLongClickListener {
    private Context context;
    private List<String> datas;
    private OnRecyclerViewItemClickListener mOnRecyclerViewItemClickListener = null;
    private OnRecyclerViewItemLongClickListener mOnRecyclerViewItemLongClickListener = null;
    private OnLoadMoreListener onLoadMoreListener = null;
    private static final int IS_HEADER = 2;
    private static final int IS_FOOTER = 3;
    private static final int IS_NORMAL = 1;
    //上拉加载更多  
    public static final int PULLUP_LOAD_MORE = 0;
    //正在加载中  
    public static final int LOADING_MORE = 1;
    //上拉加载更多状态-默认为0  
    private int load_more_status = 0;

    interface OnRecyclerViewItemClickListener {
        void onItemClick(View view, String data);
    }

    interface OnRecyclerViewItemLongClickListener {
        void onItemLongClick(View view, String data);
    }
    
    interface OnLoadMoreListener{
        void onLoadMore();
    }

    public void setmOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener listener) {
        this.mOnRecyclerViewItemClickListener = listener;
    }

    public void setOnRecyclerViewItemLongClickListener(OnRecyclerViewItemLongClickListener listener) {
        this.mOnRecyclerViewItemLongClickListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public CustomRecyclerViewAdapter(Context context, List<String> datas) {
        this.context = context;
        this.datas = datas;
    }
    public  LinearLayout view;
    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder baseViewHolder;
//        if(viewType==IS_HEADER){
//            Log.i("robin","这是Create header");
//            View view = LayoutInflater.from(context).inflate(R.layout.header, parent,false);
//            baseViewHolder = new BaseViewHolder(view,IS_HEADER);
//            return baseViewHolder;
//        } else 
        if (viewType == IS_FOOTER) {
            Log.i("robin", "这是Create footer");
            view = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.footer, parent, false);
            Log.i("robin","原始的footerView=="+view);
            baseViewHolder = new BaseViewHolder(view, IS_FOOTER);
            return baseViewHolder;
        } else if (viewType == IS_NORMAL) {
            Log.i("robin", "这是Create normal");
            View view = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
            baseViewHolder = new BaseViewHolder(view, IS_NORMAL);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            return baseViewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, int position) {
        //对不同的Item相应不同的操作
        if (position != datas.size() && holder.viewType == IS_NORMAL) {
            Log.i("robin", "这是Bind normal");
            holder.mTextView.setText(datas.get(position));
            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "点击了文字", Toast.LENGTH_SHORT).show();
                }
            });
            holder.itemView.setTag(datas.get(position));
        }
//        if(position==0&&holder.viewType==IS_HEADER){
//            Log.i("robin","这是Bind normal");
//            //header
//            holder.itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Toast.makeText(context, "点击了header", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
        if (position == datas.size() && holder.viewType == IS_FOOTER) {
            Log.i("robin", "这是Bind normal");
//            switch (load_more_status) {
//                case PULLUP_LOAD_MORE:
//                    holder.footer.setText("上拉加载更多...");
//                    break;
//                case LOADING_MORE:
//                    holder.footer.setText("正在加载更多数据...");
//                    break;
//            }
            //footer
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "点击了footer", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return datas.size() + 1;
    }

    @Override
    public void onClick(View v) {
        if (mOnRecyclerViewItemClickListener != null) {
            mOnRecyclerViewItemClickListener.onItemClick(v, (String) v.getTag());
            Log.i("robin", " v.getTag()==" + v.getTag());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnRecyclerViewItemLongClickListener != null) {
            mOnRecyclerViewItemLongClickListener.onItemLongClick(v, (String) v.getTag());
            return true;
        }
        return false;
    }

    @Override
    public int getItemViewType(int position) {
//        if (position == 0) {
//            return IS_HEADER;
//        } else 
        if (position == datas.size()) {
            return IS_FOOTER;
        } else {
            return IS_NORMAL;
        }
    }

    public class BaseViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public TextView footer;
        public ProgressBar progressBar;
        private int viewType;

        public BaseViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            if (viewType == IS_HEADER) {

            } else if (viewType == IS_FOOTER) {
                footer = (TextView) itemView.findViewById(R.id.footer);
                progressBar = (ProgressBar) itemView.findViewById(R.id.progressBar);
            } else if (viewType == IS_NORMAL) {
                mTextView = (TextView) itemView.findViewById(R.id.text);
            }
        }
    }
    public  void setBottomMargin(int height) {
        if (height < 0) return ;
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)view.getLayoutParams();
        lp.bottomMargin = height;
        view.setLayoutParams(lp);
    }

    public  int getBottomMargin() {
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)view.getLayoutParams();
        return lp.bottomMargin;
    }


    /**
     * //上拉加载更多
     * PULLUP_LOAD_MORE=0;
     * //正在加载中
     * LOADING_MORE=1;
     * //加载完成已经没有更多数据了
     * NO_MORE_DATA=2;
     *
     * @param status
     */
    public void changeMoreStatus(int status) {
        load_more_status = status;
    }
}

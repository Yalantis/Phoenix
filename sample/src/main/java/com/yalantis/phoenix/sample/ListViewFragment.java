package com.yalantis.phoenix.sample;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.yalantis.phoenix.PullToRefreshView;

import java.util.List;
import java.util.Map;

/**
 * Created by Oleksii Shliama.
 */
public class ListViewFragment extends BaseRefreshFragment {

    private PullToRefreshView mPullToRefreshView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_list_view, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(new SampleAdapter(getActivity(), R.layout.list_item, mSampleList));

        mPullToRefreshView = (PullToRefreshView) rootView.findViewById(R.id.pull_to_refresh);
        mPullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPullToRefreshView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPullToRefreshView.setRefreshing(false);
                    }
                }, REFRESH_DELAY);
            }
        });

        return rootView;
    }

    class SampleAdapter extends ArrayAdapter<Map<String, Integer>> {

        public static final String KEY_ICON = "icon";
        public static final String KEY_COLOR = "color";

        private final LayoutInflater mInflater;
        private final List<Map<String, Integer>> mData;

        public SampleAdapter(Context context, int layoutResourceId, List<Map<String, Integer>> data) {
            super(context, layoutResourceId, data);
            mData = data;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_item, parent, false);
                viewHolder.imageViewIcon = (ImageView) convertView.findViewById(R.id.image_view_icon);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.imageViewIcon.setImageResource(mData.get(position).get(KEY_ICON));
            convertView.setBackgroundResource(mData.get(position).get(KEY_COLOR));

            return convertView;
        }

        class ViewHolder {
            ImageView imageViewIcon;
        }

    }

}

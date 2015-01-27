/*
 * Copyright (c) 2013 Joakim Lindskog
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.limewoodmedia.nsdroid.fragments;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.app.SherlockFragment;
import com.limewoodMedia.nsapi.holders.RegionHappening;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.TagParser;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class RegionalHappenings extends SherlockFragment {
	@SuppressWarnings("unused")
	private static final String TAG = RegionalHappenings.class.getName();
	
	private View root;
	private TextView title;
	private ListView list;
	private ViewGroup layout;
	private List<RegionHappening> happenings;
	private ArrayAdapter<RegionHappening> listAdapter;
	private Context context;

	public RegionalHappenings() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.regional_happenings, null, false);
		title = (TextView) root.findViewById(R.id.regional_happenings_header);
		list = (ListView) root.findViewById(R.id.regional_happenings_list);
		layout = (ViewGroup) root.findViewById(R.id.layout);
		ViewTreeObserver observer = title.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
				layout.setPadding(layout.getPaddingLeft(), title.getHeight() -
						(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
							getResources().getDisplayMetrics()), layout.getPaddingRight(),
							layout.getPaddingBottom());
		    }
		});

        happenings = new ArrayList<RegionHappening>();
		listAdapter = new ArrayAdapter<RegionHappening>(context, 0, happenings) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view;
TextView msg;
        		
        		if(convertView == null) {
        			view = inflater.inflate(R.layout.event, null);
        			msg = (TextView) view.findViewById(R.id.event_message);
        			msg.setMovementMethod(LinkMovementMethod.getInstance());
        		}
        		else {
        			view = convertView;
        			msg = (TextView) view.findViewById(R.id.event_message);
        		}
        		RegionHappening event = getItem(position);
    			long timestamp = event.timestamp;
    			String time = TagParser.parseTimestamp(getContext(), timestamp);
    			String text = time + ": " + event.text;
//        		text = text.replaceAll("@@([a-z\\d_]+)@@",
//        				"<a href=\"com.limewoodMedia.nsdroid.nation://$1\">$1</a>");
//        		text = text.replaceAll("%%([a-z\\d_]+)%%",
//        				"<a href=\"com.limewoodMedia.nsdroid.region://$1\">$1</a>");
        		msg.setText(Html.fromHtml(text));
        		
        		return view;
        	}
        };
        list.setAdapter(listAdapter);
        LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
		
		return root;
	}
	
	public void onBeforeLoading() {
        if(root != null) {
            LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
		this.happenings.clear();
	}

    public void onAfterLoading() {
        if(root != null) {
            LoadingHelper.stopLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
    }
	
	public void setHappenings(String region, List<RegionHappening> happenings) {
		title.setText(region);
		if(happenings != null) {
			if(this.happenings.size() > 0) {
				this.happenings.clear();
			}
			this.happenings.addAll(happenings);
			listAdapter.notifyDataSetChanged();
		}
	}
}

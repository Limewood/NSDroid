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
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.Embassy;
import com.limewoodMedia.nsapi.holders.Embassy.EmbassyStatus;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.activities.Region;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class EmbassiesFragment extends SherlockFragment implements OnClickListener, OnItemSelectedListener {
	private static final String TAG = EmbassiesFragment.class.getName();
	
	private View root;
	private ListView list;
	private LoadingView loadingView;
	
	private List<Embassy> embassies;
	private List<Embassy> embassiesCopy;
	private ArrayAdapter<Embassy> listAdapter;
	private Context context;
	private String errorMessage;
	private String region;
	private RegionData data;

	public EmbassiesFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.embassies, null, false);
		list = (ListView) root.findViewById(R.id.embassies_list);

        embassies = new ArrayList<Embassy>();
		listAdapter = new ArrayAdapter<Embassy>(context, 0, embassies) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view;
        		TextView name, status;
        		Embassy embassy;
        		
        		if(convertView == null) {
        			view = inflater.inflate(R.layout.embassy, null);
        			name = (TextView) view.findViewById(R.id.embassy_name);
        			name.setMovementMethod(LinkMovementMethod.getInstance());
        			status = (TextView) view.findViewById(R.id.embassy_status);
        		}
        		else {
        			view = convertView;
        			name = (TextView) view.findViewById(R.id.embassy_name);
        			status = (TextView) view.findViewById(R.id.embassy_status);
        		}
        		embassy = getItem(position);
        		
        		// Region
    			String eName = TagParser.idToName(embassy.region);
    			name.setText(Html.fromHtml(eName));
    			
    			// Status
    			String str = null;
    			Resources res = getResources();
    			int background = R.drawable.embassy_established;
    			switch(embassy.status) {
    			case INVITED:
    				str = res.getString(R.string.embassy_invited);
    				background = R.drawable.embassy_invited;
    				break;
    			case PENDING:
    				str = res.getString(R.string.embassy_pending);
    				background = R.drawable.embassy_pending;
    				break;
    			case REQUESTED:
    				str = res.getString(R.string.embassy_requested);
    				background = R.drawable.embassy_requested;
    				break;
    			case DENIED:
    				str = res.getString(R.string.embassy_denied);
    				background = R.drawable.embassy_denied;
    				break;
    			case CLOSING:
    				str = res.getString(R.string.embassy_closing);
    				background = R.drawable.embassy_closing;
    				break;
    			case ESTABLISHED:
    				break;
    			}
        		status.setText(str);
        		view.setBackgroundResource(background);
        		
        		view.setTag(embassy);
        		view.setOnClickListener(EmbassiesFragment.this);
        		name.setOnClickListener(EmbassiesFragment.this);
        		status.setOnClickListener(EmbassiesFragment.this);
        		
        		return view;
        	}
        };
        list.setAdapter(listAdapter);
        
        ((Spinner)root.findViewById(R.id.embassies_filter)).setOnItemSelectedListener(this);
		
		return root;
	}
	
	public void setRegionName(String name) {
		region = name;
	}
	
	public void setEmbassies(List<Embassy> embassies) {
		if(embassies != null) {
			if(this.embassies.size() > 0) {
				this.embassies.clear();
			}
			this.embassies.addAll(embassies);
    		this.embassiesCopy = embassies;
			this.listAdapter.notifyDataSetChanged();
		}
		list.setVisibility(View.VISIBLE);
		LoadingHelper.stopLoading(loadingView);
	}
	
	public void onBeforeLoad() {
		this.embassies.clear();
    	startLoading(R.string.loading_embassies);
	}
	
	private void startLoading(int res) {
		if(loadingView == null) {
			loadingView = (LoadingView) root.findViewById(R.id.loading);
		}
		list.setVisibility(View.GONE);
		LoadingHelper.startLoading(loadingView, res, getActivity());
	}
	
	public void updateEmbassies() {
		this.embassies.clear();
    	startLoading(R.string.loading_embassies);
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
	                data = API.getInstance(context).getRegionInfo(region,
	                		RegionData.Shards.NAME, RegionData.Shards.EMBASSIES);
	                
	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownRegionException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_region, e.getRegion());
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return false;
        	};
        	
        	protected void onPostExecute(Boolean result) {
        		LoadingHelper.stopLoading(loadingView);
        		if(result) {
            		setEmbassies(data.embassies);
        		}
        		else {
        			Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	};
        }.execute();
    }

	@Override
	public void onClick(View v) {
		// Go to region
		Embassy e = (Embassy) v.getTag();
		if(e == null) {
			e = (Embassy) ((ViewGroup)v.getParent()).getTag();
		}
		if(e != null) {
			Intent i = new Intent(getActivity(), Region.class);
			i.setData(Uri.parse("com.limewoodMedia.nsdroid.region://" + e.region.replace(' ', '_')));
			getActivity().startActivity(i);
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if(embassiesCopy == null) {
			return;
		}
		List<Embassy> embassies = new ArrayList<Embassy>();
		EmbassyStatus status = null;
		switch(position) {
		case 0: // All
			this.embassies.clear();
	    	this.embassies.addAll(embassiesCopy);
	    	this.listAdapter.notifyDataSetChanged();
			return;
		case 1: // Changes
			status = null;
			break;
		case 2: // Established
			status = EmbassyStatus.ESTABLISHED;
			break;
		case 3: // Invited
			status = EmbassyStatus.INVITED;
			break;
		case 4: // Pending
			status = EmbassyStatus.PENDING;
			break;
		case 5: // Requested
			status = EmbassyStatus.REQUESTED;
			break;
		case 6: // Denied
			status = EmbassyStatus.DENIED;
			break;
		case 7: // Closing
			status = EmbassyStatus.CLOSING;
			break;
		}
    	for(Embassy em : this.embassiesCopy) {
    		if(status == null && em.status != EmbassyStatus.ESTABLISHED) {
    			embassies.add(em);
    		} else if(em.status == status) {
    			embassies.add(em);
    		}
    	}
    	this.embassies.clear();
    	this.embassies.addAll(embassies);
    	this.listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}
}

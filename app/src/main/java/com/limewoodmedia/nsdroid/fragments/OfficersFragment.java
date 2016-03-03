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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.Embassy;
import com.limewoodMedia.nsapi.holders.Embassy.EmbassyStatus;
import com.limewoodMedia.nsapi.holders.Officer;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.activities.Nation;
import com.limewoodmedia.nsdroid.activities.Region;
import com.limewoodmedia.nsdroid.views.LoadingView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OfficersFragment extends Fragment implements OnClickListener {
	private static final String TAG = OfficersFragment.class.getName();

	private View root;
	private ListView list;
	private LoadingView loadingView;

	private List<Officer> officers;
	private ArrayAdapter<Officer> listAdapter;
	private Context context;
	private String errorMessage;
	private String region;
	private RegionData data;
	private OfficersComparator officersComparator;

	public OfficersFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.officers, null, false);
		list = (ListView) root.findViewById(R.id.officers_list);

		officers = new ArrayList<Officer>();
		officersComparator = new OfficersComparator();
		listAdapter = new ArrayAdapter<Officer>(context, 0, officers) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view;
        		TextView name, office, authority;
        		Officer officer;
        		
        		if(convertView == null) {
        			view = inflater.inflate(R.layout.officer, null);
        		}
        		else {
        			view = convertView;
        		}
				name = (TextView) view.findViewById(R.id.officer_name);
				name.setMovementMethod(LinkMovementMethod.getInstance());
				office = (TextView) view.findViewById(R.id.officer_office);
				authority = (TextView) view.findViewById(R.id.officer_authority);
        		officer = getItem(position);
        		
        		// Name
    			String oName = TagParser.idToName(officer.nation);
    			name.setText(Html.fromHtml(oName));
				// Office
				office.setText(officer.office);
				// Authority
				StringBuilder sb = new StringBuilder();
				int i=0;
				for(Officer.Authority auth : officer.authority) {
					sb.append(auth.name);
					if(i+1<officer.authority.size()) {
						sb.append(", ");
					}
					i++;
				}
				authority.setText(sb.toString());

        		view.setTag(officer);
        		view.setOnClickListener(OfficersFragment.this);
        		name.setOnClickListener(OfficersFragment.this);
        		office.setOnClickListener(OfficersFragment.this);
        		
        		return view;
        	}
        };
        list.setAdapter(listAdapter);
		
		return root;
	}
	
	public void setRegionName(String name) {
		region = name;
	}
	
	public void setOfficers(List<Officer> officers) {
		if(officers != null) {
			Log.d(TAG, "Officers: "+officers.size());
			if(this.officers.size() > 0) {
				this.officers.clear();
			}
			Collections.sort(officers, officersComparator);
			this.officers.addAll(officers);
			this.listAdapter.notifyDataSetChanged();
		}
		list.setVisibility(View.VISIBLE);
		LoadingHelper.stopLoading(loadingView);
	}
	
	public void onBeforeLoad() {
		this.officers.clear();
    	startLoading(R.string.loading_officers);
	}
	
	private void startLoading(int res) {
		if(loadingView == null) {
			loadingView = (LoadingView) root.findViewById(R.id.loading);
		}
		list.setVisibility(View.GONE);
		LoadingHelper.startLoading(loadingView, res, getActivity());
	}
	
	public void updateOfficers() {
		this.officers.clear();
    	startLoading(R.string.loading_officers);
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
	                data = API.getInstance(context).getRegionInfo(region,
	                		RegionData.Shards.NAME, RegionData.Shards.OFFICERS);
	                
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
				} catch (XmlPullParserException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.xml_parser_exception);
                } catch (IOException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.api_io_exception);
                }

                return false;
        	};
        	
        	protected void onPostExecute(Boolean result) {
        		LoadingHelper.stopLoading(loadingView);
        		if(result) {
            		setOfficers(data.officers);
        		}
        		else {
        			Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	};
        }.execute();
    }

	@Override
	public void onClick(View v) {
		// Go to nation
		Officer e = (Officer) v.getTag();
		if(e == null) {
			e = (Officer) ((ViewGroup)v.getParent()).getTag();
		}
		if(e != null) {
			Intent i = new Intent(getActivity(), Nation.class);
			i.setData(Uri.parse("com.limewoodMedia.nsdroid.nation://" + e.nation.replace(' ', '_')));
			getActivity().startActivity(i);
		}
	}

	class OfficersComparator implements Comparator<Officer> {
		@Override
		public int compare(Officer lhs, Officer rhs) {
			return lhs.order-rhs.order;
		}
	}
}

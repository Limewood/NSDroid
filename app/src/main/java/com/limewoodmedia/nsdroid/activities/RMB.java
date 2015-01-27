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
package com.limewoodmedia.nsdroid.activities;

import java.util.Arrays;
import java.util.Comparator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.AlphabeticComparator;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.fragments.RMBFragment;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.views.NumberPicker;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class RMB extends SherlockFragmentActivity implements OnClickListener, NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = RMB.class.getName();
	public static boolean shouldUpdate = false;

	private RMBFragment rmb;
	private TextView regionName;
	private ImageView previous;
	private ImageView page;
	private ImageView next;
	
	private String region;
	private RegionDataParcelable data;
	private String errorMessage;
	private boolean allowPosting = false;
	private boolean homeRegion = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.rmb_single);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);
        
        rmb = (RMBFragment) getSupportFragmentManager().findFragmentById(R.id.rmb);
        regionName = (TextView) findViewById(R.id.rmb_region_name);
        // Page navigation
        previous = (ImageView) findViewById(R.id.rmb_previous);
        previous.setOnClickListener(this);
        page = (ImageView) findViewById(R.id.rmb_page);
        page.setOnClickListener(this);
        next = (ImageView) findViewById(R.id.rmb_next);
        next.setOnClickListener(this);
        
        Log.d(TAG, "DATA "+getIntent().getDataString());
        if(getIntent().getData() == null) {
        	// Get user's region
        	homeRegion = true;
        	region = NationInfo.getInstance(this).getRegionId();
        }
        else {
        	region = getIntent().getDataString().replace("com.limewoodMedia.nsdroid.region.rmb://", "");
        }
        rmb.setRegionName(region);
        
//        if(savedInstanceState == null) {
        	loadMessages();
//        } else {
//        	// Restore state
//        	data = savedInstanceState.getParcelable("region_data");
//        	doSetup();
//        }
    }
    
    @Override
    protected void onResume() {
    	shouldUpdate = homeRegion;
    	
    	// Cancel RMB notification
    	((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
    		.cancel(NotificationsHelper.NOTIFICATION_ID_RMB);
    	
    	// Start timer if it's not started and it should be
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	if(prefs.getString("rmb_update_interval", "-1").compareTo("-1") != 0) {
	    	NotificationsHelper.setAlarmForRMB(this,
	    			Integer.parseInt(prefs.getString("rmb_update_interval", "-1")));
    	}
    	
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	shouldUpdate = false;
    }
    
    private void doSetup() {
    	// Region name
		setTitle(data.name);
		regionName.setText(data.name+" "+getResources().getString(R.string.rmb_abbr));
		
		rmb.setMessages(data.messages, 0, homeRegion);
		
		Comparator<String> comparator = new AlphabeticComparator();
		Log.d(TAG, "Nations: "+data.nations);
		Arrays.sort(data.nations, comparator);
		if(Arrays.binarySearch(data.nations, NationInfo.getInstance(this).getId(), comparator) > -1) {
			allowPosting = true;
			supportInvalidateOptionsMenu();
		}
    }
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//    	super.onSaveInstanceState(outState);
//    	
//    	outState.putParcelable("region_data", data);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_rmb, menu);
        if(!allowPosting) {
        	menu.findItem(R.id.menu_post).setVisible(false);
        }
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_post:
            	rmb.prepareMessage();
            	break;
            case R.id.menu_refresh:
            	rmb.loadMessages();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadMessages() {
    	loadMessages(0);
    }
    
    private void loadMessages(final int offsetPage) {
    	if(offsetPage > 0) { // Don't update automatically if we're reading back in history
        	shouldUpdate = false;
    	} else {
        	shouldUpdate = homeRegion;
    	}
    	rmb.onBeforeLoad();
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
					RegionData.Shards[] shards = new RegionData.Shards[]{
							RegionData.Shards.NAME,
        					RegionData.Shards.NATIONS, RegionData.Shards.MESSAGES
	                			.setArgument(RegionData.Shards.Arguments.MESSAGES_OFFSET,
	                					Integer.toString(offsetPage*10))
					};
					if(homeRegion) {
		                data = API.getInstance(RMB.this).getHomeRegionInfo(RMB.this, shards);
					} else {
						data = API.getInstance(RMB.this).getRegionInfo(region, shards);
					}
	                
	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownRegionException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_region);
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return false;
        	};
        	
        	protected void onPostExecute(Boolean result) {
        		if(result) {
        			doSetup();
        		}
        		else {
        			Toast.makeText(RMB.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	};
        }.execute();
    }

	@Override
	public void onClick(View v) {
		int offset = rmb.getPageOffset();
		if(v == previous) {
			rmb.loadMessages(offset+1);
		}
		else if(v == next) {
			if(offset > 0) {
				rmb.loadMessages(offset-1);
			}
		}
		else if(v == page) {
			// Show dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.rmb_go_to_page_title);
			builder.setMessage(R.string.rmb_go_to_page_message);
			final NumberPicker picker = (NumberPicker) getLayoutInflater().inflate(R.layout.rmb_go_to_page, null);
			picker.setNumber(offset);
			builder.setView(picker);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int offset = picker.getNumber();
					dialog.dismiss();
					rmb.loadMessages(offset);
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();
		}
	}

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

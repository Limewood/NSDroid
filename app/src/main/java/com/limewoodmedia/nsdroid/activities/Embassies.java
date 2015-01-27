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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.EmbassiesFragment;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Embassies extends SherlockFragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Embassies.class.getName();

	private EmbassiesFragment embassies;
	private TextView title;
	
	private String region;
	private RegionDataParcelable data;
	private String errorMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.embassies_single);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);
        
        Log.d(TAG, "DATA "+getIntent().getDataString());
        if(getIntent().getData() == null) {
        	// Get home region's embassies
            region = NationInfo.getInstance(this).getRegionId();
        }
        else {
        	region = getIntent().getDataString().replace("com.limewoodMedia.nsdroid.region.embassies://", "");
        }
        Log.d(TAG, "Show embassies for "+region);

        title = (TextView) findViewById(R.id.embassies_title);
        embassies = (EmbassiesFragment) getSupportFragmentManager().findFragmentById(R.id.embassies);
        embassies.setRegionName(region);

//        if(savedInstanceState == null) {
        	loadEmbassies();
//        } else {
//        	data = savedInstanceState.getParcelable("region_data");
//        	doSetup();
//        }
    }
    
    private void doSetup() {
    	// Region name
		setTitle(data.name);
		title.setText(getResources().getString(R.string.embassies_title, data.name));
		
		embassies.setEmbassies(data.embassies);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
//    	outState.putParcelable("region_data", data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_embassies, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
            	embassies.updateEmbassies();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadEmbassies() {
    	embassies.onBeforeLoad();
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
	                data = API.getInstance(Embassies.this).getRegionInfo(region,
	                		RegionData.Shards.NAME, RegionData.Shards.EMBASSIES);
	                
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
        			Toast.makeText(Embassies.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	};
        }.execute();
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

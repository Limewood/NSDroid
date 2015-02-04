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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.Happening;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NationalHappenings;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.fragments.RegionalHappenings;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.holders.RMBMessageParcelable;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class NSDroid extends SherlockFragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	private static final String TAG = NSDroid.class.getName();
	public static boolean shouldUpdate = false;
	
	private NationalHappenings nationalHappenings;
	private RegionalHappenings regionalHappenings;
	private TextView lastPostHeader;
	private TextView lastPostNation;
	private TextView lastPost;
	private NationInfo info;
	private String errorMessage;
	private NationDataParcelable nData;
	private RegionDataParcelable rData;
	private RMBReceiver updateReceiver = new RMBReceiver();
    private boolean registeredUpdateReceiver = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String languageToLoad  = prefs.getString(getString(R.string.preference_locale), "");
        Locale locale = new Locale(languageToLoad); 
        Locale.setDefault(locale);
        Resources res = getBaseContext().getResources();
        Configuration config = res.getConfiguration();
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());

		info = NationInfo.getInstance(this);

        if(info.getName() == null) {
        	// Not registered a nation yet
        	Intent i = new Intent(this, Welcome.class);
        	startActivity(i);
        	return;
        } else {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
                    try {
                        NationData nData = API.getInstance(NSDroid.this).getNationInfo(info.getId(), NationData.Shards.WA_STATUS);
                        info.setWAStatus(nData.worldAssemblyStatus);
                    } catch (UnknownNationException e) {
                        e.printStackTrace();
                        Toast.makeText(NSDroid.this, getString(R.string.unknown_nation, e.getNation()), Toast.LENGTH_LONG).show();
                    } catch (RateLimitReachedException e) {
                        e.printStackTrace();
                        Toast.makeText(NSDroid.this, R.string.rate_limit_reached, Toast.LENGTH_LONG).show();
                    }
					return null;
				}
			}.execute();
		}
		setTitle(NationInfo.getInstance(NSDroid.this).getName());
        
        setContentView(R.layout.overview);

        Utils.setupNavigationDrawer(this);
        
        // Fetch flag
        LoadingHelper.loadHomeFlag(this);

        Log.d(TAG, "National happenings: "+getSupportFragmentManager().findFragmentById(R.id.national_happenings));
        nationalHappenings = (NationalHappenings) getSupportFragmentManager().findFragmentById(R.id.national_happenings);
        regionalHappenings = (RegionalHappenings) getSupportFragmentManager().findFragmentById(R.id.regional_happenings);
        
        lastPostHeader = (TextView) findViewById(R.id.latest_rmb_post_header);
        lastPostHeader.setMovementMethod(LinkMovementMethod.getInstance());
        lastPostNation = (TextView) findViewById(R.id.rmb_poster);
        lastPostNation.setMovementMethod(LinkMovementMethod.getInstance());
        lastPost = (TextView) findViewById(R.id.rmb_message);
        lastPost.setMovementMethod(LinkMovementMethod.getInstance());
    }
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//    	super.onSaveInstanceState(outState);
//    	
//    	outState.putParcelable("nation_data", nData);
//    	outState.putParcelable("region_data", rData);
//    }
	
	@Override
	public void onResume() {
        if(info.getName() == null) {
            super.onResume();
            return;
        }
		shouldUpdate = true;
		loadData();
		
        registerReceiver(updateReceiver, new IntentFilter(NotificationsHelper.RMB_UPDATE_ACTION));
        registeredUpdateReceiver = true;
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
        if(registeredUpdateReceiver) {
            unregisterReceiver(updateReceiver);
        }
		shouldUpdate = false;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
            	loadData();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads data from the NS API into the overview panels
     */
    private void loadData() {
    	errorMessage = getResources().getString(R.string.general_error);
    	// Show loading animation
    	final LoadingView loadingView = (LoadingView) findViewById(R.id.loading_post);
    	LoadingHelper.startLoading(loadingView);
    	new AsyncTask<Void, Void, Boolean>() {
    		protected void onPreExecute() {
    			nationalHappenings.onBeforeLoading();
    			regionalHappenings.onBeforeLoading();
    			lastPost.setText("");
    			lastPostNation.setText("");
    		}
    		
        	protected Boolean doInBackground(Void...params) {
				try {
					nData = API.getInstance(NSDroid.this).getNationInfo(info.getName(),
							NationData.Shards.HAPPENINGS, NationData.Shards.NAME, NationData.Shards.REGION);
					
	                rData = API.getInstance(NSDroid.this).getRegionInfo(nData.region,
	                		RegionData.Shards.NAME, RegionData.Shards.HAPPENINGS, RegionData.Shards.MESSAGES);
	                
	                // Fetch nation and region names
	                Pattern nPattern = Pattern.compile("(.*?)@@([a-z\\-\\d_]+)@@(.*?)");
	                Pattern rPattern = Pattern.compile("(.*?)%%([a-z\\-\\d_]+)%%(.*?)");
	                Matcher matcher;
	                String n, r;
//	                NationData nd;
//	                RegionData rd;
	                List<Happening> list = new ArrayList<Happening>(nData.happenings);
	                list.addAll(rData.happenings);
	                for(Happening event : list) {
	        			event.text = event.text.replaceAll("@@("+info.getName().toLowerCase(Locale.getDefault()).replace(' ', '_')+")@@",
	            				"<a href=\"com.limewoodMedia.nsdroid.nation://$1\">"+info.getName()+"</a>");
	        			event.text = event.text.replaceAll("%%("+rData.name.toLowerCase(Locale.getDefault()).replace(' ', '_')+")%%",
	            				"<a href=\"com.limewoodMedia.nsdroid.region://$1\">"+rData.name+"</a>");
	            		event.text = event.text.replaceAll("%%([a-z\\d_]+)%rmb%%",
	            				"<a href=\"com.limewoodMedia.nsdroid.region.rmb://$1\">Regional Message Board</a>");
	            		// TODO Temporary
//	        			event.text = event.text.replaceAll("@@([a-z\\d_]+)@@",
//	            				"<a href=\"com.limewoodMedia.nsdroid.nation://$1\">$1</a>");
//	        			event.text = event.text.replaceAll("%%([a-z\\d_]+)%%",
//	            				"<a href=\"com.limewoodMedia.nsdroid.region://$1\">$1</a>");
	        			
	        			matcher = nPattern.matcher(event.text);
	        			while(matcher.matches()) {
		        			n = event.text.substring(matcher.start(2), matcher.end(2));
		        			Log.d(TAG, "Nation: "+n);
		        			
//		        			nd = API.getInstance(NSDroid.this).getNationInfo(n,
//									NationData.Shards.NAME, NationData.Shards.FLAG);
//		        			Log.d(TAG, "Name: "+nd.name);
		        			n = TagParser.idToName(n);
		        			
		        			event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.nation://$2\">" +
		        					n+"</a>$3");
		        			matcher = nPattern.matcher(event.text);
	        			}
	        			matcher = rPattern.matcher(event.text);
	        			while(matcher.matches()) {
		        			r = event.text.substring(matcher.start(2), matcher.end(2));
		        			Log.d(TAG, "Region: "+r);
		        			
//		        			rd = API.getInstance(NSDroid.this).getRegionInfo(r,
//									RegionData.Shards.NAME);
//		        			Log.d(TAG, "Name: "+rd.name);
		        			r = TagParser.idToName(r);
		        			
		        			event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.region://$2\">" +
		        					r+"</a>$3");
		        			matcher = rPattern.matcher(event.text);
	        			}
	                }
	                
	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownRegionException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_region, e.getRegion());
				} catch (UnknownNationException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_nation, e.getNation());
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return false;
        	}
        	
        	protected void onPostExecute(Boolean result) {
        		// Remove loading animation
				LoadingHelper.stopLoading(loadingView);
				nationalHappenings.onAfterLoading();
				regionalHappenings.onAfterLoading();
        		if(result) {
					doSetup();
        		}
        		else {
					Toast.makeText(NSDroid.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }
    
    private void doSetup() {
    	nationalHappenings.setHappenings(nData.happenings);
        regionalHappenings.setHappenings(rData.name, rData.happenings);
        
        lastPostHeader.setText(Html.fromHtml(
        		"<a href=\"com.limewoodMedia.nsdroid.region.rmb://"+
        		rData.name.replace(' ', '_')+"\">"+
        		getResources().getString(R.string.latest_rmb_post)+"</a>"));
        if(rData.messages != null && rData.messages.size() > 0) {
            RMBMessage lastMessage = rData.messages.get(rData.messages.size()-1);
            setLastRMBMessage(lastMessage);
        }
    }
    
    private void setLastRMBMessage(RMBMessage message) {
    	lastPost.setText(TagParser.parseTagsFromHtml(message.message));
        lastPostNation.setText(Html.fromHtml("<a href=\"com.limewoodMedia.nsdroid.nation://"+
        		message.nation+"\">"+TagParser.idToName(message.nation)+"</a><br />" + 
        		TagParser.parseTimestamp(NSDroid.this, message.timestamp)));
        // Save last message details
		Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putLong("update_region_messages_timestamp", message.timestamp);
		edit.putString("update_region_messages_nation", message.nation);
		edit.commit();
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }

    private class RMBReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "Update last post");
			// Update last RMB post
			Parcelable[] pArr = intent.getParcelableArrayExtra(
					"com.limewoodMedia.nsdroid.holders.RMBMessageParcelable");
			if(pArr != null && pArr.length > 0) {
				RMBMessage lastMessage = ((RMBMessageParcelable)pArr[pArr.length-1]).msg;
	            setLastRMBMessage(lastMessage);
			}
			// Set new alarm
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NSDroid.this);
			NotificationsHelper.setAlarmForRMB(NSDroid.this,
					Integer.parseInt(prefs.getString("rmb_update_interval", "-1")));
		}
	}
}

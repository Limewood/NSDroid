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
package com.limewoodmedia.nsdroid.services;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.activities.NSDroid;
import com.limewoodmedia.nsdroid.activities.Region;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.holders.IssuesInfo;
import com.limewoodmedia.nsdroid.holders.RMBMessageParcelable;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class UpdateService extends IntentService {
	private static final String TAG = UpdateService.class.getName();
	private static final int NOTIFICATION_ID = 12402;

	private NotificationManager notificationManager;
	
	public UpdateService() {
		super("UpdateService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
    	notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Check
        Log.d(TAG, "Checking for update for: "+intent.getStringExtra("update"));
        
        // Check for Internet connection
        final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            Log.d(TAG, "No network connection");
        	return;
        }
        
        boolean showNotification = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Check a shard for updates
        if(intent.getStringExtra("API").equalsIgnoreCase("region")) {
        	// Check region shards
        	RegionData rData;
        	if(intent.getStringExtra("region").equalsIgnoreCase("-1")) {
	        	if(intent.getStringExtra("shard").equalsIgnoreCase("messages")) {
	        		// Check for new messages
                    try {
                        rData = API.getInstance(this).getRegionInfo(NationInfo.getInstance(this).getRegionId(), RegionData.Shards.MESSAGES);
                    } catch (RateLimitReachedException e) {
                        e.printStackTrace();
                        return;
                    } catch (UnknownRegionException e) {
                        e.printStackTrace();
                        return;
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    if(rData.messages != null && rData.messages.size() > 0) {
	        			RMBMessage msg = rData.messages.get(rData.messages.size()-1);
		        		long lastTS = prefs.getLong("update_region_messages_timestamp", -1);
		        		String lastNation = prefs.getString("update_region_messages_nation", "");
		        		if(lastTS != msg.timestamp
		        				&& lastNation.compareToIgnoreCase(msg.message) != 0) {
		        			// There are new messages
		        			Log.d(TAG, "New messages!");
		        			if(Region.shouldUpdate || NSDroid.shouldUpdate) { // Send broadcast to update RMB directly
		        				Intent i = new Intent(NotificationsHelper.RMB_UPDATE_ACTION);
		        				RMBMessageParcelable[] parcel = new RMBMessageParcelable[rData.messages.size()];
		        				int t=0;
		        				for(RMBMessage m : rData.messages) {
		        					parcel[t] = new RMBMessageParcelable(m);
		        					t++;
		        				}
		        				i.putExtra("com.limewoodMedia.nsdroid.holders.RMBMessageParcelable", parcel);
		        				sendBroadcast(i);
		        				
		        				if(NSDroid.shouldUpdate) {
		        					showNotification = true;
		        				}
		        			} else { // Not currently showing
		        				showNotification = true;
		        			}
		        		} else {
		        			// No new messages - set new alarm
		        			NotificationsHelper.setAlarmForRMB(this,
		        					Integer.parseInt(prefs.getString("rmb_update_interval", "-1")));
		        		}
	        		}
	        	}
        	}
        } else if(intent.getStringExtra("API").equalsIgnoreCase("issues")) {
			// Issues
            IssuesInfo issues = API.getInstance(this).getIssues();
            Log.d(TAG, "Issues: "+issues);
            if(issues != null) {
                if(issues.issues != null) {
                    Log.d(TAG, "Issues: "+issues.issues.size());
                    intent.putExtra("notification_number", issues.issues.size());
                    showNotification = true;
                }
                if(issues.nextIssue != null) {
                    Log.d(TAG, "Issues: "+issues.nextIssue);
                    NotificationsHelper.setIssuesTimer(this, issues.nextIssue);
                }
            }
		}
    	
        if(showNotification) {
	    	// Show notification icon
	    	Intent i;
			try {
                // Start activity with class name
				i = new Intent(this, Class.forName(intent.getStringExtra("class")));
                if(intent.hasExtra("page")) {
                    i.putExtra("page", intent.getIntExtra("page", 0));
                }
	            Log.d(TAG, "Build notification to update "+intent.getStringExtra("class"));
		    	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    	PendingIntent pi = PendingIntent.getActivity(this, intent.getIntExtra("notification_id", NOTIFICATION_ID), i, PendingIntent.FLAG_UPDATE_CURRENT);
		    	NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(this)
					.setContentTitle(getResources().getString(intent.getIntExtra("notification_title", R.string.notification_title)))
					.setContentText(getResources().getString(intent.getIntExtra("notification_text", R.string.notification_text)))
					.setSmallIcon(R.drawable.app_icon)
					.setAutoCancel(true)
					.setContentIntent(pi)
					.setOnlyAlertOnce(false)
					.setWhen(System.currentTimeMillis());
		    	if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notify_sound", true)
		    			&& intent.hasExtra("notification_sound")) {
		    		notifyBuilder.setSound(Uri.parse("android.resource://"+getPackageName()+"/" + intent.getIntExtra("notification_sound", -1)));
		    	}
                if(intent.hasExtra("notification_number")) {
                    notifyBuilder.setNumber(intent.getIntExtra("notification_number", 0)).setShowWhen(false);
                }
	            Log.d(TAG, "Show notification");
		    	notificationManager.notify(intent.getIntExtra("notification_id", NOTIFICATION_ID), notifyBuilder.getNotification());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
	}
}

package com.limewoodmedia.nsdroid.services;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.activities.NSDroid;
import com.limewoodmedia.nsdroid.activities.Region;
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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
        }
    	
        if(showNotification) {
	    	// Show notification icon
	    	Intent i;
			try {
                // Start activity with class name
				i = new Intent(this, Class.forName(intent.getStringExtra("class")));
                i.putExtra("page", 1); // RMB page
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
	            Log.d(TAG, "Show notification");
		    	notificationManager.notify(intent.getIntExtra("notification_id", NOTIFICATION_ID), notifyBuilder.getNotification());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        }
	}
}

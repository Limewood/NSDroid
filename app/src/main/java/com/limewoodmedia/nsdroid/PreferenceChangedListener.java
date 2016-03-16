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
package com.limewoodmedia.nsdroid;

import com.limewoodmedia.nsdroid.activities.NSDroid;
import com.limewoodmedia.nsdroid.activities.NationsList;
import com.limewoodmedia.nsdroid.db.NationsDatabase;
import com.limewoodmedia.nsdroid.receivers.UpdateReceiver;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class PreferenceChangedListener implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private static final String TAG = PreferenceChangedListener.class.getName();
	
	private Activity context;
	private Preference rmbInterval;
	private ListPreference localePref;
	private Preference nations;
	
	public PreferenceChangedListener(Activity context) {
		this.context = context;
	}
	
	/**
	 * Setup for >HoneyComb
	 * @param fragment the PreferenceFragment
	 */
	@TargetApi(11)
	public void doSetup(PreferenceFragment fragment) {
		rmbInterval = fragment.findPreference(context.getString(R.string.preference_rmb_update_interval));
		localePref = (ListPreference) fragment.findPreference(context.getString(R.string.preference_locale));
		nations = fragment.findPreference(context.getString(R.string.preference_nations));
		doSetup();
	}
	
	/**
	 * Setup for <HoneyComb
	 * @param activity the PreferenceActivity
	 */
	@SuppressWarnings("deprecation")
	public void doSetup(PreferenceActivity activity) {
		rmbInterval = activity.findPreference(context.getString(R.string.preference_rmb_update_interval));
		localePref = (ListPreference) activity.findPreference(context.getString(R.string.preference_locale));
		nations = activity.findPreference(context.getString(R.string.preference_nations));
		doSetup();
	}
	
	private void doSetup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		rmbInterval.setOnPreferenceChangeListener(this);
		localePref.setOnPreferenceChangeListener(this);
		String rmbInt = prefs.getString(context.getString(R.string.preference_rmb_update_interval), "-1");
		rmbInterval.setDefaultValue(rmbInt);
		setRMBIntervalSummary(rmbInt);
		String localeSet = prefs.getString(context.getString(R.string.preference_locale), "en");
		localePref.setDefaultValue(localeSet);
		setLocaleSummary(localeSet);
		nations.setTitle(context.getString(R.string.nations, NationsDatabase.getInstance(context).numNations()));
		nations.setSummary(context.getString(R.string.nations_current, NationInfo.getInstance(context).getName()));
		nations.setOnPreferenceClickListener(this);
	}
	
	private void setRMBIntervalSummary(String newValue) {
		int value = Integer.parseInt((String)newValue);
		if (value == -1) {
			rmbInterval.setSummary(R.string.rmb_update_interval_summary_never);
		} else if (value < 60) {
			rmbInterval.setSummary(context.getResources().getString(
					R.string.rmb_update_interval_summary_minutes, value));
		} else {
			value = (int)(value/60f);
			rmbInterval.setSummary(context.getResources().getQuantityString(
					R.plurals.rmb_update_interval_summary_hours, value, value));
		}
	}
	
	private void setLocaleSummary(String value) {
		int i=0;
		for(CharSequence val : localePref.getEntryValues()) {
			if(value.contentEquals(val)) {
				break;
			}
			i++;
		}
		localePref.setSummary(localePref.getEntries()[i]);
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Log.d(TAG, "Preference change: "+preference.getKey()+" value: "+newValue);
		if (preference.getKey().equals(context.getString(R.string.preference_rmb_update_interval))) {
			// RMB update interval
			setRMBIntervalSummary((String)newValue);
			int value = Integer.parseInt((String)newValue);
			if (value == -1) {
				// Shut down timer if active
				Log.d(TAG, "Removing timer for RMB update");
		        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		        am.cancel(PendingIntent.getBroadcast(context, 0, new Intent(context, UpdateReceiver.class), PendingIntent.FLAG_CANCEL_CURRENT));
			} else {
				// Start timer / reset if already active
				Log.d(TAG, "Setting timer for RMB update");
		        NotificationsHelper.setAlarmForRMB(context, value);
			}
		} else if (preference.getKey().equals(context.getString(R.string.preference_locale))) {
			// Interface language
			setLocaleSummary((String) newValue);
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.locale_needs_restart_title);
			builder.setMessage(R.string.locale_needs_restart_message);
			builder.setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// Restart app
					Intent i = new Intent(context, NSDroid.class);
					i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(i);
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
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference == nations) {
			// Erase current nation and go to Welcome activity
//			NationInfo.getInstance(context).setName(null);
//			Intent i = new Intent(context, Welcome.class);
			Intent i = new Intent(context, NationsList.class);
        	context.startActivity(i);
        	return true;
		}
		return false;
	}
}

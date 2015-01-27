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

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.activities.RMB;
import com.limewoodmedia.nsdroid.receivers.UpdateReceiver;

public class NotificationsHelper {
	public static final int NOTIFICATION_ID_RMB = 1;
	public static final String RMB_UPDATE_ACTION = "Update RMB";
	
	public static void setAlarmForRMB(Context context, int value) {
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(context, UpdateReceiver.class).
        putExtra("class", RMB.class.getName()).
        putExtra("update", "RMB").
        putExtra("API", "region").
        putExtra("shard", "messages").
        putExtra("region", "-1"). // Home region
        putExtra("notification_id", NOTIFICATION_ID_RMB).
        putExtra("notification_title", R.string.notification_title_rmb).
        putExtra("notification_text", R.string.notification_text_rmb).
        putExtra("notification_sound", R.raw.new_rmb_message);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis()+(value*60*1000),
        		PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
	}
}

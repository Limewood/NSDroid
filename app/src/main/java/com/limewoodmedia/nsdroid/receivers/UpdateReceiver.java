package com.limewoodmedia.nsdroid.receivers;

import com.limewoodmedia.nsdroid.services.UpdateService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setClass(context, UpdateService.class);
		context.startService(intent);
	}
}

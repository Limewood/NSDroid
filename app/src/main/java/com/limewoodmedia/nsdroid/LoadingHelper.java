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

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.appcompat.app.AppCompatActivity;

public class LoadingHelper {
	public static void startLoading(final LoadingView view, String text) {
		if(view != null) {
    		view.startLoading(text);
    	}
	}
	
	public static void startLoading(final LoadingView view, int resId, Context context) {
		startLoading(view, context.getResources().getString(resId));
	}
	
	public static void startLoading(final LoadingView view) {
		startLoading(view, null);
	}
	
	public static void stopLoading(LoadingView view) {
		if(view != null) {
			view.stopLoading();
		}
	}
	
	public static Bitmap loadFlag(String url, Context context) throws ClientProtocolException, IOException {
		if(url == null || url.length() == 0) {
			// No flag to load
			return null;
		}
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, API.getInstance(context).getUserAgent());
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		HttpGet get = new HttpGet(url);
		HttpResponse response = client.execute(get);
		InputStream stream = response.getEntity().getContent();
		return BitmapFactory.decodeStream(stream);
	}
	
	public static void loadHomeFlag(final AppCompatActivity activity) {
//		new AsyncTask<Void, Void, Bitmap>() {
//			@Override
//			protected Bitmap doInBackground(Void... params) {
//				try {
//					return NationInfo.getInstance(activity).getFlagBitmap(activity);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				} catch (ExecutionException e) {
//					e.printStackTrace();
//				}
//				return null;
//			}
//
//			protected void onPostExecute(Bitmap result) {
//				if(result != null) {
//					ActionBar actionBar = activity.getSupportActionBar();
//					actionBar.setDisplayUseLogoEnabled(true);
//					actionBar.setDisplayHomeAsUpEnabled(true);
//					actionBar.setHomeButtonEnabled(true);
//					actionBar.setLogo(new BitmapDrawable(activity.getResources(), result));
//				}
//			};
//        }.execute();
	}
}

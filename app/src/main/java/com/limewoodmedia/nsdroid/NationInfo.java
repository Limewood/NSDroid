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

import static com.limewoodMedia.nsapi.holders.NationData.Shards.FLAG;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodmedia.nsdroid.db.NationsDatabase;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

public class NationInfo {
	private static final String TAG = NationInfo.class.getName();
	private static NationInfo instance;
	
	public static synchronized NationInfo getInstance(Context context) {
    	if(instance == null) {
    		Log.d(TAG, "Instantiating NationInfo");
    		instance = new NationInfo(context);
    	}
    	return instance;
    }
	
	private Context context;
	private String name;
	private String flag;
	private Bitmap flagBitmap;
	private String password;
	/** Cached region id */
	private String regionId;
	private WAStatus waStatus;
	
	private NationInfo(Context context) {
		this.context = context;
	}
	
	public String getName() {
		if(name == null) {
			name = getString("nation_name");
		}
		return name;
	}
	
	public String getId() {
		return getName().replace(' ', '_').toLowerCase();
	}
	
	public void setName(final String name) {
		if(this.name != name) {
			this.name = name;
			if(name != null) {
				saveString("nation_name", name);
			}
			new AsyncTask<String, Void, Void>() {
				@Override
				protected Void doInBackground(String... params) {
					// Log out
					API.getInstance(context).logout();
					if(name != null) {
						// Add to database
						if(params[0] != null) {
							NationsDatabase.getInstance(context).addNation(params[0]);
						}
					}
					return null;
				}
			}.execute(name);
		}
	}
	
	public String getFlag() {
		if(flag == null) {
			flag = getString("nation_flag");
		}
		return flag;
	}
	
	public void setFlag(String flag) {
		this.flag = flag;
		saveString("nation_flag", flag);
		if(flag == null) {
			flagBitmap = null;
		}
	}
	
	/**
	 * Returns the bitmap for your nation's flag
	 * Don't call from main thread
	 * @param activity Context
	 * @return flag bitmap
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public Bitmap getFlagBitmap(final SherlockFragmentActivity activity) throws InterruptedException, ExecutionException {
		Log.d(TAG, "Flag bitmap: "+flagBitmap);
		if(flagBitmap == null) {
			String uri = getFlag();
			if(uri == null) {
        		NationData data = null;
				try {
					data = API.getInstance(activity).getNationInfo(
	                		name.replace(' ', '_'),
	                		FLAG);
	                
					flag = data.flagURL;
					try {
						flagBitmap = LoadingHelper.loadFlag(flag, activity);
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
				} catch (UnknownNationException e) {
					e.printStackTrace();
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			} else {
				try {
					flagBitmap = LoadingHelper.loadFlag(uri, activity);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return flagBitmap;
	}
	
	public void setPassword(String password, boolean store) {
		this.password = password;
		if(store) {
			// TODO Not used, but don't save a password unencrypted!
			saveString("nation_password", password);
		}
	}
	
	public String getPassword() {
		if(password == null) {
			password = getString("nation_password");
		}
		return password;
	}
	
	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}
	
	public String getRegionId() {
		return regionId;
	}

	public WAStatus getWAStatus() {
		return waStatus;
	}

	public void setWAStatus(WAStatus waStatus) {
		this.waStatus = waStatus;
	}

	private void saveString(String key, String value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	private String getString(String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
}

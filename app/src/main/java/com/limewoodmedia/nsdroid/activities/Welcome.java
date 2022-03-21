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

import static com.limewoodMedia.nsapi.holders.NationData.Shards.*;

import java.io.IOException;
import java.net.URL;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.db.NationsDatabase;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.xmlpull.v1.XmlPullParserException;

import javax.net.ssl.HttpsURLConnection;

public class Welcome extends AppCompatActivity implements OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = Welcome.class.getName();
	
	private TextView title;
	private TextView welcome;
	private EditText nation;
	private Button checkButton;
	private Button selectButton;
	private ImageView flag;
	private TextView name;
	private TextView motto;
	private TextView region;
	private ScrollView welcomeFrame;
	
	private NationData data;
	private Bitmap flagBitmap;
	private NationInfo info;
	private String errorMessage;
	private boolean adding = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.welcome);
		
		info = NationInfo.getInstance(this);
		
		title = (TextView) findViewById(R.id.welcome_title);
		welcome = (TextView) findViewById(R.id.welcome_info);
		nation = (EditText) findViewById(R.id.welcome_nation);
		checkButton = (Button) findViewById(R.id.button_check);
		selectButton = (Button) findViewById(R.id.button_select);
		flag = (ImageView) findViewById(R.id.welcome_flag);
		name = (TextView) findViewById(R.id.welcome_name);
		motto = (TextView) findViewById(R.id.welcome_motto);
		region = (TextView) findViewById(R.id.welcome_region);
		welcomeFrame = (ScrollView) findViewById(R.id.welcome_frame);

		if(info.getName() != null) {
			// We're adding a new nation
			adding = true;
			title.setText(R.string.add_nation_title);
			welcome.setText(R.string.add_nation_info);
			nation.setHint(R.string.add_nation_hint);
			selectButton.setText(R.string.add_new_nation);
		}
		checkButton.setOnClickListener(this);
		selectButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v == checkButton) {
			// Show loading animation
	    	final LoadingView loadingView = (LoadingView) findViewById(R.id.loading);
	    	LoadingHelper.startLoading(loadingView, R.string.loading, this);
	    	errorMessage = getResources().getString(R.string.general_error);
	    	// Check if nation is in database
			// Check if nation exists, if so get info
			new AsyncTask<String, Void, Boolean>() {
				protected void onPreExecute() {
					nation.setEnabled(false);
					checkButton.setEnabled(false);
					selectButton.setEnabled(false);
				}
				
	        	protected Boolean doInBackground(String...params) {
					try {
//						if(NationsDatabase.getInstance(Welcome.this).nationExists(
//								params[0])) {
//							errorMessage = getResources().getString(R.string.nation_already_added);
//							return false;
//						}
						
		                data = API.getInstance(Welcome.this).getNationInfo(
		                		params[0],
		                		NAME, FULL_NAME, FLAG, MOTTO, REGION, WA_STATUS);
		                
		                URL imageURL = new URL(data.flagURL);
		                HttpsURLConnection connection = (HttpsURLConnection) imageURL.openConnection();
		                connection.setRequestProperty("User-Agent", API.USER_AGENT);
		                int code = connection.getResponseCode();
		                Log.e(TAG, "Code: "+code);
						String response = connection.getResponseMessage();
						Log.e(TAG, "Response: "+response);
		                connection.connect();
                		flagBitmap = BitmapFactory.decodeStream(connection.getInputStream());
		                
		                return true;
					} catch (RateLimitReachedException e) {
						e.printStackTrace();
						errorMessage = getResources().getString(R.string.rate_limit_reached);
					} catch (UnknownNationException e) {
						e.printStackTrace();
						errorMessage = getResources().getString(R.string.unknown_nation, e.getNation());
					} catch (RuntimeException e) {
						e.printStackTrace();
						errorMessage = e.getMessage();
					} catch (XmlPullParserException e) {
                        e.printStackTrace();
                        errorMessage = getResources().getString(R.string.xml_parser_exception);
                    } catch (IOException e) {
                        e.printStackTrace();
                        errorMessage = getResources().getString(R.string.api_io_exception);
                    }
					
					return false;
	        	}
	        	
	        	protected void onPostExecute(Boolean result) {
	        		// Remove loading animation
					LoadingHelper.stopLoading(loadingView);
	        		nation.setEnabled(true);
					checkButton.setEnabled(true);
	        		
	        		if(result && data.fullName != null) {
		                flag.setImageBitmap(flagBitmap);
		                name.setText(data.fullName);
		                motto.setText(Html.fromHtml("\""+data.motto+"\""));
		                region.setText(data.region);
		                
		                flag.setVisibility(View.VISIBLE);
		                name.setVisibility(View.VISIBLE);
		                motto.setVisibility(View.VISIBLE);
		                region.setVisibility(View.VISIBLE);
		                selectButton.setVisibility(View.VISIBLE);
						selectButton.setEnabled(true);
		        		
		                InputMethodManager imm = (InputMethodManager)getSystemService(
		              	      Context.INPUT_METHOD_SERVICE);
		                imm.hideSoftInputFromWindow(nation.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
		                new Handler().post(new Runnable() {
							@Override
							public void run() {
				                welcomeFrame.smoothScrollTo(0, selectButton.getBottom());
							}
		                });
	        		} else {
	        			flag.setVisibility(View.GONE);
		                name.setVisibility(View.GONE);
		                motto.setVisibility(View.GONE);
		                region.setVisibility(View.GONE);
		                selectButton.setVisibility(View.GONE);
	        			Toast.makeText(Welcome.this, errorMessage, Toast.LENGTH_SHORT).show();
	        		}
	        	};
	        }.execute(nation.getText().toString().replace(' ', '_'));
		}
		else if(v == selectButton) {
			if(!adding) {
				// Save this nation as the user's nation
				info.setFlag(data.flagURL);
				info.setName(data.name);
				info.setWAStatus(data.worldAssemblyStatus);
				API.getInstance(this).setUserNation(data.name);
				
				Intent i = new Intent(this, NSDroid.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			} else {
				// Adding nation - go back to nations list
				NationsDatabase.getInstance(this).addNation(data.name);
				
				Intent i = new Intent(this, NationsList.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		}
	}
}

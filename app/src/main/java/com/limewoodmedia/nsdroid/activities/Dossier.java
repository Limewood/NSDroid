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

import java.io.IOException;

import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.holders.DossierData;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class Dossier extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Dossier.class.getName();
	
	private ViewGroup layout;
	private String errorMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dossier);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);
        
        layout = (ViewGroup) findViewById(R.id.dossier_layout);
        
        loadDossier();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	loadDossier();
    }
    
    private void loadDossier() {
    	layout.setVisibility(View.GONE);
    	final LoadingView loadingView = (LoadingView) findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_dossier, this);
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, DossierData>() {
        	protected DossierData doInBackground(Void...params) {
				try {
					if(!API.getInstance(Dossier.this).checkLogin(Dossier.this)) {
						return null;
					}
					return API.getInstance(Dossier.this).getDossier();
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				} catch (Exception e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return null;
        	}
        	
        	protected void onPostExecute(DossierData result) {
        		LoadingHelper.stopLoading(loadingView);
        		if(result != null) {
        			doSetup(result);
        		}
        		else {
        			Toast.makeText(Dossier.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }
    
    private void doSetup(DossierData dossier) {
		ViewGroup nations = (ViewGroup) findViewById(R.id.dossier_nations);
		ViewGroup regions = (ViewGroup) findViewById(R.id.dossier_regions);
		nations.removeAllViews();
		regions.removeAllViews();
		
		ViewGroup nView;
		View dView;
		TextView nameView;
		TextView regionWAD;
		for(final NationDataParcelable ndp : dossier.nations) {
			if(ndp == null) continue;
			nView = (ViewGroup) getLayoutInflater().inflate(R.layout.dossier_nation, null, false);
			nameView = (TextView)nView.findViewById(R.id.nation_name);
			if(ndp.lastActivity != null) {
		    	nameView.setText(Html.fromHtml(
		    			"<a href=\"com.limewoodMedia.nsdroid.nation://"
		    					+ndp.name+"\">"+TagParser.idToName(ndp.name)+"</a>"), TextView.BufferType.SPANNABLE);
		    	nameView.setMovementMethod(LinkMovementMethod.getInstance());
				((TextView)nView.findViewById(R.id.nation_category)).setText(
						getString(R.string.category)+": "+ndp.category);
				regionWAD = (TextView)nView.findViewById(R.id.nation_region);
				regionWAD.setText(Html.fromHtml(
						getString(R.string.region)+": <a href=\"com.limewoodMedia.nsdroid.region://"
		    					+ndp.region+"\">"+TagParser.idToName(ndp.region)+"</a>"), TextView.BufferType.SPANNABLE);
				regionWAD.setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				nameView.setText(ndp.name);
				ndp.lastActivity = getString(R.string.ex_nation);
				nView.findViewById(R.id.nation_more).setVisibility(View.GONE);
			}
			((TextView)nView.findViewById(R.id.nation_active)).setText(ndp.lastActivity);
			nView.findViewById(R.id.nation_more).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					View view = ((View)v.getParent().getParent()).findViewById(R.id.bottom_layout);
					if(view.getVisibility() == View.GONE) {
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
				}
			});
			dView = nView.findViewById(R.id.nation_delete);
			dView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(Dossier.this);
					builder.setTitle(R.string.dossier_remove_nation_title);
					builder.setMessage(getString(R.string.dossier_remove_nation_message, ndp.name));
					builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new AsyncTask<String, Void, Void>() {
								@SuppressLint("DefaultLocale")
								@Override
								protected Void doInBackground(String... params) {
									if(API.getInstance(Dossier.this).checkLogin(Dossier.this)) {
										try {
											if(API.getInstance(Dossier.this).removeNationFromDossier(
													params[0].replace(' ', '_').toLowerCase())) {
												runOnUiThread(new Runnable() {
													public void run() {
														Toast.makeText(Dossier.this, R.string.nation_removed_from_dossier, Toast.LENGTH_SHORT).show();
														loadDossier();
													}
												});
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
									return null;
								}
							}.execute((String) v.getTag());
						}
					});
					builder.show();
				}
			});
			dView.setTag(ndp.name);
			nations.addView(nView);
		}
		
		for(final RegionDataParcelable rdp : dossier.regions) {
			if(rdp == null) continue;
			nView = (ViewGroup) getLayoutInflater().inflate(R.layout.dossier_region, null, false);
			nameView = (TextView)nView.findViewById(R.id.region_name);
			if(rdp.delegate != null) {
		    	nameView.setText(Html.fromHtml(
		    			"<a href=\"com.limewoodMedia.nsdroid.region://"
		    					+rdp.name+"\">"+TagParser.idToName(rdp.name)+"</a>"), TextView.BufferType.SPANNABLE);
		    	nameView.setMovementMethod(LinkMovementMethod.getInstance());
				((TextView)nView.findViewById(R.id.region_nations)).setText(
						String.format("%5d", rdp.numNations));
				regionWAD = (TextView)nView.findViewById(R.id.region_wad);
				if(rdp.delegate.equals("--None--")) {
					regionWAD.setText(getString(R.string.wad)+" "+rdp.delegate);
				} else {
					regionWAD.setText(Html.fromHtml(
							getString(R.string.wad)+" <a href=\"com.limewoodMedia.nsdroid.nation://"
			    					+rdp.delegate+"\">"+TagParser.idToName(rdp.delegate)+"</a>"), TextView.BufferType.SPANNABLE);
					regionWAD.setMovementMethod(LinkMovementMethod.getInstance());
				}
			} else {
				nameView.setText(rdp.name);
				nView.findViewById(R.id.region_wad).setVisibility(View.GONE);
			}
			dView = nView.findViewById(R.id.region_delete);
			dView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(Dossier.this);
					builder.setTitle(R.string.dossier_remove_region_title);
					builder.setMessage(getString(R.string.dossier_remove_region_message, rdp.name));
					builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new AsyncTask<String, Void, Void>() {
								@Override
								protected Void doInBackground(String... params) {
									if(API.getInstance(Dossier.this).checkLogin(Dossier.this)) {
										try {
											if(API.getInstance(Dossier.this).removeRegionFromDossier(
													params[0])) {
												runOnUiThread(new Runnable() {
													public void run() {
														Toast.makeText(Dossier.this, R.string.region_removed_from_dossier, Toast.LENGTH_SHORT).show();
														loadDossier();
													}
												});
											}
										} catch (IOException e) {
											e.printStackTrace();
										}
									}
									return null;
								}
							}.execute((String)v.getTag());
						}
					});
					builder.show();
				}
			});
			dView.setTag(rdp.name);
			regions.addView(nView);
		}
		
		layout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dossier, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
            	loadDossier();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodmedia.nsdroid.ColorInterpolator;
import com.limewoodmedia.nsdroid.ColorInterpolator.Color;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.views.BannerView;
import com.limewoodmedia.nsdroid.views.FreedomView;
import com.limewoodmedia.nsdroid.views.LoadingView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity for nation information
 * @author Joakim Lindskog
 */
public class Nation extends SherlockFragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Nation.class.getName();

	private ViewGroup layout;
	private BannerView banner;
	private BannerView flag;
	private TextView pretitle;
	private TextView name;
	private TextView motto;
	private TextView region;
	private TextView waStatus;
	private TextView influence;
	private TextView endorsed;
	private TextView category;
	private FreedomView civilRights;
	private FreedomView economy;
	private FreedomView politicalFreedoms;
	private TextView description;
	private TextView endorsements;
	
	private String nation;
	private NationDataParcelable data;
	private String errorMessage;
	private ImageLoader imageLoader;
	private DisplayImageOptions options;
	private boolean endoByYou = false;
	private boolean myNation = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.nation);
		imageLoader = Utils.getImageLoader(this);
		this.options = Utils.getImageLoaderDisplayOptions();

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);
        
        Log.d(TAG, "DATA "+getIntent().getDataString());
        if(getIntent().getData() == null && !getIntent().hasExtra("nation")) {
        	nation = NationInfo.getInstance(this).getId();
			myNation = true;
        } else if(getIntent().hasExtra("nation")) {
			nation = getIntent().getStringExtra("nation");
			myNation = nation.equals(NationInfo.getInstance(this).getId());
		} else {
        	nation = getIntent().getDataString().replace("com.limewoodMedia.nsdroid.nation://", "");
			myNation = nation.equals(NationInfo.getInstance(this).getId());
        }
        
        layout = (ViewGroup) findViewById(R.id.nation_layout);
		banner = (BannerView) findViewById(R.id.nation_banner);
        flag = (BannerView) findViewById(R.id.nation_flag);
		pretitle = (TextView) findViewById(R.id.nation_pretitle);
        name = (TextView) findViewById(R.id.nation_name);
        motto = (TextView) findViewById(R.id.nation_motto);
        region = (TextView) findViewById(R.id.nation_region);
        region.setMovementMethod(LinkMovementMethod.getInstance());
        waStatus = (TextView) findViewById(R.id.nation_wa_status);
        influence = (TextView) findViewById(R.id.nation_influence);
		endorsed = (TextView) findViewById(R.id.nation_endorsed);
        category = (TextView) findViewById(R.id.nation_category);
        civilRights = (FreedomView) findViewById(R.id.nation_civil_rights);
        economy = (FreedomView) findViewById(R.id.nation_economy);
        politicalFreedoms = (FreedomView) findViewById(R.id.nation_political_freedoms);
        description = (TextView) findViewById(R.id.nation_description);
		endorsements = (TextView) findViewById(R.id.nation_endorsements);

//        if(savedInstanceState == null) {
        	loadNation();
//        } else {
//        	// Restore
//        	data = savedInstanceState.getParcelable("nation_data");
//        	doSetup();
//        }
    }
    
    private void loadNation() {
    	layout.setVisibility(View.GONE);
    	final LoadingView loadingView = (LoadingView) findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_nation, this);
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
	                data = API.getInstance(Nation.this).getNationInfo(nation,
	                		FREEDOMS, CATEGORY, TYPE, MOTTO, FLAG, FULL_NAME,
	                		POPULATION, ADMIRABLE, CUSTOM_LEADER, NOTABLE,
	                		SENSIBILITIES, GOVERNMENT_DESCRIPTION, TAX_RATE,
	                		INDUSTRY_DESCRIPTION, LEGISLATION, CRIME, NAME,
	                		ANIMAL, ANIMAL_TRAIT, CUSTOM_RELIGION, CURRENCY,
	                		REGION, WA_STATUS, INFLUENCE, CUSTOM_CAPITAL,
							BANNERS, FREEDOM_SCORES, ENDORSEMENTS);
	                
	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownNationException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_nation);
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return false;
        	}
        	
        	protected void onPostExecute(Boolean result) {
        		LoadingHelper.stopLoading(loadingView);
        		
        		if(result) {
                	layout.setVisibility(View.VISIBLE);
        			doSetup();
        		}
        		else {
        			Toast.makeText(Nation.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }
    
    private void doSetup() {
		setTitle(data.name);

		// Set banner
		if(data.banners != null && data.banners.length > 0) {
			String bannerURL = data.getBannerURL(data.banners[0]);
			imageLoader.displayImage(bannerURL, banner, options);
		} else {
			banner.setImageResource(R.drawable.default_white);
		}
        if(data.flagURL != null) {
            imageLoader.displayImage(data.flagURL, flag, options);
        }
		pretitle.setText(getString(R.string.pretitle, data.type));
		name.setText(data.name);
		motto.setText(Html.fromHtml(data.motto));
		region.setText(Html.fromHtml(
				"<a href=\"com.limewoodMedia.nsdroid.region://"+data.region
				.replace(' ', '_')+"\">"+data.region+"</a>"));
		switch(data.worldAssemblyStatus) {
		case NON_MEMBER:
			waStatus.setVisibility(View.GONE);
			break;
		case WA_MEMBER:
			waStatus.setText(getResources().getString(R.string.wa_member));
			if(waStatus.getVisibility() != View.VISIBLE) {
				waStatus.setVisibility(View.VISIBLE);
			}
			break;
		case WA_DELEGATE:
			waStatus.setText(getResources().getString(R.string.wa_delegate));
			if(waStatus.getVisibility() != View.VISIBLE) {
				waStatus.setVisibility(View.VISIBLE);
			}
			break;
		}
		influence.setText(getResources().getString(R.string.influence)+": "+data.influence);
		endoByYou = false;
		Log.d(TAG, "Id: "+ NationInfo.getInstance(this).getId());
		for(String e : data.endorsements) {
			Log.d(TAG, "Endo: "+e);
			if(NationInfo.getInstance(this).getId().equals(e)) {
				Log.d(TAG, "Endorsed!");
				endoByYou = true;
				break;
			}
		}
		if(!myNation && data.worldAssemblyStatus != WAStatus.NON_MEMBER &&
				data.region.equals(NationInfo.getInstance(this).getRegionId()) &&
				NationInfo.getInstance(this).getWAStatus() != WAStatus.NON_MEMBER) {
			endorsed.setText(endoByYou ? R.string.nation_endorsed : R.string.nation_not_endorsed);
			endorsed.setVisibility(View.VISIBLE);
		} else {
			endorsed.setVisibility(View.GONE);
		}
        category.setText(data.category);
        civilRights.setText(data.freedoms.civilRights);
        economy.setText(data.freedoms.economy);
        politicalFreedoms.setText(data.freedoms.politicalFreedoms);

		// Set gradients
		Color[] colors = getGradientFor(data.freedoms.civilRightsValue);
		Log.d(TAG, "Civil rights: "+colors[0]+"; "+colors[1]);
		civilRights.setGradient(colors[0], colors[1]);

		colors = getGradientFor(data.freedoms.economyValue);
		economy.setGradient(colors[0], colors[1]);

		colors = getGradientFor(data.freedoms.politicalFreedomsValue);
		politicalFreedoms.setGradient(colors[0], colors[1]);

        description.setText(data.getDescription());

		// Endorsements
		if(data.endorsements.length > 0) {
			String endos = "";
			for (String n : data.endorsements) {
				if (endos.length() > 0) {
					endos += ", ";
				}
				endos += "<a href=\"com.limewoodMedia.nsdroid.nation://" + n + "\">" + TagParser.idToName(n) + "</a>";
			}
			endorsements.setText(Html.fromHtml(endos));
			endorsements.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			endorsements.setVisibility(View.GONE);
			findViewById(R.id.nation_endorsements_title).setVisibility(View.GONE);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				supportInvalidateOptionsMenu();
			}
		});
        layout.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	outState.putParcelable("nation_data", data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_nation, menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Endorse is the first item
		if(myNation || (data != null && (data.worldAssemblyStatus == WAStatus.NON_MEMBER ||
				!data.region.equals(NationInfo.getInstance(this).getRegionId()))) ||
				NationInfo.getInstance(this).getWAStatus() == WAStatus.NON_MEMBER) {
			menu.getItem(0).setVisible(false);
		} else {
			menu.getItem(0).setTitle(endoByYou ? R.string.menu_unendorse_nation : R.string.menu_endorse_nation).setVisible(true);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_add_nation_to_dossier:
	        	new AsyncTask<Void, Void, Void>() {
	        		@Override
	        		protected Void doInBackground(Void... params) {
	    	        	if(API.getInstance(Nation.this).checkLogin(Nation.this)) {
	    	        		try {
	    						if(API.getInstance(Nation.this).addNationToDossier(nation.replace(' ', '_').toLowerCase())) {
	    							runOnUiThread(new Runnable() {
	    								public void run() {
	    	    							Toast.makeText(Nation.this, R.string.nation_added_to_dossier, Toast.LENGTH_SHORT).show();
	    								}
	    							});
	    						}
	    					} catch (IOException e) {
	    						e.printStackTrace();
	    					}
	    	        	}
	        			return null;
	        		}
	        	}.execute();
	        	break;
            case R.id.menu_refresh:
            	loadNation();
            	break;
			case R.id.menu_endorse_nation: {
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						if(API.getInstance(Nation.this).checkLogin(Nation.this)) {
							if(API.getInstance(Nation.this).endorseNation(nation.replace(' ', '_').toLowerCase(), !endoByYou)) {
								runOnUiThread(new Runnable() {
									public void run() {
										supportInvalidateOptionsMenu();
										Toast.makeText(Nation.this, endoByYou ? R.string.nation_unendorsed_msg : R.string.nation_endorsed_msg, Toast.LENGTH_SHORT).show();
										loadNation();
									}
								});
							}
						}
						return null;
					}
				}.execute();
				break;
			}
        }
        return super.onOptionsItemSelected(item);
    }

	private Color[] getGradientFor(int value) {
		return new Color[]{
				new ColorInterpolator().interpolateColor(
						new ColorInterpolator.Color(1, 0, 0, 1), new ColorInterpolator.Color(.26f, .98f, 0, 1), value/100f),
				new ColorInterpolator().interpolateColor(
						new ColorInterpolator.Color(.5f, 0, 0, 1), new ColorInterpolator.Color(.13f, .49f, 0, 1), value/100f)
		};
	}

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

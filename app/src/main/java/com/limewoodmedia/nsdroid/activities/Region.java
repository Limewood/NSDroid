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

import static com.limewoodMedia.nsapi.holders.RegionData.Shards.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.AlphabeticComparator;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.ParallelTask;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.EmbassiesFragment;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.fragments.RMBFragment;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.views.LoadingView;
import com.limewoodmedia.nsdroid.views.NumberPicker;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Region extends SherlockFragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Region.class.getName();
    public static boolean shouldUpdate = false;

    private ViewGroup wfe;
	private ViewGroup layout;
	private TextView regionName;
	private ImageView flag;
	private TextView delegate;
	private TextView founder;
	private TextView text;
	private ViewGroup fieldset;
	private TextView header;

    private ViewGroup rmb;
    private RMBFragment rmbInner;
    private TextView rmbRegion;
    private ImageView previous;
    private ImageView page;
    private ImageView next;

    private ViewGroup embassies;
    private EmbassiesFragment embassiesInner;
    private TextView embassiesTitle;
	
	private String region;
	private RegionDataParcelable data;
	private String errorMessage;
	private boolean homeRegion = false;
    private ViewPager viewPager;
    private boolean allowPosting = false;
    private ImageLoader imageLoader;
    private DisplayImageOptions options;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.region);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        imageLoader = Utils.getImageLoader(this);
        this.options = Utils.getImageLoaderDisplayOptions();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // WFE
        wfe = (ViewGroup) getLayoutInflater().inflate(R.layout.region_wfe, viewPager, false);
        regionName = (TextView) wfe.findViewById(R.id.region_name);
        flag = (ImageView) wfe.findViewById(R.id.region_flag);
        delegate = (TextView) wfe.findViewById(R.id.region_delegate);
        founder = (TextView) wfe.findViewById(R.id.region_founder);
        text = (TextView) wfe.findViewById(R.id.wfe);
        delegate.setMovementMethod(LinkMovementMethod.getInstance());
        founder.setMovementMethod(LinkMovementMethod.getInstance());
        text.setMovementMethod(LinkMovementMethod.getInstance());
        fieldset = (ViewGroup) wfe.findViewById(R.id.fieldset);
        header = (TextView) wfe.findViewById(R.id.wfe_header);
        ViewTreeObserver observer = header.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	fieldset.setPadding(fieldset.getPaddingLeft(), header.getHeight() -
						(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
							getResources().getDisplayMetrics()), fieldset.getPaddingRight(),
							fieldset.getPaddingBottom());
		    }
		});
        layout = (ViewGroup) wfe.findViewById(R.id.region_layout);

        // RMB
        rmb = (ViewGroup) getLayoutInflater().inflate(R.layout.rmb_single, viewPager, false);
        rmbInner = (RMBFragment) getSupportFragmentManager().findFragmentById(R.id.rmb);
        rmbRegion = (TextView) rmb.findViewById(R.id.rmb_region_name);
        // Page navigation
        previous = (ImageView) rmb.findViewById(R.id.rmb_previous);
        previous.setOnClickListener(rmbClickListener);
        page = (ImageView) rmb.findViewById(R.id.rmb_page);
        page.setOnClickListener(rmbClickListener);
        next = (ImageView) rmb.findViewById(R.id.rmb_next);
        next.setOnClickListener(rmbClickListener);

        // Embassies
        embassies = (ViewGroup) getLayoutInflater().inflate(R.layout.embassies_single, viewPager, false);
        embassiesTitle = (TextView) embassies.findViewById(R.id.embassies_title);
        embassiesInner = (EmbassiesFragment) getSupportFragmentManager().findFragmentById(R.id.embassies);

        // Set up view pager
        viewPager.setAdapter(new RegionPagerAdapter());
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                // Update context menu
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // Check if going straight to RMB / Embassies
        if(getIntent().hasExtra("page")) {
            viewPager.setCurrentItem(getIntent().getIntExtra("page", 0), false);
        }

        Log.d(TAG, "DATA "+getIntent().getDataString());
        if(getIntent().getData() == null) {
            // Get user's region
            homeRegion = true;
            region = NationInfo.getInstance(this).getRegionId();
        }
        else {
            String dataStr = getIntent().getDataString();
            if(dataStr.startsWith("com.limewoodMedia.nsdroid.region.rmb://")) {
                region = dataStr.replace("com.limewoodMedia.nsdroid.region.rmb://", "");
                viewPager.setCurrentItem(1, false);
            } else if(dataStr.startsWith("com.limewoodMedia.nsdroid.region.embassies://")) {
                region = dataStr.replace("com.limewoodMedia.nsdroid.region.embassies://", "");
                viewPager.setCurrentItem(2, false);
            } else if(dataStr.startsWith("com.limewoodMedia.nsdroid.region://")) {
                region = dataStr.replace("com.limewoodMedia.nsdroid.region://", "");
            }
        }
        rmbInner.setRegionName(region);
        embassiesInner.setRegionName(region);

//        if(savedInstanceState == null) {
	    	loadRegion();
//        } else {
//        	// Restore state
//        	data = savedInstanceState.getParcelable("region_data");
//        	doSetup();
//        }
    }

    @Override
    protected void onResume() {
        shouldUpdate = homeRegion;

        // Cancel RMB notification
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(NotificationsHelper.NOTIFICATION_ID_RMB);

        // Start timer if it's not started and it should be
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getString("rmb_update_interval", "-1").compareTo("-1") != 0) {
            NotificationsHelper.setAlarmForRMB(this,
                    Integer.parseInt(prefs.getString("rmb_update_interval", "-1")));
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        shouldUpdate = false;
    }

    public void showPage(int page) {
        page = Math.max(0, Math.min(2, page));
        if(viewPager != null) {
            viewPager.setCurrentItem(page, true);
        }
    }

    private void loadRegion() {
        loadRegion(0);
    }

    private void loadRegion(int offsetPage) {
    	layout.setVisibility(View.GONE);
    	final LoadingView loadingView = (LoadingView) wfe.findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_region, this);
        // Don't update automatically if we're reading back in history
        shouldUpdate = offsetPage == 0 && homeRegion;
        rmbInner.onBeforeLoad();
        embassiesInner.onBeforeLoad();
    	errorMessage = getResources().getString(R.string.general_error);
        new ParallelTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
					RegionData.Shards[] shards = new RegionData.Shards[]{
							NAME, FACTBOOK, DELEGATE, FOUNDER, FLAG,
                            EMBASSIES,
                            NATIONS, MESSAGES // Messages last, because it has ;offset=0
					};
					if(homeRegion) {
						data = API.getInstance(Region.this).getHomeRegionInfo(Region.this, shards);
                        region = TagParser.nameToId(data.name);
                        rmbInner.setRegionName(region);
                        embassiesInner.setRegionName(region);
					} else {
						data = API.getInstance(Region.this).getRegionInfo(region, shards);
					}

	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownRegionException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_region);
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				}
				
				return false;
        	}
        	
        	protected void onPostExecute(Boolean result) {
        		LoadingHelper.stopLoading(loadingView);
        		if(result) {
        			doSetup();
        		}
        		else {
        			Toast.makeText(Region.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }
    
    private void doSetup() {
    	// Region name
		setTitle(data.name);
		regionName.setText(data.name);
		// Flag
		if(data.flagURL != null) {
			imageLoader.displayImage(data.flagURL, flag, options);
		} else {
			flag.setVisibility(View.GONE);
		}
		// WA Delegate
		StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
		String wadTitle = getResources().getString(R.string.wad);
		String wad = TagParser.idToName(data.delegate);
		if(!wad.equals("0")) {
    		wad = wadTitle+" <a href=\"com.limewoodMedia.nsdroid.nation://"+wad+"\">"+wad+"</a>";
    		delegate.setText(Html.fromHtml(wad), TextView.BufferType.SPANNABLE);
		} else {
			delegate.setText(wadTitle + " " + getResources().getString(R.string.no_wad));
		}
		((Spannable)delegate.getText()).setSpan(bold, 0, wadTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		// Founder
		String found = TagParser.idToName(data.founder);
		if(!found.equals("0")) {
    		String fTitle = getResources().getString(R.string.founder);
    		found = fTitle+" <a href=\"com.limewoodMedia.nsdroid.nation://"+found+"\">"+found+"</a>";
    		founder.setText(Html.fromHtml(found), TextView.BufferType.SPANNABLE);
    		((Spannable)founder.getText()).setSpan(bold, 0, fTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    		founder.setVisibility(View.VISIBLE);
		} else {
			founder.setVisibility(View.GONE);
		}
		text.setText(TagParser.parseTagsFromHtml(data.factbook));
        layout.setVisibility(View.VISIBLE);

        doRMBSetup();

        doEmbassiesSetup();
    }

    private void doRMBSetup() {
        // Region name
        rmbRegion.setText(data.name);

        rmbInner.setMessages(data.messages, 0, homeRegion);

        Comparator<String> comparator = new AlphabeticComparator();
        Log.d(TAG, "Nations: " + data.nations);
        Arrays.sort(data.nations, comparator);
        if(Arrays.binarySearch(data.nations, NationInfo.getInstance(this).getId(), comparator) > -1) {
            allowPosting = true;
            supportInvalidateOptionsMenu();
        }
    }

    private void doEmbassiesSetup() {
        // Region name
        embassiesTitle.setText(data.name);

        embassiesInner.setEmbassies(data.embassies);
    }
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//    	super.onSaveInstanceState(outState);
//
//    	outState.putParcelable("region_data", data);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_region, menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        int page = 0;
        if(viewPager != null) {
            page = viewPager.getCurrentItem();
        }
        switch(page) {
            case 0: // WFE
                if(homeRegion) {
                    menu.findItem(R.id.menu_move_to_region).setVisible(false);
                } else {
                    menu.findItem(R.id.menu_move_to_region).setVisible(true);
                }
                menu.findItem(R.id.menu_post).setVisible(false);
                menu.findItem(R.id.menu_add_region_to_dossier).setVisible(true);
                break;
            case 1: // RMB
                menu.findItem(R.id.menu_post).setVisible(allowPosting);
                menu.findItem(R.id.menu_add_region_to_dossier).setVisible(false);
                menu.findItem(R.id.menu_move_to_region).setVisible(false);
                break;
            case 2: // Embassies
                menu.findItem(R.id.menu_post).setVisible(false);
                menu.findItem(R.id.menu_add_region_to_dossier).setVisible(false);
                menu.findItem(R.id.menu_move_to_region).setVisible(false);
                break;
        }
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case R.id.menu_add_region_to_dossier:
	        	new AsyncTask<Void, Void, Void>() {
	        		@SuppressLint("DefaultLocale")
					@Override
	        		protected Void doInBackground(Void... params) {
	    	        	if(API.getInstance(Region.this).checkLogin(Region.this)) {
	    	        		try {
	    						if(API.getInstance(Region.this).addRegionToDossier(region)) {
	    							runOnUiThread(new Runnable() {
	    								public void run() {
	    	    							Toast.makeText(Region.this, R.string.region_added_to_dossier, Toast.LENGTH_SHORT).show();
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
                switch(viewPager.getCurrentItem()) {
                    case 0: // WFE
                        loadRegion();
                        break;
                    case 1: // RMB
                        rmbInner.loadMessages();
                        break;
                    case 2: // Embassies
                        embassiesInner.updateEmbassies();
                        break;
                }
            	break;
			case R.id.menu_move_to_region:
				new AsyncTask<Void, Void, Void>() {
					@SuppressLint("DefaultLocale")
					@Override
					protected Void doInBackground(Void... params) {
						if(API.getInstance(Region.this).checkLogin(Region.this)) {
							if(API.getInstance(Region.this).moveToRegion(Region.this, region)) {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(Region.this, getString(R.string.moved_to_region, data.name), Toast.LENGTH_SHORT).show();
									}
								});
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(Region.this, getString(R.string.moved_to_region_failed, data.name), Toast.LENGTH_SHORT).show();
									};
								});
							}
						}
						return null;
					}
				}.execute();
				break;
            case R.id.menu_post:
                rmbInner.prepareMessage();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }

    private View.OnClickListener rmbClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int offset = rmbInner.getPageOffset();
            if (v == previous) {
                rmbInner.loadMessages(offset + 1);
            } else if (v == next) {
                if (offset > 0) {
                    rmbInner.loadMessages(offset - 1);
                }
            } else if (v == page) {
                // Show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(Region.this);
                builder.setTitle(R.string.rmb_go_to_page_title);
                builder.setMessage(R.string.rmb_go_to_page_message);
                final NumberPicker picker = (NumberPicker) getLayoutInflater().inflate(R.layout.rmb_go_to_page, null);
                picker.setNumber(offset);
                builder.setView(picker);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int offset = picker.getNumber();
                        dialog.dismiss();
                        rmbInner.loadMessages(offset);
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
        }
    };

    private class RegionPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Return views here
            switch(position) {
                case 0: // WFE
                    if(wfe.getParent() == null) {
                        container.addView(wfe);
                    }
                    return wfe;
                case 1: // RMB
                    if(rmb.getParent() == null) {
                        container.addView(rmb);
                    }
                    return rmb;
                case 2: // Embassies
                    if(embassies.getParent() == null) {
                        container.addView(embassies);
                    }
                    return embassies;
                default:
                    return null;
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View)object);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: // WFE
                    return getString(R.string.wfe_title);
                case 1: // RMB
                    return getString(R.string.rmb_title);
                case 2: // Embassies
                    return getString(R.string.embassies_title);
                default:
                    return null;
            }
        }
    }
}

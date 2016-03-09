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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.WorldData;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.ParallelTask;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.holders.WorldDataParcelable;
import com.limewoodmedia.nsdroid.views.LoadingView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static com.limewoodMedia.nsapi.holders.RegionData.Shards.DELEGATE;
import static com.limewoodMedia.nsapi.holders.RegionData.Shards.FACTBOOK;
import static com.limewoodMedia.nsapi.holders.RegionData.Shards.FLAG;
import static com.limewoodMedia.nsapi.holders.RegionData.Shards.FOUNDER;
import static com.limewoodMedia.nsapi.holders.RegionData.Shards.NAME;
import static com.limewoodMedia.nsapi.holders.RegionData.Shards.NUM_NATIONS;

public class World extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = World.class.getName();
    public static boolean shouldUpdate = false;

    private ViewGroup featured;
	private ViewGroup layout;
	private TextView regionName;
	private ImageView flag;
	private TextView delegate;
	private TextView founder;
	private TextView text;
	private ViewGroup fieldset;
	private TextView header;

    private ViewGroup search;
    private TextView numbers;
    private EditText searchField;
    private Button searchButton;
    private LinearLayout searchResults;

    private WorldDataParcelable wData;
	private RegionDataParcelable rData;
    private NationDataParcelable nData;
	private String errorMessage;
    private ViewPager viewPager;
    private ImageLoader imageLoader;
    private DisplayImageOptions options;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.world);
        setTitle(R.string.world);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        imageLoader = Utils.getImageLoader(this);
        this.options = Utils.getImageLoaderDisplayOptions();
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // WFE
        featured = (ViewGroup) getLayoutInflater().inflate(R.layout.world_featured, viewPager, false);
        regionName = (TextView) featured.findViewById(R.id.region_name);
        regionName.setMovementMethod(LinkMovementMethod.getInstance());
        flag = (ImageView) featured.findViewById(R.id.region_flag);
        delegate = (TextView) featured.findViewById(R.id.region_delegate);
        founder = (TextView) featured.findViewById(R.id.region_founder);
        text = (TextView) featured.findViewById(R.id.wfe);
        delegate.setMovementMethod(LinkMovementMethod.getInstance());
        founder.setMovementMethod(LinkMovementMethod.getInstance());
        text.setMovementMethod(LinkMovementMethod.getInstance());
        fieldset = (ViewGroup) featured.findViewById(R.id.fieldset);
        header = (TextView) featured.findViewById(R.id.wfe_header);
        ViewTreeObserver observer = header.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fieldset.setPadding(fieldset.getPaddingLeft(), header.getHeight() -
                                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                                        getResources().getDisplayMetrics()), fieldset.getPaddingRight(),
                        fieldset.getPaddingBottom());
            }
        });
        layout = (ViewGroup) featured.findViewById(R.id.region_layout);

        // Search
        search = (ViewGroup) getLayoutInflater().inflate(R.layout.world_search, viewPager, false);
        numbers = (TextView) search.findViewById(R.id.world_numbers);
        searchField = (EditText) search.findViewById(R.id.search_field);
        searchButton = (Button) search.findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch(searchField.getText().toString());
            }
        });
        searchResults = (LinearLayout) search.findViewById(R.id.search_results);

        // Set up view pager
        viewPager.setAdapter(new WorldPagerAdapter());
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // Update context menu
                supportInvalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // Check if going straight to a page
        if(getIntent().hasExtra("page")) {
            showPage(getIntent().getIntExtra("page", 0));
        }

//        if(savedInstanceState == null) {
	    	loadRegion();
//        } else {
//        	// Restore state
//        	rData = savedInstanceState.getParcelable("region_data");
//        	doSetup();
//        }
    }

    public void showPage(int page) {
        page = Math.max(0, Math.min(1, page));
        if(viewPager != null) {
            viewPager.setCurrentItem(page, true);
        }
    }

    private void loadRegion() {
    	final LoadingView loadingView = (LoadingView) featured.findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_region, this);
        // Don't update automatically if we're reading back in history
    	errorMessage = getResources().getString(R.string.general_error);
        new ParallelTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
                    wData = API.getInstance(World.this).getWorldInfo(
                            WorldData.Shards.FEATURED_REGION, WorldData.Shards.NUM_NATIONS, WorldData.Shards.NUM_REGIONS
                    );
                    String region = wData.featuredRegion;

                    rData = API.getInstance(World.this).getRegionInfo(region, NAME, FACTBOOK, DELEGATE, FOUNDER, FLAG);

	                return true;
				} catch (RateLimitReachedException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.rate_limit_reached);
				} catch (UnknownRegionException e) {
					e.printStackTrace();
					errorMessage = getResources().getString(R.string.unknown_region, e.getRegion());
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
        		LoadingHelper.stopLoading(loadingView);
        		if(result) {
        			doSetup();
        		}
        		else {
        			Toast.makeText(World.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }
    
    private void doSetup() {
    	// Region name
		regionName.setText(rData.name);
		// Flag
		if(rData.flagURL != null) {
			imageLoader.displayImage(rData.flagURL, flag, options);
		} else {
			flag.setVisibility(View.GONE);
		}
		// WA Delegate
		StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
		String wadTitle = getResources().getString(R.string.wad);
		String wad = TagParser.idToName(rData.delegate);
		if(!wad.equals("0")) {
    		wad = wadTitle+": <a href=\"com.limewoodMedia.nsdroid.nation://"+wad+"\">"+wad+"</a>";
    		delegate.setText(Html.fromHtml(wad), TextView.BufferType.SPANNABLE);
		} else {
			delegate.setText(wadTitle + " " + getResources().getString(R.string.no_wad));
		}
		((Spannable)delegate.getText()).setSpan(bold, 0, wadTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		// Founder
		String found = TagParser.idToName(rData.founder);
		if(!found.equals("0")) {
    		String fTitle = getResources().getString(R.string.founder);
    		found = fTitle+": <a href=\"com.limewoodMedia.nsdroid.nation://"+found+"\">"+found+"</a>";
    		founder.setText(Html.fromHtml(found), TextView.BufferType.SPANNABLE);
    		((Spannable)founder.getText()).setSpan(bold, 0, fTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    		founder.setVisibility(View.VISIBLE);
		} else {
			founder.setVisibility(View.GONE);
		}
		text.setText(TagParser.parseTagsFromHtml(rData.factbook));
        layout.setVisibility(View.VISIBLE);

        doSearchSetup();
    }

    private void doSearchSetup() {
        // Numbers
        numbers.setText(getString(R.string.world_numbers, wData.numNations, wData.numRegions));

        // Search
        searchField.setText("");
        searchResults.removeAllViews();
    }

    private void doSearch(final String string) {
        searchResults.removeAllViews();
        final LoadingView loadingView = (LoadingView) search.findViewById(R.id.loading);
        LoadingHelper.startLoading(loadingView, R.string.searching, this);
        // Don't update automatically if we're reading back in history
        errorMessage = getResources().getString(R.string.general_error);
        new ParallelTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(Void...params) {
                try {
                    try {
                        nData = API.getInstance(World.this).getNationInfo(string,
                                NationData.Shards.NAME, NationData.Shards.CATEGORY, NationData.Shards.REGION);
                    } catch (UnknownNationException e) {
                        nData = null;
                    }

                    try {
                        rData = API.getInstance(World.this).getRegionInfo(string, NAME, NUM_NATIONS, DELEGATE);
                    } catch(UnknownRegionException e) {
                        rData = null;
                    }

                    return true;
                } catch (RateLimitReachedException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.rate_limit_reached);
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
                LoadingHelper.stopLoading(loadingView);
                if(result) {
                    // Show search results
                    RelativeLayout nation = (RelativeLayout) getLayoutInflater().inflate(R.layout.search_nation, searchResults, false);
                    if(nData != null) {
                        ((TextView) nation.findViewById(R.id.nation_name)).setText(nData.name);
                        ((TextView) nation.findViewById(R.id.nation_category)).setText(nData.category);
                        ((TextView) nation.findViewById(R.id.nation_region)).setText(nData.region);
                        nation.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(World.this, Nation.class);
                                intent.setData(Uri.parse("com.limewoodMedia.nsdroid.nation://"+TagParser.nameToId(nData.name)));
                                startActivity(intent);
                            }
                        });
                    } else {
                        ((TextView) nation.findViewById(R.id.nation_name)).setText(R.string.no_nation_found);
                        ((TextView) nation.findViewById(R.id.nation_category)).setVisibility(View.GONE);
                        ((TextView) nation.findViewById(R.id.nation_region)).setVisibility(View.GONE);
                    }
                    searchResults.addView(nation);
                    RelativeLayout region = (RelativeLayout) getLayoutInflater().inflate(R.layout.search_region, searchResults, false);
                    if(rData != null) {
                        ((TextView) region.findViewById(R.id.region_name)).setText(rData.name);
                        ((TextView) region.findViewById(R.id.region_nations)).setText(Integer.toString(rData.numNations));
                        ((TextView) region.findViewById(R.id.region_wad)).setText(rData.delegate);
                        region.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(World.this, Region.class);
                                intent.setData(Uri.parse("com.limewoodMedia.nsdroid.region://" + TagParser.nameToId(rData.name)));
                                startActivity(intent);
                            }
                        });
                    } else {
                        ((TextView) region.findViewById(R.id.region_name)).setText(R.string.no_region_found);
                        ((TextView) region.findViewById(R.id.region_nations_label)).setVisibility(View.GONE);
                        ((TextView) region.findViewById(R.id.region_nations)).setVisibility(View.GONE);
                        ((TextView) region.findViewById(R.id.region_wad_label)).setVisibility(View.GONE);
                        ((TextView) region.findViewById(R.id.region_wad)).setVisibility(View.GONE);
                    }
                    searchResults.addView(region);
                }
                else {
                    Toast.makeText(World.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
                View view = World.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }.execute();
    }
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//    	super.onSaveInstanceState(outState);
//
//    	outState.putParcelable("region_data", rData);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }

    private class WorldPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Return views here
            switch(position) {
                case 0: // Featured region
                    if(featured.getParent() == null) {
                        container.addView(featured);
                    }
                    return featured;
                case 1: // Search
                    if(search.getParent() == null) {
                        container.addView(search);
                    }
                    return search;
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
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: // World
                    return getString(R.string.world);
                case 1: // Search
                    return getString(R.string.search);
                default:
                    return null;
            }
        }
    }
}

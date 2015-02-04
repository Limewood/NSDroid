/*
 * Copyright (c) 2015. Joakim Lindskog & Limewood Media
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.limewoodmedia.nsdroid.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.fragments.WACouncilFragment;

/**
 * World Assembly
 * Created by Joakim Lindskog on 2015-02-01.
 */
public class WorldAssembly extends SherlockFragmentActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    @SuppressWarnings("unused")
    private static final String TAG = Welcome.class.getName();

    private ViewGroup overview;
    private WACouncilFragment generalAssembly;
    private WACouncilFragment securityCouncil;
    private ViewPager viewPager;

    private WAData gaData;
    private WAData scData;
    private String errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.wa);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);

        viewPager = (ViewPager) findViewById(R.id.pager);

        overview = (ViewGroup) getLayoutInflater().inflate(R.layout.wa_overview, viewPager, false);
        generalAssembly = (WACouncilFragment) getSupportFragmentManager().findFragmentById(R.id.general_assembly);
        securityCouncil = (WACouncilFragment) getSupportFragmentManager().findFragmentById(R.id.security_council);

        // Set up view pager
        viewPager.setAdapter(new WAPagerAdapter());

        loadData();
    }

    /**
     * Loads data from the NS API into the overview panels
     */
    private void loadData() {
        errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
            protected void onPreExecute() {
                generalAssembly.onBeforeLoading();
                securityCouncil.onBeforeLoading();
            }

            protected Boolean doInBackground(Void...params) {
                try {
                    gaData = API.getInstance(WorldAssembly.this).getWAInfo(WACouncil.GENERAL_ASSEMBLY,
                            WAData.Shards.RESOLUTION, WAData.Shards.HAPPENINGS, WAData.Shards.VOTETRACK, WAData.Shards.LAST_RESOLUTION);

                    scData = API.getInstance(WorldAssembly.this).getWAInfo(WACouncil.SECURITY_COUNCIL,
                            WAData.Shards.RESOLUTION, WAData.Shards.VOTETRACK, WAData.Shards.LAST_RESOLUTION);

                    // Happenings - fetch nation and region names
//                    Pattern nPattern = Pattern.compile("(.*?)@@([a-z\\d_]+)@@(.*?)");
//                    Pattern rPattern = Pattern.compile("(.*?)%%([a-z\\d_]+)%%(.*?)");
//                    Matcher matcher;
//                    String n, r;
//                    List<Happening> list = new ArrayList<Happening>(gaData.happenings);
//                    list.addAll(gaData.happenings);
//                    for(Happening event : list) {
//                        event.text = event.text.replaceAll("@@("+info.getName().toLowerCase(Locale.getDefault()).replace(' ', '_')+")@@",
//                                "<a href=\"com.limewoodMedia.nsdroid.nation://$1\">"+info.getName()+"</a>");
//                        event.text = event.text.replaceAll("%%("+rData.name.toLowerCase(Locale.getDefault()).replace(' ', '_')+")%%",
//                                "<a href=\"com.limewoodMedia.nsdroid.region://$1\">"+rData.name+"</a>");
//                        event.text = event.text.replaceAll("%%([a-z\\d_]+)%rmb%%",
//                                "<a href=\"com.limewoodMedia.nsdroid.region.rmb://$1\">Regional Message Board</a>");
//
//                        matcher = nPattern.matcher(event.text);
//                        while(matcher.matches()) {
//                            n = event.text.substring(matcher.start(2), matcher.end(2));
//                            n = TagParser.idToName(n);
//
//                            event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.nation://$2\">" +
//                                    n+"</a>$3");
//                            matcher = nPattern.matcher(event.text);
//                        }
//                        matcher = rPattern.matcher(event.text);
//                        while(matcher.matches()) {
//                            r = event.text.substring(matcher.start(2), matcher.end(2));
//                            r = TagParser.idToName(r);
//
//                            event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.region://$2\">" +
//                                    r+"</a>$3");
//                            matcher = rPattern.matcher(event.text);
//                        }
//                    }

                    return true;
                } catch (RateLimitReachedException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.rate_limit_reached);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                }

                return false;
            }

            protected void onPostExecute(Boolean result) {
                generalAssembly.onAfterLoading();
                securityCouncil.onAfterLoading();
                if(result) {
                    doSetup();
                }
                else {
                    Toast.makeText(WorldAssembly.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private void doSetup() {
        // General Assembly
        generalAssembly.setCouncil(WACouncil.GENERAL_ASSEMBLY, gaData);

        // Security Council
        securityCouncil.setCouncil(WACouncil.SECURITY_COUNCIL, scData);
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }

    private class WAPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Return views here
            switch(position) {
                case 0: // Overview
                    if(overview.getParent() == null) {
                        container.addView(overview);
                    }
                    return overview;
//                case 1: // GA
//                    if(ga.getParent() == null) {
//                        container.addView(ga);
//                    }
//                    return ga;
//                case 2: // SC
//                    if(sc.getParent() == null) {
//                        container.addView(sc);
//                    }
//                    return sc;
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
            return 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: // WFE
                    return getString(R.string.world_assembly);
                case 1: // RMB
                    return getString(R.string.general_assembly);
                case 2: // Embassies
                    return getString(R.string.security_council);
                default:
                    return null;
            }
        }
    }
}

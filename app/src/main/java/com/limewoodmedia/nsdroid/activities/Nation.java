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
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.limewoodMedia.nsapi.DescriptionMapEntry;
import com.limewoodMedia.nsapi.enums.CauseOfDeath;
import com.limewoodMedia.nsapi.enums.Department;
import com.limewoodMedia.nsapi.enums.IDescriptable;
import com.limewoodMedia.nsapi.enums.IndustrySector;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.WorldData;
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
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.holders.WorldDataParcelable;
import com.limewoodmedia.nsdroid.views.BannerView;
import com.limewoodmedia.nsdroid.views.CensusView;
import com.limewoodmedia.nsdroid.views.FreedomView;
import com.limewoodmedia.nsdroid.views.LoadingView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.SeriesSelection;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Main activity for nation information
 * @author Joakim Lindskog
 */
public class Nation extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Nation.class.getName();

    private RelativeLayout overview;
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
	private FreedomView economicStrength;
	private FreedomView politicalFreedoms;
	private TextView description;
	private TextView endorsements;
    private TextView censusTitle;
    private CensusView worldRank;
    private CensusView regionRank;

    // TODO Set standard colours for government and economy
    /** Colors to be used for the pie slices. */
    private static int[] CHART_COLOURS = new int[] {
            android.graphics.Color.parseColor("#4572A7"),
            android.graphics.Color.parseColor("#AA4643"),
            android.graphics.Color.parseColor("#89A54E"),
            android.graphics.Color.parseColor("#80699B"),
            android.graphics.Color.parseColor("#3D96AE"),
            android.graphics.Color.parseColor("#DB843D"),
            android.graphics.Color.parseColor("#92A8CD"),
            android.graphics.Color.parseColor("#A47D7C"),
            android.graphics.Color.parseColor("#B5CA92"),
            android.graphics.Color.parseColor("#8bbc21"),
            android.graphics.Color.parseColor("#1aadce"),
            android.graphics.Color.parseColor("#910000")
    };

    // People
    private RelativeLayout people;
    private GraphicalView peopleChart;
    private CategorySeries peopleSeries = new CategorySeries("");
    private DefaultRenderer peopleRenderer = new DefaultRenderer();
    private TextView peopleLegend;

    // Government
    private RelativeLayout government;
    private GraphicalView governmentChart;
    private CategorySeries governmentSeries = new CategorySeries("");
    private DefaultRenderer governmentRenderer = new DefaultRenderer();
    private TextView governmentTitle;
    private TextView governmentSize;
    private TextView governmentPercent;
    private TextView governmentLegend;

    // Economy
    private RelativeLayout economy;
    private GraphicalView economyChart;
    private CategorySeries economySeries = new CategorySeries("");
    private DefaultRenderer economyRenderer = new DefaultRenderer();
    private TextView economyTitle;
    private TextView economyGDP;
    private TextView economyGDPPC;
    private TextView economyPoorest;
    private TextView economyRichest;
    private TextView economyLegend;
	
	private String nation;
	private NationDataParcelable data;
	private String errorMessage;
	private ImageLoader imageLoader;
	private DisplayImageOptions options;
	private boolean endoByYou = false;
	private boolean myNation = false;
    private ViewPager viewPager;
    private RegionDataParcelable rData;
    private WorldDataParcelable wData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nation);
		imageLoader = Utils.getImageLoader(this);
		this.options = Utils.getImageLoaderDisplayOptions();

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);

        viewPager = (ViewPager) findViewById(R.id.pager);

        // Overview
        overview = (RelativeLayout) getLayoutInflater().inflate(R.layout.nation_overview, viewPager, false);
        layout = (ViewGroup) overview.findViewById(R.id.nation_layout);
		banner = (BannerView) overview.findViewById(R.id.nation_banner);
        flag = (BannerView) overview.findViewById(R.id.nation_flag);
		pretitle = (TextView) overview.findViewById(R.id.nation_pretitle);
        name = (TextView) overview.findViewById(R.id.nation_name);
        motto = (TextView) overview.findViewById(R.id.nation_motto);
        region = (TextView) overview.findViewById(R.id.nation_region);
        region.setMovementMethod(LinkMovementMethod.getInstance());
        waStatus = (TextView) overview.findViewById(R.id.nation_wa_status);
        influence = (TextView) overview.findViewById(R.id.nation_influence);
		endorsed = (TextView) overview.findViewById(R.id.nation_endorsed);
        category = (TextView) overview.findViewById(R.id.nation_category);
        civilRights = (FreedomView) overview.findViewById(R.id.nation_civil_rights);
        economicStrength = (FreedomView) overview.findViewById(R.id.nation_economy);
        politicalFreedoms = (FreedomView) overview.findViewById(R.id.nation_political_freedoms);
        description = (TextView) overview.findViewById(R.id.nation_description);
		endorsements = (TextView) overview.findViewById(R.id.nation_endorsements);
        censusTitle = (TextView) overview.findViewById(R.id.census_title);
        worldRank = (CensusView) overview.findViewById(R.id.world_rank);
        regionRank = (CensusView) overview.findViewById(R.id.region_rank);

        setUpChartRenderer(peopleRenderer);
        setUpChartRenderer(governmentRenderer);
        setUpChartRenderer(economyRenderer);

        // People
        people = (RelativeLayout) getLayoutInflater().inflate(R.layout.nation_people, viewPager, false);
        peopleLegend = (TextView) people.findViewById(R.id.people_legend);

        // Government
        government = (RelativeLayout) getLayoutInflater().inflate(R.layout.nation_government, viewPager, false);
        governmentTitle = (TextView) government.findViewById(R.id.government_title);
        governmentSize = (TextView) government.findViewById(R.id.government_size);
        governmentPercent = (TextView) government.findViewById(R.id.government_percent);
        governmentLegend = (TextView) government.findViewById(R.id.government_legend);

        // Economy
        economy = (RelativeLayout) getLayoutInflater().inflate(R.layout.nation_economy, viewPager, false);
        economyTitle = (TextView) economy.findViewById(R.id.economy_title);
        economyGDP = (TextView) economy.findViewById(R.id.economy_gdp);
        economyGDPPC = (TextView) economy.findViewById(R.id.economy_gdppc);
        economyPoorest = (TextView) economy.findViewById(R.id.economy_poorest);
        economyRichest = (TextView) economy.findViewById(R.id.economy_richest);
        economyLegend = (TextView) economy.findViewById(R.id.economy_legend);

        // Set up view pager
        viewPager.setAdapter(new NationPagerAdapter());
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

//        if(savedInstanceState == null) {
        	loadNation();
//        } else {
//        	// Restore
//        	data = savedInstanceState.getParcelable("nation_data");
//        	doSetup();
//        }
    }

    private void setUpChartRenderer(DefaultRenderer chartRenderer) {
        Resources r = getResources();
        float labelTextSize = r.getDimension(R.dimen.pie_chart_label_size);

        chartRenderer.setZoomButtonsVisible(false);
        chartRenderer.setStartAngle(-90);
        chartRenderer.setDisplayValues(true);
        chartRenderer.setClickEnabled(true);
        chartRenderer.setInScroll(true);
        chartRenderer.setAntialiasing(true);
        chartRenderer.setLabelsTextSize(labelTextSize);
//        chartRenderer.setLegendTextSize(labelTextSize);
        chartRenderer.setShowLegend(false);
        chartRenderer.setTextTypeface(Typeface.DEFAULT);
        chartRenderer.setZoomRate(6);
        chartRenderer.setPanEnabled(false);
        chartRenderer.setLabelsColor(android.graphics.Color.BLACK);
        chartRenderer.setMargins(new int[]{
                Math.round(r.getDimension(R.dimen.pie_chart_margin_top)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_left)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_bottom)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_right))
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (peopleChart == null) {
            // People
            LinearLayout layout = (LinearLayout) people.findViewById(R.id.people_chart);
            peopleChart = ChartFactory.getPieChartView(this, peopleSeries, peopleRenderer);
            peopleChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SeriesSelection seriesSelection = peopleChart.getCurrentSeriesAndPoint();
                    if (seriesSelection != null) {
                        for (int i = 0; i < peopleSeries.getItemCount(); i++) {
                            peopleRenderer.getSeriesRendererAt(i).setHighlighted(i == seriesSelection.getPointIndex());
                        }
                        peopleChart.repaint();
                    }
                }
            });
            Resources r = getResources();
            layout.addView(peopleChart, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.round(r.getDimension(R.dimen.pie_chart_height))));
            // Government
            layout = (LinearLayout) government.findViewById(R.id.government_chart);
            governmentChart = ChartFactory.getPieChartView(this, governmentSeries, governmentRenderer);
            governmentChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SeriesSelection seriesSelection = governmentChart.getCurrentSeriesAndPoint();
                    if (seriesSelection != null) {
                        for (int i = 0; i < governmentSeries.getItemCount(); i++) {
                            governmentRenderer.getSeriesRendererAt(i).setHighlighted(i == seriesSelection.getPointIndex());
                        }
                        governmentChart.repaint();
                    }
                }
            });
            layout.addView(governmentChart, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.round(r.getDimension(R.dimen.pie_chart_height))));
            // Economy
            layout = (LinearLayout) economy.findViewById(R.id.economy_chart);
            economyChart = ChartFactory.getPieChartView(this, economySeries, economyRenderer);
            economyChart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SeriesSelection seriesSelection = economyChart.getCurrentSeriesAndPoint();
                    if (seriesSelection != null) {
                        for (int i = 0; i < economySeries.getItemCount(); i++) {
                            economyRenderer.getSeriesRendererAt(i).setHighlighted(i == seriesSelection.getPointIndex());
                        }
                        economyChart.repaint();
                    }
                }
            });
            layout.addView(economyChart, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.round(r.getDimension(R.dimen.pie_chart_height))));
        } else {
            peopleChart.repaint();
            governmentChart.repaint();
            economyChart.repaint();
        }
    }

    private void loadNation() {
    	layout.setVisibility(View.GONE);
    	final LoadingView loadingView = (LoadingView) overview.findViewById(R.id.loading);
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
							BANNERS, FREEDOM_SCORES, ENDORSEMENTS,
                            WORLD_CENSUS, REGIONAL_CENSUS,
                            DEATHS, // People
                            GOVERNMENT_BUDGET, DEMONYM, // Government
                            SECTORS, GDP, INCOME, POOREST, RICHEST // Economy
                    );

                    rData = API.getInstance(Nation.this).getRegionInfo(data.region, RegionData.Shards.NUM_NATIONS);

                    wData = API.getInstance(Nation.this).getWorldInfo(WorldData.Shards.NUM_NATIONS, WorldData.Shards.CENSUS);
	                
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
                } catch (UnknownRegionException e) {
                    e.printStackTrace();
                    errorMessage = getString(R.string.unknown_region, e.getRegion());
                }

                return false;
        	}
        	
        	protected void onPostExecute(Boolean result) {
        		LoadingHelper.stopLoading(loadingView);
        		
        		if(result) {
                	layout.setVisibility(View.VISIBLE);
        			doSetup();
                    doPeopleSetup();
                    doGovernmentSetup();
                    doEconomySetup();
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
				TagParser.nameToId(data.region).equals(NationInfo.getInstance(this).getRegionId()) &&
				NationInfo.getInstance(this).getWAStatus() != WAStatus.NON_MEMBER) {
			endorsed.setText(endoByYou ? R.string.nation_endorsed : R.string.nation_not_endorsed);
			endorsed.setVisibility(View.VISIBLE);
		} else {
			endorsed.setVisibility(View.GONE);
		}
        category.setText(data.category);
        civilRights.setText(data.freedoms.civilRights);
        economicStrength.setText(data.freedoms.economy);
        politicalFreedoms.setText(data.freedoms.politicalFreedoms);

		// Set gradients
		Color[] colors = getGradientFor(data.freedoms.civilRightsValue);
		Log.d(TAG, "Civil rights: " + colors[0] + "; " + colors[1]);
		civilRights.setGradient(colors[0], colors[1]);

		colors = getGradientFor(data.freedoms.economyValue);
		economicStrength.setGradient(colors[0], colors[1]);

		colors = getGradientFor(data.freedoms.politicalFreedomsValue);
		politicalFreedoms.setGradient(colors[0], colors[1]);

        // Census ranks
        censusTitle.setText(wData.census);
        worldRank.setText(Utils.getOrdinal(data.worldCensus));
        worldRank.setTotal(wData.numNations);
        colors = getGradientForCensus(data.worldCensus, wData.numNations);
        worldRank.setGradient(colors[0], colors[1]);
        regionRank.setText(Utils.getOrdinal(data.regionalCensus));
        regionRank.setTotal(rData.numNations);
        colors = getGradientForCensus(data.regionalCensus, rData.numNations);
        regionRank.setGradient(colors[0], colors[1]);

        description.setText(Html.fromHtml(data.getDescription().replace("\n", "<br/>")));

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

    private void doPeopleSetup() {
        // Chart
        peopleSeries.clear();
        peopleRenderer.removeAllRenderers();
        Set<Map.Entry<CauseOfDeath, Float>> deaths = data.deaths.entrySet();
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);
        Map<CauseOfDeath, String> legends = new HashMap<CauseOfDeath, String>();
        StringBuilder legend;
        String desc;
        int colour;
        for(Map.Entry<CauseOfDeath, Float> d : deaths) {
            desc = d.getKey() == CauseOfDeath.ANIMAL_ATTACK ?
                    d.getKey().getDescription().replace("Animal", data.animal.substring(0, 1).toUpperCase()+data.animal.substring(1)) :
                    d.getKey().getDescription();
            peopleSeries.add(desc, d.getValue()/100f);
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            colour = CHART_COLOURS[(peopleSeries.getItemCount() - 1) % CHART_COLOURS.length];
            renderer.setColor(colour);
            renderer.setChartValuesFormat(format);
            peopleRenderer.addSeriesRenderer(renderer);
            legend = new StringBuilder();
            legend.append("<b><font color='").append(Integer.toString(colour)).append("'>").append(desc);
            legends.put(d.getKey(), legend.toString());
        }
        peopleChart.repaint();

        // Legend
        legend = new StringBuilder();
        for(CauseOfDeath cod : CauseOfDeath.values()) {
            if(legend.length() > 0) {
                legend.append("<br/>");
            }
            if(legends.containsKey(cod)) {
                legend.append(legends.get(cod)).append(": ").append(Float.toString(data.deaths.get(cod))).append("%</font></b>");
            } else {
                legend.append("<font color='grey'>").append(cod.getDescription()).append(": ").append("0%</font>");
            }
        }
        peopleLegend.setText(Html.fromHtml(legend.toString()), TextView.BufferType.SPANNABLE);
    }

    private void doGovernmentSetup() {
        governmentTitle.setText(getString(R.string.nation_government_title, Utils.capitalize(data.demonym)));
        // Government size and percent
        double gov = 0;
        if(data.sectors.containsKey(IndustrySector.GOVERNMENT)) {
            gov = data.sectors.get(IndustrySector.GOVERNMENT);
        }
        governmentSize.setText(getString(R.string.nation_government_size, Utils.formatCurrencyAmount(this, Math.round(data.gdp*(gov/100d))), data.currency));
        governmentPercent.setText(getString(R.string.nation_government_percent, String.format("%.1f", gov)));
        governmentSeries.clear();
        governmentRenderer.removeAllRenderers();
        Set<Map.Entry<Department, Float>> depts = data.governmentBudget.entrySet();
        TreeSet<Map.Entry<IDescriptable, Float>> departments = new TreeSet<>();
        for(Map.Entry<Department, Float> d : depts) {
            departments.add(new DescriptionMapEntry(d));
        }
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);
        Map<IDescriptable, String> legends = new HashMap<>();
        StringBuilder legend;
        String desc;
        int colour;
        for(Map.Entry<IDescriptable, Float> d : departments) {
            if(d.getValue() == 0) continue;
            desc = d.getKey().getDescription();
            governmentSeries.add(desc, d.getValue()/100f);
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            colour = CHART_COLOURS[(governmentSeries.getItemCount() - 1) % CHART_COLOURS.length];
            renderer.setColor(colour);
            renderer.setChartValuesFormat(format);
            governmentRenderer.addSeriesRenderer(renderer);
            legend = new StringBuilder();
            legend.append("<b><font color='").append(Integer.toString(colour)).append("'>").append(desc);
            legends.put(d.getKey(), legend.toString());
        }
        governmentChart.repaint();

        // Legend
        legend = new StringBuilder();
        for(Department dep : Department.values()) {
            if(legend.length() > 0) {
                legend.append("<br/>");
            }
            if(legends.containsKey(dep)) {
                legend.append(legends.get(dep)).append(": ").append(Float.toString(data.governmentBudget.get(dep))).append("%</font></b>");
            } else {
                legend.append("<font color='grey'>").append(dep.getDescription()).append(": ").append("0%</font>");
            }
        }
        governmentLegend.setText(Html.fromHtml(legend.toString()), TextView.BufferType.SPANNABLE);
    }

    private void doEconomySetup() {
        economyTitle.setText(getString(R.string.nation_economy_title, Utils.capitalize(data.demonym)));
        // GDP, GDPPC, Poorest and Richest
        economyGDP.setText(getString(R.string.nation_economy_gdp, Utils.formatCurrencyAmount(this, data.gdp), data.currency));
        economyGDPPC.setText(getString(R.string.nation_economy_gdppc, Utils.formatCurrencyAmount(this, Math.round(data.gdp/(data.population*1000000f))), data.currency));
        economyPoorest.setText(getString(R.string.nation_economy_poorest, Utils.formatCurrencyAmount(this, data.poorest), data.currency));
        economyRichest.setText(getString(R.string.nation_economy_richest, Utils.formatCurrencyAmount(this, data.richest), data.currency));

        economySeries.clear();
        economyRenderer.removeAllRenderers();
        Set<Map.Entry<IndustrySector, Float>> secs = data.sectors.entrySet();
        TreeSet<Map.Entry<IDescriptable, Float>> sectors = new TreeSet<>();
        for(Map.Entry<IndustrySector, Float> d : secs) {
            sectors.add(new DescriptionMapEntry(d, false));
        }
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);

        Map<IDescriptable, String> legends = new HashMap<>();
        StringBuilder legend;
        String desc;
        int colour;
        for(Map.Entry<IDescriptable, Float> s : sectors) {
            if(s.getValue() == 0) continue;
            desc = s.getKey().getDescription();
            economySeries.add(desc, s.getValue()/100f);
            SimpleSeriesRenderer renderer = new SimpleSeriesRenderer();
            colour = CHART_COLOURS[(economySeries.getItemCount() - 1) % CHART_COLOURS.length];
            renderer.setColor(colour);
            renderer.setChartValuesFormat(format);
            economyRenderer.addSeriesRenderer(renderer);
            legend = new StringBuilder();
            legend.append("<b><font color='").append(Integer.toString(colour)).append("'>").append(desc);
            legends.put(s.getKey(), legend.toString());
        }
        economyChart.repaint();

        // Legend
        legend = new StringBuilder();
        for(IndustrySector sector : IndustrySector.values()) {
            if(legend.length() > 0) {
                legend.append("<br/>");
            }
            if(legends.containsKey(sector)) {
                legend.append(legends.get(sector)).append(": ").append(Float.toString(data.sectors.get(sector))).append("%</font></b>");
            } else {
                legend.append("<font color='grey'>").append(sector.getDescription()).append(": ").append("0%</font>");
            }
        }
        economyLegend.setText(Html.fromHtml(legend.toString()), TextView.BufferType.SPANNABLE);
    }
    
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//    	super.onSaveInstanceState(outState);
//
//    	outState.putParcelable("nation_data", data);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_nation, menu);
        return true;
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Endorse is the first item
		if(myNation || (data != null && (data.worldAssemblyStatus == WAStatus.NON_MEMBER ||
				!TagParser.nameToId(data.region).equals(NationInfo.getInstance(this).getRegionId()))) ||
				NationInfo.getInstance(this).getWAStatus() == WAStatus.NON_MEMBER) {
			menu.findItem(R.id.menu_endorse_nation).setVisible(false);
		} else {
			menu.findItem(R.id.menu_endorse_nation).setTitle(endoByYou ? R.string.menu_unendorse_nation : R.string.menu_endorse_nation).setVisible(true);
            menu.findItem(R.id.menu_endorse_nation).setVisible(true);
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

    private Color[] getGradientForCensus(int value, int total) {
        return new Color[]{
                new ColorInterpolator().interpolateColor(
                        new ColorInterpolator.Color(1, 0, 0, 1), new ColorInterpolator.Color(.26f, .98f, 0, 1), 1-value/(float)total),
                new ColorInterpolator().interpolateColor(
                        new ColorInterpolator.Color(.5f, 0, 0, 1), new ColorInterpolator.Color(.13f, .49f, 0, 1), 1-value/(float)total)
        };
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }

    private class NationPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Return views here
            switch(position) {
                case 0: // Overview
                    if(overview.getParent() == null) {
                        container.addView(overview);
                    }
                    return overview;
                case 1: // People
                    if(people.getParent() == null) {
                        container.addView(people);
                    }
                    return people;
                case 2: // Government
                    if(government.getParent() == null) {
                        container.addView(government);
                    }
                    return government;
                case 3: // Economy
                    if(economy.getParent() == null) {
                        container.addView(economy);
                    }
                    return economy;
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
            return 4;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: // Overview
                    return getString(R.string.nation_overview_header);
                case 1: // People
                    return getString(R.string.nation_people_header);
                case 2: // Government
                    return getString(R.string.nation_government_header);
                case 3: // Economy
                    return getString(R.string.nation_economy_header);
                default:
                    return null;
            }
        }
    }
}

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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.enums.WAVote;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.holders.Happening;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WAHappening;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.fragments.WACouncilFragment;
import com.limewoodmedia.nsdroid.views.LoadingView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private TextView happenings;
    private ViewPager viewPager;

    private NationData nationData;
    private WAData gaData;
    private WAData scData;
    private String errorMessage;
    private final NumberFormat format = NumberFormat.getPercentInstance();

    private ViewGroup gaPage;
    private TextView gaTitle;
    private TextView gaCategory;
    private TextView gaProposer;
    private TextView gaNation;
    private TextView gaText;
    private TextView gaBelow;
    private GraphicalView gaChart;
    private XYMultipleSeriesDataset gaSeries = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer gaRenderer = new XYMultipleSeriesRenderer();
    private XYSeriesRenderer gaForRenderer;
    private XYSeriesRenderer gaAgainstRenderer;

    private ViewGroup scPage;
    private TextView scTitle;
    private TextView scCategory;
    private TextView scProposer;
    private TextView scNation;
    private TextView scText;
    private TextView scBelow;
    private GraphicalView scChart;
    private XYMultipleSeriesDataset scSeries = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer scRenderer = new XYMultipleSeriesRenderer();
    private XYSeriesRenderer scForRenderer;
    private XYSeriesRenderer scAgainstRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_Sherlock_Light_DarkActionBar);
        setContentView(R.layout.wa);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        format.setMaximumFractionDigits(0);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);

        viewPager = (ViewPager) findViewById(R.id.pager);
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

        overview = (ViewGroup) getLayoutInflater().inflate(R.layout.wa_overview, viewPager, false);
        generalAssembly = (WACouncilFragment) getSupportFragmentManager().findFragmentById(R.id.general_assembly);
        generalAssembly.setCouncil(WACouncil.GENERAL_ASSEMBLY);
        securityCouncil = (WACouncilFragment) getSupportFragmentManager().findFragmentById(R.id.security_council);
        securityCouncil.setCouncil(WACouncil.SECURITY_COUNCIL);
        happenings = (TextView) overview.findViewById(R.id.wa_happenings);
        gaPage = (ViewGroup) getLayoutInflater().inflate(R.layout.wa_ga_sc, viewPager, false);
        gaTitle = (TextView) gaPage.findViewById(R.id.title);
        gaCategory = (TextView) gaPage.findViewById(R.id.category);
        gaProposer = (TextView) gaPage.findViewById(R.id.proposer);
        gaNation = (TextView) gaPage.findViewById(R.id.nation);
        gaText = (TextView) gaPage.findViewById(R.id.text);
        gaBelow = (TextView) gaPage.findViewById(R.id.below_text);
        scPage = (ViewGroup) getLayoutInflater().inflate(R.layout.wa_ga_sc, viewPager, false);
        scTitle = (TextView) scPage.findViewById(R.id.title);
        scCategory = (TextView) scPage.findViewById(R.id.category);
        scProposer = (TextView) scPage.findViewById(R.id.proposer);
        scNation = (TextView) scPage.findViewById(R.id.nation);
        scText = (TextView) scPage.findViewById(R.id.text);
        scBelow = (TextView) scPage.findViewById(R.id.below_text);

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
                happenings.setText(R.string.loading);
                gaPage.findViewById(R.id.layout).setVisibility(View.GONE);
                LoadingHelper.startLoading((LoadingView) gaPage.findViewById(R.id.loading));
                scPage.findViewById(R.id.layout).setVisibility(View.GONE);
                LoadingHelper.startLoading((LoadingView) scPage.findViewById(R.id.loading));
            }

            protected Boolean doInBackground(Void...params) {
                try {
                    gaData = API.getInstance(WorldAssembly.this).getWAInfo(WACouncil.GENERAL_ASSEMBLY,
                            WAData.Shards.RESOLUTION, WAData.Shards.HAPPENINGS, WAData.Shards.VOTETRACK, WAData.Shards.LAST_RESOLUTION);

                    scData = API.getInstance(WorldAssembly.this).getWAInfo(WACouncil.SECURITY_COUNCIL,
                            WAData.Shards.RESOLUTION, WAData.Shards.VOTETRACK, WAData.Shards.LAST_RESOLUTION);

                    nationData = API.getInstance(WorldAssembly.this).getNationInfo(NationInfo.getInstance(WorldAssembly.this).getId(),
                            NationData.Shards.NAME, NationData.Shards.GA_VOTE, NationData.Shards.SC_VOTE);

                    // Happenings - fetch nation and region names
                    Pattern nPattern = Pattern.compile("(.*?)@@([a-z\\d_]+)@@(.*?)");
                    Pattern rPattern = Pattern.compile("(.*?)%%([a-z\\d_]+)%%(.*?)");
                    Matcher matcher;
                    String n, r;
                    List<Happening> list = new ArrayList<Happening>(gaData.happenings);
                    list.addAll(gaData.happenings);
                    for(Happening event : list) {
                        matcher = nPattern.matcher(event.text);
                        while(matcher.matches()) {
                            n = event.text.substring(matcher.start(2), matcher.end(2));
                            n = TagParser.idToName(n);

                            event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.nation://$2\">" +
                                    n+"</a>$3");
                            matcher = nPattern.matcher(event.text);
                        }
                        matcher = rPattern.matcher(event.text);
                        while(matcher.matches()) {
                            r = event.text.substring(matcher.start(2), matcher.end(2));
                            r = TagParser.idToName(r);

                            event.text = matcher.replaceFirst("$1<a href=\"com.limewoodMedia.nsdroid.region://$2\">" +
                                    r+"</a>$3");
                            matcher = rPattern.matcher(event.text);
                        }
                    }

                    return true;
                } catch (RateLimitReachedException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.rate_limit_reached);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                } catch (UnknownNationException e) {
                    e.printStackTrace();
                    errorMessage = getResources().getString(R.string.unknown_nation);
                }

                return false;
            }

            protected void onPostExecute(Boolean result) {
                generalAssembly.onAfterLoading();
                securityCouncil.onAfterLoading();
                gaPage.findViewById(R.id.layout).setVisibility(View.VISIBLE);
                LoadingHelper.stopLoading((LoadingView) gaPage.findViewById(R.id.loading));
                scPage.findViewById(R.id.layout).setVisibility(View.VISIBLE);
                LoadingHelper.stopLoading((LoadingView) scPage.findViewById(R.id.loading));
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
        // General Assembly overview
        generalAssembly.loadCouncil(WACouncil.GENERAL_ASSEMBLY, gaData);

        // Security Council overview
        securityCouncil.loadCouncil(WACouncil.SECURITY_COUNCIL, scData);

        // Happenings
        StringBuilder happText = null;
        for(WAHappening happ : gaData.happenings) {
            if(happText == null) {
                happText = new StringBuilder();
            } else {
                happText.append("<br/><br/>");
            }
            happText.append("<i>" + TagParser.parseTimestamp(this, happ.timestamp) + ":</i> " + happ.text);
        }
        if(happText != null) {
            happenings.setText(Html.fromHtml(happText.toString()));
            happenings.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            happenings.setVisibility(View.GONE);
        }

        Resources r = getResources();

        // General Assembly
        if(gaData.resolution.name != null) {
            gaTitle.setText(gaData.resolution.name);
            String catText = gaData.resolution.category;
            if(catText.equals("Repeal")) {
                catText += " of GA#" + (Integer.parseInt(gaData.resolution.option)+1);
            } else {
                catText += "; Strength: " + gaData.resolution.option;
            }
            gaCategory.setText(catText);
            gaProposer.setText(Html.fromHtml(getString(R.string.wa_proposed_by) + " <a href=\"com.limewoodMedia.nsdroid.nation://"
                    + gaData.resolution.proposedBy + "\">" + TagParser.idToName(gaData.resolution.proposedBy) +"</a>"));
            gaProposer.setMovementMethod(LinkMovementMethod.getInstance());
            gaNation.setText(Html.fromHtml("<b>" + nationData.name + ":</b> " + nationData.generalAssemblyVote));
            gaText.setText(Html.fromHtml(TagParser.parseTags(gaData.resolution.desc.replace("\n", "<br/>"))));
            float total = gaData.resolution.votes.forVotes + gaData.resolution.votes.againstVotes;
            int[] daysHours = Utils.getDaysHours(gaData.resolution.created+7*24*60*60);
            int days = daysHours[0];
            int hours = daysHours[1];
            gaBelow.setText(Html.fromHtml(getString(R.string.wa_votes_for) + ": " + gaData.resolution.votes.forVotes + " ("
                    + format.format(gaData.resolution.votes.forVotes/total) + ")<br/>" + getString(R.string.wa_votes_against) + ": "
                    + gaData.resolution.votes.againstVotes + " (" + format.format(gaData.resolution.votes.againstVotes/total) + ")<br/><br/>"));
            // TODO Add when voting ends
//                    + getString(R.string.voting_ends, days, hours, r.getQuantityString(R.plurals.days, days), r.getQuantityString(R.plurals.hours, hours))));

            // Chart
            gaSeries.clear();
            XYSeries forVotes = new XYSeries("For");
            XYSeries againstVotes = new XYSeries("Against");
            int i=0;
            int forMax = 0;
            for(Integer f : gaData.resolution.voteTrack.forVotes) {
                forVotes.add(i++, f);
                if(f > forMax) {
                    forMax = f;
                }
            }
            i=0;
            int againstMax = 0;
            for(Integer a : gaData.resolution.voteTrack.againstVotes) {
                againstVotes.add(i++, a);
                if(a > againstMax) {
                    againstMax = a;
                }
            }
            gaSeries.addSeries(forVotes);
            gaSeries.addSeries(againstVotes);

            gaForRenderer = new XYSeriesRenderer();
            gaAgainstRenderer = new XYSeriesRenderer();
            setUpChartRenderer(gaRenderer, gaForRenderer, gaAgainstRenderer);
            gaRenderer.setYAxisMax(Math.max(forMax, againstMax)*1.2f);

            LinearLayout layout = (LinearLayout) gaPage.findViewById(R.id.chart);
            gaChart = ChartFactory.getLineChartView(this, gaSeries, gaRenderer);
            layout.addView(gaChart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, r.getDimensionPixelSize(R.dimen.wa_area_chart_height)));

            gaChart.repaint();
        } else {
            gaTitle.setText(getString(R.string.none));
            gaCategory.setVisibility(View.GONE);
            gaProposer.setVisibility(View.GONE);
            gaText.setVisibility(View.GONE);
            gaBelow.setVisibility(View.GONE);
            gaPage.findViewById(R.id.chart).setVisibility(View.GONE);
        }

        // Security Council
        if(scData.resolution.name != null) {
            scTitle.setText(scData.resolution.name);
            String catText = scData.resolution.category + "; ";
            String[] arr = scData.resolution.option.split(":");
            if(arr[0].equals("N")) {
                catText += getString(R.string.nation) + " <a href=\"com.limewoodMedia.nsdroid.nation://"
                        + arr[1] + "\">" + TagParser.idToName(arr[1]) +"</a>";
            } else if(arr[0].equals("R")) {
                catText += getString(R.string.region) + " <a href=\"com.limewoodMedia.nsdroid.region://"
                        + arr[1] + "\">" + TagParser.idToName(arr[1]) +"</a>";
            } else {
                catText += scData.resolution.option;
            }
            scCategory.setText(Html.fromHtml(catText));
            scCategory.setMovementMethod(LinkMovementMethod.getInstance());
            scProposer.setText(Html.fromHtml(getString(R.string.wa_proposed_by) + " <a href=\"com.limewoodMedia.nsdroid.nation://"
                    + scData.resolution.proposedBy + "\">" + TagParser.idToName(scData.resolution.proposedBy) + "</a>"));
            scProposer.setMovementMethod(LinkMovementMethod.getInstance());
            scNation.setText(Html.fromHtml("<b>" + nationData.name + ":</b> " + nationData.securityCouncilVote));
            scText.setText(Html.fromHtml(TagParser.parseTags(scData.resolution.desc.replace("\n", "<br/>"))));
            float total = scData.resolution.votes.forVotes + scData.resolution.votes.againstVotes;
            int[] daysHours = Utils.getDaysHours(scData.resolution.created+7*24*60*60);
            int days = daysHours[0];
            int hours = daysHours[1];
            scBelow.setText(Html.fromHtml("<b>" + getString(R.string.wa_votes_for) + ":</b> " + scData.resolution.votes.forVotes + " ("
                    + format.format(scData.resolution.votes.forVotes/total) + ")<br/><b>" + getString(R.string.wa_votes_against) + ":</b> "
                    + scData.resolution.votes.againstVotes + " (" + format.format(scData.resolution.votes.againstVotes/total) + ")<br/><br/>"));
                    // TODO Add when voting ends
//                    + getString(R.string.voting_ends, days, hours, r.getQuantityString(R.plurals.days, days), r.getQuantityString(R.plurals.hours, hours))));

            // Chart
            scSeries.clear();
            XYSeries forVotes = new XYSeries("For");
            XYSeries againstVotes = new XYSeries("Against");
            int i=0;
            int forMax = 0;
            for(Integer f : scData.resolution.voteTrack.forVotes) {
                forVotes.add(i++, f);
                if(f > forMax) {
                    forMax = f;
                }
            }
            i=0;
            int againstMax = 0;
            for(Integer a : scData.resolution.voteTrack.againstVotes) {
                againstVotes.add(i++, a);
                if(a > againstMax) {
                    againstMax = a;
                }
            }
            scSeries.addSeries(forVotes);
            scSeries.addSeries(againstVotes);

            scForRenderer = new XYSeriesRenderer();
            scAgainstRenderer = new XYSeriesRenderer();
            setUpChartRenderer(scRenderer, scForRenderer, scAgainstRenderer);
            scRenderer.setYAxisMax(Math.max(forMax, againstMax)*1.2f);

            LinearLayout layout = (LinearLayout) scPage.findViewById(R.id.chart);
            scChart = ChartFactory.getLineChartView(this, scSeries, scRenderer);
            layout.addView(scChart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, r.getDimensionPixelSize(R.dimen.wa_area_chart_height)));

            scChart.repaint();
        } else {
            scTitle.setText(getString(R.string.none));
            scCategory.setVisibility(View.GONE);
            scProposer.setVisibility(View.GONE);
            scText.setVisibility(View.GONE);
            scBelow.setVisibility(View.GONE);
            scPage.findViewById(R.id.chart).setVisibility(View.GONE);
        }

        supportInvalidateOptionsMenu();
    }

    private void setUpChartRenderer(XYMultipleSeriesRenderer chartRenderer, XYSeriesRenderer forRenderer, XYSeriesRenderer againstRenderer) {
        Log.d(TAG, "Set up chart renderer");
        Resources r = getResources();
        float legendTextSize = r.getDimension(R.dimen.area_chart_legend_size);
        float labelTextSize = r.getDimension(R.dimen.area_chart_label_size);

        // For renderer
        forRenderer.setColor(r.getColor(R.color.wa_for));
        forRenderer.setChartValuesTextSize(legendTextSize);
        forRenderer.setDisplayChartValues(true);
        XYSeriesRenderer.FillOutsideLine line = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BELOW);
        line.setColor(r.getColor(R.color.wa_for_below));
        forRenderer.addFillOutsideLine(line);

        againstRenderer.setColor(r.getColor(R.color.wa_against));
        againstRenderer.setChartValuesTextSize(legendTextSize);
        againstRenderer.setDisplayChartValues(true);
        line = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BELOW);
        line.setColor(r.getColor(R.color.wa_against_below));
        againstRenderer.addFillOutsideLine(line);

        chartRenderer.setZoomButtonsVisible(false);
        chartRenderer.setClickEnabled(true);
        chartRenderer.setInScroll(true);
        chartRenderer.setAntialiasing(true);
        chartRenderer.setShowLegend(true);
        chartRenderer.setLegendTextSize(legendTextSize);
        chartRenderer.setLabelsTextSize(labelTextSize);
        chartRenderer.setTextTypeface(Typeface.DEFAULT);
        chartRenderer.setPanEnabled(false);
        chartRenderer.setShowLabels(true);
        chartRenderer.setXAxisMin(0);
        chartRenderer.setXAxisMax(24 * 4 + 1); // 1 per hour, 4 days, 1 at origo
        chartRenderer.setYAxisMin(0);
        chartRenderer.setXLabels(0);
        chartRenderer.setXLabelsAngle(-25);
        chartRenderer.setXLabelsAlign(Paint.Align.RIGHT);
        chartRenderer.setFitLegend(true);
        for(int i=1; i<5; i++) {
            chartRenderer.addXTextLabel(24*i, i+" "+r.getQuantityString(R.plurals.days, i));
        }
        chartRenderer.setYLabels(0);
        chartRenderer.setGridColor(Color.WHITE);
        chartRenderer.setMarginsColor(Color.WHITE);
        chartRenderer.setMargins(new int[]{0, 0, r.getDimensionPixelSize(R.dimen.area_chart_margin_bottom), 0});
        chartRenderer.setBackgroundColor(Color.WHITE);
        chartRenderer.setApplyBackgroundColor(true);
        chartRenderer.removeAllRenderers();
        chartRenderer.addSeriesRenderer(forRenderer);
        chartRenderer.addSeriesRenderer(againstRenderer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_wa, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        switch (viewPager.getCurrentItem()) {
            case 0: // Overview
                menu.findItem(R.id.menu_vote_for).setVisible(false);
                menu.findItem(R.id.menu_vote_against).setVisible(false);
                menu.findItem(R.id.menu_vote_none).setVisible(false);
                break;
            case 1: // GA
            case 2: // SC
                if(NationInfo.getInstance(this).getWAStatus() == WAStatus.NON_MEMBER) {
                    menu.findItem(R.id.menu_vote_for).setVisible(false);
                    menu.findItem(R.id.menu_vote_against).setVisible(false);
                    menu.findItem(R.id.menu_vote_none).setVisible(false);
                }
                if(nationData != null) {
                    Log.d(TAG, "Page: "+viewPager.getCurrentItem() + "; ga: "+nationData.generalAssemblyVote+"; sc: "+nationData.securityCouncilVote);
                    WAVote vote = viewPager.getCurrentItem() == 1 ? nationData.generalAssemblyVote : nationData.securityCouncilVote;
                    switch (vote) {
                        case FOR:
                            menu.findItem(R.id.menu_vote_for).setVisible(false);
                            menu.findItem(R.id.menu_vote_against).setVisible(true);
                            menu.findItem(R.id.menu_vote_none).setVisible(true);
                            break;
                        case AGAINST:
                            menu.findItem(R.id.menu_vote_against).setVisible(false);
                            menu.findItem(R.id.menu_vote_for).setVisible(true);
                            menu.findItem(R.id.menu_vote_none).setVisible(true);
                            break;
                        case UNDECIDED:
                            menu.findItem(R.id.menu_vote_none).setVisible(false);
                            menu.findItem(R.id.menu_vote_against).setVisible(true);
                            menu.findItem(R.id.menu_vote_for).setVisible(true);
                            break;
                    }
                }
                break;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final WACouncil council = viewPager.getCurrentItem() == 1 ? WACouncil.GENERAL_ASSEMBLY : WACouncil.SECURITY_COUNCIL;
        WAVote vote = null;
        switch (item.getItemId()) {
            case R.id.menu_vote_for:
                vote = WAVote.FOR;
                break;
            case R.id.menu_vote_against: {
                vote = WAVote.AGAINST;
                break;
            }
            case R.id.menu_vote_none: {
                vote = WAVote.UNDECIDED;
                break;
            }
            case R.id.menu_refresh:
                loadData();
                break;
        }
        if(vote != null) {
            final WAVote finalVote = vote;
            new AlertDialog.Builder(this).setTitle(R.string.wa_vote_dialog_title)
                    .setMessage(getString(R.string.wa_vote_dialog_message, vote == WAVote.FOR ? getString(R.string.wa_vote_dialog_for)
                    : vote == WAVote.AGAINST ? getString(R.string.wa_vote_dialog_against) : getString(R.string.wa_vote_dialog_none)))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected void onPreExecute() {
                                    LoadingHelper.startLoading((LoadingView) (council == WACouncil.GENERAL_ASSEMBLY ? gaPage : scPage).findViewById(R.id.loading));
                                }

                                @Override
                                protected Void doInBackground(Void... vote) {
                                    if (API.getInstance(WorldAssembly.this).checkLogin(WorldAssembly.this)) {
                                        try {
                                            Log.d(TAG, "Vote: " + finalVote);
                                            if (API.getInstance(WorldAssembly.this).voteOnWAProposal(council, finalVote)) {
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(WorldAssembly.this, R.string.wa_voted, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(WorldAssembly.this, R.string.wa_voted_error, Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void aVoid) {
                                    LoadingHelper.stopLoading((LoadingView) (council == WACouncil.GENERAL_ASSEMBLY ? gaPage : scPage).findViewById(R.id.loading));
                                    loadData();
                                }
                            }.execute();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
        return super.onOptionsItemSelected(item);
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
                case 1: // GA
                    if(gaPage.getParent() == null) {
                        container.addView(gaPage);
                    }
                    return gaPage;
                case 2: // SC
                    if(scPage.getParent() == null) {
                        container.addView(scPage);
                    }
                    return scPage;
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

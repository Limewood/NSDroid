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
package com.limewoodmedia.nsdroid.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.Utils;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment showing overview of WA Council
 */
public class WACouncilFragment extends Fragment {
	@SuppressWarnings("unused")
	private static final String TAG = WACouncilFragment.class.getName();
    private static final long RESOLUTION_TIME = 60*60*24*5; // Five days

	private View root;
	private TextView atVote;
	private TextView title;
    private TextView votes;
    private TextView below;
    private GraphicalView chart;
    private XYMultipleSeriesDataset series = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	private ViewGroup layout;
	private Context context;
    private com.limewoodMedia.nsapi.enums.WACouncil council;
    private XYSeriesRenderer forRenderer;
    private XYSeriesRenderer againstRenderer;
    private final NumberFormat format = NumberFormat.getPercentInstance();
    private int legend;

	public WACouncilFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.wa_council, null, false);
		atVote = (TextView) root.findViewById(R.id.at_vote_text);
        title = (TextView) root.findViewById(R.id.council_header);
        title.setText(legend != -1 ? legend : R.string.general_assembly);
        votes = (TextView) root.findViewById(R.id.at_vote_votes);
        below = (TextView) root.findViewById(R.id.at_vote_below);
		layout = (ViewGroup) root.findViewById(R.id.layout);
		ViewTreeObserver observer = title.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                layout.setPadding(layout.getPaddingLeft(), title.getHeight() -
                                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
                                        getResources().getDisplayMetrics()), layout.getPaddingRight(),
                        layout.getPaddingBottom());
            }
        });
        format.setMaximumFractionDigits(0);

        LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
		
		return root;
	}

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        TypedArray a = activity.obtainStyledAttributes(attrs,R.styleable.WACouncilFragment);

        legend = a.getResourceId(R.styleable.WACouncilFragment_legend, -1);

        a.recycle();
    }

    public void onBeforeLoading() {
        if(root != null) {
            root.findViewById(R.id.layout).setVisibility(View.GONE);
            LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
	}

    public void onAfterLoading() {
        if(root != null) {
            root.findViewById(R.id.layout).setVisibility(View.VISIBLE);
            LoadingHelper.stopLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
    }
	
	public void loadCouncil(WACouncil council, WAData data) {
        setCouncil(council);

        if(data.resolution.name != null) {
            atVote.setText(Html.fromHtml("<b>" + getString(R.string.wa_at_vote) + ":</b> " + data.resolution.name));
            float total = data.resolution.votes.forVotes + data.resolution.votes.againstVotes;
            votes.setText(Html.fromHtml("<b>" + getString(R.string.wa_votes_for) + ":</b> " + format.format(data.resolution.votes.forVotes/total) + "<br/>"
                    + "<b>" + getString(R.string.wa_votes_against) + ":</b> " + format.format(data.resolution.votes.againstVotes/total)));
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(data.resolution.created * 1000);
            Log.d(TAG, "Created: " + cal.getTimeInMillis());
            cal.add(Calendar.DAY_OF_YEAR, 7);
            Log.d(TAG, "Ending: "+cal.getTimeInMillis());
            int[] daysHours = Utils.getWADaysHoursLeft(data.resolution.voteTrack.forVotes.length);
            int days = daysHours[0];
            int hours = daysHours[1];
            Resources r = getResources();
            below.setText(Html.fromHtml(
                    getString(R.string.voting_ends, days, hours,
                    r.getQuantityString(R.plurals.days, days), r.getQuantityString(R.plurals.hours, hours)) +
                      "<br/><br/><b>" + getString(R.string.wa_recent) + ":</b> "+data.lastResolution));

            // Set up chart
            Log.d(TAG, "Set up values");
            series.clear();
            CategorySeries forVotes = new CategorySeries("For");
            CategorySeries againstVotes = new CategorySeries("Against");
            forVotes.add("For", data.resolution.votes.forVotes);
            againstVotes.add("Against", data.resolution.votes.againstVotes);
            series.addSeries(forVotes.toXYSeries());
            series.addSeries(againstVotes.toXYSeries());

            setUpChartRenderer(renderer);
            renderer.setYAxisMax(Math.max(data.resolution.votes.forVotes, data.resolution.votes.againstVotes)*1.2f);

            LinearLayout layout = (LinearLayout) root.findViewById(R.id.at_vote_chart);
            chart = ChartFactory.getBarChartView(getActivity(), series, renderer, BarChart.Type.DEFAULT);
            layout.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, r.getDimensionPixelSize(R.dimen.wa_bar_chart_height)));

            chart.repaint();
        } else {
            atVote.setText(R.string.none);
            votes.setVisibility(View.GONE);
            root.findViewById(R.id.at_vote_chart).setVisibility(View.GONE);
            below.setText(Html.fromHtml("<br/><br/><b>" + getString(R.string.wa_recent) + ":</b> " + data.lastResolution));
        }
	}

    private void setUpChartRenderer(XYMultipleSeriesRenderer chartRenderer) {
        Log.d(TAG, "Set up chart renderer");
        Resources r = getResources();
        float labelTextSize = r.getDimension(R.dimen.bar_chart_label_size);

        // For renderer
        forRenderer = new XYSeriesRenderer();
        forRenderer.setColor(r.getColor(R.color.wa_for));
        forRenderer.setChartValuesTextSize(labelTextSize);
        forRenderer.setDisplayChartValues(true);

        againstRenderer = new XYSeriesRenderer();
        againstRenderer.setColor(r.getColor(R.color.wa_against));
        againstRenderer.setChartValuesTextSize(labelTextSize);
        againstRenderer.setDisplayChartValues(true);

        chartRenderer.setZoomButtonsVisible(false);
        chartRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
        chartRenderer.setClickEnabled(true);
        chartRenderer.setInScroll(true);
        chartRenderer.setAntialiasing(true);
        chartRenderer.setShowLegend(false);
        chartRenderer.setTextTypeface(Typeface.DEFAULT);
        chartRenderer.setPanEnabled(false);
        chartRenderer.setShowLabels(false);
        chartRenderer.setXAxisMin(-0.5);
        chartRenderer.setXAxisMax(2.5);
        chartRenderer.setYAxisMin(0);
        chartRenderer.setXLabels(0);
        chartRenderer.setYLabels(0);
        chartRenderer.setBarWidth(r.getDimensionPixelSize(R.dimen.wa_bar_width));
        chartRenderer.setGridColor(Color.WHITE);
        chartRenderer.setMarginsColor(Color.WHITE);
        chartRenderer.setMargins(new int[]{0, 0, 0, 0});
        chartRenderer.setBackgroundColor(Color.WHITE);
        chartRenderer.setApplyBackgroundColor(true);
        chartRenderer.removeAllRenderers();
        chartRenderer.addSeriesRenderer(forRenderer);
        chartRenderer.addSeriesRenderer(againstRenderer);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(chart != null) {
            Log.d(TAG, "Repaint chart");
            chart.repaint();
        }
    }

    public void setCouncil(WACouncil council) {
        this.council = council;
    }
}

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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Calendar;
import java.util.Locale;

public class WACouncilFragment extends SherlockFragment {
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

        LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
		
		return root;
	}
	
	public void onBeforeLoading() {
        if(root != null) {
            LoadingHelper.startLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
	}

    public void onAfterLoading() {
        if(root != null) {
            LoadingHelper.stopLoading((com.limewoodmedia.nsdroid.views.LoadingView) root.findViewById(R.id.loading));
        }
    }
	
	public void setCouncil(WACouncil council, WAData data) {
		this.council = council;
        title.setBackgroundResource(council == WACouncil.GENERAL_ASSEMBLY ? R.drawable.ga_legend : R.drawable.sc_legend);

        title.setText(council == WACouncil.GENERAL_ASSEMBLY ? R.string.general_assembly : R.string.security_council);
        if(data.resolution.name != null) {
            atVote.setText(Html.fromHtml("<b>At vote:</b> " + data.resolution.name));
            votes.setText(Html.fromHtml("<b>For:</b> " + data.resolution.votes.forVotes + "<br/>"
                    + "<b>Against:</b> " + data.resolution.votes.againstVotes));
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.add(Calendar.DAY_OF_YEAR, 5);
            long now = cal.getTimeInMillis() / 1000;
            long seconds = now - data.resolution.created;
            int hours = (int) seconds / 60 / 60;
            int days = hours / 24;
            Resources r = getResources();
            below.setText(Html.fromHtml(getString(R.string.voting_ends, days, hours,
                    r.getQuantityString(R.plurals.days, days), r.getQuantityString(R.plurals.hours, hours))
                    +"<br/><br/><b>Recent:</b> "+data.lastResolution));

            // Set up chart
            series.clear();
            XYSeries forVotes = new XYSeries("For");
            XYSeries againstVotes = new XYSeries("Against");
            forVotes.add(0, data.resolution.votes.forVotes);
            againstVotes.add(0, data.resolution.votes.againstVotes);
            series.addSeries(forVotes);
            series.addSeries(againstVotes);

            chart.repaint();
        } else {
            atVote.setText(R.string.none);
            votes.setVisibility(View.GONE);
            chart.setVisibility(View.GONE);
            below.setText(Html.fromHtml("<br/><br/><b>Recent:</b> " + data.lastResolution));
        }
	}

    private void setUpChartRenderer(XYMultipleSeriesRenderer chartRenderer) {
        Resources r = getResources();
        float labelTextSize = r.getDimension(R.dimen.pie_chart_label_size);

        chartRenderer.setZoomButtonsVisible(false);
        chartRenderer.setOrientation(XYMultipleSeriesRenderer.Orientation.HORIZONTAL);
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
        chartRenderer.setXAxisMin(0);
        chartRenderer.setXAxisMax(0);
        chartRenderer.setXLabels(0);
        chartRenderer.setMargins(new int[]{
                Math.round(r.getDimension(R.dimen.pie_chart_margin_top)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_left)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_bottom)),
                Math.round(r.getDimension(R.dimen.pie_chart_margin_right))
        });
        chartRenderer.addSeriesRenderer(forRenderer);
        chartRenderer.addSeriesRenderer(againstRenderer);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(chart == null) {
            // Create chart
            LinearLayout layout = (LinearLayout) root.findViewById(R.id.at_vote_chart);
            chart = ChartFactory.getBarChartView(getActivity(), series, renderer, BarChart.Type.DEFAULT);
            layout.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));

            Resources r = getResources();

            // For renderer
            forRenderer = new XYSeriesRenderer();
            forRenderer.setColor(r.getColor(R.color.ga_for));
            forRenderer.setFillPoints(true);
            forRenderer.setLineWidth(2);
            forRenderer.setDisplayChartValues(true);

            againstRenderer = new XYSeriesRenderer();
            againstRenderer.setColor(r.getColor(R.color.sc_against));
            againstRenderer.setFillPoints(true);
            againstRenderer.setLineWidth(2);
            againstRenderer.setDisplayChartValues(true);

            setUpChartRenderer(renderer);
        } else {
            chart.repaint();
        }
    }
}

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
package com.limewoodmedia.nsdroid.fragments;

import java.io.IOException;

import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.CustomAlertDialogBuilder;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.db.IssuesDatabase;
import com.limewoodmedia.nsdroid.holders.CensusChange;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.holders.IssueResult;
import com.limewoodmedia.nsdroid.views.ChoiceView;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.xml.sax.XMLReader;

/**
 * Fragment for showing a specific issue
 */
public class IssueDetailFragment extends Fragment implements OnClickListener {
	private static final String TAG = IssueDetailFragment.class.getName();
	
	private View root;
	private TextView title;
	private TextView theIssue;
	private TextView text;
	private TextView theDebate;
	private ViewGroup choicesArea;
	private ScrollView scrollView;
	private ViewGroup layout;
	private Context context;
	private int issueId;
	private AsyncTask<Void, Void, Issue> loadIssue;
    private IssuesDatabase db;

	public IssueDetailFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
        this.db = IssuesDatabase.getInstance(getActivity());
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.issue_detail, null, false);
		title = (TextView) root.findViewById(R.id.issue_detail_title);
		ViewTreeObserver observer = title.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
				scrollView.setPadding(scrollView.getPaddingLeft(), title.getHeight() -
						(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
							getResources().getDisplayMetrics()), scrollView.getPaddingRight(),
							scrollView.getPaddingBottom());
		    }
		});
		theIssue = (TextView) root.findViewById(R.id.the_issue_title);
		text = (TextView) root.findViewById(R.id.issue_text);
		theDebate = (TextView) root.findViewById(R.id.the_debate_title);
		choicesArea = (ViewGroup) root.findViewById(R.id.issue_choices_area);
		scrollView = (ScrollView) root.findViewById(R.id.issue_scroll_view);
		layout = (ViewGroup) root.findViewById(R.id.layout);

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
		
		return root;
	}
	
	public void reloadIssue() {
		loadIssue(issueId);
	}
	
	public void loadIssue(final int id) {
		issueId = id;
		layout.setVisibility(View.GONE);
    	final LoadingView loadingView = (LoadingView) root.findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_issue, getActivity());
        title.setText(R.string.issue_loading);
		theIssue.setText(R.string.the_issue);
		theDebate.setText(R.string.the_debate);
		loadIssue = new AsyncTask<Void, Void, Issue>() {
            private int previous = -1;

			@Override
			protected Issue doInBackground(Void... params) {
				if(API.getInstance(context).checkLogin(getActivity())) {
                    // Get previous choice
                    previous = db.getPreviousIssueChoiceIndex(id);
                    try {
                        return API.getInstance(getActivity()).getIssue(id);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
				return null;
			}
			
			protected void onPostExecute(Issue result) {
				if(isAdded()) {
					LoadingHelper.stopLoading(loadingView);
					layout.setVisibility(View.VISIBLE);
					if(result != null) {
						choicesArea.removeAllViews();
						title.setText(Html.fromHtml(result.name));

						text.setText(Html.fromHtml(result.text));
						ChoiceView cText;
						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
								LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
						params.setMargins(0, 0, 0, 9);
						Log.d(TAG, "Selected "+result.selectedChoice);

                        int i=0;
						for(String choice : result.choices) {
							cText = (ChoiceView) getLayoutInflater(null).inflate(R.layout.issue_choice, null);
							cText.setTextColor(getResources().getColor(android.R.color.black));
							cText.setTag(R.id.choice_index, i);
							cText.setTag(R.id.choice_type, true);
							cText.setOnClickListener(IssueDetailFragment.this);
                            // Indicate previous choice
                            if(previous == i) {
                                cText.setBackgroundResource(R.drawable.choice_background_previous);
                                choice += "<br/><small><font color='grey'>" + getString(R.string.previous_choice) + "</font></small>";
                            } else {
                                cText.setBackgroundResource(R.drawable.choice_background);
                            }
                            cText.setText(Html.fromHtml(choice));
							if(!result.dismissed) {
								cText.setSelected(result.selectedChoice == i);
								cText.setDismissed(false);
							} else {
								cText.setDismissed(true);
							}
							cText.setFocusable(true);
							cText.setClickable(true);
							choicesArea.addView(cText, params);
							i++;
						}
					} else {
                        Toast.makeText(getActivity(), R.string.api_io_exception, Toast.LENGTH_SHORT).show();
                    }
				}
			}
		}.execute();
	}
	
	/**
	 * Dismisses the current issue
	 */
	public void dismissIssue() {
		// Show confirm dialog
		CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
		builder.setTitle(R.string.issue_dismiss_title)
			.setMessage(getResources().getString(R.string.issue_dismiss_text))
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, int which) {
                    new AsyncTask<Integer, Void, IssueResult>() {
                        @Override
                        protected IssueResult doInBackground(Integer... params) {
                            if (API.getInstance(getActivity()).checkLogin(getActivity())) {
                                try {
                                   IssueResult result = API.getInstance(getActivity()).answerIssue(
                                            issueId, (Integer) params[0]);
                                    if (result != null) {
                                        db.setIssueChoice(issueId, -2);
                                    }
                                    return result;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            return null;
                        }

                        protected void onPostExecute(IssueResult result) {
                            Log.d(TAG, "Result: " + result);
                            // Go back to list
                            dialog.dismiss();
							getFragmentManager().popBackStack();
                        }
                    }.execute(-1);
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

	@Override
	public void onClick(final View v) {
		if(v.getTag(R.id.choice_type) != null) {
			Log.d(TAG, "Choice " + v.getTag(R.id.choice_index) + " was clicked");
			// Show confirm dialog
			CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
			builder.setTitle(R.string.issue_choose_title)
				.setMessage(getResources().getString(R.string.issue_choose_text,
						((Integer)v.getTag(R.id.choice_index))+1))
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        new AsyncTask<Integer, Void, IssueResult>() {
                            @Override
                            protected IssueResult doInBackground(Integer... params) {
                                if (API.getInstance(getActivity()).checkLogin(getActivity())) {
                                    try {
                                        IssueResult result = API.getInstance(getActivity()).answerIssue(
                                                issueId, (Integer) params[0]);
                                        if (result != null) {
                                            db.setIssueChoice(issueId, params[0]);
                                        }
                                        return result;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                return null;
                            }

                            protected void onPostExecute(IssueResult result) {
                                Log.d(TAG, "Result: " + result);
                                showIssueResult(result);
                                dialog.dismiss();
                            }
                        }.execute((Integer) v.getTag(R.id.choice_index));
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
	}
	
	private void showIssueResult(IssueResult result) {
		if(isAdded()) {
			choicesArea.removeAllViews();
			theIssue.setText(R.string.the_talking_point);
			text.setText(Html.fromHtml(result.result));
			theDebate.setText(R.string.recent_trends);
			RelativeLayout cText;
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			params.setMargins(0, 0, 0, 9);
			TextView trend;
			TextView trendName;
			for(CensusChange change : result.censusChangeList) {
				cText = (RelativeLayout) getLayoutInflater(null).inflate(R.layout.recent_trend, null);
				trend = (TextView) cText.findViewById(R.id.trend);
				trend.setText(change.percent);
				trendName = (TextView) cText.findViewById(R.id.trend_name);
				trendName.setText(change.name);
				if(change.increase) {
					trend.setTextColor(getResources().getColor(R.color.medium_green));
					trendName.setTextColor(getResources().getColor(R.color.medium_green));
				} else {
					trend.setTextColor(getResources().getColor(R.color.decrease_red));
					trendName.setTextColor(getResources().getColor(R.color.decrease_red));
				}
				((TextView)cText.findViewById(R.id.trend_metric)).setText(change.metric);
				choicesArea.addView(cText, params);
				scrollView.scrollTo(0,0);
			}
		}
	}
	
	@Override
	public void onPause() {
		if(loadIssue != null && !loadIssue.isCancelled()) {
			loadIssue.cancel(true);
		}
		super.onPause();
	}
}

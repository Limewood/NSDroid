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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.CustomAlertDialogBuilder;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.views.ChoiceView;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ScrollView;
import android.widget.TextView;

public class IssueDetailFragment extends SherlockFragment implements OnClickListener {
	private static final String TAG = IssueDetailFragment.class.getName();
	
	private View root;
	private TextView title;
	private TextView text;
	private TextView position;
	private ViewGroup choicesArea;
	private ScrollView scrollView;
	private ViewGroup layout;
	private Context context;
	private int issueId;
	private AsyncTask<Void, Void, Issue> loadIssue;

	public IssueDetailFragment() {
		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
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
		text = (TextView) root.findViewById(R.id.issue_text);
		position = (TextView) root.findViewById(R.id.issue_position);
		choicesArea = (ViewGroup) root.findViewById(R.id.issue_choices_area);
		scrollView = (ScrollView) root.findViewById(R.id.issue_scroll_view);
		layout = (ViewGroup) root.findViewById(R.id.layout);

        ActionBar actionBar = getSherlockActivity().getSupportActionBar();
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
		loadIssue = new AsyncTask<Void, Void, Issue>() {
			@Override
			protected Issue doInBackground(Void... params) {
				if(API.getInstance(context).checkLogin(getActivity())) {
					return API.getInstance(getActivity()).getIssue(id);
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
						int i=0;
						Log.d(TAG, "Selected "+result.selectedChoice);
						if(result.selectedChoice > -1) {
							position.setText(getResources().getString(R.string.issue_position_choice,
									result.selectedChoice));
						} else if(result.dismissed) {
							position.setText(R.string.issue_position_dismiss);
						} else {
							position.setText(R.string.issue_position_undecided);
						}
						
						for(String choice : result.choices) {
							cText = (ChoiceView) getLayoutInflater(null).inflate(R.layout.issue_choice, null);
							cText.setText(Html.fromHtml(choice));
							cText.setTextColor(getResources().getColor(android.R.color.black));
							cText.setTag(R.id.choice_index, i);
							cText.setTag(R.id.choice_type, true);
							cText.setOnClickListener(IssueDetailFragment.this);
							if(!result.dismissed) {
								cText.setSelected(result.selectedChoice == i+1);
								cText.setDismissed(false);
							} else {
								cText.setDismissed(true);
							}
							cText.setFocusable(true);
							cText.setClickable(true);
							choicesArea.addView(cText, params);
							i++;
						}
					}
				}
			};
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
					new AsyncTask<Integer, Void, Boolean>() {
						@Override
						protected Boolean doInBackground(Integer... params) {
							if(API.getInstance(getActivity()).checkLogin(getActivity())) {
								try {
									return API.getInstance(getActivity()).answerIssue(
											issueId, (Integer)params[0]);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							return false;
						}
						
						protected void onPostExecute(Boolean result) {
							Log.d(TAG, "Result: "+result);
							// Show as dismissed
							showAsDismissed();
							dialog.dismiss();
						};
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
			Log.d(TAG, "Choice "+v.getTag(R.id.choice_index)+" was clicked");
			// Show confirm dialog
			CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
			builder.setTitle(R.string.issue_choose_title)
				.setMessage(getResources().getString(R.string.issue_choose_text,
						((Integer)v.getTag(R.id.choice_index))+1))
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, int which) {
						new AsyncTask<Integer, Void, Boolean>() {
							@Override
							protected Boolean doInBackground(Integer... params) {
								if(API.getInstance(getActivity()).checkLogin(getActivity())) {
									try {
										return API.getInstance(getActivity()).answerIssue(
												issueId, (Integer)params[0]);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								return false;
							}
							
							protected void onPostExecute(Boolean result) {
								Log.d(TAG, "Result: "+result);
								selectOption(v);
								dialog.dismiss();
							};
						}.execute((Integer)v.getTag(R.id.choice_index));
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
	
	private void selectOption(View v) {
		int len = choicesArea.getChildCount();
		ChoiceView child;
		Log.d(TAG, "Index "+v.getTag(R.id.choice_index));
		for(int i=0; i<len; i++) {
			child = (ChoiceView) choicesArea.getChildAt(i);
			Log.d(TAG, "Child index "+child.getTag(R.id.choice_index));
			if(child.isDismissed()) {
				child.setDismissed(false);
			}
			if(child.getTag(R.id.choice_index) == v.getTag(R.id.choice_index)) {
				Log.d(TAG, "Select "+child.getTag(R.id.choice_index));
				child.setSelected(true);
			} else {
				Log.d(TAG, "Unselect "+child.getTag(R.id.choice_index));
				child.setSelected(false);
			}
		}
		if(isAdded()) {
			position.setText(getResources().getString(R.string.issue_position_choice,
					((Integer) v.getTag(R.id.choice_index)) + 1));
		}
	}
	
	private void showAsDismissed() {
		int len = choicesArea.getChildCount();
		ChoiceView child;
		for(int i=0; i<len; i++) {
			child = (ChoiceView) choicesArea.getChildAt(i);
			if(child.isSelected()) {
				child.setSelected(false);
			}
			child.setDismissed(true);
		}
		position.setText(R.string.issue_position_dismiss);
	}
	
	@Override
	public void onPause() {
		if(loadIssue != null && !loadIssue.isCancelled()) {
			loadIssue.cancel(true);
		}
		super.onPause();
	}
}

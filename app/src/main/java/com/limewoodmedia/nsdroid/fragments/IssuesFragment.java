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

import java.util.ArrayList;
import java.util.List;

import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.activities.Issues;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.holders.IssuesInfo;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class IssuesFragment extends Fragment {
	private static final String TAG = IssuesFragment.class.getName();
	
	private View root;
	private TextView title;
	private ListView list;
    private TextView text;
	
	private List<Issue> issues;
	private ArrayAdapter<Issue> listAdapter;
	private Context context;
	private AsyncTask<Void, Void, IssuesInfo> loadIssues;

	public IssuesFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.issues, null, false);
		list = (ListView) root.findViewById(R.id.issues_list);
		title = (TextView) root.findViewById(R.id.issues_title);
		title.setText(getResources().getString(R.string.issues_title,
                NationInfo.getInstance(getActivity()).getName()));
		ViewTreeObserver observer = title.getViewTreeObserver();
		observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    @Override
		    public void onGlobalLayout() {
		    	list.setPadding(list.getPaddingLeft(), title.getHeight() -
						(int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15,
							getResources().getDisplayMetrics()), list.getPaddingRight(),
							list.getPaddingBottom());
		    }
		});
        text = (TextView) root.findViewById(R.id.issues_text);
        text.setVisibility(View.GONE);

        issues = new ArrayList<Issue>();
		listAdapter = new ArrayAdapter<Issue>(context, 0, issues) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view;
        		TextView text;
        		
        		if(convertView == null) {
        			view = inflater.inflate(R.layout.issue_list_item, null);
        			text = (TextView) view.findViewById(R.id.issue_item);
        			text.setMovementMethod(LinkMovementMethod.getInstance());
        		}
        		else {
        			view = convertView;
        			text = (TextView) view.findViewById(R.id.issue_item);
        		}
    			Issue issue = getItem(position);
    			String txt = "&#8226; <a href=\"com.limewoodMedia.nsdroid.issue://" + issue.id + "\">" +
    					issue.name + "</a> " + (issue.pending ? getResources().getString(R.string.issue_pending)
    							: (issue.dismissed ? getResources().getString(R.string.issue_dismissed) : ""));
    			text.setText(Html.fromHtml(txt));
        		
        		return view;
        	}
        };
        list.setAdapter(listAdapter);
		
		return root;
	}
	
	@Override
	public void onPause() {
		if(loadIssues != null && !loadIssues.isCancelled()) {
			loadIssues.cancel(true);
		}
		super.onPause();
	}
	
	@Override
	public void onResume() {
		loadIssues();
		super.onResume();
	}
	
	public void loadIssues() {
		Log.d(TAG, "Loading issues");
		issues.clear();
		final LoadingView loadingView = (LoadingView) root.findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView, R.string.loading_issues, getActivity());
		loadIssues = new AsyncTask<Void, Void, IssuesInfo>() {
			@Override
			protected IssuesInfo doInBackground(Void... params) {
				if(API.getInstance(context).checkLogin(getActivity())) {
					return API.getInstance(getActivity()).getIssues();
				}
				return null;
			}
			
			protected void onPostExecute(IssuesInfo result) {
				if(isAdded()) {
					LoadingHelper.stopLoading(loadingView);
					if(result != null) {
						if(result.issues != null) {
                            issues.addAll(result.issues);
                            listAdapter.notifyDataSetChanged();
                        }
						if(result.issues == null || result.issues.isEmpty()) {
							Toast.makeText(getActivity(), R.string.no_issues, Toast.LENGTH_SHORT).show();
						}
                        if(result.nextIssue != null) {
                            text.setText(getString(R.string.next_issue_in, result.nextIssue));
                            text.setVisibility(View.VISIBLE);
							// Set timer, if the option is on
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            if(prefs.getBoolean(Issues.OPTION_POLL_ISSUES, true)) {
                                NotificationsHelper.setIssuesTimer(context, result.nextIssue);
                            }
                        } else {
                            text.setVisibility(View.GONE);
                        }
					} else {
						Toast.makeText(getActivity(), R.string.could_not_get_issues, Toast.LENGTH_SHORT).show();
					}
				}
			};
		}.execute();
	}
	
	/**
	 * Dismisses all issues
	 */
//	public void dismissAllIssues() {
//		// Dismiss all
//		if(issues == null || issues.size() == 0) {
//			Toast.makeText(getActivity(), R.string.no_issues, Toast.LENGTH_SHORT).show();
//		}
//		// Show confirm dialog
//		CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
//		builder.setTitle(R.string.issues_dismiss_all_title)
//			.setMessage(getResources().getString(R.string.issues_dismiss_all_text))
//			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(final DialogInterface dialog, int which) {
//					new AsyncTask<Integer, Void, Boolean>() {
//						@Override
//						protected Boolean doInBackground(Integer... params) {
//							if(API.getInstance(getActivity()).checkLogin(getActivity())) {
//								try {
//									boolean success = false;
//									for(Issue issue : issues) {
//										success = API.getInstance(getActivity()).answerIssue(
//												issue.id, (Integer)params[0]);
//									}
//									return success;
//								} catch (IOException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//							}
//							return false;
//						}
//
//						protected void onPostExecute(Boolean result) {
//							Log.d(TAG, "Result: "+result);
//							// Reload issues
//							loadIssues();
//							dialog.dismiss();
//						};
//					}.execute(-1);
//				}
//			})
//			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					dialog.dismiss();
//				}
//			})
//			.show();
//	}
}

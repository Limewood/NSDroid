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
import java.util.ArrayList;
import java.util.List;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NotificationsHelper;
import com.limewoodmedia.nsdroid.TagParser;
import com.limewoodmedia.nsdroid.holders.RMBMessageParcelable;
import com.limewoodmedia.nsdroid.views.LoadingView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.TextKeyListener;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

public class RMBFragment extends Fragment implements OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = RMBFragment.class.getName();
	
	private View root;
	private ListView list;
	private EditText messageBox;
	private ViewGroup postArea;
	private FrameLayout dimmer;
	private LoadingView loadingView;
	private Button postClose;
	private Button postSend;
	
	private List<RMBMessage> posts;
	private ArrayAdapter<RMBMessage> listAdapter;
	private Context context;
	private String errorMessage;
	private String region;
	private RegionData data;
	private int pageOffset = 0;
	private RMBReceiver updateReceiver = new RMBReceiver();
	private boolean homeRegion = false;

	public RMBFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = getActivity();
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		root = inflater.inflate(R.layout.rmb, null, false);
		list = (ListView) root.findViewById(R.id.rmb_messages);
		dimmer = (FrameLayout) root.findViewById(R.id.dimmer);
		messageBox = (EditText) root.findViewById(R.id.rmb_post_message);
		postArea = (ViewGroup) root.findViewById(R.id.rmb_post_area);
		postClose = (Button) postArea.findViewById(R.id.rmb_post_close);
		postClose.setOnClickListener(this);
		postSend = (Button) postArea.findViewById(R.id.rmb_post_send);
		postSend.setOnClickListener(this);

        posts = new ArrayList<RMBMessage>();
        final String deletedMessage = getString(R.string.message_deleted_by_author);
		listAdapter = new ArrayAdapter<RMBMessage>(context, 0, posts) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View view;
        		TextView nation = null, msg;
        		RMBMessage post = getItem(position);
        		
        		if(convertView == null) {
        			if(post.message.compareTo(deletedMessage) == 0) {
        				// This is a deleted message
            			view = inflater.inflate(R.layout.rmb_post_deleted, null);
            			view.setTag(deletedMessage);
        			} else {
            			view = inflater.inflate(R.layout.rmb_post, null);
            			nation = (TextView) view.findViewById(R.id.rmb_poster);
            			nation.setMovementMethod(LinkMovementMethod.getInstance());
        			}
        			msg = (TextView) view.findViewById(R.id.rmb_message);
        			msg.setMovementMethod(LinkMovementMethod.getInstance());
        		}
        		else {
        			if((post.message.compareTo(deletedMessage) == 0
        					&& convertView.getTag() == deletedMessage)
        					|| (post.message.compareTo(deletedMessage) != 0
        					&& convertView.getTag() != deletedMessage)) {
        				view = convertView;
            			msg = (TextView) view.findViewById(R.id.rmb_message);
        			} else {
        				if(post.message.compareTo(deletedMessage) == 0) {
            				// This is a deleted message
                			view = inflater.inflate(R.layout.rmb_post_deleted, null);
                			view.setTag(deletedMessage);
            			} else {
                			view = inflater.inflate(R.layout.rmb_post, null);
                			nation = (TextView) view.findViewById(R.id.rmb_poster);
                			nation.setMovementMethod(LinkMovementMethod.getInstance());
            			}
            			msg = (TextView) view.findViewById(R.id.rmb_message);
            			msg.setMovementMethod(LinkMovementMethod.getInstance());
        			}
        			nation = (TextView) view.findViewById(R.id.rmb_poster);
        		}
    			if(view.getTag() == deletedMessage) {
            		msg.setText(Html.fromHtml("Post self-deleted by <a href='com.limewoodMedia.nsdroid.nation://"+post.nation+"'>"+
            				TagParser.idToName(post.nation)+"</a>."));
    			} else {
        			long timestamp = post.timestamp;
        			String time = TagParser.parseTimestamp(getContext(), timestamp);
        			String poster = "<a href=\"com.limewoodMedia.nsdroid.nation://" + post.nation + "\">" +
        							TagParser.idToName(post.nation) + "</a><br />" + time;
					if(post.embassy != null) {
						poster += " from<br /><a href=\"com.limewoodMedia.nsdroid.region://" + TagParser.nameToId(post.embassy) + "\">" +
								post.embassy + "</a>";
					}
    				nation.setText(Html.fromHtml(poster));
            		msg.setText(TagParser.parseTagsFromHtml(post.message, true));
    			}
        		
        		return view;
        	}
        };
        list.setAdapter(listAdapter);
        registerForContextMenu(list);
		
		return root;
	}
	
	public void setRegionName(String name) {
		region = name;
	}
	
	public int getPageOffset() {
		return this.pageOffset;
	}

	/**
	 * Shows the text box and buttons for posting a message on the RMB
	 */
	public void prepareMessage() {
		postArea.setVisibility(View.VISIBLE);
		messageBox.requestFocus();
	}
	
	@Override
	public void onResume() {
        getActivity().registerReceiver(updateReceiver, new IntentFilter(NotificationsHelper.RMB_UPDATE_ACTION));
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(updateReceiver);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.rmb_post_close:
			closeMessageBox();
			break;
			
		case R.id.rmb_post_send:
			if(messageBox.getText().length() == 0) {
				Toast.makeText(context, R.string.rmb_post_no_text, Toast.LENGTH_SHORT).show();
				break;
			}
			dimmer.getForeground().setAlpha(150);
	    	startLoading(R.string.loading_on_rmb_post);
	    	messageBox.setEnabled(false);
	    	postClose.setEnabled(false);
	    	postSend.setEnabled(false);
			new AsyncTask<String, Void, Boolean>() {
				@Override
				protected Boolean doInBackground(String... params) {
					try {
						if(API.getInstance(context).checkLogin(getActivity())) {
							return API.getInstance(context).postToRMB(
									region, params[0]);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					return false;
				}
				
				protected void onPostExecute(Boolean result) {
					LoadingHelper.stopLoading(loadingView);
	        		dimmer.getForeground().setAlpha(0);
	    	    	messageBox.setEnabled(true);
	    	    	postClose.setEnabled(true);
	    	    	postSend.setEnabled(true);
					if(result) {
						Toast.makeText(context, R.string.message_posted, Toast.LENGTH_SHORT).show();
						closeMessageBox();
				    	TextKeyListener.clear(messageBox.getText());
						// Update RMB messages
						loadMessages();
					} else {
						Toast.makeText(context, R.string.message_error, Toast.LENGTH_SHORT).show();
					}
				}
        	}.execute(messageBox.getText().toString());
			break;
		}
	}
	
	private void closeMessageBox() {
		postArea.setVisibility(View.GONE);
		list.requestFocus();
	}
	
	public void setMessages(List<RMBMessage> messages, int offset, boolean homeRegion) {
		this.homeRegion = homeRegion;
		if(messages != null) {
			this.pageOffset = offset;
			if(this.posts.size() > 0) {
				this.posts.clear();
			}
			this.posts.addAll(messages);
			this.listAdapter.notifyDataSetChanged();
			dimmer.getForeground().setAlpha(0);

			// Save last message details
			if(posts.size() > 0 && homeRegion) {
				Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
				RMBMessage post = posts.get(posts.size()-1);
				edit.putLong("update_region_messages_timestamp", post.timestamp);
				edit.putString("update_region_messages_nation", post.nation);
				edit.commit();
			}
		}
		LoadingHelper.stopLoading(loadingView);
	}
	
	public void onBeforeLoad() {
		this.posts.clear();
		dimmer.getForeground().setAlpha(150);
    	startLoading(R.string.loading_rmb);
	}
	
	private void startLoading(int res) {
		if(loadingView == null) {
			loadingView = (LoadingView) root.findViewById(R.id.loading);
		}
		LoadingHelper.startLoading(loadingView, res, getActivity());
	}
	
	public void loadMessages() {
		loadMessages(0);
	}
	
	public void loadMessages(final int offsetPage) {
		dimmer.getForeground().setAlpha(150);
    	startLoading(R.string.loading_rmb);
    	errorMessage = getResources().getString(R.string.general_error);
        new AsyncTask<Void, Void, Boolean>() {
        	protected Boolean doInBackground(Void...params) {
				try {
					data = API.getInstance(context).getRegionInfo(region,
							RegionData.Shards.MESSAGES
	                			.setArgument(RegionData.Shards.Arguments.MESSAGES_OFFSET,
	                					Integer.toString(offsetPage*10)));
	                
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
        		dimmer.getForeground().setAlpha(0);
        		if(result) {
        			if(data.messages.size() > 0) {
        				posts.clear();
        				setMessages(data.messages, offsetPage, homeRegion);
        			}
        		}
        		else {
        			Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_rmb, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info == null) {
            return super.onContextItemSelected(item);
        }
        switch(item.getItemId()) {
            case R.id.context_menu_quote:
                // Quote RMB post
                RMBMessage post = listAdapter.getItem(info.position);
                messageBox.setText(messageBox.getText()+"[quote="+post.nation+";"+post.id+"]"+post.message+"[/quote]\n");
                postArea.setVisibility(View.VISIBLE);
                messageBox.requestFocus();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private class RMBReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Update RMB
			Parcelable[] pArr = intent.getParcelableArrayExtra(
					"com.limewoodMedia.nsdroid.holders.RMBMessageParcelable");
			if(pArr != null && pArr.length > 0) {
				List<RMBMessage> messages = new ArrayList<RMBMessage>(pArr.length);
				for(Parcelable p : pArr) {
					messages.add(((RMBMessageParcelable)p).msg);
				}
				// This is always the home region
				setMessages(messages, 0, true);
			}
			// Set new alarm
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			NotificationsHelper.setAlarmForRMB(getActivity(),
					Integer.parseInt(prefs.getString("rmb_update_interval", "-1")));
		}
	}
}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodmedia.nsdroid.API;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.db.NationsDatabase;

import org.xmlpull.v1.XmlPullParserException;

/**
 * Lists all saved nations
 * @author Joakim Lindskog
 *
 */
public class NationsList extends AppCompatActivity {
	private static final String TAG = NationsList.class.getName();
	
	private ArrayAdapter<String> adapter;
	private List<String> nations;
	private ListView listView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.nations_list);

		this.listView = (ListView) findViewById(R.id.list);
		this.nations = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, 0, nations) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view;
        		TextView name;
        		String n;
        		
        		if(convertView == null) {
        			view = getLayoutInflater().inflate(R.layout.nations_list_item, null);
        		}
        		else {
        			view = convertView;
        		}
    			name = (TextView) view.findViewById(R.id.name);
        		
        		// Nation name
    			n = getItem(position);
    			name.setText(n);
    			view.setTag(n);
    			Log.d(TAG, "n: "+n);
    			if(n.equals(NationInfo.getInstance(NationsList.this).getName())) {
    				view.setBackgroundColor(getResources().getColor(R.color.active_nation));
    			} else {
    				view.setBackgroundColor(Color.TRANSPARENT);
    			}
        		
        		return view;
			}
		};
		listView.setAdapter(adapter);
		
		listView.setLongClickable(true);
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
				final String name = (String) v.getTag();

				AlertDialog.Builder builder = new AlertDialog.Builder(NationsList.this);
				builder.setTitle(R.string.nations_list_remove_title);
				builder.setMessage(R.string.nations_list_remove_message);
				builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, int which) {
						// Remove nation from database
						// If this is the current nation, switch to the first nation
						// If it is the only nation, go back to welcome screen
						new AsyncTask<Void, Void, Integer>() {
							@Override
							protected Integer doInBackground(Void... params) {
								NationsDatabase db = NationsDatabase.getInstance(NationsList.this);
								boolean removed = db.removeNation(name);
								if (!removed) {
									return -1;
								}
								List<String> nations = db.getAllNations();
								NationInfo info = NationInfo.getInstance(NationsList.this);
								if (name.equals(info.getName())) {
									info.setName(null);
									info.setFlag(null);
									if (nations.size() == 0) {
										// No more nations
										return -2;
									} else {
										// Switch to first nation
										return 1;
									}
								}

								return 0;
							}

							protected void onPostExecute(Integer result) {
								dialog.dismiss();
								switch (result) {
									case -1: // Failed to remove nation
										Toast.makeText(NationsList.this, R.string.remove_nation_failed, Toast.LENGTH_SHORT).show();
										break;

									case -2: {// No more nations
										Intent i = new Intent(NationsList.this, Welcome.class);
										i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										startActivity(i);
										break;
									}

									case 0: // Success
										listNations();
										break;

									case 1: {
										listNations(true);
										final NationInfo info = NationInfo.getInstance(NationsList.this);
										info.setName(nations.get(0));
										new AsyncTask<Void, Void, Void>() {
											@Override
											protected Void doInBackground(Void... params) {
												NationData nData = null;
												try {
													nData = API.getInstance(NationsList.this).getNationInfo(info.getId(), NationData.Shards.WA_STATUS);
													info.setWAStatus(nData.worldAssemblyStatus);
												} catch (RateLimitReachedException e) {
													e.printStackTrace();
												} catch (UnknownNationException e) {
													e.printStackTrace();
												} catch (XmlPullParserException e) {
													e.printStackTrace();
												} catch (IOException e) {
													e.printStackTrace();
												}
												return null;
											}
										}.execute();

										Intent i = new Intent(NationsList.this, NSDroid.class);
										i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										startActivity(i);
										break;
									}
								}
							}

							;
						}.execute();
					}
				});
				builder.show();

				return true;
			}
		});
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListItemClick(listView, view, position, id);
			}
		});

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
		
		listNations();
	}
	
	private void listNations() {
		listNations(false);
	}
	
	private void listNations(boolean wait) {
		AsyncTask<Void, Void, List<String>> task = new AsyncTask<Void, Void, List<String>>() {
			@Override
			protected List<String> doInBackground(Void... params) {
				NationsDatabase db = NationsDatabase.getInstance(NationsList.this);
				List<String> nations = db.getAllNations();
				
				return nations;
			}
			
			protected void onPostExecute(List<String> nations) {
				Log.d(TAG, "Nations: "+nations.size());
				NationsList.this.nations.clear();
				NationsList.this.nations.addAll(nations);
				adapter.notifyDataSetChanged();
			};
		};
		try {
			task.execute().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	protected void onListItemClick(ListView l, final View v, int position, long id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.nations_list_switch_title);
		builder.setMessage(R.string.nations_list_switch_message);
		builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Switch to this nation
				final NationInfo info = NationInfo.getInstance(NationsList.this);
				info.setName((String)v.getTag());
				info.setFlag(null);
				new AsyncTask<Void, Void, Void>() {
                    private String errorMessage;

					@Override
					protected Void doInBackground(Void... params) {
                        NationData nData = null;
                        try {
                            nData = API.getInstance(NationsList.this).getNationInfo(info.getId(), NationData.Shards.WA_STATUS);
                            info.setWAStatus(nData.worldAssemblyStatus);
                            API.getInstance(NationsList.this).getHomeRegionInfo(NationsList.this);
                        } catch (RateLimitReachedException e) {
                            e.printStackTrace();
                            errorMessage = getString(R.string.rate_limit_reached);
                        } catch (UnknownNationException e) {
                            e.printStackTrace();
                            errorMessage = getString(R.string.unknown_nation, e.getNation());
                        } catch (UnknownRegionException e) {
                            e.printStackTrace();
                            errorMessage = getString(R.string.unknown_region, e.getRegion());
                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                            errorMessage = getResources().getString(R.string.xml_parser_exception);
                        } catch (IOException e) {
                            e.printStackTrace();
                            errorMessage = getResources().getString(R.string.api_io_exception);
                        }
                        return null;
					}

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        if(errorMessage != null) {
                            Toast.makeText(NationsList.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
				
				dialog.dismiss();
				
				Intent i = new Intent(NationsList.this, NSDroid.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(i);
			}
		});
		builder.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_nations_list, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_add_nation:
			Intent i = new Intent(this, Welcome.class);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}

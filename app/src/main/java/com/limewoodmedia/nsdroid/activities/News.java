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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.limewoodmedia.nsdroid.views.LoadingView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * News activity
 */
public class News extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	private static final String TAG = News.class.getName();
    private static final String NEWS_URL = "http://www.nationstates.net/pages/news/index.rss";
	
	private WebView webView;
    private String errorMessage;
    private SimpleDateFormat inputDateFormat;
    private SimpleDateFormat outputDateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.news);

        Utils.setupNavigationDrawer(this);
        
        // Fetch flag
        LoadingHelper.loadHomeFlag(this);

        this.webView = (WebView) findViewById(R.id.web_view);

        this.inputDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        this.outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
            	loadData();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads data from the NS API into the overview panels
     */
    private void loadData() {
    	errorMessage = getResources().getString(R.string.general_error);
    	// Show loading animation
    	final LoadingView loadingView = (LoadingView) findViewById(R.id.loading);
    	LoadingHelper.startLoading(loadingView);
    	new AsyncTask<Void, Void, Boolean>() {
            StringBuilder html;

            @Override
            protected void onPreExecute() {
                html = new StringBuilder();
            }

            protected Boolean doInBackground(Void...params) {
				try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser xpp = factory.newPullParser();

                    HttpGet get = new HttpGet(NEWS_URL);
                    HttpClient client = new DefaultHttpClient();
                    HttpResponse response = client.execute(get);

	                xpp.setInput(response.getEntity().getContent(), "UTF-8");
                    String tagName;
                    while (xpp.next() != XmlPullParser.END_DOCUMENT) {
                        switch (xpp.getEventType()) {
                            case XmlPullParser.START_TAG:
                                tagName = xpp.getName().toLowerCase();
                                if (tagName.equals("item")) {
                                    html.append(parseItem(xpp));
                                }
                                else {
                                    Log.w(TAG, "Unknown rss tag: " + tagName);
                                }
                                break;
                        }
                    }
	                
	                return true;
				} catch (RuntimeException e) {
					e.printStackTrace();
					errorMessage = e.getMessage();
				} catch (XmlPullParserException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                }

                return false;
        	}
        	
        	protected void onPostExecute(Boolean result) {
        		// Remove loading animation
				LoadingHelper.stopLoading(loadingView);
        		if(result) {
                    webView.getSettings().setUseWideViewPort(false);
                    webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
					webView.loadDataWithBaseURL("http://www.nationstates.net",
                            "<link rel='stylesheet' href='/ns_v1422597706.css'>"
                            +"<style>h2 {color: #008000;}</style>"
                            +html.toString(), "text/html", "utf-8", null);
        		}
        		else {
					Toast.makeText(News.this, errorMessage, Toast.LENGTH_SHORT).show();
        		}
        	}
        }.execute();
    }

    private String parseItem(XmlPullParser xpp) throws IOException, XmlPullParserException {
        String title = "";
        Date date = new Date();
        String desc = "";
        String tagName;
        loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
            switch (xpp.getEventType()) {
                case XmlPullParser.START_TAG:
                    tagName = xpp.getName().toLowerCase();
                    if (tagName.equals("title")) {
                        title = xpp.nextText();
                    }
                    else if(tagName.equals("description")) {
                        desc = xpp.nextText().replace("\n", " ");
                    }
                    else if(tagName.equals("link")) {
                        try {
                            date = inputDateFormat.parse(xpp.nextText().replace("http://www.nationstates.net/page=news/", "").substring(0, 10));
                        } catch (ParseException e) {
                            e.printStackTrace();
                            date = new Date();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = xpp.getName().toLowerCase();
                    if (tagName.equals("item")) {
                        break loop;
                    }
            }
        return "<h2>"+title+"</h2><p><font color='grey'>"+ outputDateFormat.format(date)+"</font></p><p>"+desc+"</p><br/><br/>";
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

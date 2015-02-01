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
package com.limewoodmedia.nsdroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xmlpull.v1.XmlPullParserException;

import com.limewoodMedia.nsapi.INSAPI;
import com.limewoodMedia.nsapi.NSAPI;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.holders.DossierData;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.holders.WADataParcelable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.text.InputType;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.Toast;

/**
 * This class handles calls to the NationStates API
 * as well as login and page scraping where there is no API endpoint
 * @author Joakim Lindskog
 *
 */
public class API {
	private static final String TAG = API.class.getName();
	private static final String USER_AGENT = "NSDroid Android app for NationStates " +
			"(code.google.com/p/nsdroid/), created by Laevendell (joakim@limewoodmedia.com)";
	private static final int VERSION = 7;
	private static API instance;
	
	public static synchronized API getInstance(Context context) {
		if(instance == null) {
			instance = new API(context);
		}
		return instance;
	}
	
	private Context context;
	private HttpContext httpContext;
	private ShowLoginRunnable showLoginRunnable;
	private RegionPasswordRunnable regionPasswordRunnable;
	private INSAPI nsapi;
	
	private API(Context context) {
		this.context = context;
		this.showLoginRunnable = new ShowLoginRunnable();
		this.regionPasswordRunnable = new RegionPasswordRunnable();
		this.nsapi = new NSAPI();
		this.nsapi.setUserAgent(USER_AGENT);
		this.nsapi.setVersion(VERSION);
	}
	
	private HttpClient getClient() {
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		return client;
	}
	
	public void setUserNation(String userNation) {
		this.nsapi.setUserAgent(USER_AGENT + "; used by nation "+userNation);
	}
	
	public String getUserAgent() {
		return nsapi.getUserAgent();
	}
	
	/**
	 * Fetches nation info
	 * @param nation nation id
	 * @param shards fetching options (shards)
	 * @return an NationDataParcelable object with the info
	 * @throws XmlPullParserException if there was a problem with the xml parser
	 * @throws IOException if there was a network problem
	 * @throws RateLimitReachedException if the rate limit was reached
	 * @throws UnknownNationException if the nation was not found
	 */
	public synchronized NationDataParcelable getNationInfo(String nation, NationData.Shards...shards)
			throws RateLimitReachedException, UnknownNationException {
        return new NationDataParcelable(nsapi.getNationInfo(nation, shards));
	}
	
	/**
	 * Fetches region info
	 * @param region region id
	 * @param shards fetching options (shards)
	 * @return an NSData object with the info
	 * @throws XmlPullParserException if there was a problem with the xml parser
	 * @throws IOException if there was a network problem
	 * @throws UnknownRegionException if the region could not be found
	 * @throws RateLimitReachedException if the rate limit was reached
	 */
	public synchronized RegionDataParcelable getRegionInfo(String region, RegionData.Shards...shards)
			throws RateLimitReachedException, UnknownRegionException {

        return new RegionDataParcelable(nsapi.getRegionInfo(region, shards));
	}

    /**
     * Fetches WA info
     * @param council which council to get info from
     * @param shards fetching options (shards)
     * @return an NSData object with the info
     * @throws XmlPullParserException if there was a problem with the xml parser
     * @throws IOException if there was a network problem
     * @throws UnknownRegionException if the region could not be found
     * @throws RateLimitReachedException if the rate limit was reached
     */
    public synchronized WADataParcelable getWAInfo(WACouncil council, WAData.Shards...shards)
            throws RateLimitReachedException, UnknownRegionException {

        return new WADataParcelable(nsapi.getWAInfo(council, shards));
    }
	
	public synchronized RegionDataParcelable getHomeRegionInfo(Context context, RegionData.Shards...shards)
			throws RateLimitReachedException, UnknownRegionException {
		// Check if region is cached
		NationInfo info = NationInfo.getInstance(context);
		String region = info.getRegionId();
		RegionData rData;
		if(region == null) {
			// Check region
			NationData nData = nsapi.getNationInfo(info.getId(), NationData.Shards.REGION);
			region = nData.region;
			// Fetch region info
			rData = nsapi.getRegionInfo(region, shards);
			// Cache region
			info.setRegionId(region);
		} else {
			// Add nations shard to check that the nation is indeed here
			RegionData.Shards[] args = new RegionData.Shards[shards.length+1];
			int i=0;
			for(RegionData.Shards shard : shards) {
				args[i] = shard;
				i++;
			}
			args[args.length-1] = RegionData.Shards.NATIONS;
			// Fetch region info
			rData = nsapi.getRegionInfo(region, args);
			Comparator<String> comparator = new AlphabeticComparator();
			Arrays.sort(rData.nations, comparator);
			if(Arrays.binarySearch(rData.nations, info.getId(), comparator) < 0) {
				// The nation has moved
				NationData nData = nsapi.getNationInfo(info.getId(), NationData.Shards.REGION);
				region = nData.region;
				// Fetch region info
				rData = nsapi.getRegionInfo(region, shards);
				// Cache region
				info.setRegionId(region);
			}
		}

        return new RegionDataParcelable(rData);
	}
	
	/**
	 * Posts a message to the RMB of your region
	 * @param region the region id
	 * @param message the message to post
	 * @return true if successful
	 * @throws IOException if there was a problem with the connection
	 */
	public boolean postToRMB(String region, String message) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/region=" + region);
		
		try {
	        HttpResponse response = client.execute(httpGet, httpContext);
	        StringBuilder builder = new StringBuilder();
	        BufferedReader reader = null;
		    try {
		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
		        		.getContent(), "UTF-8"));
		        for (String line; (line = reader.readLine()) != null;) {
		            builder.append(line.trim());
		        }
		    } finally {
		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		    }

			String chkPrefix = "<input type=\"hidden\" name=\"chk\" value=\"";
			String chk = builder.substring(builder.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Chk: "+chk);
			
			// Post
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=lodgermbpost/region="
					+region);

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
	        nameValuePairs.add(new BasicNameValuePair("chk", chk));
	        nameValuePairs.add(new BasicNameValuePair("message", message));
	        nameValuePairs.add(new BasicNameValuePair("lodge_message", "+Lodge+Message+"));
	        nameValuePairs.add(new BasicNameValuePair("preview", "+Preview+"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

	        response = client.execute(httpPost, httpContext);
	        builder = new StringBuilder();
	        reader = null;
		    try {
		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
		        		.getContent(), "UTF-8"));
		        for (String line; (line = reader.readLine()) != null;) {
		            builder.append(line.trim());
		        }
		    } finally {
		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		    }
		    if(builder.toString().contains("<p class=\"info\">Your message has been lodged!")){
		    	return true;
		    }
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	/**
	 * Retrieves all your current issues
	 * NOTE: Requires login
	 * @return current issues
	 */
	public List<Issue> getIssues() {
		if(!isLoggedIn()) {
			return null;
		}
		
		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/page=dilemmas");
		
		try {
	        HttpResponse response = client.execute(httpGet, httpContext);
	        StringBuilder builder = new StringBuilder();
	        BufferedReader reader = null;
		    try {
		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
		        		.getContent(), "UTF-8"));
		        for (String line; (line = reader.readLine()) != null;) {
		            builder.append(line.trim());
		        }
		    } finally {
		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		    }

		    List<Issue> issues = new ArrayList<Issue>();
		    
			String prefix = "<ul class=\"dilemmalist\">";
			String dilemmas = builder.substring(builder.indexOf(prefix)+prefix.length());
			dilemmas = dilemmas.substring(0, dilemmas.indexOf("</ul>"));
			
			Issue issue;
			String[] items = dilemmas.split("\\<li\\>");
			Pattern pattern = Pattern.compile(
					"\\<a href=\"page=show_dilemma/dilemma=(\\d+)\"\\>(\\<strong\\>)?(.+?)" +
							"(\\</strong\\>)?\\</a\\>(.+)",
					Pattern.DOTALL);
			String status;
            Log.d(TAG, "Issues: "+items.length);
			for(String item : items) {
				if(item.length() == 0) {
					continue;
				}
				issue = new Issue();
				try {
					issue.id = Integer.parseInt(pattern.matcher(item).replaceAll("$1"));
					issue.name = pattern.matcher(item).replaceAll("$3");
					status = pattern.matcher(item).replaceAll("$5").trim()
							.replace("[", "").replace("]", "");
					issue.dismissed = status.equalsIgnoreCase("dismissed");
					issue.pending = status.equalsIgnoreCase("legislation pending");
					Log.d(TAG, "Issue: "+issue.id+" - "+issue.name);
					issues.add(issue);
				} catch(NumberFormatException e) {
                    e.printStackTrace();
				}
			}
			return issues;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	/**
	 * Retrieves the issue with specified id
	 * NOTE: Requires login
	 * @param id the id of the issue
	 * @return the issue or null if not found
	 */
	public Issue getIssue(int id) {
		if(!isLoggedIn()) {
			return null;
		}
		
		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/page=show_dilemma/dilemma="+id);
		
		try {
	        HttpResponse response = client.execute(httpGet, httpContext);
	        StringBuilder builder = new StringBuilder();
	        BufferedReader reader = null;
		    try {
		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
		        		.getContent(), "UTF-8"));
		        for (String line; (line = reader.readLine()) != null;) {
		            builder.append(line.trim());
		        }
		    } finally {
		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		    }

		    Issue issue = new Issue();
		    issue.id = id;
		    
		    // Issue name
		    String prefix = "<div class=\"dpaper4\"><p>";
			String text = builder.substring(builder.indexOf(prefix)+prefix.length());
			text = text.substring(0, text.indexOf("</div>"));
			issue.name = text;
		    
		    // Issue text
			prefix = "<form method=\"POST\" action=\"page=show_dilemma/dilemma=";
			text = builder.substring(builder.indexOf(prefix)+prefix.length());
			text = text.substring(0, text.indexOf("</p>"));
			
			Pattern pattern = Pattern.compile("(\\d+)\"\\>\\<p\\>(.+)", Pattern.DOTALL);
			text = pattern.matcher(text).replaceAll("$2");
			issue.text = text;
			
			// Issue choices
			prefix = "<ol class=\"diloptions\">";
			text = builder.substring(builder.indexOf(prefix)+prefix.length());
			text = text.substring(0, text.indexOf("</ol>"));
			
			String[] items = text.split("\\<li( class=\"chosendiloption\")?\\>");
			pattern = Pattern.compile(
					"\\<p\\>(.+?)\\n?(\\<p\\>)(\\<button type=\"submit\" name=\"choice-(\\d+)\")?(.+)",
					Pattern.DOTALL);
			List<String> choices = new ArrayList<String>();
			String choice;
			for(String item : items) {
				choice = pattern.matcher(item).replaceAll("$1");
				if(choice.trim().length() > 0) {
					choices.add(choice);
				}
				Log.d(TAG, "Choice: "+choice);
			}
			issue.choices = choices;
			
			// Selected issue
			int selected = -1;
			prefix = "<h5>The Government Position</h5>";
			text = builder.substring(builder.indexOf(prefix)+prefix.length());
			text = text.substring(0, text.indexOf('.'));
			
			pattern = Pattern.compile("(\n?)\\<p\\>(.+)(\\d+)", Pattern.DOTALL);
			try {
				selected = Integer.parseInt(pattern.matcher(text).replaceAll("$3"));
			} catch(NumberFormatException e) {
				// No issue selected
//				e.printStackTrace();
			}
			
			// Check if dismissed
			prefix = "<button type=\"submit\" name=\"choice--1\"";
			if(builder.indexOf(prefix) == -1) {
				issue.dismissed = true;
			}
			
			issue.selectedChoice = selected;
			return issue;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return null;
	}
	
	/**
	 * Answers an issue
	 * @param issueId the id of the issue
	 * @param choice the choice made
	 * @return true if successful
	 * @throws IOException if there was a problem with the connection
	 */
	public boolean answerIssue(int issueId, int choice) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		// Post
		try {
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=show_dilemma/dilemma="+issueId);

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	        nameValuePairs.add(new BasicNameValuePair("choice-"+choice, "Accept"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

	        client.execute(httpPost, httpContext);
			return true;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	/**
	 * @return the dossier data
	 */
	public DossierData getDossier() {
		if(!isLoggedIn()) {
			return null;
		}
		
		DossierData data = new DossierData();
		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/page=dossier");
		
		try {
	        HttpResponse response = client.execute(httpGet, httpContext);
	        StringBuilder builder = new StringBuilder();
	        BufferedReader reader = null;
		    try {
		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
		        		.getContent(), "UTF-8"));
		        for (String line; (line = reader.readLine()) != null;) {
		            builder.append(line.trim());
		        }
		    } finally {
		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
		    }

		    // Dossier nations
		    String prefix = "<table class=\"shiny\"";
			String text = builder.substring(builder.indexOf(prefix)+prefix.length());
			prefix = "<tbody>";
			text = text.substring(text.indexOf(prefix)+prefix.length());
			String nText = text.substring(0, text.indexOf("</tbody>"));
			String[] arr = nText.split("<tr>");
			NationDataParcelable[] nations = new NationDataParcelable[arr.length];
			NationDataParcelable ndp;
			int i=0;
			String[] nInfo, nArr;
			for(String s : arr) {
				System.out.println("String: "+s);
				if(s.trim().length() == 0) continue;
				ndp = new NationDataParcelable();
				if(s.split("<td").length == 3) {
					ndp.name = s.substring(s.lastIndexOf("<td>")+4, s.lastIndexOf("</td>"));
				} else {
					ndp.name = TagParser.idToName(s.substring(s.indexOf("href=\"nation=")+13, s.indexOf("\" class=\"nlink")));
					nInfo = s.split("<td align=\"center\">");
					ndp.category = nInfo[2].substring(0, nInfo[2].indexOf("</td>"));
					nArr = nInfo[3].split("<br>");
					ndp.lastActivity = nArr[0].replace("Active ", "")
							.replace("minutes", "m")
							.replace("minute", "m")
							.replace("days", "d")
							.replace("day", "d")
							.replace("seconds", "s")
							.replace("hours", "h")
							.replace("hour", "h");
					ndp.region = nArr[1].substring(nArr[1].indexOf('>')+1, nArr[1].indexOf("</a>"));
				}
				nations[i] = ndp;
				i++;
			}
			data.nations = nations;
		    
		    // Dossier regions
//			prefix = "<table class=\"shiny\"";
//			text = text.substring(text.indexOf(prefix)+prefix.length());
			prefix = "<tbody>";
			text = text.substring(text.indexOf(prefix)+prefix.length());
			String rText = text.substring(0, text.indexOf("</tbody>"));
			arr = rText.split("<tr>");
			RegionDataParcelable[] regions = new RegionDataParcelable[arr.length];
			RegionDataParcelable rdp;
			i=0;
			String[] rArr;
			for(String s : arr) {
				if(s.trim().length() == 0) continue;
				rdp = new RegionDataParcelable();
				nArr = s.split("<td>");
				rArr = nArr[1].split("<td align=\"center\">");
				rdp.name = rArr[0].substring(rArr[0].indexOf('>')+1, rArr[0].indexOf("</a>"));
				rdp.numNations = Integer.parseInt(rArr[1].substring(0, rArr[1].indexOf('<')));
				if(nArr[2].contains("<a href=")) {
					rdp.delegate = nArr[2].substring(nArr[2].indexOf("<a href=\"nation=")+16,
							nArr[2].indexOf("\" class"));
				} else {
					rdp.delegate = nArr[2].substring(0, nArr[2].indexOf('<'));
				}
				regions[i] = rdp;
				i++;
			}
			
			data.regions = regions;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
		return data;
	}

	/**
	 * Adds a nation to your dossier
	 * @param nation the name of the nation
	 * @return true if successful
	 * @throws IOException
	 */
	public boolean addNationToDossier(String nation) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		// Post
		try {
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=dossier");

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("nation", nation));
	        nameValuePairs.add(new BasicNameValuePair("action", "add"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

	        client.execute(httpPost, httpContext);
	        return true;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}

	/**
	 * Removes a nation from your dossier
	 * @param nation the name of the nation
	 * @return true if successful
	 * @throws IOException
	 */
	public boolean removeNationFromDossier(String nation) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		// Post
		try {
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=dossier");

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("nation", nation));
	        nameValuePairs.add(new BasicNameValuePair("action", "remove"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

	        client.execute(httpPost, httpContext);
	        return true;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}

	/**
	 * Adds a region to your dossier
	 * @param region the name of the region
	 * @return true if successful
	 * @throws IOException
	 */
	public boolean addRegionToDossier(String region) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		// Post
		try {
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=dossier/action=add/region="+region);

	        client.execute(httpPost, httpContext);
	        return true;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}

	/**
	 * Removes a region from your dossier
	 * @param region the name of the region
	 * @return true if successful
	 * @throws IOException
	 */
	@SuppressLint("DefaultLocale")
	public boolean removeRegionFromDossier(String region) throws IOException {
		if(!isLoggedIn()) {
			return false;
		}
		
		HttpClient client = getClient();
		// Post
		try {
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=dossier");
			
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("remove_region_"+region.replace(' ', '_').toLowerCase(), "true"));
	        nameValuePairs.add(new BasicNameValuePair("remove_from_region_dossier", "Remove Marked Regions"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));
	        
	        HttpResponse response = client.execute(httpPost, httpContext);
	        
	        if(response.getStatusLine().getStatusCode() != 200) {
		        httpPost = new HttpPost("http://www.nationstates.net/page=dossier");
				
		        nameValuePairs.clear();
		        nameValuePairs.add(new BasicNameValuePair("remove_region_"+region, "true"));
		        nameValuePairs.add(new BasicNameValuePair("remove_from_region_dossier", "Remove Marked Regions"));
		        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));
	        }
	        
	        client.execute(httpPost, httpContext);
	        return true;
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		return false;
	}
	
	/**
	 * Performs login
	 * @param nation nation name
	 * @param password nation password
	 * @return true if successful
	 */
	public boolean login(String nation, String password) {
		if(password == null) {
			return false;
		}
		
	    HttpClient client = getClient();

	    HttpPost httpPost = new HttpPost("http://www.nationstates.net/");
	    if(httpContext == null) {
		    httpContext = new BasicHttpContext();
		    BasicCookieStore cookieStore = new BasicCookieStore();
		    httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    }

	    try {
	        // Add user name and password
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("logging_in", "1"));
	        nameValuePairs.add(new BasicNameValuePair("nation", nation));
	        nameValuePairs.add(new BasicNameValuePair("password", password));
	        nameValuePairs.add(new BasicNameValuePair("autologin", "no"));
	        nameValuePairs.add(new BasicNameValuePair("submit", "Login"));
	        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

	        /* HttpResponse response = */client.execute(httpPost, httpContext);
//	        StringBuilder builder = new StringBuilder();
//	        BufferedReader reader = null;
//		    try {
//		        reader = new BufferedReader(new InputStreamReader(response.getEntity()
//		        		.getContent(), "UTF-8"));
//		        for (String line; (line = reader.readLine()) != null;) {
//		            builder.append(line.trim());
//		        }
//		    } finally {
//		        if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
//		    }
//			Log.d(TAG, "\n\n");
//		    int len = builder.length();
//		    for(int i=0; i<len; i+=1024) {
//	            if(i+1024<len)
//	                Log.d(TAG, builder.substring(i, i+1024));
//	            else
//	                Log.d(TAG, builder.substring(i, len));
//	        }
		    // <p class="error">Incorrect password. Please try again.<fieldset>
	        return isLoggedIn();
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return false;
	}
	
	public boolean isLoggedIn() {
		if(httpContext == null) {
			return false;
		}
		List<Cookie> cookies = ((BasicCookieStore)httpContext.getAttribute(ClientContext.COOKIE_STORE)).getCookies();
		for(Cookie cookie : cookies) {
			if(cookie.getDomain().equals(".nationstates.net") && cookie.getName().equals("pin")
					&& !cookie.getValue().equals("-1") && !cookie.isExpired(new Date())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Call this from an activity to show login dialog if not logged in
	 * NOTE: Must be called from background thread!
	 * @return true if login succeeded
	 */
	public boolean checkLogin(final Activity activity) {
		Log.d(TAG, "Check login");
		if(isLoggedIn()) {
			return true;
		}
		// Try to log in
		Log.d(TAG, "Try to log in");
		final String pswd = NationInfo.getInstance(context).getPassword();
		Log.d(TAG, "Password "+pswd+" activity: "+activity);
		if(pswd != null && login(NationInfo.getInstance(context).getId(), pswd)) {
			return true;
		}
		else if(activity != null) {
			synchronized(showLoginRunnable) {
				Log.d(TAG, "Run login runnable");
				showLoginRunnable.setActivity(activity);
				activity.runOnUiThread(showLoginRunnable);
				try {
					Log.d(TAG, "Wait for login runnable");
					showLoginRunnable.wait();

					boolean result = false;
					while(!showLoginRunnable.isDismissed()) {
						Log.d(TAG, "Try to login again");
						String pass = showLoginRunnable.getEnteredPassword();
						if(pass != null) {
							result = login(NationInfo.getInstance(activity).getId(), pass);
							if(result) {
								// Cache password
								NationInfo.getInstance(activity).setPassword(pass, false);
								showLoginRunnable.dismissDialog();
							}
							else if(!showLoginRunnable.isDismissed()) {
								// Show error in dialog
								showLoginRunnable.setLoginError();
							}
						}
						showLoginRunnable.wait();
					}
					
					Log.d(TAG, "Return value: "+result);
					return result;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	private class ShowLoginRunnable implements Runnable {
		private Activity activity;
		private AlertDialog dialog;
		private String enteredPassword;
		private boolean error;
		private boolean dismissed;
		
		public void setActivity(Activity activity) {
			this.activity = activity;
		}
		
		public void dismissDialog() {
			dialog.dismiss();
		}
		
		public String getEnteredPassword() {
			return enteredPassword;
		}
		
		public void setLoginError() {
			error = true;
			activity.runOnUiThread(this);
		}
		
		public boolean isDismissed() {
			return dismissed;
		}
		
		@Override
		public void run() {
			dismissed = false;
			if(error) {
//				dialog.setMessage(activity.getText(R.string.enter_password_text) + "\n" +
//						activity.getText(R.string.enter_password_error));
				Toast.makeText(activity, R.string.enter_password_error, Toast.LENGTH_SHORT).show();
				error = false;
				return;
			}
			Log.d(TAG, "Running login runnable");
			
			CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

			alert.setTitle(R.string.enter_password_title);
			alert.setMessage(R.string.enter_password_text);

			// Set an EditText view to get user input 
			final EditText input = new EditText(activity);
			input.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			alert.setView(input);

			alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, int whichButton) {
					final String pass = input.getText().toString();
					Log.d(TAG, "Pass: "+pass);
					Log.d(TAG, "Pass is "+(pass != null)+" "+(pass.length() > 0));
					if(pass != null && pass.length() > 0) {
						Log.d(TAG, "Create asynctask");
						enteredPassword = pass;
						synchronized(showLoginRunnable) {
							showLoginRunnable.notify();
						}
					}
				}
			});

			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing
					dialog.dismiss();
				}
			});
			
			Log.d(TAG, "Show dialog");
			dialog = alert.show();
			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					Log.d(TAG, "Notify login runnable");
					dismissed = true;
					synchronized(showLoginRunnable) {
						showLoginRunnable.notify();
					}
				}
			});
		}
	}

	public void logout() {
		if(!isLoggedIn()) {
			return;
		}
		HttpClient client = getClient();

	    HttpGet httpGet = new HttpGet("http://www.nationstates.net/?logout=1");
	    if(httpContext == null) {
		    httpContext = new BasicHttpContext();
		    BasicCookieStore cookieStore = new BasicCookieStore();
		    httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	    }
	    
	    try {
			client.execute(httpGet, httpContext);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean endorseNation(String nation, boolean endorse) {
		if(!isLoggedIn()) {
			return false;
		}

		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/nation=" + nation);

		try {
			HttpResponse response = client.execute(httpGet, httpContext);
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent(), "UTF-8"));
				for (String line; (line = reader.readLine()) != null;) {
					builder.append(line.trim());
				}
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			}

			String chkPrefix = "<input type=\"hidden\" name=\"localid\" value=\"";
			String chk = builder.substring(builder.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Localid: "+chk);

			// Post
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/cgi-bin/endorse.cgi");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("localid", chk));
			nameValuePairs.add(new BasicNameValuePair("nation", nation));
			nameValuePairs.add(new BasicNameValuePair("action", endorse ? "endorse" : "unendorse"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

			response = client.execute(httpPost, httpContext);
			builder = new StringBuilder();
			reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent(), "UTF-8"));
				for (String line; (line = reader.readLine()) != null;) {
					builder.append(line.trim());
				}
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			}
			if(builder.toString().toLowerCase().contains(endorse ? "withdraw your endorsement" : "endorse "+TagParser.idToName(nation).toLowerCase())){
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean moveToRegion(Activity activity, String region) {
		if(!isLoggedIn()) {
			return false;
		}

		HttpClient client = getClient();
		HttpGet httpGet = new HttpGet("http://www.nationstates.net/region=" + region);

		try {
			HttpResponse response = client.execute(httpGet, httpContext);
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent(), "UTF-8"));
				for (String line; (line = reader.readLine()) != null;) {
					builder.append(line.trim());
				}
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			}

			String chkPrefix = "<input type=\"hidden\" name=\"localid\" value=\"";
			String chk = builder.substring(builder.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Localid: "+chk);

			// Post
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=change_region");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("localid", chk));
			nameValuePairs.add(new BasicNameValuePair("region_name", region));
			nameValuePairs.add(new BasicNameValuePair("move_region", "Test"));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

			response = client.execute(httpPost, httpContext);
			builder = new StringBuilder();
			reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent(), "UTF-8"));
				for (String line; (line = reader.readLine()) != null;) {
					builder.append(line.trim());
				}
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			}
			if(builder.toString().contains("This region is password-protected")) {
				return enterRegionPassword(activity, region, chk);
			} else if(builder.toString().contains("<p class=\"info\">Success!")){
				// Update nation info
				NationInfo.getInstance(context).setRegionId(region);
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean moveToRegionWithPassword(String region, String password, String localid) {
		if(!isLoggedIn()) {
			return false;
		}

		HttpClient client = getClient();

		try {
			// Post
			HttpPost httpPost = new HttpPost("http://www.nationstates.net/page=change_region");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
			nameValuePairs.add(new BasicNameValuePair("localid", localid));
			nameValuePairs.add(new BasicNameValuePair("region_name", region));
			nameValuePairs.add(new BasicNameValuePair("move_region", "1"));
			nameValuePairs.add(new BasicNameValuePair("password", password));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-1"));

			HttpResponse response = client.execute(httpPost, httpContext);
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(response.getEntity()
						.getContent(), "UTF-8"));
				for (String line; (line = reader.readLine()) != null;) {
					builder.append(line.trim());
				}
			} finally {
				if (reader != null) try { reader.close(); } catch (IOException logOrIgnore) {}
			}
			if(builder.toString().contains("<p class=\"info\">Success!")){
				// Update nation info
				NationInfo.getInstance(context).setRegionId(region);
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean enterRegionPassword(final Activity activity, String region, final String localid) {
		Log.d(TAG, "Enter region password");
		if(!isLoggedIn()) {
			return false;
		}
		// Try to enter password
		Log.d(TAG, "Try to enter password");
		if(activity != null) {
			synchronized(regionPasswordRunnable) {
				Log.d(TAG, "Run region password runnable");
				regionPasswordRunnable.setActivity(activity);
				activity.runOnUiThread(regionPasswordRunnable);
				try {
					Log.d(TAG, "Wait for region password runnable");
					regionPasswordRunnable.wait();

					boolean result = false;
					while(!regionPasswordRunnable.isDismissed()) {
						Log.d(TAG, "Try the region password again");
						String pass = regionPasswordRunnable.getEnteredPassword();
						if(pass != null) {
							// Enter region password
							result = moveToRegionWithPassword(region, pass, localid);
							regionPasswordRunnable.dismissDialog();
						}
						regionPasswordRunnable.wait();
					}

					Log.d(TAG, "Return value: "+result);
					return result;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	private class RegionPasswordRunnable implements Runnable {
		private Activity activity;
		private AlertDialog dialog;
		private String enteredPassword;
		private boolean error;
		private boolean dismissed;

		public void setActivity(Activity activity) {
			this.activity = activity;
		}

		public void dismissDialog() {
			dialog.dismiss();
		}

		public String getEnteredPassword() {
			return enteredPassword;
		}

		public void setLoginError() {
			error = true;
			activity.runOnUiThread(this);
		}

		public boolean isDismissed() {
			return dismissed;
		}

		@Override
		public void run() {
			dismissed = false;
			if(error) {
//				dialog.setMessage(activity.getText(R.string.enter_password_text) + "\n" +
//						activity.getText(R.string.enter_password_error));
				Toast.makeText(activity, R.string.enter_password_error, Toast.LENGTH_SHORT).show();
				error = false;
				return;
			}
			Log.d(TAG, "Running region password runnable");

			CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

			alert.setTitle(R.string.region_password_title);
			alert.setMessage(R.string.region_password_text);

			// Set an EditText view to get user input
			final EditText input = new EditText(activity);
			input.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			alert.setView(input);

			alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, int whichButton) {
					final String pass = input.getText().toString();
					Log.d(TAG, "Pass: "+pass);
					Log.d(TAG, "Pass is "+(pass != null)+" "+(pass.length() > 0));
					if(pass != null && pass.length() > 0) {
						Log.d(TAG, "Create asynctask");
						enteredPassword = pass;
						synchronized(regionPasswordRunnable) {
							regionPasswordRunnable.notify();
						}
					}
				}
			});

			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Do nothing
					dialog.dismiss();
				}
			});

			Log.d(TAG, "Show dialog");
			dialog = alert.show();
			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					Log.d(TAG, "Notify region password runnable");
					dismissed = true;
					synchronized(regionPasswordRunnable) {
						regionPasswordRunnable.notify();
					}
				}
			});
		}
	}
}

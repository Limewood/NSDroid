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

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.limewoodMedia.nsapi.INSAPI;
import com.limewoodMedia.nsapi.NSAPI;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.enums.WAVote;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WorldData;
import com.limewoodmedia.nsdroid.holders.CensusChange;
import com.limewoodmedia.nsdroid.holders.ChoiceHolder;
import com.limewoodmedia.nsdroid.holders.DossierData;
import com.limewoodmedia.nsdroid.holders.Issue;
import com.limewoodmedia.nsdroid.holders.IssueResult;
import com.limewoodmedia.nsdroid.holders.IssuesInfo;
import com.limewoodmedia.nsdroid.holders.NationDataParcelable;
import com.limewoodmedia.nsdroid.holders.RegionDataParcelable;
import com.limewoodmedia.nsdroid.holders.WADataParcelable;
import com.limewoodmedia.nsdroid.holders.WorldDataParcelable;
import com.limewoodmedia.nsdroid.requests.AddNationToDossierRequest;
import com.limewoodmedia.nsdroid.requests.AddRegionToDossierRequest;
import com.limewoodmedia.nsdroid.requests.AnswerIssueRequest;
import com.limewoodmedia.nsdroid.requests.EndorseNationRequest;
import com.limewoodmedia.nsdroid.requests.LoginRequest;
import com.limewoodmedia.nsdroid.requests.MoveToRegionRequest;
import com.limewoodmedia.nsdroid.requests.MoveToRegionWithPasswordRequest;
import com.limewoodmedia.nsdroid.requests.NSStringRequest;
import com.limewoodmedia.nsdroid.requests.RMBPostRequest;
import com.limewoodmedia.nsdroid.requests.RemoveNationFromDossierRequest;
import com.limewoodmedia.nsdroid.requests.RemoveRegionFromDossierRequest;
import com.limewoodmedia.nsdroid.requests.VoteOnWAProposalRequest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
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
	public static final String USER_AGENT = "NSDroid Android app for NationStates " +
			"(github.com/Limewood/NSDroid), created by Laevendell (android@limewoodmedia.com)";
	private static final int VERSION = 7;
	private static API instance;
	public static final String BASE_URL = "https://www.nationstates.net";
	private static final String LOGIN_DOMAIN = "nationstates.net";
	private static final long LOGIN_EXPIRY = 31536000; // One year
    public static final int SOCKET_TIMEOUT_MS = 60000;
	
	public static synchronized API getInstance(Context context) {
		if(instance == null) {
			instance = new API(context);
		}
		return instance;
	}
	
	private Context context;
	private ShowLoginRunnable showLoginRunnable;
	private RegionPasswordRunnable regionPasswordRunnable;
	private INSAPI nsapi;
	private RequestQueue queue;
	private CookieManager cookieManager;
	
	private API(Context context) {
		this.context = context;
		cookieManager = new CookieManager();
		cookieManager.getCookieStore().removeAll();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);
		this.queue = Volley.newRequestQueue(context);
		this.showLoginRunnable = new ShowLoginRunnable();
		this.regionPasswordRunnable = new RegionPasswordRunnable();
		this.nsapi = new NSAPI();
		PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		this.nsapi.setUserAgent(USER_AGENT + (pInfo != null ? "; "+context.getString(R.string.version, pInfo.versionName) : ""));
		this.nsapi.setVersion(VERSION);
	}
	
	public void setUserNation(String userNation) {
		PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		this.nsapi.setUserAgent(USER_AGENT + (pInfo != null ? "; "+context.getString(R.string.version, pInfo.versionName) : "") + "; used by nation " + userNation);
		Log.d(TAG, "User agent: "+nsapi.getUserAgent());
	}
	
	public String getUserAgent() {
		return nsapi.getUserAgent();
	}

	private NSStringRequest getStringRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		return new NSStringRequest(method, url, listener, errorListener, getUserAgent());
	}

	private NSStringRequest getStringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
		return getStringRequest(Request.Method.GET, url, listener, errorListener);
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
            throws RateLimitReachedException, UnknownNationException, IOException, XmlPullParserException {
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
            throws RateLimitReachedException, UnknownRegionException, IOException, XmlPullParserException {

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
            throws RateLimitReachedException, IOException, XmlPullParserException {

        return new WADataParcelable(nsapi.getWAInfo(council, shards));
    }
	
	public synchronized RegionDataParcelable getHomeRegionInfo(Context context, RegionData.Shards...shards)
            throws RateLimitReachedException, UnknownRegionException, UnknownNationException, IOException, XmlPullParserException {
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

	public synchronized WorldDataParcelable getWorldInfo(WorldData.Shards...shards)
			throws RateLimitReachedException, IOException, XmlPullParserException {

		return new WorldDataParcelable(nsapi.getWorldInfo(shards));
	}
	
	/**
	 * Posts a message to the RMB of your region
	 * @param region the region id
	 * @param message the message to post
	 * @return true if successful
	 * @throws IOException if there was a problem with the connection
	 */
	public boolean postToRMB(String region, String message) throws IOException {
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL + "/region=" + region+"/template-overall=none", future, future);
		queue.add(request);
		try {
			String response = future.get();

			String chkPrefix = "<input type=\"hidden\" name=\"chk\" value=\"";
			String chk = response.substring(response.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Chk: " + chk);
			
			// Post
			future = RequestFuture.newFuture();
			RMBPostRequest postRequest = new RMBPostRequest(region, chk, message, future, future, getUserAgent());
			queue.add(postRequest);
			response = future.get();

		    if(response.contains("<p class=\"info\">Your message has been lodged!")){
		    	return true;
		    }
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Retrieves all your current issues
	 * NOTE: Requires login
	 * @return current issues
	 */
	public IssuesInfo getIssues() {
//		if(!isLoggedIn()) {
//			return null;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/page=dilemmas/template-overall=none", future, future);
		queue.add(request);

		try {
			String response = future.get();
			Log.d(TAG, "Response: "+response);

		    List<Issue> issues = new ArrayList<Issue>();

			Issue issue;
			Pattern pattern = Pattern.compile(
					"<a href=\"/page=show_dilemma/dilemma=(\\d+)\"(.+?)>(.+?)<div class=\"dpaper4\"><p><p>(.+?)</div>",
					Pattern.DOTALL);
			Matcher matcher = pattern.matcher(response);
			while(matcher.find()) {
				Log.e(TAG, "Found match");
				issue = new Issue();
				try {
                    issue.id = Integer.parseInt(matcher.group(1));
                    issue.name = matcher.group(4);
                    Log.d(TAG, "Issue: " + issue.id + " - " + issue.name);
                    issues.add(issue);
				} catch(NumberFormatException e) {
                    e.printStackTrace();
				}
			}

            IssuesInfo info = new IssuesInfo();
            info.issues = issues;

            pattern = Pattern.compile("<span id=\"deliverytime\">(.+?)<\\/span>");
            matcher = pattern.matcher(response);
            if(matcher.find()) {
                info.nextIssue = matcher.group(1);
            } else {
                pattern = Pattern.compile("\\$\\('#nextdilemmacountdown'\\)\\.countdown\\(\\{timestamp:new Date\\((\\d+?)\\)\\}\\);");
                matcher = pattern.matcher(response);
                if(matcher.find()) {
                    String timeString = Utils.getTimeString((long)(Long.parseLong(matcher.group(1))/1000f), context);
                    info.nextIssue = context.getString(R.string.in_time, timeString);
                }
            }

			return info;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
	public Issue getIssue(int id) throws IOException {
//		if(!isLoggedIn()) {
//			return null;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/page=show_dilemma/dilemma="+id+"/template-overall=none", future, future);
		queue.add(request);
		
		try {
			String response = future.get();

		    Issue issue = new Issue();
		    issue.id = id;
		    
		    // Issue name
            Pattern pTitle = Pattern.compile("<div class=\"dpaper4\"><p>(.+?)<\\/div>");
            Matcher mTitle = pTitle.matcher(response);
            if(mTitle.find()) {
                issue.name = mTitle.group(1);
            }
		    
		    // Issue text
            Pattern pText = Pattern.compile("<div class=\"dilemma\"><h5>The Issue<\\/h5>\\n<p>(.+?)<\\/p>");
            Matcher mText = pText.matcher(response);
			if(mText.find()) {
                issue.text = mText.group(1);
            }
			
			// Issue choices
			Pattern pattern = Pattern.compile("<p>(.+?)\\n<p class=\"dilemmaaccept\"><button type=\"submit\" name=\"choice-(\\d+)\"");
            Matcher matcher = pattern.matcher(response);
			List<ChoiceHolder> choices = new ArrayList<ChoiceHolder>();
			ChoiceHolder choice;
			while(matcher.find()) {
                choice = new ChoiceHolder(matcher.group(1), Integer.parseInt(matcher.group(2)));
				if(choice.choiceText.trim().length() > 0) {
					choices.add(choice);
				}
				Log.d(TAG, "Choice: "+choice.choiceText+"; index: "+choice.index);
			}
			issue.choices = choices;

			return issue;
	    } catch (StringIndexOutOfBoundsException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
			e.printStackTrace();
			throw new IOException(e);
		} catch (ExecutionException e) {
			e.printStackTrace();
			throw new IOException(e);
		}
	}
	
	/**
	 * Answers an issue
	 * @param issueId the id of the issue
	 * @param choice the choice made
	 * @return true if successful
	 * @throws IOException if there was a problem with the connection
	 */
	public IssueResult answerIssue(int issueId, int choice) throws IOException {
//		if(!isLoggedIn()) {
//			return null;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		AnswerIssueRequest request = new AnswerIssueRequest(issueId, choice, future, future, getUserAgent());
		queue.add(request);

		// Post
		try {
			String response = future.get();

			if(choice == -1) { // Dismissed
				return new IssueResult();
			}

			Pattern pattern = Pattern.compile("\\<div class=\"dilemma\"\\>\\<h5\\>The Talking Point\\<\\/h5\\>\\<p\\>(.+?)\\<\\/div\\>");
			Pattern census = Pattern.compile("\\<span class=\"wc1 (wcg|wcr)\"\\>(.+?)\\<span class=\"smalltext\"\\>(.+?) \\<span class=\"wc2 (wcg|wcr)\"\\>(.+?)\\<\\/span\\>");
			Matcher pMatcher = pattern.matcher(response);
			IssueResult issueResult = new IssueResult();
			if(pMatcher.find()) {
				issueResult.result = pMatcher.group(1);
			} else {
				issueResult.result = "";
			}
			Matcher cMatcher = census.matcher(response);
			CensusChange change;
			while(cMatcher.find()) {
				change = new CensusChange();
				change.name = cMatcher.group(2);
				change.metric = cMatcher.group(3);
				change.percent = cMatcher.group(5);
				change.increase = cMatcher.group(1).equals("wcg");
				issueResult.censusChangeList.add(change);
				Log.d(TAG, "Change: "+change.name+"; "+change.metric+"; "+change.percent+"; "+change.increase);
			}

			return issueResult;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @return the dossier data
	 */
	public DossierData getDossier() throws Exception{
//		if(!isLoggedIn()) {
//			return null;
//		}

		DossierData data = new DossierData();
		RequestFuture<String> future;
		StringRequest request;

        List<NationDataParcelable> nations = new ArrayList<NationDataParcelable>();
        List<RegionDataParcelable> regions = new ArrayList<RegionDataParcelable>();
        boolean nationsDone = false, regionsDone = false;
        outer: for(int start=0; true; start+=15) {
            // Get first 15, then next page until we find duplicates
			future = RequestFuture.newFuture();
			request = getStringRequest(BASE_URL+"/page=dossier/template-overall=none?start="+start+"/rstart="+start, future, future);
			queue.add(request);

            try {
                String response = future.get();

				// Dossier nations
				Pattern pNations = Pattern.compile("<tr><td align=\"center\"><input type=\"checkbox\" name=\"remove_nation_(.+?)\"><\\/td><td>(<a class=\"nationrss\" href=\"\\/cgi-bin\\/rss\\.cgi\\?nation=(.+?)\"><img src=\"(.+?)\" alt=\"RSS\" title=\"(.+?)\"><\\/a><a href=\"nation=(.+?)\" class=\"nlink\"><img src=\"(.+?)\" class=\"smallflag\" alt=\"(.*?)\"><span>(.+?)<\\/span><\\/a><\\/td><td align=\"center\">(.+?)<\\/td><td align=\"center\">Active <time datetime=\"(.+?)\" data-epoch=\"(.+?)\">(.+?)<\\/time><br><a href=\"region=(.+?)\">(.+?)<\\/a>|(.+?)<\\/td><\\/tr>)");
				Matcher mNations = pNations.matcher(response);
				NationDataParcelable ndp;
				nat: while(mNations.find()) {
					ndp = new NationDataParcelable();
					ndp.name = mNations.group(1);
					if(mNations.group(13) != null) {
						ndp.category = mNations.group(9);
						ndp.lastActivity = mNations.group(13);
						ndp.region = mNations.group(14);
					} else {
						ndp.lastActivity = null;
					}

					for (NationDataParcelable nd : nations) {
						if (nd.name.equals(ndp.name)) {
							nationsDone = true;
							if(regionsDone) {
								break outer;
							}
							break nat;
						}
					}
					nations.add(ndp);
				}

                // Dossier regions
				Pattern pRegions = Pattern.compile("<tr><td align=\"center\"><input type=\"checkbox\" name=\"remove_region_(.+?)\"><\\/td><td><a href=\"region=(.+?)\" class=\"rlink\">(.+?)<img src=\"(.+?)\" class=\"smallflag\" alt=\"Flag\" title=\"Regional Flag\"><\\/a><\\/td><td align=\"center\">(.+?)<\\/td><td>(<a href=\"nation=(.+?)\" class=\"nlink\"><img src=\"(.+?)\" class=\"smallflag\" alt=\"(.*?)\"><span>(.+?)<\\/span><\\/a>|(.+?))<\\/td><\\/tr>");
				Matcher mRegions = pRegions.matcher(response);
				RegionDataParcelable rdp;
				reg: while(mRegions.find()) {
					rdp = new RegionDataParcelable();
					rdp.name = mRegions.group(1);
					rdp.numNations = Integer.parseInt(mRegions.group(5));
					if(mRegions.group(7) != null) {
						rdp.delegate = mRegions.group(7);
					} else {
						rdp.delegate = "--None--";
					}

					for (RegionDataParcelable rd : regions) {
						if (rd.name.equals(rdp.name)) {
							regionsDone = true;
							if(nationsDone) {
								break outer;
							}
							break reg;
						}
					}
					regions.add(rdp);
				}
            } catch (InterruptedException e) {
				e.printStackTrace();
				throw e;
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw e;
			}
		}
        data.nations = nations.toArray(new NationDataParcelable[nations.size()]);
        data.regions = regions.toArray(new RegionDataParcelable[regions.size()]);

		return data;
	}

	/**
	 * Adds a nation to your dossier
	 * @param nation the name of the nation
	 * @return true if successful
	 * @throws IOException
	 */
	public boolean addNationToDossier(String nation) throws IOException {
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		AddNationToDossierRequest request = new AddNationToDossierRequest(nation, future, future, getUserAgent());
		queue.add(request);

		// Post
		try {
			future.get();
	        return true;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		RemoveNationFromDossierRequest request = new RemoveNationFromDossierRequest(nation, future, future, getUserAgent());
		queue.add(request);

		// Post
		try {
			future.get();
	        return true;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		AddRegionToDossierRequest request = new AddRegionToDossierRequest(region, future, future, getUserAgent());
		queue.add(request);

		// Post
		try {
			future.get();
	        return true;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		RemoveRegionFromDossierRequest request = new RemoveRegionFromDossierRequest(region, future, future, getUserAgent());
		queue.add(request);

		// Post
		try {
			future.get();
	        return true;
	    } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}

    /**
     * Vote on a World Assembly proposal
     * @param council the WACouncil
     * @param vote the vote
     * @return true if successful
     * @throws IOException
     */
    public boolean voteOnWAProposal(WACouncil council, WAVote vote) throws IOException {
//        if(!isLoggedIn()) {
//            return false;
//        }

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/page="+(council == WACouncil.GENERAL_ASSEMBLY ? "ga" : "sc"+"/template-overall=none"), future, future);
		queue.add(request);

        try {
			String response = future.get();

            String chkPrefix = "<input type=\"hidden\" name=\"localid\" value=\"";
            String chk = response.substring(response.indexOf(chkPrefix)+chkPrefix.length());
            chk = chk.substring(0, chk.indexOf("\">"));
            Log.d(TAG, "Localid: " + chk);

            // Post
			future = RequestFuture.newFuture();
			VoteOnWAProposalRequest voteRequest = new VoteOnWAProposalRequest(council, vote, chk, future, future, getUserAgent());
			queue.add(voteRequest);
			response = future.get();

            int index = response.indexOf("<" + (council == WACouncil.GENERAL_ASSEMBLY ? "h4" : "p") + " class=\"info\">");
            if(index > -1) {
                String res = response.substring(index);
                res = res.substring(0, res.indexOf("</" + (council == WACouncil.GENERAL_ASSEMBLY ? "h4" : "p") + ">"));
                if (res.toLowerCase().contains(vote == WAVote.FOR ? "'s vote for \"" : vote == WAVote.AGAINST ? "'s vote against \"" : "\" has been withdrawn.")) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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
		if(password == null || password.isEmpty()) {
			return false;
		}

		cookieManager.getCookieStore().removeAll();

		RequestFuture<String> future = RequestFuture.newFuture();
		LoginRequest request = new LoginRequest(nation, password, future, future, getUserAgent());
		queue.add(request);
		try {
			future.get();
			return verifyLogin();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean verifyLogin() {
        Log.d(TAG, "Verify login");
		boolean autologin = false;
		boolean pin = false;

		List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
		for (HttpCookie c : cookies) {
			if (c.getName().equals("autologin")) {
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
				preferences.edit().putString("autologin", c.getValue()).commit();
				Log.d(TAG, "autologin: "+c.getValue());
				autologin =  true;
				if(pin) break;
			} else if (c.getName().equals("pin")) {
				pin =  true;
				if(autologin) break;
			}
		}

        Log.d(TAG, "Autologin: "+autologin+"; pin: "+pin);
		return autologin && pin;
	}

	private boolean isLoggedIn() {
        Log.d(TAG, "Is logged in?");
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String autologin = preferences.getString("autologin", null);
		if(autologin == null) {
			verifyLogin();
			autologin = preferences.getString("autologin", null);
			if(autologin == null) return false;
		}
		HttpCookie cookie = new HttpCookie("autologin", autologin);
		cookie.setPath("/");
		cookie.setDomain(LOGIN_DOMAIN);
		cookie.setMaxAge(LOGIN_EXPIRY);
		cookieManager.getCookieStore().add(URI.create(BASE_URL), cookie);

		RequestFuture<String> future = RequestFuture.newFuture();
		final String finalAutologin = autologin;
		NSStringRequest request = new NSStringRequest(BASE_URL, future, future, getUserAgent()) {
			@Override
			public Map<String, String> getHeaders() {
				Map<String,String> params = super.getHeaders();
				params.put("Cookie", String.format("autologin=%s", finalAutologin));
				return params;
			}
		};
		queue.add(request);
		try {
			future.get(5, TimeUnit.MINUTES);

			return verifyLogin();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
            Log.d(TAG, "Timeout");
			e.printStackTrace();
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
		Log.d(TAG, "Password " + pswd + " activity: " + activity);
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

	public boolean logout() {
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/?logout=1", future, future);
		queue.add(request);
	    
	    try {
			future.get();

            // Clear cookies
            if(cookieManager != null) {
                java.net.CookieStore store = cookieManager.getCookieStore();
                if(store != null) {
                    store.removeAll();
                }
            }
            // Clear autologin
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if(preferences.contains("autologin")) {
                preferences.edit().remove("autologin").commit();
            }
            // Clear cached password
            NationInfo.getInstance(context).setPassword(null, false);

            return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        return false;
	}

	public boolean endorseNation(String nation, boolean endorse) {
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/nation=" + nation+"/template-overall=none", future, future);
		queue.add(request);

		try {
			String response = future.get();

			String chkPrefix = "<input type=\"hidden\" name=\"localid\" value=\"";
			String chk = response.substring(response.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Localid: "+chk);

			// Post
			future = RequestFuture.newFuture();
			EndorseNationRequest endorseRequest = new EndorseNationRequest(nation, chk, endorse, future, future, getUserAgent());
			queue.add(request);
			response = future.get();

			if(response.toLowerCase().contains(endorse ? "withdraw your endorsement" : "endorse "+TagParser.idToName(nation).toLowerCase())){
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean moveToRegion(Activity activity, String region) {
//		if(!isLoggedIn()) {
//			return false;
//		}

		RequestFuture<String> future = RequestFuture.newFuture();
		StringRequest request = getStringRequest(BASE_URL+"/region=" + region+"/template-overall=none", future, future);
		queue.add(request);

		try {
			String response = future.get();

			String chkPrefix = "<input type=\"hidden\" name=\"localid\" value=\"";
			String chk = response.substring(response.indexOf(chkPrefix)+chkPrefix.length());
			chk = chk.substring(0, chk.indexOf("\">"));
			Log.d(TAG, "Localid: "+chk);

			// Post
			future = RequestFuture.newFuture();
			MoveToRegionRequest moveRequest = new MoveToRegionRequest(region, chk, future, future, getUserAgent());
			queue.add(moveRequest);
			response = future.get();

			if(response.contains("This region is password-protected")) {
				return enterRegionPassword(activity, region, chk);
			} else if(response.contains("<p class=\"info\">Success!")) {
				// Update nation info
				NationInfo.getInstance(context).setRegionId(region);
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean moveToRegionWithPassword(String region, String password, String localid) {
		if(!isLoggedIn()) {
			return false;
		}

		RequestFuture<String> future = RequestFuture.newFuture();
		MoveToRegionWithPasswordRequest request = new MoveToRegionWithPasswordRequest(region, localid, password, future, future, getUserAgent());
		queue.add(request);

		try {
			// Post
			String response = future.get();

			if(response.contains("<p class=\"info\">Success!")){
				// Update nation info
				NationInfo.getInstance(context).setRegionId(region);
				return true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
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

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
package com.limewoodMedia.nsapi;

import com.limewoodMedia.nsapi.enums.CauseOfDeath;
import com.limewoodMedia.nsapi.enums.Department;
import com.limewoodMedia.nsapi.enums.IArguments;
import com.limewoodMedia.nsapi.enums.IShards;
import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.enums.WAVote;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.Embassy;
import com.limewoodMedia.nsapi.holders.NSData;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.NationFreedoms;
import com.limewoodMedia.nsapi.holders.NationHappening;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.RegionHappening;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WAHappening;
import com.limewoodMedia.nsapi.holders.WAMemberLogHappening;
import com.limewoodMedia.nsapi.holders.WAVotes;
import com.limewoodMedia.nsapi.holders.WorldData;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Java API for the NationStates Shards API
 * @author Joakim Lindskog
 *
 */
public class NSAPI implements INSAPI {
	public static final String API = "http://www.nationstates.net/cgi-bin/api.cgi";
	public static final String API_USER_AGENT = "Java NSAPI library by Laevendell (code.google.com/p/ns-api/); ";
	public static final int DEFAULT_RATE_LIMIT = 49; // One lower to be on the safe side
	
	private Queue<Date> calls = new ConcurrentLinkedQueue<Date>();
	private int rateLimit = DEFAULT_RATE_LIMIT;
	private boolean useRateLimit = true;
	private String userAgent = null;
	private int version = -1;

	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#setUserAgent(java.lang.String)
	 */
	@Override
	public void setUserAgent(String userAgent) {
		this.userAgent = API_USER_AGENT + userAgent;
	}

	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getUserAgent()
	 */
	@Override
	public String getUserAgent() {
		return userAgent;
	}

	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#setVersion(int)
	 */
	@Override
	public void setVersion(int version) {
		this.version = version;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getVersion()
	 */
	@Override
	public int getVersion() {
		return version;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#setRateLimit(int)
	 */
	@Override
	public void setRateLimit(int rateLimit) {
		this.rateLimit = rateLimit;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getRateLimit()
	 */
	@Override
	public int getRateLimit() {
		return rateLimit;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#setRateLimitEnabled(boolean)
	 */
	@Override
	public void setRateLimitEnabled(boolean enabled) {
		useRateLimit = enabled;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#isRateLimitEnabled()
	 */
	@Override
	public boolean isRateLimitEnabled() {
		return useRateLimit;
	}

	/**
	 * Makes sure you do not exceed the NS API rate limit
	 * @return true if it's OK to make a call to the NS API
	 * @throws IllegalArgumentException if no User-Agent was set
	 */
	private boolean makeCall() throws IllegalArgumentException {
		if(this.userAgent == null) {
			throw new IllegalArgumentException("No User-Agent set! Use setUserAgent(String).");
		}
		if(!useRateLimit) {
			return true;
		}
		synchronized (this.calls) {
			if(this.calls.size() < rateLimit) {
				this.calls.add(new Date());
				return true;
			}
			Calendar thirty = Calendar.getInstance();
			thirty.add(Calendar.SECOND, -30);
			Date now = thirty.getTime();
			for(Date d = this.calls.peek(); !this.calls.isEmpty() && d.before(now); d = this.calls.peek()) {
				this.calls.poll();
			}
			if(this.calls.size() < rateLimit) {
				this.calls.add(new Date());
				return true;
			}
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getNationInfo(java.lang.String, com.limewoodMedia.nsapi.holders.NationData.Shards[])
	 */
	@Override
	public NationData getNationInfo(String name, NationData.Shards...shards)
			throws RateLimitReachedException, UnknownNationException {
		if (!makeCall()) {
			throw new RateLimitReachedException();
		}
		NSData data = null;
		try {
			data = getInfo("?nation=" + name.replace(' ', '_'), shards);
			XmlPullParser xpp = null;
			xpp = data.xpp;
			String tagName = null;
			NationData nation = new NationData();
			while (xpp.next() != XmlPullParser.END_DOCUMENT) {
				switch (xpp.getEventType()) {
				case XmlPullParser.TEXT:
					if (xpp.getText().contains("Unknown nation")) {
						throw new UnknownNationException(name);
					}
					break;
				case XmlPullParser.START_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(NationData.Shards.CATEGORY.getTag())) {
						nation.category = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.FREEDOMS.getTag())) {
						nation.freedoms = parseFreedoms(xpp, nation.freedoms);
					}
					else if (tagName.equals(NationData.Shards.FULL_NAME.getTag())) {
						nation.fullName = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.MOTTO.getTag())) {
						nation.motto = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.FLAG.getTag())) {
						nation.flagURL = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.REGION.getTag())) {
						nation.region = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.POPULATION.getTag())) {
						nation.population = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.ADMIRABLE.getTag())) {
						nation.admirable = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.NOTABLE.getTag())) {
						nation.notable = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.SENSIBILITIES.getTag())) {
						nation.sensibilities = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.GOVERNMENT_DESCRIPTION.getTag())) {
						nation.governmentDescription = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.TAX_RATE.getTag())) {
						nation.taxRate = Float.parseFloat(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.INDUSTRY_DESCRIPTION.getTag())) {
						nation.industryDescription = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.LEGISLATION.getTag())) {
						nation.legislation = parseLegislation(xpp);
					}
					else if (tagName.equals(NationData.Shards.CRIME.getTag())) {
						nation.crime = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.NAME.getTag())) {
						nation.name = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.ANIMAL.getTag())) {
						nation.animal = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.ANIMAL_TRAIT.getTag())) {
						nation.animalTrait = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.CURRENCY.getTag())) {
						nation.currency = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.LEADER.getTag())) {
						String str = xpp.nextText();
						nation.leader = (str.length() > 0 ? str : null);
					}
					else if (tagName.equals(NationData.Shards.RELIGION.getTag())) {
						String str = xpp.nextText();
						nation.religion = (str.length() > 0 ? str : null);
					}
					else if (tagName.equals(NationData.Shards.HAPPENINGS.getTag())) {
						nation.happenings = parseNationHappenings(xpp);
					}
					else if (tagName.equals(NationData.Shards.TYPE.getTag())) {
						nation.type = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.WA_STATUS.getTag())) {
						nation.worldAssemblyStatus = WAStatus.parse(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.ENDORSEMENTS.getTag())) {
						String text = xpp.nextText();
						if(text.length() == 0) {
							nation.endorsements = new String[0];
						} else {
							nation.endorsements = text.split(",");
						}
					}
					else if (tagName.equals(NationData.Shards.GA_VOTE.getTag())) {
						nation.generalAssemblyVote = WAVote.parse(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.SC_VOTE.getTag())) {
						nation.generalAssemblyVote = WAVote.parse(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.MAJOR_INDUSTRY.getTag())) {
						nation.majorIndustry = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.GOVERNMENT_PRIORITY.getTag())) {
						nation.governmentPriority = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.GOVERNMENT_BUDGET.getTag())) {
						nation.governmentBudget = parseBudget(xpp);
					}
					else if (tagName.equals(NationData.Shards.FOUNDED.getTag())) {
						String str = xpp.nextText();
						nation.founded = (str.equals("0") ? "In antiquity" : str);
					}
					else if (tagName.equals(NationData.Shards.FIRST_LOGIN.getTag())) {
						nation.firstLogin = Long.parseLong(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.LAST_ACTIVITY.getTag())) {
						String str = xpp.nextText();
						nation.lastActivity = (str.equals("0") ? "In antiquity" : str);
					}
					else if (tagName.equals(NationData.Shards.LAST_LOGIN.getTag())) {
						nation.lastLogin = Long.parseLong(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.INFLUENCE.getTag())) {
						nation.influence = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.FREEDOM_SCORES.getTag())) {
						nation.freedoms = parseFreedomScores(xpp, nation.freedoms);
					}
					else if (tagName.equals(NationData.Shards.PUBLIC_SECTOR.getTag())) {
						String str = xpp.nextText();
						nation.publicSector = Float.parseFloat(str);
					}
					else if (tagName.equals(NationData.Shards.DEATHS.getTag())) {
						nation.deaths = parseDeaths(xpp);
					}
					else if (tagName.equals(NationData.Shards.CAPITAL.getTag())) {
						nation.capital = xpp.nextText();
					}
					else if (tagName.equals(NationData.Shards.REGIONAL_CENSUS.getTag())) {
						nation.regionalCensus = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.WORLD_CENSUS.getTag())) {
						nation.worldCensus = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(NationData.Shards.CENSUS_SCORE.getTag())) {
						if(nation.censusScore == null) {
							nation.censusScore = new HashMap<Integer, Float>();
						}
						int id = Integer.parseInt(xpp.getAttributeValue(null,
								NationData.Shards.Attributes.CENSUS_SCORE_ID.getName()));
						nation.censusScore.put(id, Float.parseFloat(xpp.nextText()));
					}
					else if (tagName.equals(NationData.Shards.BANNERS.getTag())) {
						nation.banners = parseBanners(xpp);
					}
                    else if (tagName.equals(NationData.Shards.DEMONYM.getTag())) {
                        nation.demonym = xpp.nextText();
                    }
                    else if (tagName.equals(NationData.Shards.DEMONYM2.getTag())) {
                        nation.demonym2 = xpp.nextText();
                    }
                    else if (tagName.equals(NationData.Shards.DEMONYM2_PLURAL.getTag())) {
                        nation.demonym2Plural = xpp.nextText();
                    }
					else if (!tagName.equals(NationData.ROOT_TAG)) {
						System.err.println("Unknown nation tag: " + tagName);
					}
					break;
				}
			}
			return nation;
		} catch(XmlPullParserException e) {
			throw new RuntimeException("Failed to parse XML", e);
		} catch(IOException e) {
			throw new RuntimeException("IOException parsing XML", e);
		}
		finally {
			if (data != null) {
				try {
					data.stream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private NationFreedoms parseFreedoms(XmlPullParser xpp, NationFreedoms freedoms)
		throws XmlPullParserException, IOException {
		String tagName = null;
		if (freedoms == null) {
			freedoms = new NationFreedoms();
		}
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_CIVIL_RIGHTS.getTag())) {
					freedoms.civilRights = xpp.nextText();
				}
				else if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_ECONOMY.getTag())) {
					freedoms.economy = xpp.nextText();
				}
				else if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_POLITICAL_FREEDOM.getTag())) {
					freedoms.politicalFreedoms = xpp.nextText();
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.FREEDOMS.getTag())) {
					break loop;
				}
			}
		}
		return freedoms;
	}

	private NationFreedoms parseFreedomScores(XmlPullParser xpp, NationFreedoms freedoms)
		throws XmlPullParserException, IOException {
		String tagName = null;
		if (freedoms == null)
			freedoms = new NationFreedoms();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_CIVIL_RIGHTS.getTag())) {
					freedoms.civilRightsValue = Integer.parseInt(xpp.nextText());
				}
				else if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_ECONOMY.getTag())) {
					freedoms.economyValue = Integer.parseInt(xpp.nextText());
				}
				else if (tagName.equals(NationData.Shards.SubTags.FREEDOMS_POLITICAL_FREEDOM.getTag())) {
					freedoms.politicalFreedomsValue = Integer.parseInt(xpp.nextText());
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.FREEDOM_SCORES.getTag())) {
					break loop;
				}
			}
		return freedoms;
	}

	private String parseLegislation(XmlPullParser xpp)
		throws XmlPullParserException, IOException {
		String legislation = null;
		String tagName = null;
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.SubTags.LEGISLATION_LAW.getTag())) {
					if (legislation == null) {
						legislation = xpp.nextText();
						if(legislation.trim().length() > 0) {
							legislation = legislation.substring(0, 1).toUpperCase() + legislation.substring(1);
						}
					}
					else {
						legislation += "||&&|| " + xpp.nextText();
					}
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.LEGISLATION.getTag())) {
					if(legislation.length() > 0) {
						legislation += ". ";
					}
					break loop;
				}
				break;
			}
		int i = legislation.lastIndexOf("||&&||");
		if(i > -1) {
			legislation = legislation.substring(0, i).replace("||&&||", ",") +
					legislation.substring(i).replace("||&&||", " and");
		}
		return legislation;
	}

	private List<NationHappening> parseNationHappenings(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		long ts = -1L;
		String text = null;
		ArrayList<NationHappening> happenings = new ArrayList<NationHappening>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.SubTags.HAPPENINGS_EVENT.getTag())) {
					// Get timestamp
					xpp.nextTag();
					ts = Long.parseLong(xpp.nextText());
					// Get text
					xpp.nextTag();
					text = xpp.nextText();
					happenings.add(new NationHappening(ts, text));
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.HAPPENINGS.getTag())) {
					break loop;
				}
			}
		return happenings;
	}

	private Map<Department, Float> parseBudget(XmlPullParser xpp)
		throws XmlPullParserException, IOException {
        String tagName = null;
        String str = null;
        float value = -1;
        HashMap<Department, Float> budget = new HashMap<Department, Float>();
        Department department = null;
        loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
            switch (xpp.getEventType()) {
                case XmlPullParser.START_TAG:
                    tagName = xpp.getName().toLowerCase();
                    if ((department = Department.parseTag(tagName)) != null) {
                        str = xpp.nextText();
                        value = Float.parseFloat(str);
                        budget.put(department, value);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tagName = xpp.getName().toLowerCase();
                    if (tagName.equals(NationData.Shards.GOVERNMENT_BUDGET.getTag())) {
                        break loop;
                    }
                    break;
            }
		return budget;
	}

	private Map<CauseOfDeath, Float> parseDeaths(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		String type = null;
		String str = null;
		float value = -1;
		HashMap<CauseOfDeath, Float> deaths = new HashMap<CauseOfDeath, Float>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.SubTags.DEATHS_CAUSE.getTag())) {
					type = xpp.getAttributeValue(null, NationData.Shards.Attributes.DEATHS_CAUSE_TYPE.getName());
					str = xpp.nextText();
					value = Float.parseFloat(str);
					deaths.put(CauseOfDeath.parse(type), value);
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(NationData.Shards.DEATHS.getTag())) {
					break loop;
				}
				break;
			}
		return deaths;
	}

	private String[] parseBanners(XmlPullParser xpp)
			throws XmlPullParserException, IOException {
		String tagName = null;
		String banner = null;
		List<String> banners = new ArrayList<String>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
				case XmlPullParser.START_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(NationData.Shards.SubTags.BANNERS_BANNER.getTag())) {
						banner = xpp.nextText();
						banners.add(banner);
					}
					break;
				case XmlPullParser.END_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(NationData.Shards.BANNERS.getTag())) {
						break loop;
					}
					break;
			}
		}
		return banners.toArray(new String[banners.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getRegionInfo(java.lang.String, com.limewoodMedia.nsapi.holders.RegionData.Shards[])
	 */
	@Override
	public RegionData getRegionInfo(String name, RegionData.Shards...shards)
		throws RateLimitReachedException, UnknownRegionException {
		if (!makeCall()) {
			throw new RateLimitReachedException();
		}
		NSData data = null;
		try {
			data = getInfo(new StringBuilder().append("?region=").append(name.replace(' ', '_')).toString(), shards);
			String tagName = null;
			XmlPullParser xpp = data.xpp;
			RegionData region = new RegionData();
			while (xpp.next() != XmlPullParser.END_DOCUMENT)
				switch (xpp.getEventType()) {
				case XmlPullParser.TEXT:
					if (xpp.getText().contains("Unknown region")) {
						throw new UnknownRegionException(name);
				}
					break;
				case XmlPullParser.START_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(RegionData.Shards.FLAG.getTag())) {
						region.flagURL = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.NAME.getTag())) {
						region.name = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.FACTBOOK.getTag())) {
						region.factbook = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.DELEGATE.getTag())) {
						region.delegate = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.FOUNDER.getTag())) {
						region.founder = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.HAPPENINGS.getTag())) {
						region.happenings = parseRegionHappenings(xpp);
					}
					else if (tagName.equals(RegionData.Shards.MESSAGES.getTag())) {
						region.messages = parseRMBMessages(xpp);
					}
					else if (tagName.equals(RegionData.Shards.NUM_NATIONS.getTag())) {
						region.numNations = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(RegionData.Shards.NATIONS.getTag())) {
						region.nations = xpp.nextText().split(":");
					}
					else if (tagName.equals(RegionData.Shards.DELEGATE_VOTES.getTag())) {
						region.delegateVotes = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(RegionData.Shards.GA_VOTES.getTag())) {
						region.generalAssemblyVotes = parseWAVotes(xpp, RegionData.Shards.GA_VOTES.getTag());
					}
					else if (tagName.equals(RegionData.Shards.SC_VOTES.getTag())) {
						region.securityCouncilVotes = parseWAVotes(xpp, RegionData.Shards.SC_VOTES.getTag());
					}
					else if (tagName.equals(RegionData.Shards.POWER.getTag())) {
						region.power = xpp.nextText();
					}
					else if (tagName.equals(RegionData.Shards.EMBASSIES.getTag())) {
						region.embassies = parseEmbassies(xpp);
					}
					else if (tagName.equals(RegionData.Shards.TAGS.getTag())) {
						region.tags = parseTags(xpp);
					}
					else if (!tagName.equals(RegionData.ROOT_TAG)) {
						System.err.println("Unknown region tag: " + tagName);
					}
					break;
				}
			return region;
		} catch(XmlPullParserException e) {
			throw new RuntimeException("Failed to parse XML", e);
		} catch(IOException e) {
			throw new RuntimeException("IOException parsing XML", e);
		}
		finally {
			if(data != null) {
				try {
					data.stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<RegionHappening> parseRegionHappenings(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		long ts = -1L;
		String text = null;
		ArrayList<RegionHappening> happenings = new ArrayList<RegionHappening>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.SubTags.HAPPENINGS_EVENT.getTag())) {
					// Get timestamp
					xpp.nextTag();
					ts = Long.parseLong(xpp.nextText());
					// Get text
					xpp.nextTag();
					text = xpp.nextText();
					happenings.add(new RegionHappening(ts, text));
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.HAPPENINGS.getTag())) {
					break loop;
				}
			}
		}
		return happenings;
	}

	private List<RMBMessage> parseRMBMessages(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		long ts = -1L;
		String nation = null;
		String text = null;
		ArrayList<RMBMessage> messages = new ArrayList<RMBMessage>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.SubTags.MESSAGES_POST.getTag())) {
					// Get timestamp
					xpp.nextTag();
					ts = Long.parseLong(xpp.nextText());
					// Get nation name
					xpp.nextTag();
					nation = xpp.nextText();
					// Get text
					xpp.nextTag();
					text = xpp.nextText();
					messages.add(new RMBMessage(ts, nation, text));
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.MESSAGES.getTag())) {
					break loop;
				}
			}
		}
		// Reset offset
		RegionData.Shards.MESSAGES.setArgument(RegionData.Shards.Arguments.MESSAGES_OFFSET, "0");
		return messages;
	}

	private WAVotes parseWAVotes(XmlPullParser xpp, String vote)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		WAVotes votes = new WAVotes();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.SubTags.WA_VOTES_FOR.getTag())) {
					String str = xpp.nextText();
					votes.forVotes = str.length() == 0 ? 0 : Integer.parseInt(str);
				}
				else if (tagName.equals(RegionData.Shards.SubTags.WA_VOTES_AGAINST.getTag())) {
					String str = xpp.nextText();
					votes.againstVotes = str.length() == 0 ? 0 : Integer.parseInt(str);
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(vote)) {
					break loop;
				}
			}
		}
		return votes;
	}

	private List<Embassy> parseEmbassies(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		Embassy embassy = null;
		List<Embassy> embassies = new ArrayList<Embassy>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.SubTags.EMBASSIES_EMBASSY.getTag())) {
					embassy = new Embassy();
					embassy.status = Embassy.EmbassyStatus.parse(xpp.getAttributeValue(null, "type"));
					embassy.region = xpp.nextText();
					embassies.add(embassy);
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.EMBASSIES.getTag())) {
					break loop;
				}
			}
		}
		return embassies;
	}

	private List<String> parseTags(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		List<String> tags = new ArrayList<String>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT) {
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.SubTags.TAGS_TAG.getTag())) {
					tags.add(xpp.nextText());
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(RegionData.Shards.TAGS.getTag())) {
					break loop;
				}
			}
		}
		return tags;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getWorldInfo(WorldData.Shards[])
	 */
	@Override
	public WorldData getWorldInfo(WorldData.Shards...shards) {
		if (!makeCall()) {
			throw new RateLimitReachedException();
		}
		NSData data = null;
		try {
			data = getInfo("?", shards);
			XmlPullParser xpp = null;
			xpp = data.xpp;
			String tagName = null;
			WorldData world = new WorldData();
			while (xpp.next() != XmlPullParser.END_DOCUMENT) {
				switch (xpp.getEventType()) {
				case XmlPullParser.START_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(WorldData.Shards.NUM_NATIONS.getTag())) {
						world.numNations = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WorldData.Shards.NUM_REGIONS.getTag())) {
						world.numRegions = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WorldData.Shards.CENSUS.getTag())) {
						world.census = xpp.nextText();
					}
					else if (tagName.equals(WorldData.Shards.CENSUS_ID.getTag())) {
						world.censusId = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WorldData.Shards.CENSUS_SIZE.getTag())) {
						world.censusSize = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WorldData.Shards.CENSUS_SCALE.getTag())) {
						world.censusScale = xpp.nextText();
					}
					else if (tagName.equals(WorldData.Shards.CENSUS_MEDIAN.getTag())) {
						world.censusMedian = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WorldData.Shards.FEATURED_REGION.getTag())) {
						world.featuredRegion = xpp.nextText();
					}
					else if (tagName.equals(WorldData.Shards.NEW_NATIONS.getTag())) {
						world.newNations = xpp.nextText().split(",");
					}
					else if (tagName.equals(WorldData.Shards.REGIONS_BY_TAG.getTag())) {
						world.regionsByTag = xpp.nextText().split(",");
					}
					else {
						System.err.println("Unknown world tag: " + tagName);
					}
					break;
				}
			}
			return world;
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Failed to parse XML", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException parsing XML", e);
		}
		finally {
			if (data != null) {
				try {
					data.stream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.limewoodMedia.nsapi.INSAPI#getWorldInfo(WorldData.Shards[])
	 */
	@Override
	public WAData getWAInfo(WACouncil council, WAData.Shards...shards) {
		if (!makeCall()) {
			throw new RateLimitReachedException();
		}
		NSData data = null;
		try {
			data = getInfo("?wa="+council.getId(), shards);
			XmlPullParser xpp = null;
			xpp = data.xpp;
			String tagName = null;
			WAData wa = new WAData();
			while (xpp.next() != XmlPullParser.END_DOCUMENT) {
				switch (xpp.getEventType()) {
				case XmlPullParser.START_TAG:
					tagName = xpp.getName().toLowerCase();
					if (tagName.equals(WAData.Shards.NUM_NATIONS.getTag())) {
						wa.numNations = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WAData.Shards.NUM_DELEGATES.getTag())) {
						wa.numDelegates = Integer.parseInt(xpp.nextText());
					}
					else if (tagName.equals(WAData.Shards.DELEGATES.getTag())) {
						wa.delegates = xpp.nextText().split(",");
					}
					else if (tagName.equals(WAData.Shards.MEMBERS.getTag())) {
						wa.members = xpp.nextText().split(",");
					}
					else if (tagName.equals(WAData.Shards.HAPPENINGS.getTag())) {
						wa.happenings = parseWAHappenings(xpp);
					}
					else if (tagName.equals(WAData.Shards.MEMBER_LOG.getTag())) {
						wa.memberLog = parseWAMemberLog(xpp);
					}
					else if (tagName.equals(WAData.Shards.LAST_RESOLUTION.getTag())) {
						// TODO FIXME This tag can contain invalid xml!
//						wa.lastResolution = xpp.nextText();
					}
					else {
						System.err.println("Unknown WA tag: " + tagName);
					}
					break;
				}
			}
			return wa;
		} catch (XmlPullParserException e) {
			throw new RuntimeException("Failed to parse XML", e);
		} catch (IOException e) {
			throw new RuntimeException("IOException parsing XML", e);
		}
		finally {
			if (data != null) {
				try {
					data.stream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private List<WAHappening> parseWAHappenings(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		long ts = -1L;
		String text = null;
		ArrayList<WAHappening> happenings = new ArrayList<WAHappening>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(WAData.Shards.SubTags.HAPPENINGS_EVENT.getTag())) {
					// Get timestamp
					xpp.nextTag();
					ts = Long.parseLong(xpp.nextText());
					// Get text
					xpp.nextTag();
					text = xpp.nextText();
					happenings.add(new WAHappening(ts, text));
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(WAData.Shards.HAPPENINGS.getTag())) {
					break loop;
				}
			}
		return happenings;
	}

	private List<WAMemberLogHappening> parseWAMemberLog(XmlPullParser xpp)
		throws NumberFormatException, XmlPullParserException, IOException {
		String tagName = null;
		long ts = -1L;
		String text = null;
		ArrayList<WAMemberLogHappening> happenings = new ArrayList<WAMemberLogHappening>();
		loop: while (xpp.next() != XmlPullParser.END_DOCUMENT)
			switch (xpp.getEventType()) {
			case XmlPullParser.START_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(WAData.Shards.SubTags.MEMBER_LOG_EVENT.getTag())) {
					// Get timestamp
					xpp.nextTag();
					ts = Long.parseLong(xpp.nextText());
					// Get text
					xpp.nextTag();
					text = xpp.nextText();
					happenings.add(new WAMemberLogHappening(ts, text));
				}
				break;
			case XmlPullParser.END_TAG:
				tagName = xpp.getName().toLowerCase();
				if (tagName.equals(WAData.Shards.MEMBER_LOG.getTag())) {
					break loop;
				}
			}
		return happenings;
	}

	/**
	 * Fetches data from the NationStates Shards API
	 * @param urlStart the start of the URL
	 * @param shards what shards to include
	 * @return an NSData object with the XmlPullParser and InputStream
	 * @throws XmlPullParserException if there was a problem with parsing the xml
	 * @throws java.io.IOException if there was a network problem
	 */
	private synchronized NSData getInfo(String urlStart, IShards...shards)
			throws XmlPullParserException, IOException {
		KXmlParser xpp = new KXmlParser();
		String shardsStr = null;
		for (IShards s : shards) {
			if(shardsStr == null) {
				shardsStr = s.getName();
			} else {
				shardsStr += "+" + s.getName();
			}
			if(s.getArguments() != null) {
				Map<IArguments, String> args = s.getArguments();
				for(Entry<IArguments, String> a : args.entrySet()) {
					shardsStr += ";" + a.getKey().getName() + "=" + a.getValue();
				}
			}
		}
		String str = API + urlStart + (this.version > -1 ? "&v=" + this.version : "") +
				"&q=" + shardsStr;
//		System.out.println("Str "+str);
		
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, this.userAgent);
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		HttpGet get = new HttpGet(str);
		HttpResponse response = client.execute(get);
		InputStream stream = response.getEntity().getContent();
		
		xpp.setInput(stream, "ISO-8859-15");
		return new NSData(xpp, stream);
	}
}
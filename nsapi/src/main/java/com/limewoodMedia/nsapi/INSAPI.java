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

import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.exceptions.RateLimitReachedException;
import com.limewoodMedia.nsapi.exceptions.UnknownNationException;
import com.limewoodMedia.nsapi.exceptions.UnknownRegionException;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WorldData;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Interface for Java API for NationStates API
 * @author Joakim Lindskog
 *
 */
public interface INSAPI {
	/**
	 * Sets the User-Agent header
	 * Note: this needs to be set to be able to access the API
	 * @param userAgent the User-Agent string to use
	 */
	public void setUserAgent(String userAgent);
	
	/**
	 * @return the User-Agent
	 */
	public String getUserAgent();

	/**
	 * Sets version of the NationStates API to use
	 * @param version the version of the NS API to use
	 */
	public void setVersion(int version);
	
	/**
	 * @return the currently used version
	 */
	public int getVersion();
	
	/**
	 * Sets the rate limit - default is 49 (per 30 seconds)
	 * CAUTION: make sure your application doesn't exceed the rate limit
	 * @param rateLimit max number of calls per 30 seconds
	 */
	public void setRateLimit(int rateLimit);
	
	/**
	 * @return the current rate limit (max number of calls per 30 seconds)
	 */
	public int getRateLimit();
	
	/**
	 * Enables/disables the rate limit (default is enabled)
	 * CAUTION: only disable the rate limit if you handle it properly elsewhere
	 * @param enabled true to enable the rate limit, false to disable it
	 */
	public void setRateLimitEnabled(boolean enabled);
	
	/**
	 * @return whether the rate limit is enabled
	 */
	public boolean isRateLimitEnabled();

	/**
	 * Fetches information on a nation
	 * @param name the nation id
	 * @param shards the shards to request
	 * @return a NationData object with nation info
	 * @throws com.limewoodMedia.nsapi.exceptions.RateLimitReachedException if the rate limit was reached (but not exceeded)
	 * @throws com.limewoodMedia.nsapi.exceptions.UnknownNationException if the nation could not be found
	 */
	public NationData getNationInfo(String name, NationData.Shards... shards)
            throws RateLimitReachedException, UnknownNationException, IOException, XmlPullParserException;

	/**
	 * Fetches information on a region
	 * @param name the region id
	 * @param shards the shards to request
	 * @return a RegionData object with region info
	 * @throws com.limewoodMedia.nsapi.exceptions.RateLimitReachedException if the rate limit was reached (but not exceeded)
	 * @throws com.limewoodMedia.nsapi.exceptions.UnknownRegionException if the region could not be found
	 */
	public RegionData getRegionInfo(String name, RegionData.Shards... shards)
            throws RateLimitReachedException, UnknownRegionException, IOException, XmlPullParserException;
	
	/**
     * Fetches information on the world
     * @param shards the shards to request
     * @return a WorldData object with world info
     * @throws com.limewoodMedia.nsapi.exceptions.RateLimitReachedException if the rate limit was reached (but not exceeded)
     */
	public WorldData getWorldInfo(WorldData.Shards... shards)
            throws RateLimitReachedException, IOException, XmlPullParserException;
	
	/**
	 * Fetches information on the World Assembly
	 * @param council what council to query (not used for some shards)
	 * @param shards the shards to request
	 * @return a WAData object with World Assembly info
	 * @throws com.limewoodMedia.nsapi.exceptions.RateLimitReachedException if the rate limit was reached (but not exceeded)
	 */
	public WAData getWAInfo(WACouncil council, WAData.Shards... shards)
            throws RateLimitReachedException, IOException, XmlPullParserException;
}
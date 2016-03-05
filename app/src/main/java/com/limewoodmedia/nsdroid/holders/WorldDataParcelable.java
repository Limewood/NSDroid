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
package com.limewoodmedia.nsdroid.holders;

import android.os.Parcel;
import android.os.Parcelable;

import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.holders.Resolution;
import com.limewoodMedia.nsapi.holders.VoteTrack;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WAVotes;
import com.limewoodMedia.nsapi.holders.WorldData;

public class WorldDataParcelable extends WorldData implements Parcelable {
	public WorldDataParcelable() {}

	public WorldDataParcelable(WorldData data) {
		this.numNations = data.numNations;
		this.numRegions = data.numRegions;
		this.census = data.census;
		this.censusId = data.censusId;
		this.censusScale = data.censusScale;
		this.featuredRegion = data.featuredRegion;
		this.newNations = data.newNations;
        this.regionsByTag = data.regionsByTag;
	}

	public static final Creator<WorldDataParcelable> CREATOR
	    = new Creator<WorldDataParcelable>() {
			public WorldDataParcelable createFromParcel(Parcel in) {
				WorldDataParcelable data = new WorldDataParcelable();
				data.numNations = in.readInt();
				data.numRegions = in.readInt();
                data.census = in.readString();
                data.censusId = in.readInt();
                data.censusScale = in.readString();
                data.featuredRegion = in.readString();
                int len = in.readInt();
                if(len > 0) {
                    data.newNations = new String[len];
                    for (int i = 0; i < len; i++) {
                        data.newNations[i] = in.readString();
                    }
                }
                len = in.readInt();
                if(len > 0) {
                    data.regionsByTag = new String[len];
                    for (int i = 0; i < len; i++) {
                        data.regionsByTag[i] = in.readString();
                    }
                }

                return data;
			}
			
			public WorldDataParcelable[] newArray(int size) {
			    return new WorldDataParcelable[size];
			}
		};
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.numNations);
        dest.writeInt(this.numRegions);
        dest.writeString(this.census);
        dest.writeInt(this.censusId);
        dest.writeString(this.censusScale);
        dest.writeString(this.featuredRegion);
        if(this.newNations != null) {
            dest.writeInt(this.newNations.length);
            for(String n : this.newNations) {
                dest.writeString(n);
            }
        } else {
            dest.writeInt(0);
        }
        if(this.regionsByTag != null) {
            dest.writeInt(this.regionsByTag.length);
            for(String r : this.regionsByTag) {
                dest.writeString(r);
            }
        } else {
            dest.writeInt(0);
        }
	}
}

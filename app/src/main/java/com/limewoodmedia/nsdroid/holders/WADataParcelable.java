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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import com.limewoodMedia.nsapi.enums.WACouncil;
import com.limewoodMedia.nsapi.holders.Embassy;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.RegionHappening;
import com.limewoodMedia.nsapi.holders.Resolution;
import com.limewoodMedia.nsapi.holders.VoteTrack;
import com.limewoodMedia.nsapi.holders.WAData;
import com.limewoodMedia.nsapi.holders.WAVotes;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class WADataParcelable extends WAData implements Parcelable {
	public WADataParcelable() {}

	public WADataParcelable(WAData data) {
		this.council = data.council;
		this.resolution = data.resolution;
		this.delegates = data.delegates;
		this.lastResolution = data.lastResolution;
		this.memberLog = data.memberLog;
		this.happenings = data.happenings;
		this.members = data.members;
		this.numNations = data.numNations;
		this.numDelegates = data.numDelegates;
	}

	public static final Creator<WADataParcelable> CREATOR
	    = new Creator<WADataParcelable>() {
			public WADataParcelable createFromParcel(Parcel in) {
				WADataParcelable data = new WADataParcelable();
				data.council = WACouncil.values()[in.readInt()];
				data.resolution = new Resolution();
                data.resolution.category = in.readString();
                data.resolution.proposedBy = in.readString();
                data.resolution.option = in.readString();
                data.resolution.created = in.readLong();
                data.resolution.desc = in.readString();
                data.resolution.name = in.readString();
                data.resolution.voteTrack = new VoteTrack();
                int num = in.readInt();
                data.resolution.voteTrack.againstVotes = new Integer[num];
                for(int i=0; i<num; i++) {
                    data.resolution.voteTrack.againstVotes[i] = in.readInt();
                }
                num = in.readInt();
                data.resolution.voteTrack.forVotes = new Integer[num];
                for(int i=0; i<num; i++) {
                    data.resolution.voteTrack.forVotes[i] = in.readInt();
                }
                data.resolution.votes = new WAVotes(in.readInt(), in.readInt());
			    return data;
			}
			
			public WADataParcelable[] newArray(int size) {
			    return new WADataParcelable[size];
			}
		};
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(council.ordinal());
		dest.writeString(resolution.category);
        dest.writeString(resolution.proposedBy);
		dest.writeString(resolution.option);
        dest.writeLong(resolution.created);
        dest.writeString(resolution.desc);
        dest.writeString(resolution.name);
        dest.writeInt(resolution.voteTrack.againstVotes.length);
        for(Integer i : resolution.voteTrack.againstVotes) {
            dest.writeInt(i);
        }
        dest.writeInt(resolution.voteTrack.forVotes.length);
        for(Integer i : resolution.voteTrack.forVotes) {
            dest.writeInt(i);
        }
        dest.writeInt(resolution.votes.forVotes);
        dest.writeInt(resolution.votes.againstVotes);
	}
}

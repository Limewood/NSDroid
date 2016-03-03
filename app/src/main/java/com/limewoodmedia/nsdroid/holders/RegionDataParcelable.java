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
package com.limewoodmedia.nsdroid.holders;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import com.limewoodMedia.nsapi.holders.Embassy;
import com.limewoodMedia.nsapi.holders.Officer;
import com.limewoodMedia.nsapi.holders.RegionHappening;
import com.limewoodMedia.nsapi.holders.RegionData;
import com.limewoodMedia.nsapi.holders.RMBMessage;
import com.limewoodMedia.nsapi.holders.WAVotes;

public class RegionDataParcelable extends RegionData implements Parcelable {
	public Bitmap flagBitmap;
	
	public RegionDataParcelable() {}
	
	public RegionDataParcelable(RegionData data) {
		this.name = data.name;
		this.flagURL = data.flagURL;
		this.delegate = data.delegate;
		this.founder = data.founder;
		this.factbook = data.factbook;
		this.happenings = data.happenings;
		this.messages = data.messages;
		this.numNations = data.numNations;
		this.nations = data.nations;
		this.delegateVotes = data.delegateVotes;
		this.generalAssemblyVotes = data.generalAssemblyVotes;
		this.securityCouncilVotes = data.securityCouncilVotes;
		this.power = data.power;
		this.embassies = data.embassies;
		this.tags = data.tags;
		this.officers = data.officers;
	}
	
	public static final Parcelable.Creator<RegionDataParcelable> CREATOR
	    = new Parcelable.Creator<RegionDataParcelable>() {
			public RegionDataParcelable createFromParcel(Parcel in) {
				RegionDataParcelable data = new RegionDataParcelable();
				data.name = in.readString();
				data.flagURL = in.readString();
				data.delegate = in.readString();
				data.founder = in.readString();
				data.factbook = in.readString();
				int num = in.readInt();
				data.happenings = new ArrayList<RegionHappening>(num);
				for(int i=0; i<num; i++) {
					data.happenings.add(new RegionHappening(in.readLong(), in.readString()));
				}
				num = in.readInt();
				data.messages = new ArrayList<RMBMessage>(num);
				for(int i=0; i<num; i++) {
					data.messages.add(new RMBMessage(in.readLong(), in.readLong(), in.readString(), in.readString()));
				}
				data.numNations = in.readInt();
				num = in.readInt();
				if(num > 0) {
					data.nations = new String[num];
					in.readStringArray(data.nations);
				}
				data.delegateVotes = in.readInt();
				data.generalAssemblyVotes = new WAVotes(in.readInt(), in.readInt());
				data.securityCouncilVotes = new WAVotes(in.readInt(), in.readInt());
				data.power = in.readString();
				data.embassies = new ArrayList<Embassy>();
				num = in.readInt();
				for(int i=0; i<num; i++) {
					data.embassies.add(new Embassy(in.readString(), Embassy.EmbassyStatus.parse(in.readString())));
				}
				data.tags = new ArrayList<String>();
				in.readStringList(data.tags);
				data.officers = new ArrayList<>();
				int len = in.readInt();
				Officer officer;
				for(int i=0; i<len; i++) {
					officer = new Officer();
					officer.nation = in.readString();
					officer.office = in.readString();
					int l = in.readInt();
					for(int a=0; a<l; a++) {
						officer.authority.add(Officer.Authority.getByCode((char)in.readInt()));
					}
					officer.appointed = in.readLong();
					officer.appointer = in.readString();
					officer.order = in.readInt();
					data.officers.add(officer);
				}
				
				// Read flag bitmap
				byte[] bArr = in.createByteArray();
				if(bArr != null) {
					data.flagBitmap = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
				}
			    return data;
			}
			
			public RegionDataParcelable[] newArray(int size) {
			    return new RegionDataParcelable[size];
			}
		};
	
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(flagURL);
		dest.writeString(delegate);
		dest.writeString(founder);
		dest.writeString(factbook);
		if(happenings != null) {
			// Write number of happenings
			dest.writeInt(happenings.size());
			// Write happenings
			for(RegionHappening h : happenings) {
				dest.writeLong(h.timestamp);
				dest.writeString(h.text);
			}
		} else {
			dest.writeInt(0);
		}
		if(messages != null) {
			// Write number of RMB messages
			dest.writeInt(messages.size());
			// Write messages
			for(RMBMessage m : messages) {
                dest.writeLong(m.id);
				dest.writeLong(m.timestamp);
				dest.writeString(m.nation);
				dest.writeString(m.message);
			}
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(numNations);
		if(nations != null) {
			dest.writeInt(nations.length);
			dest.writeStringArray(nations);
		} else {
			dest.writeInt(0);
		}
		dest.writeInt(delegateVotes);
		dest.writeInt(generalAssemblyVotes != null ? generalAssemblyVotes.forVotes : 0);
		dest.writeInt(generalAssemblyVotes != null ? generalAssemblyVotes.againstVotes : 0);
		dest.writeInt(securityCouncilVotes != null ? securityCouncilVotes.forVotes : 0);
		dest.writeInt(securityCouncilVotes != null ? securityCouncilVotes.againstVotes : 0);
		dest.writeString(power);
		if(embassies != null) {
			dest.writeInt(embassies.size());
			for(Embassy e : embassies) {
				dest.writeString(e.region);
				if(e.status != null) {
					dest.writeString(e.status.getDescription());
				} else {
					dest.writeString(null);
				}
			}
		} else {
			dest.writeInt(0);
		}
		dest.writeStringList(tags);
		dest.writeInt(officers.size());
		for(Officer o : officers) {
			dest.writeString(o.nation);
			dest.writeString(o.office);
			dest.writeInt(o.authority.size());
			for(Officer.Authority a : o.authority) {
				dest.writeInt(a.code);
			}
			dest.writeLong(o.appointed);
			dest.writeString(o.appointer);
			dest.writeInt(o.order);
		}
		
		// Write flag bitmap
		if(flagBitmap != null) {
			// Write byte array from bitmap
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			flagBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			dest.writeByteArray(stream.toByteArray());
		}
	}
}

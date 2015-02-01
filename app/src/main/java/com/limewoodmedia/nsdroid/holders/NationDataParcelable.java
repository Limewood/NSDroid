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
import java.util.HashMap;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import com.limewoodMedia.nsapi.enums.CauseOfDeath;
import com.limewoodMedia.nsapi.enums.Department;
import com.limewoodMedia.nsapi.enums.WAStatus;
import com.limewoodMedia.nsapi.enums.WAVote;
import com.limewoodMedia.nsapi.holders.NationData;
import com.limewoodMedia.nsapi.holders.NationFreedoms;
import com.limewoodMedia.nsapi.holders.NationHappening;

public class NationDataParcelable extends NationData implements Parcelable {
	public Bitmap flagBitmap;
	
	public NationDataParcelable() {}
	
	public NationDataParcelable(NationData data) {
		this.name = data.name;
		this.fullName = data.fullName;
		this.motto = data.motto;
		this.flagURL = data.flagURL;
		this.region = data.region;
		this.category = data.category;
		this.population = data.population;
		this.freedoms = data.freedoms;
		this.admirable = data.admirable;
		this.notable = data.notable;
		this.sensibilities = data.sensibilities;
		this.governmentDescription = data.governmentDescription;
		this.taxRate = data.taxRate;
		this.industryDescription = data.industryDescription;
		this.animal = data.animal;
		this.animalTrait = data.animalTrait;
		this.crime = data.crime;
		this.currency = data.currency;
		this.leader = data.leader;
		this.religion = data.religion;
		this.legislation = data.legislation;
		this.happenings = data.happenings;
		this.type = data.type;
		this.worldAssemblyStatus = data.worldAssemblyStatus;
		this.endorsements = data.endorsements;
		this.generalAssemblyVote = data.generalAssemblyVote;
		this.securityCouncilVote = data.securityCouncilVote;
		this.majorIndustry = data.majorIndustry;
		this.governmentPriority = data.governmentPriority;
		this.leader = data.leader;
		this.governmentBudget = data.governmentBudget;
		this.founded = data.founded;
		this.firstLogin = data.firstLogin;
		this.lastActivity = data.lastActivity;
		this.lastLogin = data.lastLogin;
		this.influence = data.influence;
		this.publicSector = data.publicSector;
		this.deaths = data.deaths;
		this.capital = data.capital;
		this.banners = data.banners;
        this.demonym = data.demonym;
        this.demonym2 = data.demonym2;
        this.demonym2Plural = data.demonym2Plural;
	}
	
	public static final Parcelable.Creator<NationDataParcelable> CREATOR
	    = new Parcelable.Creator<NationDataParcelable>() {
			public NationDataParcelable createFromParcel(Parcel in) {
				NationDataParcelable data = new NationDataParcelable();
				data.name = in.readString();
				data.fullName = in.readString();
				data.motto = in.readString();
				data.flagURL = in.readString();
				data.region = in.readString();
				data.category = in.readString();
				data.population = in.readInt();
				data.freedoms = new NationFreedoms();
				data.freedoms.civilRights = in.readString();
				data.freedoms.civilRightsValue = in.readInt();
				data.freedoms.economy = in.readString();
				data.freedoms.economyValue = in.readInt();
				data.freedoms.politicalFreedoms = in.readString();
				data.freedoms.politicalFreedomsValue = in.readInt();
				data.admirable = in.readString();
				data.notable = in.readString();
				data.sensibilities = in.readString();
				data.governmentDescription = in.readString();
				data.taxRate = in.readFloat();
				data.industryDescription = in.readString();
				data.animal = in.readString();
				data.animalTrait = in.readString();
				data.crime = in.readString();
				data.currency = in.readString();
				data.leader = in.readString();
				data.religion = in.readString();
				data.legislation = in.readString();
				int num = in.readInt();
				data.happenings = new ArrayList<NationHappening>(num);
				for(int i=0; i<num; i++) {
					data.happenings.add(new NationHappening(in.readLong(), in.readString()));
				}
				data.type = in.readString();
				data.worldAssemblyStatus = WAStatus.parse(in.readString());
				num = in.readInt();
				if(num > 0) {
					data.endorsements = new String[num];
					in.readStringArray(data.endorsements);
				}
				data.generalAssemblyVote = WAVote.parse(in.readString());
				data.securityCouncilVote = WAVote.parse(in.readString());
				data.majorIndustry = in.readString();
				data.governmentPriority = in.readString();
				data.leader = in.readString();
				data.governmentBudget = new HashMap<Department, Float>();
                num = in.readInt();
                for(int i=0; i<num; i++) {
                    data.governmentBudget.put(Department.parse(in.readString()), in.readFloat());
                }
				data.founded = in.readString();
				data.firstLogin = in.readLong();
				data.lastActivity = in.readString();
				data.lastLogin = in.readLong();
				data.influence = in.readString();
				data.publicSector = in.readFloat();
				data.deaths = new HashMap<CauseOfDeath, Float>();
				num = in.readInt();
				for(int i=0; i<num; i++) {
					data.deaths.put(CauseOfDeath.parse(in.readString()), in.readFloat());
				}
				data.capital = in.readString();
                data.banners = new String[in.readInt()];
                for(int i=0; i<data.banners.length; i++) {
                    data.banners[i] = in.readString();
                }
                data.demonym = in.readString();
                data.demonym2 = in.readString();
                data.demonym2Plural = in.readString();
				
				// Read flag bitmap
				byte[] bArr = in.createByteArray();
				data.flagBitmap = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
			    return data;
			}
			
			public NationDataParcelable[] newArray(int size) {
			    return new NationDataParcelable[size];
			}
		};

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(fullName);
		dest.writeString(motto);
		dest.writeString(flagURL);
		dest.writeString(region);
		dest.writeString(category);
		dest.writeInt(population);
		if(freedoms != null) {
			// Write one entry for each value in freedoms
			dest.writeString(freedoms.civilRights);
			dest.writeInt(freedoms.civilRightsValue);
			dest.writeString(freedoms.economy);
			dest.writeInt(freedoms.economyValue);
			dest.writeString(freedoms.politicalFreedoms);
			dest.writeInt(freedoms.politicalFreedomsValue);
		} else {
			for(int i=0; i<3; i++) {
				dest.writeString(null);
				dest.writeInt(0);
			}
		}
		dest.writeString(admirable);
		dest.writeString(notable);
		dest.writeString(sensibilities);
		dest.writeString(governmentDescription);
		dest.writeFloat(taxRate);
		dest.writeString(industryDescription);
		dest.writeString(animal);
		dest.writeString(animalTrait);
		dest.writeString(crime);
		dest.writeString(currency);
		dest.writeString(leader);
		dest.writeString(religion);
		dest.writeString(legislation);
		if(happenings != null) {
			// Write number of happenings
			dest.writeInt(happenings.size());
			// Write happenings
			for(NationHappening h : happenings) {
				dest.writeLong(h.timestamp);
				dest.writeString(h.text);
			}
		} else {
			dest.writeInt(0);
		}
		dest.writeString(type);
		dest.writeString(worldAssemblyStatus != null ? worldAssemblyStatus.getDescription() : null);
		if(endorsements != null) {
			dest.writeInt(endorsements.length);
			dest.writeStringArray(endorsements);
		} else {
			dest.writeInt(0);
		}
		dest.writeString(generalAssemblyVote != null ? generalAssemblyVote.getName() : null);
		dest.writeString(securityCouncilVote != null ? securityCouncilVote.getName() : null);
		dest.writeString(majorIndustry);
		dest.writeString(governmentPriority);
		dest.writeString(leader);
        if(governmentBudget != null) {
            dest.writeInt(governmentBudget.size());
            for(Entry<Department, Float> e : governmentBudget.entrySet()) {
                dest.writeString(e.getKey().getDescription());
                dest.writeFloat(e.getValue());
            }
        } else {
            dest.writeInt(0);
        }
		dest.writeString(founded);
		dest.writeLong(firstLogin);
		dest.writeString(lastActivity);
		dest.writeLong(lastLogin);
		dest.writeString(influence);
		dest.writeFloat(publicSector);
		if(deaths != null) {
			dest.writeInt(deaths.size());
			for(Entry<CauseOfDeath, Float> e : deaths.entrySet()) {
				dest.writeString(e.getKey().getDescription());
				dest.writeFloat(e.getValue());
			}
		} else {
			dest.writeInt(0);
		}
		dest.writeString(capital);
        dest.writeInt(banners.length);
        for(String b : banners) {
            dest.writeString(b);
        }
        dest.writeString(demonym);
        dest.writeString(demonym2);
        dest.writeString(demonym2Plural);
		
		// Write flag bitmap
		if(flagBitmap != null) {
			// Write byte array from bitmap
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			flagBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			dest.writeByteArray(stream.toByteArray());
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}
}

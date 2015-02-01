package com.limewoodmedia.nsdroid.holders;

import com.limewoodMedia.nsapi.holders.RMBMessage;

import android.os.Parcel;
import android.os.Parcelable;

public class RMBMessageParcelable implements Parcelable {
	public RMBMessage msg;
	
	public RMBMessageParcelable(RMBMessage msg) {
		this.msg = new RMBMessage(msg.id, msg.timestamp, msg.nation, msg.message);
	}
	
	public RMBMessageParcelable(long id, long timestamp, String nation, String message) {
		this.msg = new RMBMessage(id, timestamp, nation, message);
	}
	
	public static final Parcelable.Creator<RMBMessageParcelable> CREATOR
    = new Parcelable.Creator<RMBMessageParcelable>() {
		public RMBMessageParcelable createFromParcel(Parcel in) {
			RMBMessageParcelable data = new RMBMessageParcelable(in.readLong(), in.readLong(), in.readString(), in.readString());
			
		    return data;
		}
		
		public RMBMessageParcelable[] newArray(int size) {
		    return new RMBMessageParcelable[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		if(msg == null) return;
        dest.writeLong(msg.id);
		dest.writeLong(msg.timestamp);
		dest.writeString(msg.nation);
		dest.writeString(msg.message);
	}

}

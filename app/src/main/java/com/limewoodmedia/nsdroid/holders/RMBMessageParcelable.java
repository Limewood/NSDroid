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

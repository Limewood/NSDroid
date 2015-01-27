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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.limewoodmedia.nsdroid.R;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.FloatMath;
import android.util.Log;
import android.widget.TextView;

@SuppressWarnings("unused")
public class TagParser {
	private static Map<String, String> tags = new HashMap<String, String>();
	static {
		tags.put("\\[(b|B)\\](.+?)\\[/(b|B)\\]", "<b>$2</b>");
		tags.put("\\[(i|I)\\](.+?)\\[/(i|I)\\]", "<i>$2</i>");
		tags.put("\\[(u|U)\\](.+?)\\[/(u|U)\\]", "<u>$2</u>");
		tags.put("\\[(nation)\\](.+?)\\[/(nation)\\]",
				"<a href=\"com.limewoodMedia.nsdroid.nation://$2\">$2</a>");
		tags.put("\\[(nation)=short\\](.+?)\\[/(nation)\\]",
				"<a href=\"com.limewoodMedia.nsdroid.nation://$2\">$2</a>");
		tags.put("\\[(nation)=noflag\\](.+?)\\[/(nation)\\]",
				"<a href=\"com.limewoodMedia.nsdroid.nation://$2\">$2</a>");
		tags.put("\\[(nation)=short\\+noflag\\](.+?)\\[/(nation)\\]",
				"<a href=\"com.limewoodMedia.nsdroid.nation://$2\">$2</a>");
		tags.put("\\[(nation)=([A-Za-z\\s_\\d]+)\\](.+?)",
				"<a href=\"com.limewoodMedia.nsdroid.nation://$2\">$2</a>");
		tags.put("\\[(region)\\](.+?)\\[/(region)\\]",
				"<a href=\"com.limewoodMedia.nsdroid.region://$2\">$2</a>");
		tags.put("\\[(region)=([A-Za-z\\s_\\d]+)\\](.+?)",
				"<a href=\"com.limewoodMedia.nsdroid.region://$2\">$2</a>");
		tags.put("\\[(hr)\\]", "<br />------------------------------<br />");
		tags.put("\\[(url)\\=(.+?)\\](.+?)\\[/(url)\\]", "<a href=\"$2\">$3</a>");
		tags.put("\\[(color)=(.+?)\\](.+?)\\[/(color)\\]", "<font color=\"$2\">$3</font>");
		tags.put("\\[/?(list)\\]", "");
		tags.put("\\[\\*\\](.+?)\n?", "\t&#8226; $1");
		tags.put("\\[quote=(.+?);(\\d+?)\\](.+?)\\[/quote\\]",
				"<i><b><a href=\"com.limewoodMedia.nsdroid.nation://$1\">$1</a> "
				+"wrote:</b><br/><font color=\"#676767\">$3</font></i><br/>");
	}
	
	public static String parseTags(String text) {
		for(Entry<String, String> tag : tags.entrySet()) {
			text = Pattern.compile(tag.getKey(), tag.getKey().contains("\\[\\*\\]") ? 0 : Pattern.DOTALL|Pattern.CASE_INSENSITIVE)
					.matcher(text).replaceAll(tag.getValue());
		}
		return text;
	}
	
	public static Spanned parseTagsFromHtml(String text) {
		return parseTagsFromHtml(text, false);
	}
	
	public static Spanned parseTagsFromHtml(String text, boolean parseURLs) {
		String str = TagParser.parseTags(
				Html.fromHtml(
						TagParser.nl2br(text)).toString());
		if(parseURLs) {
			// TODO Send user to internal activity to warn about external link?
			str = str.replaceAll("http[s]?\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,4}(/\\S*)?",
					"<a href=\"$0\">$0</a>");
		}
        return Html.fromHtml(TagParser.nl2br(str));
	}
	
	public static String nl2br(String text) {
        if(text == null) return "";
		return text.replace("\n", "<br />");
	}
	
	public static String parseTimestamp(Context context, long timestamp) {
		Date now = new Date();
		long diff = now.getTime() - timestamp * 1000;
		int HOUR = 1000 * 60 * 60;
		int DAY = HOUR * 24;
		int days = (int) Math.floor(diff / (float) DAY);
		int hours = (int) Math.floor((diff - (days * DAY)) / (float) HOUR);
		int minutes = 0, seconds = 0;
		if(days == 0 && hours == 0) {
			minutes = (int) Math.floor((diff - (days * DAY) - (hours * HOUR)) / (float) (1000 * 60));
			seconds = (int) Math.floor((diff - (days * DAY) - (hours * HOUR) - (minutes * (1000 * 60))) / 1000f);
		}
		Object[] attr = null;
		if(days > 0 && hours > 0) {
			// Days and hours
			attr = new Object[] {
					days, context.getResources().getQuantityString(R.plurals.days, days),
					hours, context.getResources().getQuantityString(R.plurals.hours, hours)
			};
		} else if(hours > 0) {
			// Just hours
			attr = new Object[] {
					hours, context.getResources().getQuantityString(R.plurals.hours, hours)
			};
		} else if(days > 0) {
			// Just days
			attr = new Object[] {
					days, context.getResources().getQuantityString(R.plurals.days, days),
			};
		} else if(minutes > 0) {
			// Just minutes
			attr = new Object[] {
					minutes, context.getResources().getQuantityString(R.plurals.minutes, minutes)
			};
		} else {
			// Just seconds
			attr = new Object[] {
					seconds, context.getResources().getQuantityString(R.plurals.seconds, seconds)
			};
		}
        if(attr.length == 2) {
            return context.getString(R.string.time_passed, attr[0], attr[1]);
        } else {
            return context.getString(R.string.time_passed_days_hours, attr[0], attr[1], attr[2], attr[3]);
        }
	}
	
	public static String idToName(String id) {
        if(id == null) return "";
        id = id.substring(0, 1).toUpperCase(Locale.getDefault())+id.substring(1);
        Pattern camel = Pattern.compile("(.*?)_([a-z\\d])(.*?)");
        Matcher m = camel.matcher(id);
		while(m.matches()) {
			id = m.replaceFirst("$1 "+id.substring(m.start(2), m.end(2)).toUpperCase(Locale.getDefault())+"$3");
			m = camel.matcher(id);
		}
		return id;
	}

	public static String nameToId(String name) {
		return name.replace(' ', '_').toLowerCase();
	}
}

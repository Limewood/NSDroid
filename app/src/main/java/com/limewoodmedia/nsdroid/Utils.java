/*
 * Copyright (c) 2014. Joakim Lindskog & Limewood Media
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

package com.limewoodmedia.nsdroid;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.limewoodmedia.nsdroid.activities.Dossier;
import com.limewoodmedia.nsdroid.activities.Issues;
import com.limewoodmedia.nsdroid.activities.NSDroid;
import com.limewoodmedia.nsdroid.activities.Nation;
import com.limewoodmedia.nsdroid.activities.News;
import com.limewoodmedia.nsdroid.activities.Preferences;
import com.limewoodmedia.nsdroid.activities.Region;
import com.limewoodmedia.nsdroid.activities.World;
import com.limewoodmedia.nsdroid.activities.WorldAssembly;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Joakim Lindskog
 *         2014-12-16
 */
public class Utils {
    public static String NUMBER_FORMAT = "%,d";

	public static int dpToPx(int dp, Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		return px;
	}

    /**
     * Sets up the navigation drawer
     * @param activity the Activity
     * @return the navigation drawer fragment
     */
    public static NavigationDrawerFragment setupNavigationDrawer(AppCompatActivity activity) {
        NavigationDrawerFragment navigationDrawerFragment = (NavigationDrawerFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Home button
        ActionBar actionBar = activity.getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set up the drawers
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) activity.findViewById(R.id.drawer_layout));

        return navigationDrawerFragment;
    }

    public static void onNavigationDrawerItemSelected(final Context context, int id) {
        Intent intent;
        switch(id) {
            case R.id.menu_start:
                intent = new Intent(context, NSDroid.class);
                context.startActivity(intent);
                break;
            case R.id.menu_nation:
                intent = new Intent(context, Nation.class);
                context.startActivity(intent);
                break;
            case R.id.submenu_issues:
                intent = new Intent(context, Issues.class);
                context.startActivity(intent);
                break;
            case R.id.submenu_dossier:
                intent = new Intent(context, Dossier.class);
                context.startActivity(intent);
                break;
            case R.id.menu_region:
                intent = new Intent(context, Region.class);
                context.startActivity(intent);
                break;
            case R.id.submenu_rmb: // Your region's RMB
                if(context instanceof Region) {
                    ((Region)context).showPage(1);
                } else {
                    intent = new Intent(context, Region.class);
                    intent.putExtra("page", 1);
                    context.startActivity(intent);
                }
                break;
            case R.id.submenu_officers: // Your region's officers
                if(context instanceof Region) {
                    ((Region)context).showPage(2);
                } else {
                    intent = new Intent(context, Region.class);
                    intent.putExtra("page", 2);
                    context.startActivity(intent);
                }
                break;
            case R.id.submenu_embassies: // Your region's embassies
                if(context instanceof Region) {
                    ((Region)context).showPage(3);
                } else {
                    intent = new Intent(context, Region.class);
                    intent.putExtra("page", 3);
                    context.startActivity(intent);
                }
                break;
            case R.id.menu_world: // World
                intent = new Intent(context, World.class);
                context.startActivity(intent);
                break;
            case R.id.menu_wa: // World Assembly
                intent = new Intent(context, WorldAssembly.class);
                context.startActivity(intent);
                break;
            case R.id.menu_news: // News
                intent = new Intent(context, News.class);
                context.startActivity(intent);
                break;
            case R.id.menu_settings: // Preferences
                intent = new Intent(context, Preferences.class);
                context.startActivity(intent);
                break;
            case R.id.menu_logout: // Log out
                Toast.makeText(context, R.string.logging_out, Toast.LENGTH_LONG).show();
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
                        return API.getInstance(context).logout();
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if(result) {
                            Toast.makeText(context, R.string.logged_out, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
        }
    }

    public static String capitalize(String str) {
        if(str == null) return null;
        return str.substring(0, 1).toUpperCase()+str.substring(1);
    }

    /**
     * Converts a timestamp (seconds) to days and hours
     * @param timestamp the timestamp to convert
     * @return an int array with {days, hours}
     */
    public static int[] getDaysHours(long timestamp) {
        long seconds = (long) Math.floor((timestamp - System.currentTimeMillis() / 1000f));
        int hours = (int) Math.floor(seconds / 3600f);
        int days = (int) Math.floor(hours / 24f);
        hours = hours - days*24;
        return new int[]{days, hours};
    }

    /**
     * Converts a timestamp (seconds) to a time string
     * @param timestamp the timestamp to convert
     * @return a time string
     */
    public static String getTimeString(long timestamp, Context context) {
        long seconds = (long) Math.floor((timestamp - System.currentTimeMillis() / 1000f));
        int minutes = (int) Math.floor(seconds / 60f);
        int hours = (int) Math.floor(seconds / 3600f);
        int days = (int) Math.floor(hours / 24f);
        minutes = minutes - hours*60;
        hours = hours - days*24;
        if(days > 0) {
            return context.getString(R.string.days_hours, days, hours);
        } else if(hours > 0) {
            return context.getString(R.string.hours_minutes, hours, minutes);
        } else {
            return context.getString(R.string.minutes, minutes);
        }
    }

    public static int[] getWADaysHoursLeft(int hoursPassed) {
        int hours = 4*24-hoursPassed; // 4 days voting
        int days = (int) Math.floor(hours / 24f);
        hours = hours - days*24;
        return new int[]{days, hours};
    }

    public static String formatCurrencyAmount(Context context, long amount) {
        int len = String.valueOf(amount).length()-1;

        String formatted = null;
        if(len >= 12) { // Trillion
            formatted = context.getString(R.string.trillion, String.format(NUMBER_FORMAT, Math.round(amount/1000000000000d)));
        } else if(len >= 9) { // Billion
            formatted = context.getString(R.string.billion, String.format(NUMBER_FORMAT, Math.round(amount/1000000000d)));
        } else if(len >= 6) { // Million
            formatted = context.getString(R.string.million, String.format(NUMBER_FORMAT, Math.round(amount/1000000d)));
        } else {
            formatted = String.format(NUMBER_FORMAT, amount);
        }

        return formatted;
    }

    public static String getOrdinal(int num) {
        switch (num % 10) {
            case 1:
                return num+"st";
            case 2:
                return num+"nd";
            case 3:
                return num+"rd";
            default:
                return num+"th";
        }
    }

    /**
     * Parse a date from an NS date string
     * @param nsDate NS date string (in x hours y minutes)
     * @param margin margin in minutes
     * @return a Date
     */
    public static Date dateFromString(String nsDate, int margin) {
        Calendar cal = Calendar.getInstance();
        Pattern pattern;
        Matcher matcher;
        if(nsDate.contains("minutes")) {
            // Hours and minutes or just minutes
            pattern = Pattern.compile("((\\d+?) hours)?( )?((\\d+?) minutes)");
            matcher = pattern.matcher(nsDate);
            if(matcher.find()) {
                if (matcher.group(2) != null) {
                    cal.add(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(2)));
                }
                if (matcher.group(5) != null) {
                    cal.add(Calendar.MINUTE, Integer.parseInt(matcher.group(5)));
                }
            }
        } else {
            // Just hours
            pattern = Pattern.compile("((\\d+?) hours)");
            matcher = pattern.matcher(nsDate);
            if(matcher.find()) {
                if(matcher.group(2) != null) {
                    cal.add(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(2)));
                }
            }
        }
        if(margin > 0) {
            // Add margin
            cal.add(Calendar.MINUTE, margin);
        }
        return cal.getTime();
    }
}

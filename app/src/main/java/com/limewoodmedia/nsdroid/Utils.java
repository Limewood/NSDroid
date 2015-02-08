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
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.limewoodmedia.nsdroid.activities.Dossier;
import com.limewoodmedia.nsdroid.activities.Issues;
import com.limewoodmedia.nsdroid.activities.NSDroid;
import com.limewoodmedia.nsdroid.activities.Nation;
import com.limewoodmedia.nsdroid.activities.News;
import com.limewoodmedia.nsdroid.activities.Preferences;
import com.limewoodmedia.nsdroid.activities.Region;
import com.limewoodmedia.nsdroid.activities.WorldAssembly;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

/**
 * @author Joakim Lindskog
 *         2014-12-16
 */
public class Utils {
	public static ImageLoaderConfiguration getImageLoaderConfig(Context context) {
		return new ImageLoaderConfiguration.Builder(context)
				.threadPriority(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory()
				.threadPoolSize(2)
				.diskCacheFileNameGenerator(new Md5FileNameGenerator())
				.diskCacheSize(200 * 1024 * 1024) // 200 Mb
				.memoryCacheSize(20 * 1024 * 1024) // 20 Mb
				.tasksProcessingOrder(QueueProcessingType.LIFO)
				.build();
	}

	public static ImageLoader getImageLoader(Context context) {
		ImageLoader imageLoader = ImageLoader.getInstance();
		if(!imageLoader.isInited()) {
			imageLoader.init(getImageLoaderConfig(context));
		}
		return imageLoader;
	}

	public static DisplayImageOptions getImageLoaderDisplayOptions() {
		return new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.default_white)
				.showImageForEmptyUri(R.drawable.default_white)
				.showImageOnFail(R.drawable.default_white)
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.considerExifParams(true)
				.imageScaleType(ImageScaleType.EXACTLY)
				.build();
	}

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
    public static NavigationDrawerFragment setupNavigationDrawer(SherlockFragmentActivity activity) {
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

    public static void onNavigationDrawerItemSelected(Context context, int id) {
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
            case R.id.submenu_embassies: // Your region's embassies
                if(context instanceof Region) {
                    ((Region)context).showPage(2);
                } else {
                    intent = new Intent(context, Region.class);
                    intent.putExtra("page", 2);
                    context.startActivity(intent);
                }
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
}

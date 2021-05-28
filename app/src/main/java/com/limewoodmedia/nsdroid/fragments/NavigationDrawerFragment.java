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

package com.limewoodmedia.nsdroid.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.limewoodmedia.nsdroid.R;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
//    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private NSActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
	private View mFragmentContainerView;

    private boolean mUserLearnedDrawer;
//	private View containerView;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		ViewGroup layout = (ViewGroup) (inflater.inflate(
				R.layout.fragment_navigation_drawer, container, false));
		MenuItemHolder[] holders = new MenuItemHolder[]{
				new MenuItemHolder(R.id.menu_start, R.drawable.icon_start, R.string.menu_start),
				new MenuItemHolder(R.id.menu_nation, R.drawable.icon_nation, R.string.menu_nation),
                new MenuItemHolder(R.id.submenu_issues, R.drawable.icon_issues, R.string.menu_issues),
                new MenuItemHolder(R.id.submenu_dossier, R.drawable.icon_dossier, R.string.menu_dossier),
				new MenuItemHolder(R.id.menu_region, R.drawable.icon_region, R.string.menu_region),
                new MenuItemHolder(R.id.submenu_rmb, R.drawable.icon_rmb, R.string.menu_rmb),
                new MenuItemHolder(R.id.submenu_officers, R.drawable.icon_officers, R.string.menu_officers),
                new MenuItemHolder(R.id.submenu_embassies, R.drawable.icon_embassies, R.string.menu_embassies),
                new MenuItemHolder(R.id.menu_world, R.drawable.icon_world, R.string.menu_world),
                new MenuItemHolder(R.id.menu_wa, R.drawable.icon_wa, R.string.menu_wa),
                new MenuItemHolder(R.id.menu_news, R.drawable.icon_news, R.string.menu_news),
				new MenuItemHolder(R.id.menu_settings, R.drawable.icon_settings, R.string.menu_settings),
                new MenuItemHolder(R.id.menu_logout, R.drawable.icon_logout, R.string.menu_logout)
		};
		View view;
		for(final MenuItemHolder holder : holders) {
			view = layout.findViewById(holder.viewId);
			if(view == null) continue;
			((ImageView)view.findViewById(R.id.menu_item_image)).setImageResource(holder.imageId);
			((TextView)view.findViewById(R.id.menu_item_text)).setText(holder.textId);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectMenuItem(holder.viewId);
				}
			});
			view.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					switch (motionEvent.getAction()) {
						case MotionEvent.ACTION_DOWN:
							view.setBackgroundColor(getActivity().getResources().getColor(R.color.menu_item_selected));
							break;
						case MotionEvent.ACTION_UP:
						case MotionEvent.ACTION_OUTSIDE:
						case MotionEvent.ACTION_CANCEL:
						case MotionEvent.ACTION_HOVER_EXIT:
						case MotionEvent.ACTION_POINTER_UP:
							view.setBackgroundColor(0x00000000);
							break;
					}
					return false;
				}
			});
		}
//		layout.setOnTouchListener(new View.OnTouchListener() {
//			@Override
//			public boolean onTouch(View view, MotionEvent motionEvent) {
//				return true;
//			}
//		});
        return layout;
    }

	private void selectMenuItem(int id) {
		if (mDrawerLayout != null) {
			mDrawerLayout.closeDrawer(mFragmentContainerView);
		}
		if (mCallbacks != null) {
			mCallbacks.onNavigationDrawerItemSelected(id);
		}
	}

	public ActionBarDrawerToggle getDrawerToggle() {
		return mDrawerToggle;
	}

	public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, final DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new NSActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        );

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
//			containerView = activity.findViewById(R.id.container);
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        // If the drawer is open, show the global app actions in the action bar. See also
//        // showGlobalContextActionBar, which controls the top-left area of the action bar.
//        if (mDrawerLayout != null && isDrawerOpen()) {
//            showGlobalContextActionBar();
//        }
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);

	}

//    /**
//     * Per the navigation drawer design guidelines, updates the action bar to show the global app
//     * 'context', rather than just what's in the current screen.
//     */
//    private void showGlobalContextActionBar() {
//        ActionBar actionBar = getActionBar();
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//        actionBar.setTitle(R.string.app_name);
//    }

//    private ActionBar getActionBar() {
//        return getActivity().getActionBar();
//    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int id);
    }

	class MenuItemHolder {
		public int viewId;
		public int imageId;
		public int textId;

		public MenuItemHolder(int viewId, int imageId, int textId) {
			this.viewId = viewId;
			this.imageId = imageId;
			this.textId = textId;
		}
	}

    private class NSActionBarDrawerToggle extends ActionBarDrawerToggle {
        // android.R.id.home as defined by public API in v11
        private static final int ID_HOME = 0x0102002c;

        public NSActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout,
                                       @DrawableRes int drawerImageRes, @StringRes int openDrawerContentDescRes,
                                       @StringRes int closeDrawerContentDescRes) {
            super(activity, drawerLayout, null, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            super.onDrawerClosed(drawerView);
            if (!isAdded()) {
                return;
            }

            getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            if (!isAdded()) {
                return;
            }

            if (!mUserLearnedDrawer) {
                // The user manually opened the drawer; store this flag to prevent auto-showing
                // the navigation drawer automatically in the future.
                mUserLearnedDrawer = true;
                SharedPreferences sp = PreferenceManager
                        .getDefaultSharedPreferences(getActivity());
                sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).commit();
            }

            getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
        }

        public void onDrawerSlide(View drawerView, float slideOffset) {
//				float moveFactor = (drawerView.getWidth() * slideOffset);
//				if(containerView != null) {
//					containerView.setTranslationX(moveFactor);
//				} else {
//					containerView = getActivity().findViewById(R.id.container);
//					containerView.setTranslationX(moveFactor);
//				}
            super.onDrawerSlide(drawerView, slideOffset);
        }

        /**
         * This method should be called by your <code>Activity</code>'s
         * {@link Activity#onOptionsItemSelected(android.view.MenuItem) onOptionsItemSelected} method.
         * If it returns true, your <code>onOptionsItemSelected</code> method should return true and
         * skip further processing.
         *
         * @param item the MenuItem instance representing the selected menu item
         * @return true if the event was handled and further processing should not occur
         */
        public boolean onOptionsItemSelected(MenuItem item) {
            if (item != null && item.getItemId() == ID_HOME && isDrawerIndicatorEnabled()) {
                if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
                return true;
            }
            return false;
        }
    }
}

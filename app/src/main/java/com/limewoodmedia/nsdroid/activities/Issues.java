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
package com.limewoodmedia.nsdroid.activities;

import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.LoadingHelper;
import com.limewoodmedia.nsdroid.NationInfo;
import com.limewoodmedia.nsdroid.Utils;
import com.limewoodmedia.nsdroid.fragments.IssuesFragment;
import com.limewoodmedia.nsdroid.fragments.NavigationDrawerFragment;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class Issues extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
	public static final String TAG = Issues.class.getName();

	private TextView issuesHeader;
	private IssuesFragment issuesFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.issues_single);

        // Fetch flag
        LoadingHelper.loadHomeFlag(this);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        Utils.setupNavigationDrawer(this);
        
        issuesHeader = (TextView) findViewById(R.id.issues_header);
        issuesHeader.setText(getResources().getString(R.string.issues_header, NationInfo.getInstance(this).getName()));
        issuesFragment = (IssuesFragment) getSupportFragmentManager().findFragmentById(R.id.issues);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_issues, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
            	issuesFragment.loadIssues();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigationDrawerItemSelected(int id) {
        Utils.onNavigationDrawerItemSelected(this, id);
    }
}

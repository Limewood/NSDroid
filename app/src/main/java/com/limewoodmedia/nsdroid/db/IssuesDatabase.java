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

package com.limewoodmedia.nsdroid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Database of previously answered issues
 * @author Joakim Lindskog
 *
 */
public class IssuesDatabase extends SQLiteOpenHelper {
	private static final String TAG = IssuesDatabase.class.getName();

    // Database Version TODO If this changes, implement copy of old values!
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "issuesDB";

    // Issues table name
    private static final String TABLE_ISSUES = "issues";

    // Issues Table Columns names
    private static final String KEY_ID = "issue_id";
    private static final String KEY_CHOICE_INDEX = "issue_choice_index";

    private static IssuesDatabase instance;

    public static synchronized IssuesDatabase getInstance(Context context) {
    	if(instance == null) {
    		Log.d(TAG, "Instantiating database");
    		instance = new IssuesDatabase(context.getApplicationContext());
    	}
    	return instance;
    }

    private Context context;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

	private IssuesDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Log.d(TAG, "Constructor");
        this.context = context;
    }
 
    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate");
        createTable(db, true);
    }
    
    private synchronized void createTable(SQLiteDatabase db, boolean createStartingBottles) {
		Log.d(TAG, "createTable");
    	String CREATE_TABLE = "CREATE TABLE " + TABLE_ISSUES + "("
                + KEY_ID + " INTEGER PRIMARY KEY, " + KEY_CHOICE_INDEX + " INTEGER)";
        db.execSQL(CREATE_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade");
		// TODO Copy old database
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ISSUES);
 
        // Create tables again
        onCreate(db);
    }
    
    /**
     * Gets the previous issue choice index for the issue
     * @return index of issue choice
     */
    public synchronized int getPreviousIssueChoiceIndex(int issueId) {
    	readLock.lock();
    	SQLiteDatabase db = null;
    	try {
        	db = getReadableDatabase();
	        Cursor cursor = db.query(true, TABLE_ISSUES, new String[]{
                    KEY_CHOICE_INDEX
	        }, KEY_ID + "=?", new String[]{Integer.toString(issueId)}, null, null, null, null);

	        if(cursor.moveToFirst()) {
	            return cursor.getInt(0);
	        } else {
                return -1;
            }
    	} finally {
    		if(db != null) {
    			db.close();
    		}
    		readLock.unlock();
    	}
    }
    
    /**
     * Sets the choice for this issue
     * @param issueId the id of the issue
     * @param choiceIndex the index of the choice
     */
    public synchronized void setIssueChoice(int issueId, int choiceIndex) {
		writeLock.lock();
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_CHOICE_INDEX, choiceIndex);

			Cursor c = db.query(true, TABLE_ISSUES, new String[]{KEY_ID},
					KEY_ID +"=?", new String[]{Integer.toString(issueId)}, null, null, null, null);
			if(!c.moveToFirst()) {
                values.put(KEY_ID, issueId);
				db.insert(TABLE_ISSUES, null, values);
			} else {
				db.update(TABLE_ISSUES, values, KEY_ID + "=?", new String[]{Integer.toString(issueId)});
			}
		} finally {
			if(db != null) {
				db.close();
			}
			writeLock.unlock();
		}
    }

	public synchronized void clear() throws SQLException {
		writeLock.lock();
		try {
			Log.d(TAG, "clear");
			SQLiteDatabase db = getWritableDatabase();
			// Drop table
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ISSUES);
	        
	        // Create table without tutorial
	        createTable(db, false);
	    	db.close();
		} finally {
			writeLock.unlock();
		}
	}
}
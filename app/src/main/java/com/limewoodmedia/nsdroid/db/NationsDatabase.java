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
package com.limewoodmedia.nsdroid.db;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database of saved nations (main and puppets)
 * @author Joakim Lindskog
 *
 */
public class NationsDatabase extends SQLiteOpenHelper {
	private static final String TAG = NationsDatabase.class.getName();
	
    // Database Version TODO If this changes, implement copy of old values!
    private static final int DATABASE_VERSION = 2;
 
    // Database Name
    private static final String DATABASE_NAME = "nationsDB";
 
    // Nations table name
    private static final String TABLE_NATIONS = "nations";
 
    // Nations Table Columns names
    private static final String KEY_NAME = "nation_name";
    
    private static NationsDatabase instance;
    
    public static synchronized NationsDatabase getInstance(Context context) {
    	if(instance == null) {
    		Log.d(TAG, "Instantiating database");
    		instance = new NationsDatabase(context.getApplicationContext());
    	}
    	return instance;
    }
    
    private Context context;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

	private NationsDatabase(Context context) {
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
    	String CREATE_TABLE = "CREATE TABLE " + TABLE_NATIONS + "("
                + KEY_NAME + " STRING PRIMARY KEY)";
        db.execSQL(CREATE_TABLE);
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade");
		// TODO Copy old database
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NATIONS);
 
        // Create tables again
        onCreate(db);
    }
    
    /**
     * Gets a list of all nations
     * @return a list of nation names
     */
    public synchronized List<String> getAllNations() {
    	readLock.lock();
    	SQLiteDatabase db = null;
    	try {
        	db = getReadableDatabase();
	        Cursor cursor = db.query(true, TABLE_NATIONS, new String[]{
	        		KEY_NAME
	        }, null, null, null, null, null, null);
	     
	        List<String> list = new ArrayList<String>();
	        while(cursor.moveToNext()) {
	            list.add(cursor.getString(0));
	        }
	        
	        return list;
    	} finally {
    		if(db != null) {
    			db.close();
    		}
    		readLock.unlock();
    	}
    }
    
    /**
     * Gets the number of nations in the database
     * @return the number of nations
     */
    public synchronized int numNations() {
    	readLock.lock();
    	SQLiteDatabase db = null;
    	try {
    		db = getReadableDatabase();
    		Cursor c = db.rawQuery("SELECT COUNT(*) FROM "+TABLE_NATIONS, null);
    		c.moveToFirst();
    		int num = c.getInt(0);
    		
    		return num;
    	} finally {
    		if(db != null) {
    			db.close();
    		}
    		readLock.unlock();
    	}
    }
    
    /**
     * Adds a nation to the database
     * @param nation the nation name to add
     * @return true if added
     */
    public synchronized boolean addNation(String nation) {
		writeLock.lock();
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			
			Cursor c = db.query(true, TABLE_NATIONS, new String[]{KEY_NAME},
					KEY_NAME+"=?", new String[]{nation}, null, null, null, null);
			if(!c.moveToFirst()) {
				ContentValues values = new ContentValues();
				values.put(KEY_NAME, nation);
				
				long id = db.insert(TABLE_NATIONS, null, values);
				
				return id != -1;
			} else {
				return false;
			}
		} finally {
			if(db != null) {
				db.close();
			}
			writeLock.unlock();
		}
    }
	
	/**
	 * Removes a nation from the database
	 * @param nation the name of the nation to remove
	 * @return true if successful
	 */
	public synchronized boolean removeNation(String nation) {
		writeLock.lock();
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			
			int rows = db.delete(TABLE_NATIONS, KEY_NAME+"=?", new String[]{nation});
			
			return rows != 0;
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
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NATIONS);
	        
	        // Create table without tutorial
	        createTable(db, false);
	    	db.close();
		} finally {
			writeLock.unlock();
		}
	}

	public synchronized boolean nationExists(String nation) {
		readLock.lock();
		SQLiteDatabase db = null;
		try {
			db = getReadableDatabase();
			Cursor c = db.rawQuery("SELECT COUNT(*) FROM "+TABLE_NATIONS+" WHERE "+KEY_NAME+"=?", new String[]{nation});
    		c.moveToFirst();
    		
    		return c.getInt(0) > 0;
		} finally {
	    	db.close();
			readLock.unlock();
		}
	}
}
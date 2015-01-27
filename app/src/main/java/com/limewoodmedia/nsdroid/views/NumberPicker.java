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
package com.limewoodmedia.nsdroid.views;

import com.limewoodmedia.nsdroid.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class NumberPicker extends RelativeLayout implements OnClickListener {
	private static final int MIN_VALUE = 0;
	
	private EditText text;
	private Button increase;
	private Button decrease;
	
	public NumberPicker(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    
	    LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    inflater.inflate(R.layout.number_picker, this, true);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		text = (EditText) findViewById(R.id.number_picker_text);
		text.setText(Integer.toString(MIN_VALUE));
		increase = (Button) findViewById(R.id.number_picker_increase);
		increase.setOnClickListener(this);
		decrease = (Button) findViewById(R.id.number_picker_decrease);
		decrease.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int page = Integer.parseInt(text.getText().toString());
		if(v == increase) {
			text.setText(Integer.toString(page+1));
		}
		else if(v == decrease && page > MIN_VALUE) {
			text.setText(Integer.toString(page-1));
		}
	}
	
	public int getNumber() {
		return Integer.parseInt(text.getText().toString());
	}
	
	public void setNumber(int number) {
		text.setText(Integer.toString(number));
	}
}

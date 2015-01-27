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
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class LoadingView extends FrameLayout {
	private TextView text;
	private ImageView image;
	
	public LoadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		text = (TextView) findViewById(R.id.loading_text);
		image = (ImageView) findViewById(R.id.loading_image);
	}
	
	public void startLoading(String loadingText) {
        if(getVisibility() == View.VISIBLE && text.getText().equals(loadingText)) return;
		setVisibility(View.VISIBLE);
		if(loadingText != null) {
			text.setText(loadingText);
			text.setVisibility(View.VISIBLE);
		}
		else {
			text.setVisibility(View.GONE);
		}
		image.post(new Runnable() {
			@Override
			public void run() {
            	((AnimationDrawable)image.getDrawable()).start();
			}
		});
	}
	
	public void stopLoading() {
		((AnimationDrawable)image.getDrawable()).stop();
		setVisibility(View.GONE);
	}
}

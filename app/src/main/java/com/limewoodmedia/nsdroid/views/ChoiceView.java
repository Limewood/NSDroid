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
import android.widget.TextView;

public class ChoiceView extends TextView {
	private static final int[] STATE_DISMISSED = {R.attr.state_dismissed};
	
	private boolean dismissed = false;
	
	public ChoiceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	}
	
	public void setDismissed(boolean dismissed) {
		this.dismissed = dismissed;
		if(dismissed) {
			setTextColor(getResources().getColor(R.color.dismissed_colour));
		} else {
			setTextColor(getResources().getColor(android.R.color.black));
		}
		refreshDrawableState();
	}
	
	public boolean isDismissed() {
		return dismissed;
	}
	
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
	    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
	    if(dismissed) {
	        mergeDrawableStates(drawableState, STATE_DISMISSED);
	    }
	    return drawableState;
	}
}

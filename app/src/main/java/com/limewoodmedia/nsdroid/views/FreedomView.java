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

package com.limewoodmedia.nsdroid.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.limewoodmedia.nsdroid.ColorInterpolator;
import com.limewoodmedia.nsdroid.R;
import com.limewoodmedia.nsdroid.Utils;

/**
 * Custom view for freedom values
 * @author Joakim Lindskog
 */
public class FreedomView extends LinearLayout {
	private TextView text;

	public FreedomView(Context context) {
		super(context);
		init(null, 0);
	}

	public FreedomView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs, 0);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public FreedomView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle) {
		// Inflate view
		inflate(getContext(), R.layout.freedom_bubble, this);

		this.text = (TextView) findViewById(R.id.freedom_value);

		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.FreedomView, defStyle, 0);

		String label = a.getString(
				R.styleable.FreedomView_label);
		if(label != null) {
			((TextView)findViewById(R.id.freedom_label)).setText(label);
		}

		a.recycle();
	}

	public void setText(CharSequence str) {
		this.text.setText(str);
	}

	public void setGradient(ColorInterpolator.Color start, ColorInterpolator.Color end) {
		View v = findViewById(R.id.freedom_value);
		GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
				Color.argb(ColorInterpolator.toIntRGB(start.a),
						ColorInterpolator.toIntRGB(start.r),
						ColorInterpolator.toIntRGB(start.g),
						ColorInterpolator.toIntRGB(start.b)),
				Color.argb(ColorInterpolator.toIntRGB(end.a),
						ColorInterpolator.toIntRGB(end.r),
						ColorInterpolator.toIntRGB(end.g),
						ColorInterpolator.toIntRGB(end.b))
		});
		drawable.setShape(GradientDrawable.RECTANGLE);
		int px = Utils.dpToPx(17, getContext());
		drawable.setCornerRadii(new float[]{0, 0, 0, 0, px, px, px, px});
		v.setBackgroundDrawable(drawable);
	}
}

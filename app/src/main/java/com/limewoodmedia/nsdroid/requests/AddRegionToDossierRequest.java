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
package com.limewoodmedia.nsdroid.requests;

import com.android.volley.Response;
import com.limewoodmedia.nsdroid.API;

import java.util.HashMap;
import java.util.Map;

/**
 * Request to add region to dossier
 * Created by joakim on 2016-03-08.
 */
public class AddRegionToDossierRequest extends NSStringRequest {
    private Map<String, String> mParams;

    public AddRegionToDossierRequest(String region, Response.Listener<String> listener, Response.ErrorListener errorListener, String userAgent) {
        super(Method.POST, API.BASE_URL + "/page=dossier/action=add/region=" + region, listener, errorListener, userAgent);
        mParams = new HashMap<String, String>();
    }

    @Override
    public Map<String, String> getParams() {
        return mParams;
    }
}

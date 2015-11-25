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

package com.limewoodMedia.nsapi.enums;

import com.limewoodMedia.nsapi.holders.NationData;

/**
 * An enum of government departments in a nation
 * Created by Joakim Lindskog on 2015-01-31.
 */
public enum Department implements IDescriptable {
    ADMINISTRATION("Administration", NationData.Shards.SubTags.BUDGET_ADMINISTRATION),
    DEFENCE("Defence", NationData.Shards.SubTags.BUDGET_DEFENCE),
    EDUCATION("Education", NationData.Shards.SubTags.BUDGET_EDUCATION),
    ENVIRONMENT("Environment", NationData.Shards.SubTags.BUDGET_ENVIRONMENT),
    HEALTHCARE("Healthcare", NationData.Shards.SubTags.BUDGET_HEALTHCARE),
    COMMERCE("Industry", NationData.Shards.SubTags.BUDGET_COMMERCE),
    INTERNATIONAL_AID("International Aid", NationData.Shards.SubTags.BUDGET_INTERNATIONAL_AID),
    LAW_AND_ORDER("Law & Order", NationData.Shards.SubTags.BUDGET_LAW_AND_ORDER),
    PUBLIC_TRANSPORT("Public Transport", NationData.Shards.SubTags.BUDGET_PUBLIC_TRANSPORT),
    SOCIAL_EQUALITY("Social Policy", NationData.Shards.SubTags.BUDGET_SOCIAL_EQUALITY),
    SPIRITUALITY("Spirituality", NationData.Shards.SubTags.BUDGET_SPIRITUALITY),
    WELFARE("Welfare", NationData.Shards.SubTags.BUDGET_WELFARE);

    private String description;
    private NationData.Shards.SubTags subTag;

    public static Department parse(String name) {
        for (Department d : values()) {
            if (d.description.equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }

    public static Department parseTag(String tagName) {
        for (Department d : values()) {
            if (d.subTag.getTag().equals(tagName)) {
                return d;
            }
        }
        return null;
    }

    private Department(String description, NationData.Shards.SubTags subTag) {
        this.description = description;
        this.subTag = subTag;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return description;
    }
}

package com.limewoodMedia.nsapi.enums;

import com.limewoodMedia.nsapi.holders.NationData;

/**
 * Industry sectors
 * Created by joakim on 2015-11-25.
 */
public enum IndustrySector implements IDescriptable {
    BLACKMARKET("Black market", NationData.Shards.SubTags.SECTORS_BLACKMARKET),
    GOVERNMENT("Government", NationData.Shards.SubTags.SECTORS_GOVERNMENT),
    INDUSTRY("Private industry", NationData.Shards.SubTags.SECTORS_INDUSTRY),
    PUBLIC("State-owned industry", NationData.Shards.SubTags.SECTORS_PUBLIC);

    private String description;
    private NationData.Shards.SubTags subTag;

    public static IndustrySector parse(String name) {
        for (IndustrySector d : values()) {
            if (d.description.equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }

    public static IndustrySector parseTag(String tagName) {
        for (IndustrySector d : values()) {
            if (d.subTag.getTag().equals(tagName)) {
                return d;
            }
        }
        return null;
    }

    private IndustrySector(String description, NationData.Shards.SubTags subTag) {
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

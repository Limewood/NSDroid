package com.limewoodMedia.nsapi;

import com.limewoodMedia.nsapi.enums.Department;
import com.limewoodMedia.nsapi.enums.IDescriptable;
import com.limewoodMedia.nsapi.enums.IndustrySector;

import java.util.Map;

/**
 * Map entries that can be compared
 * Created by joakim on 2015-11-25.
 */
public class DescriptionMapEntry implements Comparable<Map.Entry<IDescriptable, Float>>, Map.Entry<IDescriptable, Float> {
    private IDescriptable key;
    private Float value;

    public DescriptionMapEntry(IDescriptable key, Float value) {
        this.key = key;
        this.value = value;
    }

    public DescriptionMapEntry(Map.Entry<Department, Float> entry) {
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    public DescriptionMapEntry(Map.Entry<IndustrySector, Float> entry, boolean unused) {
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    @Override
    public int compareTo(Map.Entry<IDescriptable, Float> o) {
        return key.getDescription().compareTo(o.getKey().getDescription());
    }

    @Override
    public IDescriptable getKey() {
        return key;
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public Float setValue(Float value) {
        this.value = value;
        return this.value;
    }
}

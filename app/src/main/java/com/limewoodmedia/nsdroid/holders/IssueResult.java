package com.limewoodmedia.nsdroid.holders;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of an issue
 * Created by joakim on 2016-03-03.
 */
public class IssueResult {
    public String result;
    public List<CensusChange> censusChangeList;

    public IssueResult() {
        this.censusChangeList = new ArrayList<>();
    }
}

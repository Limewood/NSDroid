package com.limewoodMedia.nsapi.holders;

import java.util.ArrayList;
import java.util.List;

/**
 * Officer
 * Created by joakim on 2016-03-03.
 */
public class Officer {
    public enum Authority {
        EXECUTIVE('X', "Executive"),
        WORLD_ASSEMBLY('W', "World Assembly"),
        APPEARANCE('A', "Appearance"),
        BORDER_CONTROL('B', "Border Control"),
        COMMUNICATIONS('C', "Communications"),
        EMBASSIES('E', "Embassies"),
        POLLS('P', "Polls");

        public char code;
        public String name;

        Authority(char code, String name) {
            this.code = code;
            this.name = name;
        }

        public static Authority getByCode(char code) {
            for(Authority a : Authority.values()) {
                if(a.code == code) {
                    return a;
                }
            }
            return null;
        }
    }

    public String nation;
    public String office;
    public long appointed;
    public String appointer;
    public int order;
    public List<Authority> authority;

    public Officer() {
        this.authority = new ArrayList<>();
    }
}

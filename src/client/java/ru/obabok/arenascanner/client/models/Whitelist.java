package ru.obabok.arenascanner.client.models;

import java.util.ArrayList;

public class Whitelist {
    public ArrayList<WhitelistItem> whitelist;
    public Whitelist(ArrayList<WhitelistItem> _whitelist){
        this.whitelist = _whitelist;
    }
}

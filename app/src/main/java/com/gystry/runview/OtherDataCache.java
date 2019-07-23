package com.gystry.runview;

import java.util.List;

public class OtherDataCache {
    private List<OtherOneData> list;

    private OtherDataCache() {

    }

    private static class OtherHolder {
        static OtherDataCache otherDataCache = new OtherDataCache();
    }

    public static OtherDataCache getInstance() {
        return OtherHolder.otherDataCache;
    }

    public List<OtherOneData> getList() {
        return list;
    }

    public void setList(List<OtherOneData> list) {
        this.list = list;
    }
}

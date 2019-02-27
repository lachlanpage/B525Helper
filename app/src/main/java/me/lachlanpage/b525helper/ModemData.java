package me.lachlanpage.b525helper;

// Simple class to handle data creation via Modem class.
// Each ModemData class corresponds to one row in a list view and is handled by ModemDataAdapter
public class ModemData {

    private String mDataName;
    private String mDataKey; // key refers to RESULT

    public String getmDataName() {
        return mDataName;
    }

    public void setmDataName(String mDataName) {
        this.mDataName = mDataName;
    }

    public String getmDataKey() {
        return mDataKey;
    }

    public void setmDataKey(String mDataKey) {
        this.mDataKey = mDataKey;
    }

    public ModemData(String name, String key) {
        mDataName = name;
        mDataKey = key;
    }


}

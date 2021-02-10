package it.systemslab.systemslabnotificationlistener;

import android.content.Context;
import android.content.SharedPreferences;

import static it.systemslab.systemslabnotificationlistener.Const.ALIAS;
import static it.systemslab.systemslabnotificationlistener.Const.KEY;

public class SharedPrefManager {

    private static SharedPreferences sp;
    private final static String VALUE_NOT_FOUND = "VALUE_NOT_FOUND";
    private static SharedPreferences.Editor editor;
    private final Context context;

    public SharedPrefManager(Context context) {
        this.context = context;
    }

    public static SharedPrefManager getInstance(Context context) {
        return new SharedPrefManager(context);
    }

    private void openHandler() {
        String file = context.getString(R.string.preference_file_key);
        if (sp == null)
            sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
    }

    private void init() {
        if (sp == null) openHandler();
        if (editor == null) editor = sp.edit();
    }

    private void writeString(String key, String value) {
        init();
        editor.putString(key, value);
        editor.commit();
    }

    private String readString(String key) {
        if (key == null) return null;
        if (sp == null) openHandler();

        return sp.getString(key, null);
    }

    public String getAlias() {
        return readString(ALIAS);
    }

    public void setAlias(String alias) {
        writeString(ALIAS, alias);
    }

    public String getKey() {
        return readString(KEY);
    }

    public void setKey(String key) {
        writeString(KEY, key);
    }
}
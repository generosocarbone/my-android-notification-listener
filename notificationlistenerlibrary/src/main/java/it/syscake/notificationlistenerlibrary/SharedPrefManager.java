package it.syscake.notificationlistenerlibrary;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private static final String ALIAS = "it.systemslab.systemslabnotificationlistener.alias";
    private static final String KEY = "it.systemslab.systemslabnotificationlistener.key";

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

    private void writeBoolean(String key, boolean value) {
        init();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private boolean readBoolean(String key) {
        if (key == null) return false;
        if (sp == null) openHandler();

        try {
            return sp.getBoolean(key, false);
        } catch (Exception e) {
            writeBoolean(key, false);
            return false;
        }
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

    public void enablePackage(String packageName) {
        writeBoolean(packageName, true);
    }

    public void disablePackage(String packageName) {
        writeBoolean(packageName, false);
    }

    public boolean getPackageEnabled(String packageName) {
        return readBoolean(packageName);
    }
}
package com.archos.mediacenter.video.utils;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
public final class SettingsBackup {
    public static String encode(SharedPreferences prefs) throws JSONException {
        JSONObject root = new JSONObject();
        for (Map.Entry<String,?> entry : prefs.getAll().entrySet()) {
            Object v = entry.getValue(); JSONObject item = new JSONObject();
            String type = v instanceof Boolean ? "boolean" : v instanceof Integer ? "int" : v instanceof Long ? "long"
                : v instanceof Float ? "float" : v instanceof Set ? "set" : "string";
            item.put("type", type);
            item.put("value", v instanceof Set ? new JSONArray((Set<?>)v) : v);
            root.put(entry.getKey(),item);
        }
        return root.toString();
    }
    public static SharedPreferences.Editor decode(SharedPreferences prefs, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        SharedPreferences.Editor editor = prefs.edit();
        for (Iterator<String> it = root.keys(); it.hasNext();) {
            String key = it.next(); JSONObject item = root.getJSONObject(key);
            switch (item.getString("type")) {
                case "boolean": editor.putBoolean(key,item.getBoolean("value")); break;
                case "int": editor.putInt(key,item.getInt("value")); break;
                case "long": editor.putLong(key,item.getLong("value")); break;
                case "float": editor.putFloat(key,(float)item.getDouble("value")); break;
                case "string": editor.putString(key,item.getString("value")); break;
                case "set":
                    Set<String> values = new HashSet<>(); JSONArray array = item.getJSONArray("value");
                    for(int i=0;i<array.length();i++) values.add(array.getString(i));
                    editor.putStringSet(key,values); break;
                default: throw new JSONException("Unsupported setting type");
            }
        }
        return editor;
    }
    private SettingsBackup() {}
}

package com.example.newsai.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookmarkStorage {
    private static final String PREF_NAME = "bookmark_storage";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String TAG = "BookmarkStorage";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static List<SavedArticle> getBookmarks(Context context) {
        List<SavedArticle> list = new ArrayList<>();
        String raw = prefs(context).getString(KEY_BOOKMARKS, null);
        if (TextUtils.isEmpty(raw)) return list;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(fromJson(obj));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse bookmarks", e);
        }
        return list;
    }

    public static void addBookmark(Context context, SavedArticle article) {
        try {
            List<SavedArticle> current = getBookmarks(context);
            String key = getKey(article);
            List<SavedArticle> filtered = new ArrayList<>();
            for (SavedArticle item : current) {
                if (!key.equals(getKey(item))) {
                    filtered.add(item);
                }
            }
            filtered.add(article);
            save(context, filtered);
        } catch (Exception e) {
            Log.e(TAG, "addBookmark error", e);
        }
    }

    public static void removeBookmark(Context context, String key) {
        if (TextUtils.isEmpty(key)) return;
        List<SavedArticle> current = getBookmarks(context);
        List<SavedArticle> filtered = new ArrayList<>();
        for (SavedArticle article : current) {
            if (!key.equals(getKey(article))) {
                filtered.add(article);
            }
        }
        save(context, filtered);
    }

    public static boolean isBookmarked(Context context, String key) {
        if (TextUtils.isEmpty(key)) return false;
        List<SavedArticle> current = getBookmarks(context);
        for (SavedArticle article : current) {
            if (key.equals(getKey(article))) return true;
        }
        return false;
    }

    private static void save(Context context, List<SavedArticle> list) {
        JSONArray array = new JSONArray();
        for (SavedArticle article : list) {
            array.put(toJson(article));
        }
        prefs(context).edit().putString(KEY_BOOKMARKS, array.toString()).apply();
    }

    private static JSONObject toJson(SavedArticle article) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", article.getId());
            obj.put("title", article.getTitle());
            obj.put("imageUrl", article.getImageUrl());
            obj.put("url", article.getUrl());
            obj.put("sourceUrl", article.getSourceUrl());
            obj.put("content", article.getContent());
            obj.put("date", article.getDate());
            obj.put("postedAt", article.getPostedAt());
            obj.put("sentiment", article.getSentiment());
            obj.put("spam", article.getSpam());
        } catch (JSONException ignored) {}
        return obj;
    }

    private static SavedArticle fromJson(JSONObject obj) {
        return new SavedArticle(
                obj.optString("id", ""),
                obj.optString("title", ""),
                obj.optString("imageUrl", ""),
                obj.optString("url", ""),
                obj.optString("sourceUrl", ""),
                obj.optString("content", ""),
                obj.optString("date", ""),
                obj.optString("postedAt", ""),
                obj.optString("sentiment", ""),
                obj.optString("spam", "")
        );
    }

    private static String getKey(SavedArticle article) {
        if (article == null) return "";
        if (!TextUtils.isEmpty(article.getUrl())) return article.getUrl();
        if (!TextUtils.isEmpty(article.getId())) return article.getId();
        return article.getTitle();
    }
}


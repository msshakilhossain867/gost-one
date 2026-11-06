package com.citomoltd.banglagoststory.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.HashMap;

public class DatabaseHelper extends SQLiteAssetHelper {
    public DatabaseHelper(Context context) {
        super(context, "story_data.db", null, 1);
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from rs_story", null);
    }

    public Cursor getAllDataVt() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from vt_story", null);
    }

    public Cursor getAllDataBookmark() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from bookmark", null);
    }

    public Cursor getAllDataPrime() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from prime_story", null);
    }

    public void deleteBookmark(String id) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete("bookmark", "id=?", new String[]{id});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isBookmarked(String name, String link) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT id FROM bookmark WHERE name = ? OR link = ?", new String[]{name, link});
            return (cursor != null && cursor.moveToFirst());
        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public boolean removeBookmark(String name, String link) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            int rows = db.delete("bookmark", "name = ? OR link = ?", new String[]{name, link});
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getTableCount(String tableName) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public void updateFav(int id, int fav) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_fav", fav);
        db.update("rs_story", contentValues, "id=?", new String[]{String.valueOf(id)});

    }

    public void updateFaVT(int id, int fav) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_fav", fav);
        db.update("vt_story", contentValues, "id=?", new String[]{String.valueOf(id)});
    }

    public void updateFavPrime(int id, int fav) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_fav", fav);
        db.update("prime_story", contentValues, "id=?", new String[]{String.valueOf(id)});
    }

    public Cursor getAllFavorites() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT id, name, link, 'rs' AS story_type FROM rs_story WHERE is_fav = 1 " +
                "UNION ALL " +
                "SELECT id, name, link, 'vt' AS story_type FROM vt_story WHERE is_fav = 1 " +
                "UNION ALL " +
                "SELECT id, name, link, 'prime' AS story_type FROM prime_story WHERE is_fav = 1";
        return db.rawQuery(query, null);
    }

    public Cursor searchFavorites(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT id, name, link, 'rs' AS story_type FROM rs_story WHERE is_fav = 1 AND name LIKE '%" + key + "%' " +
                "UNION ALL " +
                "SELECT id, name, link, 'vt' AS story_type FROM vt_story WHERE is_fav = 1 AND name LIKE '%" + key + "%' " +
                "UNION ALL " +
                "SELECT id, name, link, 'prime' AS story_type FROM prime_story WHERE is_fav = 1 AND name LIKE '%" + key + "%'";
        return db.rawQuery(query, null);
    }

    public void removeFavorite(String id, String storyType, String storyName) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("is_fav", 0);
            if ("vt".equals(storyType)) {
                db.update("vt_story", cv, "id=?", new String[]{id});
            } else if ("prime".equals(storyType)) {
                db.update("prime_story", cv, "id=?", new String[]{id});
            } else if ("rs".equals(storyType)) {
                db.update("rs_story", cv, "id=?", new String[]{id});
            }
            if (storyName != null && !storyName.isEmpty()) {
                db.update("rs_story", cv, "name=?", new String[]{storyName});
                db.update("vt_story", cv, "name=?", new String[]{storyName});
                db.update("prime_story", cv, "name=?", new String[]{storyName});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getFavoriteCount() {
        Cursor cursor = null;
        try {
            cursor = getAllFavorites();
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return 0;
    }

    public int getBookmarkCount() {
        return getTableCount("bookmark");
    }

    public HashMap<String, String> getRandomStory() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT name, link FROM (" +
                    "SELECT name, link FROM rs_story " +
                    "UNION ALL SELECT name, link FROM vt_story " +
                    "UNION ALL SELECT name, link FROM prime_story" +
                    ") ORDER BY RANDOM() LIMIT 1";
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", cursor.getString(0));
                map.put("link", cursor.getString(1));
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        // Secondary fallback in case of any issues
        try {
            cursor = db.rawQuery("SELECT name, link FROM rs_story LIMIT 1", null);
            if (cursor != null && cursor.moveToFirst()) {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", cursor.getString(0));
                map.put("link", cursor.getString(1));
                return map;
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public String getStoryLinkByName(String title) {
        if (title == null || title.trim().isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT link FROM (" +
                    "SELECT name, link FROM rs_story " +
                    "UNION ALL SELECT name, link FROM vt_story " +
                    "UNION ALL SELECT name, link FROM prime_story" +
                    ") WHERE name = ? LIMIT 1";
            cursor = db.rawQuery(query, new String[]{title.trim()});
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }


    public boolean adBookmark(String name, String link) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getWritableDatabase();

            // Check if the item already exists
            String query = "SELECT * FROM bookmark WHERE name = ? OR link = ?";
            cursor = db.rawQuery(query, new String[]{name, link});

            if (cursor != null && cursor.moveToFirst()) {
                // Item already exists
                return false;
            }

            // Insert the new bookmark if it doesn't exist
            ContentValues conVal = new ContentValues();
            conVal.put("name", name);
            conVal.put("link", link);

            long result = db.insert("bookmark", null, conVal);

            return result != -1; // Return true if insertion succeeded
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Failure
        } finally {
            if (cursor != null) {
                cursor.close(); // Close the cursor
            }
            if (db != null) {
                db.close(); // Ensure the database is closed
            }
        }
    }


    public Cursor searchDat(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from bookmark where name like '%" + key + "%'", null);
    }

    public Cursor searchPrime(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from prime_story where name like '%" + key + "%'", null);
    }

    public Cursor searchRS(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from rs_story where name like '%" + key + "%'", null);
    }

    public Cursor searchVt(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from vt_story where name like '%" + key + "%'", null);
    }

}

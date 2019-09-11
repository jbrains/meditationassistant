package sh.ftp.rocketninelabs.meditationassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "meditationassistant";
    private static final String TABLE_SESSIONS = "sessions";

    // Column names
    private static final String KEY_ID = "id";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_STARTED = "started";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_LENGTH = "length";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ISPOSTED = "isposted";
    private static final String KEY_STREAKDAY = "streakday";
    private static final String KEY_MODIFIED = "modified";
    private static DatabaseHandler databaseHandler;

    private SQLiteDatabase db = null;
    private MeditationAssistant ma = null;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        ma = (MeditationAssistant) context;
        db = this.getWritableDatabase();
    }

    public static synchronized DatabaseHandler getInstance(Context context) {
        if (databaseHandler == null) {
            databaseHandler = new DatabaseHandler(context.getApplicationContext());
        }
        return databaseHandler;
    }

    private ArrayList<SessionSQL> unmarshalResult(Cursor c) {
        ArrayList<SessionSQL> sessionList = new ArrayList<SessionSQL>();
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    SessionSQL session = new SessionSQL(
                            c.getLong(c.getColumnIndex(KEY_ID)),
                            c.getLong(c.getColumnIndex(KEY_STARTED)),
                            c.getLong(c.getColumnIndex(KEY_COMPLETED)),
                            c.getLong(c.getColumnIndex(KEY_LENGTH)),
                            c.getString(c.getColumnIndex(KEY_MESSAGE)),
                            c.getLong(c.getColumnIndex(KEY_ISPOSTED)),
                            c.getLong(c.getColumnIndex(KEY_STREAKDAY)),
                            c.getLong(c.getColumnIndex(KEY_MODIFIED)));

                    sessionList.add(session);
                } while (c.moveToNext());
            }

            c.close();
        }

        return sessionList;
    }

    private MeditationAssistant getMeditationAssistant() {
        return ma;
    }

    boolean addSession(SessionSQL session, long updateSessionStarted) {
        if (session._modified == 0) {
            session._modified = getMeditationAssistant().getTimestamp();
        }

        ContentValues values = new ContentValues();
        values.put(KEY_STARTED, session._started);
        values.put(KEY_COMPLETED, session._completed);
        values.put(KEY_LENGTH, session._length);
        values.put(KEY_MESSAGE, session._message);
        values.put(KEY_ISPOSTED, session._isposted);
        values.put(KEY_STREAKDAY, session._streakday);
        values.put(KEY_MODIFIED, session._modified);

        if (updateSessionStarted == 0) {
            updateSessionStarted = session._started;
        }

        ArrayList<SessionSQL> existingSessions = unmarshalResult(db.rawQuery("SELECT  * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`=? OR `" + KEY_STARTED + "`=?", new String[]{String.valueOf(session._started), String.valueOf(updateSessionStarted)}));
        if (existingSessions.isEmpty()) {
            Log.d("MeditationAssistant", "INSERTING");
            db.insert(TABLE_SESSIONS, null, values);
        } else {
            Log.d("MeditationAssistant", "UPDATING");
            for (SessionSQL es : existingSessions) {
                if (es._modified >= session._modified) {
                    Log.d("MeditationAssistant", "EXISTING SESSION MODIFIED");
                    return false;
                }
            }

            Cursor c = db.rawQuery("UPDATE `" + TABLE_SESSIONS + "` SET `" + KEY_STARTED + "`=?, `" + KEY_COMPLETED + "`=?, `" + KEY_LENGTH + "`=?, `" + KEY_MESSAGE + "`=?, `" + KEY_ISPOSTED + "`=?, `" + KEY_STREAKDAY + "`=?, `" + KEY_MODIFIED + "`=? WHERE `" + KEY_STARTED + "`=? OR`" + KEY_STARTED + "`=?",
                    new String[]{String.valueOf(session._started), String.valueOf(session._completed), String.valueOf(session._length), session._message, String.valueOf(session._isposted), String.valueOf(session._streakday), String.valueOf(session._modified), String.valueOf(session._started), String.valueOf(updateSessionStarted)});
            c.moveToFirst();
            c.close();
        }

        getMeditationAssistant().notifySessionsUpdated();
        return true;
    }

    public void deleteSession(SessionSQL session) {
        db.delete(TABLE_SESSIONS, KEY_ID + " = ?",
                new String[]{String.valueOf(session.getID())});

        getMeditationAssistant().notifySessionsUpdated();
    }

    public ArrayList<SessionSQL> getAllSessions() {
        String selectQuery = "SELECT  * FROM `" + TABLE_SESSIONS + "` ORDER BY " + "`" + KEY_STARTED + "` DESC";
        return unmarshalResult(db.rawQuery(selectQuery, null));
    }

    public int getNumSessions() {
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_SESSIONS);
    }

    public int getTotalTimeSpentMeditating() {
        int time_meditating = 0;
        Cursor cursor = db.rawQuery("SELECT SUM(`" + KEY_LENGTH + "`) FROM  `" + TABLE_SESSIONS + "`", null);
        if (cursor.moveToFirst()) {
            time_meditating = cursor.getInt(0);
        }
        cursor.close();

        return time_meditating;
    }

    int numSessionsByDate(Calendar date) {
        ArrayList<Long> sessionWindow = getMeditationAssistant().dateToSessionWindow(date);
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_SESSIONS, "started>=? AND started<?", new String[]{String.valueOf(sessionWindow.get(0)), String.valueOf(sessionWindow.get(1))});
    }

    int numSessionsByStarted(long started) {
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_SESSIONS, "started=?", new String[]{String.valueOf(started)});
    }

    public ArrayList<SessionSQL> getSessionsByDate(Calendar date) {
        ArrayList<Long> sessionWindow = getMeditationAssistant().dateToSessionWindow(date);
        return unmarshalResult(db.rawQuery("SELECT * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`>=? AND `" + KEY_STARTED + "`<? ORDER BY `" + KEY_STARTED + "` ASC", new String[]{String.valueOf(sessionWindow.get(0)), String.valueOf(sessionWindow.get(1))}));
    }

    public SessionSQL getSessionByDate(Calendar date) {
        ArrayList<SessionSQL> sessions = getSessionsByDate(date);
        if (sessions.isEmpty()) {
            return null;
        }

        return sessions.get(0);
    }

    SessionSQL getSessionByStarted(long started) {
        ArrayList<SessionSQL> sessions = unmarshalResult(db.rawQuery("SELECT * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`=? LIMIT 1", new String[]{String.valueOf(started)}));
        if (sessions.isEmpty()) {
            return null;
        }

        return sessions.get(0);
    }

    int getLongestSessionLength() {
        int longestsessionlength = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(`" + KEY_LENGTH + "`) FROM `" + TABLE_SESSIONS + "`", null);
        if (cursor.moveToFirst()) {
            longestsessionlength = cursor.getInt(0);
        }
        cursor.close();

        return longestsessionlength;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("MeditationAssistant", "CREATING DATABASE VERSION " + String.valueOf(DATABASE_VERSION));
        db.execSQL("CREATE TABLE `" + TABLE_SESSIONS + "` ("
                + "`" + KEY_ID + "` INTEGER PRIMARY KEY, "
                + "`" + KEY_STARTED + "` INTEGER, `" + KEY_COMPLETED + "` INTEGER, "
                + "`" + KEY_LENGTH + "` INTEGER, `" + KEY_MESSAGE + "` STRING, "
                + "`" + KEY_ISPOSTED + "` INTEGER, `" + KEY_STREAKDAY + "` INTEGER, "
                + "`" + KEY_MODIFIED + "` INTEGER" + ")");

        db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
        db.execSQL("CREATE INDEX `" + KEY_COMPLETED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_COMPLETED + "`)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("MeditationAssistant", "DATABASE UPGRADE INITIATED - Old: " + String.valueOf(oldVersion) + " New: " + String.valueOf(newVersion));
        int curVer = oldVersion;
        while (curVer < newVersion) {
            curVer++;
            Log.d("MeditationAssistant", "UPGRADING DATABASE to " + String.valueOf(curVer));
            switch (curVer) {
                case 2:
                    db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_STREAKDAY + "` INTEGER");
                    break;
                case 3:
                    db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
                    break;
                case 4:
                    /* Fix for incorrect upgrade code */
                    try {
                        db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_STREAKDAY + "` INTEGER");
                    } catch (Exception e) {
                        // Column already exists
                    }
                    try {
                        db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
                    } catch (Exception e) {
                        // Column already exists
                    }

                    db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_COMPLETED + "` INTEGER");
                    db.execSQL("CREATE INDEX `" + KEY_COMPLETED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_COMPLETED + "`)");

                    Cursor cursor = db.rawQuery("SELECT  * FROM `" + TABLE_SESSIONS + "`", null);
                    if (cursor.moveToFirst()) {
                        long session_completed;

                        do {
                            try {
                                session_completed = cursor.getLong(cursor.getColumnIndex(KEY_COMPLETED));
                            } catch (Exception e) {
                                session_completed = 0;
                                Log.d("MeditationAssistant", "Error fetching completed:");
                                e.printStackTrace();
                            }

                            if (session_completed == 0) {
                                Log.d("MeditationAssistant", "UPDATE `" + TABLE_SESSIONS + "` SET `" + KEY_COMPLETED +
                                        "`='" + (cursor.getLong(cursor.getColumnIndex(KEY_STARTED)) + cursor.getLong(cursor.getColumnIndex(KEY_LENGTH))) + "' WHERE `" + KEY_ID + "`='"
                                        + cursor.getLong(cursor.getColumnIndex(KEY_ID)) + "'");
                                Cursor c = db.rawQuery("UPDATE `" + TABLE_SESSIONS + "` SET `" + KEY_COMPLETED +
                                        "`='" + (cursor.getLong(cursor.getColumnIndex(KEY_STARTED)) + cursor.getLong(cursor.getColumnIndex(KEY_LENGTH))) + "' WHERE `" + KEY_ID + "`='"
                                        + cursor.getLong(cursor.getColumnIndex(KEY_ID)) + "'", null);
                                c.moveToFirst();
                                c.close();
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();

                    break;
                case 5:
                    try {
                        db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_MODIFIED + "` INTEGER");
                    } catch (Exception e) {
                        // Column already exists
                    }

                    break;
            }
        }
    }
}
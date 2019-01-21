package sh.ftp.rocketninelabs.meditationassistant;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 4;
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

    public ArrayList<Long> dateToSessionWindow(Calendar c) {
        ArrayList<Long>sessionWindow = new ArrayList<Long>();
        ArrayList<Integer> streakbuffertime = getMeditationAssistant().getStreakBufferTime();
        Calendar sessionWindowCalendar = (Calendar) c.clone();
        
        sessionWindowCalendar.set(Calendar.HOUR_OF_DAY, streakbuffertime.get(0));
        sessionWindowCalendar.set(Calendar.MINUTE, streakbuffertime.get(1));
        sessionWindow.add(sessionWindowCalendar.getTimeInMillis() / 1000);

        sessionWindowCalendar.add(Calendar.DATE, 1);
        sessionWindowCalendar.set(Calendar.HOUR_OF_DAY, streakbuffertime.get(0));
        sessionWindowCalendar.set(Calendar.MINUTE, streakbuffertime.get(1));
        sessionWindow.add(sessionWindowCalendar.getTimeInMillis() / 1000);

        return sessionWindow;
    }

    public String sessionToDate(SessionSQL session) {
        if (session._completed != null) {
            return timestampToDate(session._completed * 1000);
        } else {
            return timestampToDate((session._started + session._length) * 1000);
        }
    }

    private String timestampToDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.US);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        Date api_date = cal.getTime();

        return sdf.format(api_date);
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
                            c.getLong(c.getColumnIndex(KEY_STREAKDAY)));

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

    void addSession(SessionSQL session) {
        ContentValues values = new ContentValues();
        values.put(KEY_STARTED, session._started);
        values.put(KEY_COMPLETED, session._completed);
        values.put(KEY_LENGTH, session._length);
        values.put(KEY_MESSAGE, session._message);
        values.put(KEY_ISPOSTED, session._isposted);
        values.put(KEY_STREAKDAY, session._streakday);

        db.insert(TABLE_SESSIONS, null, values);

        getMeditationAssistant().notifySessionsUpdated();
    }

    public int updateSession(SessionSQL session) {
        ContentValues values = new ContentValues();
        values.put(KEY_STARTED, session._started);
        values.put(KEY_COMPLETED, session._completed);
        values.put(KEY_LENGTH, session._length);
        values.put(KEY_MESSAGE, session._message);
        values.put(KEY_ISPOSTED, session._isposted);
        values.put(KEY_STREAKDAY, session._streakday);

        int result = db.update(TABLE_SESSIONS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(session.getID())});

        getMeditationAssistant().notifySessionsUpdated();
        return result;
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

    public ArrayList<SessionSQL> getAllLocalSessions() {
        String selectQuery = "SELECT  * FROM `" + TABLE_SESSIONS + "`";
        return unmarshalResult(db.rawQuery(selectQuery, null));
    }

    public int getNumSessions() {
        String countQuery = "SELECT COUNT(*) FROM `" + TABLE_SESSIONS + "`";

        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.moveToFirst();
        int numsessions = cursor.getInt(0);
        cursor.close();

        return numsessions;
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
        ArrayList<Long> sessionWindow = dateToSessionWindow(date);
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`>=? AND `" + KEY_STARTED + "`<?", new String[]{String.valueOf(sessionWindow.get(0)), String.valueOf(sessionWindow.get(1))});

        cursor.moveToFirst();
        int numsessions = cursor.getInt(0);
        cursor.close();

        return numsessions;
    }

    public ArrayList<SessionSQL> getSessionsByDate(Calendar date) {
        ArrayList<Long> sessionWindow = dateToSessionWindow(date);
        return unmarshalResult(db.rawQuery("SELECT * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`>=? AND `" + KEY_STARTED + "`<? ORDER BY `" + KEY_STARTED + "` ASC LIMIT 1", new String[]{String.valueOf(sessionWindow.get(0)), String.valueOf(sessionWindow.get(1))}));
    }

    public SessionSQL getSessionByDate(Calendar date) {
        ArrayList<SessionSQL> sessions = getSessionsByDate(date);
        if (sessions.isEmpty()) {
            return null;
        }

        return sessions.get(0);
    }

    SessionSQL getSessionByStarted(long started) {
        ArrayList<SessionSQL> sessions =  unmarshalResult(db.rawQuery("SELECT * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_STARTED + "`=? LIMIT 1", new String[]{String.valueOf(started)}));
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
                + "`" + KEY_ISPOSTED + "` INTEGER, `" + KEY_STREAKDAY + "` INTEGER" + ")");

        db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
        db.execSQL("CREATE INDEX `" + KEY_COMPLETED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_COMPLETED + "`)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("MeditationAssistant", "DATABASE UPGRADE INITIATED - Old: " + String.valueOf(oldVersion) + " New: " + String.valueOf(newVersion));
        int curVer = oldVersion;
        while (curVer < newVersion) {
            curVer++;
            switch (curVer) {
                case 2:
                    Log.d("MeditationAssistant", "UPGRADING DATABASE to " + String.valueOf(curVer));

                    db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_STREAKDAY + "` INTEGER");
                    break;
                case 3:
                    Log.d("MeditationAssistant", "UPGRADING DATABASE to " + String.valueOf(curVer));

                    db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
                    break;
                case 4:
                    Log.d("MeditationAssistant", "UPGRADING DATABASE to " + String.valueOf(curVer));

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
                                db.rawQuery("UPDATE `" + TABLE_SESSIONS + "` SET `" + KEY_COMPLETED +
                                        "`='" + (cursor.getLong(cursor.getColumnIndex(KEY_STARTED)) + cursor.getLong(cursor.getColumnIndex(KEY_LENGTH))) + "' WHERE `" + KEY_ID + "`='"
                                        + cursor.getLong(cursor.getColumnIndex(KEY_ID)) + "'", null);
                            }
                        } while (cursor.moveToNext());
                    }
                    cursor.close();

                    break;
            }
        }
    }
}
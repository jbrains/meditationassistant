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
    private static final String KEY_DATE = "date";
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

    void addSession(SessionSQL session) {
        ContentValues values = new ContentValues();
        values.put(KEY_STARTED, session._started);
        values.put(KEY_COMPLETED, session._completed);
        values.put(KEY_LENGTH, session._length);
        values.put(KEY_MESSAGE, session._message);
        values.put(KEY_DATE, sessionToAPIDate(session));
        values.put(KEY_ISPOSTED, session._isposted);
        values.put(KEY_STREAKDAY, session._streakday);

        db.insert(TABLE_SESSIONS, null, values);

        getMeditationAssistant().recalculateMeditationStreak();
        getMeditationAssistant().notifySessionsUpdated();
    }

    public String sessionToAPIDate(SessionSQL session) {
        if (session._completed != null) {
            return timestampToAPIDate(session._completed * 1000);
        } else {
            return timestampToAPIDate((session._started + session._length) * 1000);
        }
    }

    public String timestampToAPIDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("d-M-yyyy", Locale.US);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        Date api_date = cal.getTime();

        return sdf.format(api_date);
    }

    public MeditationAssistant getMeditationAssistant() {
        return ma;
    }

    public void deleteSession(SessionSQL session) {
        db.delete(TABLE_SESSIONS, KEY_ID + " = ?",
                new String[]{String.valueOf(session.getID())});

        getMeditationAssistant().notifySessionsUpdated();
    }

    public ArrayList<SessionSQL> getAllSessions() {
        ArrayList<SessionSQL> sessionList = new ArrayList<SessionSQL>();

        String selectQuery = "SELECT  * FROM `" + TABLE_SESSIONS + "` ORDER BY "
                + "`" + KEY_COMPLETED + "` DESC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                SessionSQL session = new SessionSQL(cursor.getLong(cursor
                        .getColumnIndex(KEY_ID)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_COMPLETED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_LENGTH)), cursor.getString(cursor
                        .getColumnIndex(KEY_MESSAGE)), cursor.getLong(cursor
                        .getColumnIndex(KEY_ISPOSTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STREAKDAY)));

                sessionList.add(session);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return sessionList;
    }

    public ArrayList<SessionSQL> getAllLocalSessions() {
        ArrayList<SessionSQL> sessionList = new ArrayList<SessionSQL>();

        String selectQuery = "SELECT  * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_ISPOSTED + "` = 0";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                SessionSQL session = new SessionSQL(cursor.getLong(cursor
                        .getColumnIndex(KEY_ID)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_COMPLETED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_LENGTH)), cursor.getString(cursor
                        .getColumnIndex(KEY_MESSAGE)), cursor.getLong(cursor
                        .getColumnIndex(KEY_ISPOSTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STREAKDAY)));

                sessionList.add(session);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return sessionList;
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

    int numSessionsByDate(Calendar dateCalendar) {
        String date = String.valueOf(dateCalendar.get(Calendar.DAY_OF_MONTH)) + "-"
                + String.valueOf(dateCalendar.get(Calendar.MONTH) + 1) + "-"
                + String.valueOf(dateCalendar.get(Calendar.YEAR));
        Calendar nextDate = (Calendar) dateCalendar.clone();
        nextDate.add(Calendar.DATE, 1);
        String dateLateNight = String.valueOf(nextDate.get(Calendar.DAY_OF_MONTH)) + "-"
                + String.valueOf(nextDate.get(Calendar.MONTH) + 1) + "-"
                + String.valueOf(nextDate.get(Calendar.YEAR));
        Cursor cursor = db.rawQuery("SELECT * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_DATE + "`=? OR `" + KEY_DATE + "`=?", new String[]{date, dateLateNight});

        Calendar startedCalendar = Calendar.getInstance();
        Calendar midnightCalendar = Calendar.getInstance();
        int numsessions = 0;
        if (cursor.moveToFirst()) {
            do {
                String sessionDate = cursor.getString(cursor
                        .getColumnIndex(KEY_DATE));
                long sessionStarted = cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED));

                startedCalendar.setTimeInMillis(sessionStarted * 1000);

                midnightCalendar.setTimeInMillis(sessionStarted * 1000);
                midnightCalendar.set(Calendar.HOUR, 0);
                midnightCalendar.set(Calendar.MINUTE, 0);
                midnightCalendar.set(Calendar.SECOND, 0);
                midnightCalendar.set(Calendar.MILLISECOND, 0);
                if ((sessionDate.equals(date) && startedCalendar.getTimeInMillis() - midnightCalendar.getTimeInMillis() > 14400000) || (sessionDate.equals(dateLateNight) && startedCalendar.getTimeInMillis() - midnightCalendar.getTimeInMillis() <= 14400000)) {
                    numsessions++;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return numsessions;
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

    SessionSQL getSessionByStarted(long started) {
        SessionSQL session = null;

        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{KEY_ID,
                        KEY_STARTED, KEY_COMPLETED, KEY_LENGTH, KEY_MESSAGE, KEY_ISPOSTED, KEY_STREAKDAY},
                KEY_STARTED + "=?", new String[]{String.valueOf(started)},
                null, null, null, "1"
        );
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();

                session = new SessionSQL(cursor.getLong(cursor
                        .getColumnIndex(KEY_ID)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_COMPLETED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_LENGTH)), cursor.getString(cursor
                        .getColumnIndex(KEY_MESSAGE)), cursor.getLong(cursor
                        .getColumnIndex(KEY_ISPOSTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STREAKDAY)));
            }

            cursor.close();
        }

        return session;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("MeditationAssistant", "CREATING DATABASE VERSION " + String.valueOf(DATABASE_VERSION));
        db.execSQL("CREATE TABLE `" + TABLE_SESSIONS + "` ("
                + "`" + KEY_ID + "` INTEGER PRIMARY KEY, "
                + "`" + KEY_STARTED + "` INTEGER, `" + KEY_COMPLETED + "` INTEGER, "
                + "`" + KEY_LENGTH + "` INTEGER, `" + KEY_MESSAGE + "` STRING, "
                + "`" + KEY_DATE + "` STRING, `" + KEY_ISPOSTED + "` INTEGER, `" + KEY_STREAKDAY + "` INTEGER" + ")");

        db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
        db.execSQL("CREATE INDEX `" + KEY_COMPLETED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_COMPLETED + "`)");
        db.execSQL("CREATE INDEX `" + KEY_DATE + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_DATE + "`)");
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

                    /* Fix for incorrect upgrade code */
                    try {
                        db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_DATE + "` STRING");
                    } catch (Exception e) {
                        // Column already exists
                    }
                    db.execSQL("CREATE INDEX `" + KEY_STARTED + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_STARTED + "`)");
                    db.execSQL("CREATE INDEX `" + KEY_DATE + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_DATE + "`)");
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
                        db.execSQL("ALTER TABLE `" + TABLE_SESSIONS + "` ADD COLUMN `" + KEY_DATE + "` STRING");
                        db.execSQL("CREATE INDEX `" + KEY_DATE + "_idx` ON `" + TABLE_SESSIONS + "` (`" + KEY_DATE + "`)");
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

    public SessionSQL getSessionByDate(String date) {
        Log.d("MeditationAssistant", "SQL: get session by date " + date);
        SessionSQL session = null;

        Cursor cursor = db.query(TABLE_SESSIONS, new String[]{KEY_ID,
                        KEY_STARTED, KEY_COMPLETED, KEY_LENGTH, KEY_MESSAGE, KEY_ISPOSTED, KEY_STREAKDAY},
                KEY_DATE + "=?", new String[]{date},
                null, null, null, "1"
        );
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();

                session = new SessionSQL(cursor.getLong(cursor
                        .getColumnIndex(KEY_ID)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_COMPLETED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_LENGTH)), cursor.getString(cursor
                        .getColumnIndex(KEY_MESSAGE)), cursor.getLong(cursor
                        .getColumnIndex(KEY_ISPOSTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STREAKDAY)));
            }

            cursor.close();
        }

        return session;
    }

    public ArrayList<SessionSQL> getSessionsByDate(String date) {
        ArrayList<SessionSQL> sessionList = new ArrayList<SessionSQL>();

        Cursor cursor = db.rawQuery("SELECT  * FROM `" + TABLE_SESSIONS + "` WHERE `" + KEY_DATE + "`=? ORDER BY `" + KEY_STARTED + "` ASC", new String[]{date});

        if (cursor.moveToFirst()) {
            do {
                SessionSQL session = new SessionSQL(cursor.getLong(cursor
                        .getColumnIndex(KEY_ID)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STARTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_COMPLETED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_LENGTH)), cursor.getString(cursor
                        .getColumnIndex(KEY_MESSAGE)), cursor.getLong(cursor
                        .getColumnIndex(KEY_ISPOSTED)), cursor.getLong(cursor
                        .getColumnIndex(KEY_STREAKDAY)));

                sessionList.add(session);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return sessionList;
    }
}
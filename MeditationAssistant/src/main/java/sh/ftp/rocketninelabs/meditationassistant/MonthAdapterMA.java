package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public abstract class MonthAdapterMA extends BaseAdapter {
    private final int[] mDaysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30,
            31, 30, 31};
    private GregorianCalendar mCalendar;
    private Calendar mCalendarToday;
    private Context mContext;
    private DisplayMetrics mDisplayMetrics;
    private List<String> mItems;
    private int mMonth;
    private int mYear;
    private int mDaysShown;
    private int mDaysLastMonth;
    private int mDaysNextMonth;
    private int mDayHeight;
    private String[] mDays = null;
    private MeditationAssistant ma = null;
    private ProgressActivity pa = null;

    public MonthAdapterMA(Context c, int month, int year,
                          DisplayMetrics metrics, ProgressActivity _pa, MeditationAssistant _ma) {
        mContext = c;
        ma = _ma;
        mDayHeight = getMeditationAssistant().dpToPixels(50);

        mMonth = month;
        mYear = year;
        mCalendar = new GregorianCalendar(mYear, mMonth, 1);
        mCalendarToday = Calendar.getInstance();
        mDisplayMetrics = metrics;
        pa = _pa;

        mDays = new String[]{mContext.getString(R.string.dayMondayShort),
                mContext.getString(R.string.dayTuesdayShort),
                mContext.getString(R.string.dayWednesdayShort),
                mContext.getString(R.string.dayThursdayShort),
                mContext.getString(R.string.dayFridayShort),
                mContext.getString(R.string.daySaturdayShort),
                mContext.getString(R.string.daySundayShort)};
        populateMonth();
    }

    private int daysInMonth(int month) {
        int daysInMonth = mDaysInMonth[month];
        if (month == 1 && mCalendar.isLeapYear(mYear))
            daysInMonth++;
        return daysInMonth;
    }

    private int getBarHeight() {
        switch (mDisplayMetrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                return 48;
            case DisplayMetrics.DENSITY_MEDIUM:
                return 32;
            case DisplayMetrics.DENSITY_LOW:
                return 24;
            default:
                return 48;
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    private int[] getDate(int position) {
        int date[] = new int[3];
        if (position <= 6) {
            return null; // day names
        } else if (position <= mDaysLastMonth + 6) {
            // previous month
            date[0] = Integer.parseInt(mItems.get(position));
            if (mMonth == 0) {
                date[1] = 11;
                date[2] = mYear - 1;
            } else {
                date[1] = mMonth - 1;
                date[2] = mYear;
            }
        } else if (position <= mDaysShown - mDaysNextMonth) {
            // current month
            date[0] = position - (mDaysLastMonth + 6);
            date[1] = mMonth;
            date[2] = mYear;
        } else {
            // next month
            date[0] = Integer.parseInt(mItems.get(position));
            if (mMonth == 11) {
                date[1] = 0;
                date[2] = mYear + 1;
            } else {
                date[1] = mMonth + 1;
                date[2] = mYear;
            }
        }
        return date;
    }

    private int getDay(int day) {
        switch (day) {
            case Calendar.MONDAY:
                return 0;
            case Calendar.TUESDAY:
                return 1;
            case Calendar.WEDNESDAY:
                return 2;
            case Calendar.THURSDAY:
                return 3;
            case Calendar.FRIDAY:
                return 4;
            case Calendar.SATURDAY:
                return 5;
            case Calendar.SUNDAY:
                return 6;
            default:
                return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public MeditationAssistant getMeditationAssistant() {
        return ma;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView view = new TextView(mContext);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        view.setText(mItems.get(position));

        int[] date = getDate(position);
        if (date != null) {
            view.setTextSize(20);
            Calendar dateCalendar = Calendar.getInstance();
            dateCalendar.set(Calendar.DAY_OF_MONTH, date[0]);
            dateCalendar.set(Calendar.MONTH, date[1]);
            dateCalendar.set(Calendar.YEAR, date[2]);
            int numSessions = getMeditationAssistant().db.numSessionsByDate(dateCalendar);
            if (numSessions > 0) {
                // At least one meditation session exists for this date

                view.setTag(R.id.calendarDate, date);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int[] date = (int[]) view.getTag(R.id.calendarDate);
                        if (date != null) {
                            pa.goToSessionAtDate(date);
                        }
                    }
                });

                if (getMeditationAssistant().getMAThemeString().equals("dark")) {
                    /*int bgcolor = (date[1] == mMonth) ? R.color.highlighted_text_light
                            : R.color.dim_foreground_light;*/
                    int bgcolor = (date[1] == mMonth) ? android.R.color.holo_blue_dark
                            : android.R.color.secondary_text_dark;

                    // : android.R.color.dim_foreground_light;

                    view.setBackgroundColor(getMeditationAssistant()
                            .getResources().getColor(bgcolor));
                } else {
                    int bgcolor = (date[1] == mMonth) ? R.color.highlighted_text_dark
                            : R.color.dim_foreground_dark;
                    //: R.color.dim_foreground_holo_dark;

                    view.setBackgroundColor(getMeditationAssistant()
                            .getResources().getColor(bgcolor));
                }
            }

            view.setHeight(mDayHeight);
            if (date[1] != mMonth) { // previous or next month
                if (getMeditationAssistant().getMAThemeString().equals("dark")) {
                    view.setTextColor(getMeditationAssistant().getResources()
                            .getColor(android.R.color.tertiary_text_light));
                } else {
                    view.setTextColor(getMeditationAssistant().getResources()
                            .getColor(android.R.color.tertiary_text_dark));
                }
            } else { // current month
                view.setTextColor(getMeditationAssistant()
                        .getResources()
                        .getColor(
                                getMeditationAssistant()
                                        .getTheme()
                                        .obtainStyledAttributes(
                                                getMeditationAssistant()
                                                        .getMATheme(),
                                                new int[]{android.R.attr.textColorPrimary}
                                        )
                                        .getResourceId(0, 0)
                        ));

                if (isToday(date[0], date[1], date[2])) {
                    view.setTypeface(null, Typeface.BOLD);
                    view.setPaintFlags(view.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                }
            }
        } else {
            view.setTextSize(16);
            view.setPadding(0, 0, 0,
                    getMeditationAssistant().dpToPixels(1));
            view.setTypeface(null, Typeface.BOLD);
            view.setTextColor(getMeditationAssistant().getResources().getColor(
                    getMeditationAssistant()
                            .getTheme()
                            .obtainStyledAttributes(
                                    getMeditationAssistant().getMATheme(),
                                    new int[]{android.R.attr.textColorPrimary})
                            .getResourceId(0, 0)
            ));
            view.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        }

        onDate(date, position, view);
        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    private boolean isToday(int day, int month, int year) {
        return mCalendarToday.get(Calendar.MONTH) == month
                && mCalendarToday.get(Calendar.YEAR) == year
                && mCalendarToday.get(Calendar.DAY_OF_MONTH) == day;
    }

    /**
     * @param date     - null if day title (0 - dd / 1 - mm / 2 - yy)
     * @param position - position in item list
     * @param item     - view for date
     */
    protected abstract void onDate(int[] date, int position, View item);

    private void populateMonth() {
        mItems = new ArrayList<String>();
        for (String day : mDays) {
            mItems.add(day);
            mDaysShown++;
        }

        int firstDay = getDay(mCalendar.get(Calendar.DAY_OF_WEEK));
        int prevDay;
        if (mMonth == 0)
            prevDay = daysInMonth(11) - firstDay + 1;
        else
            prevDay = daysInMonth(mMonth - 1) - firstDay + 1;
        for (int i = 0; i < firstDay; i++) {
            mItems.add(String.valueOf(prevDay + i));
            mDaysLastMonth++;
            mDaysShown++;
        }

        int daysInMonth = daysInMonth(mMonth);
        for (int i = 1; i <= daysInMonth; i++) {
            mItems.add(String.valueOf(i));
            mDaysShown++;
        }

        mDaysNextMonth = 1;
        while (mDaysShown % 7 != 0) {
            mItems.add(String.valueOf(mDaysNextMonth));
            mDaysShown++;
            mDaysNextMonth++;
        }
    }
}
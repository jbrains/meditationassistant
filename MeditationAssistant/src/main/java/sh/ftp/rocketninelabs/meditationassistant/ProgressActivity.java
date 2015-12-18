package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public class ProgressActivity extends FragmentActivity {
    public SparseArray<SessionSQL> sessions_map = new SparseArray<SessionSQL>();
    TabHost mTabHost;
    ViewPager mViewPager;
    ProgressPagerAdapter mPagerAdapter = null;
    int pagePosition = 0;
    private AlertDialog addSessionDialog = null;
    private int SESSIONS_FRAGMENT = 0;
    private int CALENDAR_FRAGMENT = 1;
    private int STATS_FRAGMENT = 2;
    private SessionsFragment sessionsFragment = null;
    private MeditationAssistant ma = null;
    private MenuItem menuCalendarBack = null;
    private MenuItem menuCalendarForward = null;
    private String beingSet = "started";
    private Button btnSetDateStarted = null;
    private Button btnSetTimeStarted = null;
    private Button btnSetDateCompleted = null;
    private Button btnSetTimeCompleted = null;
    private EditText editAddSessionMessage = null;
    private int startedYear = -1;
    private int startedMonth = -1;
    private int startedDay = -1;
    private int startedHour = -1;
    private int startedMinute = -1;
    private int completedYear = -1;
    private int completedMonth = -1;
    private int completedDay = -1;
    private int completedHour = -1;
    private int completedMinute = -1;
    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    if (beingSet.equals("started")) {
                        startedYear = year;
                        startedMonth = monthOfYear;
                        startedDay = dayOfMonth;

                        if (completedYear == -1 || completedMonth == -1 || completedDay == -1) {
                            completedYear = startedYear;
                            completedMonth = startedMonth;
                            completedDay = startedDay;
                        } else if (completedYear != -1 && completedMonth != -1 && completedDay != -1) {
                            Calendar c_started = Calendar.getInstance();
                            c_started.set(Calendar.YEAR, startedYear);
                            c_started.set(Calendar.MONTH, startedMonth);
                            c_started.set(Calendar.DAY_OF_MONTH, startedDay);
                            c_started.set(Calendar.HOUR_OF_DAY, 0);
                            c_started.set(Calendar.MINUTE, 0);
                            c_started.set(Calendar.SECOND, 0);
                            c_started.set(Calendar.MILLISECOND, 0);

                            Calendar c_completed = Calendar.getInstance();
                            c_completed.set(Calendar.YEAR, completedYear);
                            c_completed.set(Calendar.MONTH, completedMonth);
                            c_completed.set(Calendar.DAY_OF_MONTH, completedDay);
                            c_completed.set(Calendar.HOUR_OF_DAY, 0);
                            c_completed.set(Calendar.MINUTE, 0);
                            c_completed.set(Calendar.SECOND, 0);
                            c_completed.set(Calendar.MILLISECOND, 0);

                            if (c_started.getTimeInMillis() > c_completed.getTimeInMillis()) {
                                completedYear = startedYear;
                                completedMonth = startedMonth;
                                completedDay = startedDay;
                            }
                        }
                    } else {
                        completedYear = year;
                        completedMonth = monthOfYear;
                        completedDay = dayOfMonth;
                    }

                    updateDateAndTimeButtons();
                }
            };
    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    if (beingSet.equals("started")) {
                        startedHour = hourOfDay;
                        startedMinute = minute;

                        if (completedHour == -1 && completedMinute == -1) {
                            completedHour = startedHour;
                            completedMinute = startedMinute;
                        }
                    } else {
                        completedHour = hourOfDay;
                        completedMinute = minute;
                    }

                    updateDateAndTimeButtons();
                }
            };
    private AlertDialog sessionDetailsDialog = null;

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    public void updateDateAndTimeButtons() {
        SimpleDateFormat sdf_date = new SimpleDateFormat("MMMM d",
                Locale.getDefault());
        SimpleDateFormat sdf_time = new SimpleDateFormat("h:mm a",
                Locale.getDefault());

        sdf_date.setTimeZone(TimeZone.getDefault());
        sdf_time.setTimeZone(TimeZone.getDefault());

        if (startedYear == -1 || startedMonth == -1 || startedDay == -1) {
            btnSetDateStarted.setText(getString(R.string.setDate));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, startedYear);
            c.set(Calendar.MONTH, startedMonth);
            c.set(Calendar.DAY_OF_MONTH, startedDay);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            btnSetDateStarted.setText(sdf_date.format(c.getTime()));
        }
        if (completedYear == -1 || completedMonth == -1 || completedDay == -1) {
            btnSetDateCompleted.setText(getString(R.string.setDate));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, completedYear);
            c.set(Calendar.MONTH, completedMonth);
            c.set(Calendar.DAY_OF_MONTH, completedDay);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            btnSetDateCompleted.setText(sdf_date.format(c.getTime()));
        }

        if (startedHour == -1 || startedMinute == -1) {
            btnSetTimeStarted.setText(getString(R.string.setTime));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, startedHour);
            c.set(Calendar.MINUTE, startedMinute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            btnSetTimeStarted.setText(sdf_time.format(c.getTime()));
        }
        if (completedHour == -1 || completedMinute == -1) {
            btnSetTimeCompleted.setText(getString(R.string.setTime));
        } else {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, completedHour);
            c.set(Calendar.MINUTE, completedMinute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            btnSetTimeCompleted.setText(sdf_time.format(c.getTime()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        setContentView(R.layout.activity_progress);
        getActionBar().setDisplayHomeAsUpEnabled(true); /// todo: not necessary on settings activity why?
        //getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        getMeditationAssistant().utility_ads.loadAd(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mPagerAdapter = new ProgressPagerAdapter(
                getSupportFragmentManager());

        mViewPager.setAdapter(mPagerAdapter);

        /*ActionBar.TabListener tabListener = new ActionBar.TabListener() {

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }

            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

            }
        };

        getActionBar().addTab(getActionBar().newTab()
                .setText(getString(R.string.sessions))
                .setTabListener(tabListener));
        getActionBar().addTab(getActionBar().newTab()
                .setText(getString(R.string.statistics))
                .setTabListener(tabListener));

        getActionBar().addTab(getActionBar().newTab()
                .setText(getString(R.string.calendar))
                .setTabListener(tabListener));*/

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.titles);
        tabStrip.setDrawFullUnderline(true);

        if (getMeditationAssistant().getMAThemeString().equals("buddhism")) {
            tabStrip.setBackgroundColor(getResources().getColor(R.color.buddhism_tab_background));
            tabStrip.setTabIndicatorColor(getResources().getColor(R.color.buddhism_tab_color));
            tabStrip.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
        } else {
            tabStrip.setTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
        }


       /*ViewPagerIndicator titleIndicator = (TitlePageIndicator) findViewById(R.id.titles);
        titleIndicator.setViewPager(mViewPager);

        titleIndicator.setFooterIndicatorStyle(IndicatorStyle.Underline);

        titleIndicator
                .setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position,
                                               float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        if (menuCalendarBack != null
                                && menuCalendarForward != null) {
                            pagePosition = position;
                        }
                    }
                });*/

        String defaulttab = getMeditationAssistant().getPrefs().getString(
                "pref_progresstab", "calendar");
        if (defaulttab.equals("sessions")) {
            mViewPager.setCurrentItem(SESSIONS_FRAGMENT, false);
        } else if (defaulttab.equals("stats")) {
            mViewPager.setCurrentItem(STATS_FRAGMENT, false);
        } else {
            mViewPager.setCurrentItem(CALENDAR_FRAGMENT, false);
        }

        getMeditationAssistant().utility.initializeTracker(this);
    }

    public void goToSessionAtDate(int[] date) {
        if (date != null) {
            SessionSQL sessionsql = getMeditationAssistant().db.getSessionByDate(String.valueOf(date[0]) + "-" + String.valueOf(date[1] + 1) + "-" + String.valueOf(date[2]));
            if (sessionsql != null) {
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(sessionsql._completed * 1000);
                Date sess_date = cal.getTime();

                if (getMeditationAssistant().db.numSessionsByDate(cal) > 1) {
                    if (sessionDetailsDialog != null && sessionDetailsDialog.isShowing()) {
                        sessionDetailsDialog.dismiss();
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy",
                            Locale.getDefault());
                    SimpleDateFormat sdf2 = new SimpleDateFormat("h:mm a",
                            Locale.getDefault());

                    ArrayList<SessionSQL> sessions = getMeditationAssistant().db.getSessionsByDate(String.valueOf(date[0]) + "-" + String.valueOf(date[1] + 1) + "-" + String.valueOf(date[2]));

                    final ArrayAdapter<String> sessionsDialogAdapter = new ArrayAdapter<String>(
                            this,
                            android.R.layout.select_dialog_item);

                    sessions_map.clear();
                    int session_index = 0;
                    for (Iterator<SessionSQL> i = sessions.iterator(); i.hasNext(); ) {
                        SessionSQL session = i.next();
                        Calendar cal2 = Calendar.getInstance();
                        cal2.setTimeInMillis(session._completed * 1000);
                        sessionsDialogAdapter.add(String.valueOf(session._length / 3600) + ":"
                                + String.format("%02d", (session._length % 3600) / 60)
                                + " - " + sdf2.format(cal2.getTime()));

                        sessions_map.put(session_index, session);
                        session_index++;
                    }

                    if (sessionDetailsDialog != null && sessionDetailsDialog.isShowing()) {
                        sessionDetailsDialog.dismiss();
                    }

                    sessionDetailsDialog = new AlertDialog.Builder(this)
                            .setIcon(
                                    getResources().getDrawable(
                                            getMeditationAssistant().getTheme().obtainStyledAttributes(getMeditationAssistant().getMATheme(true),
                                                    new int[]{R.attr.actionIconGoToToday})
                                                    .getResourceId(0, 0)
                                    )
                            )
                            .setTitle(sdf.format(sess_date))
                            .setAdapter(sessionsDialogAdapter,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            SessionSQL session = sessions_map.get(which);
                                            if (session != null) {
                                                showSessionPopup(session);
                                            }

                                            try {
                                                dialog.dismiss();
                                            } catch (Exception e) {

                                            }
                                        }
                                    })
                            .create();

                    sessionDetailsDialog.show();
                } else {
                    showSessionPopup(sessionsql);
                }

                /*mViewPager.setCurrentItem(SESSIONS_FRAGMENT, false);
                Integer session_position = ((SessionAdapter) sessionsFragment.getListAdapter()).sessions_map.get(String.valueOf(date[0]) + "-" + String.valueOf(date[1] + 1) + "-" + String.valueOf(date[2]));
                if (session_position != null) {
                    Log.d("MeditationAssistant", "Position: " + String.valueOf(session_position));
                    sessionsFragment.getListView().smoothScrollToPosition(session_position);
                }*/
            }
        }
    }

    private void showSessionPopup(SessionSQL sessionsql) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sessionsql._completed * 1000);
        Date sess_date = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy h:mm a",
                Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat sdf2 = new SimpleDateFormat("h:mm a",
                Locale.getDefault());
        sdf2.setTimeZone(TimeZone.getDefault());

        String session_title = String.valueOf(sessionsql._length / 3600) + ":"
                + String.format("%02d", (sessionsql._length % 3600) / 60)
                + " - " + sdf.format(sess_date);

        cal.setTimeInMillis(sessionsql._started * 1000);
        String session_started = sdf2.format(cal.getTime());

        View detailsView = LayoutInflater.from(this).inflate(
                R.layout.session_details,
                (ViewGroup) findViewById(R.id.sessionDetails_root));

        TextView txtSessionDetailsStarted = (TextView) detailsView.findViewById(R.id.txtSessionDetailsStarted);
        TextView txtSessionDetailsMessage = (TextView) detailsView.findViewById(R.id.txtSessionDetailsMessage);

        txtSessionDetailsStarted.setText(String.format(getString(R.string.sessionStartedAt), session_started));

        if (!sessionsql._message.trim().equals("")) {
            txtSessionDetailsMessage.setText(sessionsql._message.trim());
        } else {
            View divSessionDetailsMessage = detailsView.findViewById(R.id.divSessionDetailsMessage);

            divSessionDetailsMessage.setVisibility(View.GONE);
            txtSessionDetailsMessage.setVisibility(View.GONE);
        }

        if (sessionDetailsDialog != null && sessionDetailsDialog.isShowing()) {
            sessionDetailsDialog.dismiss();
        }

        sessionDetailsDialog = new AlertDialog.Builder(this)
                .setIcon(
                        getResources().getDrawable(
                                getMeditationAssistant().getTheme().obtainStyledAttributes(getMeditationAssistant().getMATheme(true),
                                        new int[]{R.attr.actionIconGoToToday})
                                        .getResourceId(0, 0)
                        )
                )
                .setTitle(session_title)
                .setView(detailsView)
                .create();

        sessionDetailsDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.progress, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        } else if (i == R.id.addSession) {
            if (addSessionDialog != null) {
                try {
                    if (addSessionDialog.isShowing()) {
                        addSessionDialog.dismiss();
                    }
                } catch (WindowManager.BadTokenException e) {
                    // Activity is not in the foreground
                }
            }

            //debug

            if (getMeditationAssistant()
                    .getTimeStartMeditate() > 0) {
                getMeditationAssistant().shortToast(getString(R.string.addSessionMeditating));
                return true;
            }

            startedYear = -1;
            startedMonth = -1;
            startedDay = -1;
            startedHour = -1;
            startedMinute = -1;

            completedYear = -1;
            completedMonth = -1;
            completedDay = -1;
            completedHour = -1;
            completedMinute = -1;

            View addSessionView = LayoutInflater.from(this).inflate(
                    R.layout.session_add,
                    (ViewGroup) findViewById(R.id.sessionAdd_root));

            btnSetDateStarted = (Button) addSessionView.findViewById(R.id.btnSetDateStarted);
            btnSetTimeStarted = (Button) addSessionView.findViewById(R.id.btnSetTimeStarted);
            btnSetDateCompleted = (Button) addSessionView.findViewById(R.id.btnSetDateCompleted);
            btnSetTimeCompleted = (Button) addSessionView.findViewById(R.id.btnSetTimeCompleted);
            editAddSessionMessage = (EditText) addSessionView.findViewById(R.id.editAddSessionMessage);

            btnSetDateStarted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    beingSet = "started";
                    DatePickerDialog dateDialog = null;

                    if (startedYear == -1 || startedMonth == -1 || startedDay == -1) {
                        Calendar c = Calendar.getInstance();
                        dateDialog = new DatePickerDialog(ProgressActivity.this,
                                mDateSetListener,
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    } else {
                        dateDialog = new DatePickerDialog(ProgressActivity.this,
                                mDateSetListener,
                                startedYear, startedMonth, startedDay);
                    }

                    dateDialog.show();
                }
            });
            btnSetTimeStarted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    beingSet = "started";
                    TimePickerDialog timeDialog = null;

                    if (startedHour == -1 || startedMinute == -1) {
                        Calendar c = Calendar.getInstance();
                        timeDialog = new TimePickerDialog(ProgressActivity.this,
                                mTimeSetListener,
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
                    } else {
                        timeDialog = new TimePickerDialog(ProgressActivity.this,
                                mTimeSetListener,
                                startedHour, startedMinute, false);
                    }

                    timeDialog.show();
                }
            });
            btnSetDateCompleted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    beingSet = "completed";
                    DatePickerDialog dateDialog = null;

                    if (completedYear == -1 || completedMonth == -1 || completedDay == -1) {
                        Calendar c = Calendar.getInstance();
                        dateDialog = new DatePickerDialog(ProgressActivity.this,
                                mDateSetListener,
                                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    } else {
                        dateDialog = new DatePickerDialog(ProgressActivity.this,
                                mDateSetListener,
                                completedYear, completedMonth, completedDay);
                    }

                    dateDialog.show();
                }
            });
            btnSetTimeCompleted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    beingSet = "completed";
                    TimePickerDialog timeDialog = null;

                    if (completedHour == -1 || completedMinute == -1) {
                        Calendar c = Calendar.getInstance();
                        timeDialog = new TimePickerDialog(ProgressActivity.this,
                                mTimeSetListener,
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
                    } else {
                        timeDialog = new TimePickerDialog(ProgressActivity.this,
                                mTimeSetListener,
                                completedHour, completedMinute, false);
                    }

                    timeDialog.show();
                }
            });

            updateDateAndTimeButtons();

            addSessionDialog = new AlertDialog.Builder(this)
                    .setIcon(
                            getResources().getDrawable(
                                    getMeditationAssistant().getTheme().obtainStyledAttributes(getMeditationAssistant().getMATheme(true),
                                            new int[]{R.attr.actionIconNew})
                                            .getResourceId(0, 0)
                            )
                    )
                    .setTitle(getString(R.string.addSession))
                    .setView(addSessionView)
                    .setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                            int which) {
                            // Overridden later
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface,
                                            int which) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();

            addSessionDialog.show();

            Button saveButton = addSessionDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (startedYear == -1 || startedMonth == -1 || startedDay == -1 || startedHour == -1 || startedMinute == -1 || completedYear == -1 || completedMonth == -1 || completedDay == -1 || completedHour == -1 || completedMinute == -1) {
                        getMeditationAssistant().shortToast(getString(R.string.invalidDateOrTime));
                    } else {
                        Calendar c_started = Calendar.getInstance();
                        c_started.set(Calendar.YEAR, startedYear);
                        c_started.set(Calendar.MONTH, startedMonth);
                        c_started.set(Calendar.DAY_OF_MONTH, startedDay);
                        c_started.set(Calendar.HOUR_OF_DAY, startedHour);
                        c_started.set(Calendar.MINUTE, startedMinute);
                        c_started.set(Calendar.SECOND, 0);
                        c_started.set(Calendar.MILLISECOND, 0);

                        Calendar c_completed = Calendar.getInstance();
                        c_completed.set(Calendar.YEAR, completedYear);
                        c_completed.set(Calendar.MONTH, completedMonth);
                        c_completed.set(Calendar.DAY_OF_MONTH, completedDay);
                        c_completed.set(Calendar.HOUR_OF_DAY, completedHour);
                        c_completed.set(Calendar.MINUTE, completedMinute);
                        c_completed.set(Calendar.SECOND, 0);
                        c_completed.set(Calendar.MILLISECOND, 0);

                        if (c_started.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() || c_completed.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() || c_completed.getTimeInMillis() <= c_started.getTimeInMillis()) {
                            getMeditationAssistant().shortToast(getString(R.string.invalidDateOrTime));
                        } else if (getMeditationAssistant().db
                                .getSessionByStarted(c_started.getTimeInMillis() / 1000) == null) {
                            getMeditationAssistant().getMediNET().resetSession();
                            getMeditationAssistant().getMediNET().session.started = c_started.getTimeInMillis() / 1000;
                            getMeditationAssistant().getMediNET().session.length = ((c_completed.getTimeInMillis() / 1000) - (c_started.getTimeInMillis() / 1000));
                            getMeditationAssistant().getMediNET().session.completed = c_completed.getTimeInMillis() / 1000;
                            getMeditationAssistant().getMediNET().session.message = editAddSessionMessage.getText().toString().trim();
                            getMeditationAssistant().getMediNET().saveSession(true, false);

                            addSessionDialog.dismiss();

                            if (sessionsFragment != null) {
                                sessionsFragment.refreshSessionList();
                            }

                            /*if (calendarFragment != null) {
                                calendarFragment.refreshMonthDisplay();
                            }*/
                        } else {
                            getMeditationAssistant().shortToast(getString(R.string.addSessionExists));
                        }
                    }
                }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        getMeditationAssistant().utility.trackingStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getMeditationAssistant().utility.trackingStop(this);
    }

    @Override
    public void onPause() {
        getMeditationAssistant().utility_ads.pauseAd(this);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        getMeditationAssistant().utility_ads.resumeAd(this);
    }

    @Override
    public void onDestroy() {
        getMeditationAssistant().utility_ads.destroyAd(this);
        super.onDestroy();
    }

    class ProgressPagerAdapter extends FragmentPagerAdapter {
        private final String[] ProgressPages = new String[]{
                getString(R.string.sessions), getString(R.string.calendar), getString(R.string.statistics)};

        public ProgressPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return ProgressPages.length;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == SESSIONS_FRAGMENT) {
                sessionsFragment = new SessionsFragment();
                return sessionsFragment;
            } else if (position == CALENDAR_FRAGMENT) {
                CalendarFragment calendarFragment = new CalendarFragment();
                return calendarFragment;
            } else if (position == STATS_FRAGMENT) {
                return new StatsFragment();
            } else {
                return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ProgressPages[position % ProgressPages.length].toUpperCase();
        }
    }
}

/*
public class ProgressActivity extends FragmentActivity
        implements ItemListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list))
                    .setActivateOnItemClick(true);
        }
    }

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, fragment)
                    .apply();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(ItemDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
}
*/
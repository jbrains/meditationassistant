package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
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
import android.widget.ArrayAdapter;
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
    private int SESSIONS_FRAGMENT = 0;
    private int CALENDAR_FRAGMENT = 1;
    private int STATS_FRAGMENT = 2;
    private SessionsFragment sessionsFragment = null;
    private MeditationAssistant ma = null;


    private AlertDialog sessionsExportedDialog = null;
    private AlertDialog sessionDetailsDialog = null;

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) this.getApplication();
        }
        return ma;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(getMeditationAssistant().getMATheme());
        setContentView(R.layout.activity_progress);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mPagerAdapter = new ProgressPagerAdapter(
                getSupportFragmentManager());

        mViewPager.setAdapter(mPagerAdapter);

        PagerTabStrip tabStrip = (PagerTabStrip) findViewById(R.id.titles);
        tabStrip.setDrawFullUnderline(true);

        if (getMeditationAssistant().getMAThemeString().equals("buddhism")) {
            tabStrip.setBackgroundColor(getResources().getColor(R.color.buddhism_tab_background));
            tabStrip.setTabIndicatorColor(getResources().getColor(R.color.buddhism_tab_color));
            tabStrip.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
        } else {
            tabStrip.setTabIndicatorColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

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
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, date[0]);
            c.set(Calendar.MONTH, date[1]);
            c.set(Calendar.YEAR, date[2]);

            SessionSQL sessionsql = getMeditationAssistant().db.getSessionByDate(c);
            if (sessionsql != null) {
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(sessionsql._completed * 1000);
                Date sess_date = cal.getTime();

                if (getMeditationAssistant().db.numSessionsByDate(cal) == 1) {
                    getMeditationAssistant().showSessionDialog(sessionsql, ProgressActivity.this);
                    return;
                }

                if (sessionDetailsDialog != null && sessionDetailsDialog.isShowing()) {
                    sessionDetailsDialog.dismiss();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy",
                        Locale.getDefault());
                SimpleDateFormat sdf2 = new SimpleDateFormat("h:mm a",
                        Locale.getDefault());

                ArrayList<SessionSQL> sessions = getMeditationAssistant().db.getSessionsByDate(c);

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
                                            getMeditationAssistant().showSessionDialog(session, ProgressActivity.this);
                                        }

                                        try {
                                            dialog.dismiss();
                                        } catch (Exception e) {
                                        }
                                    }
                                })
                        .create();
                sessionDetailsDialog.show();
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
            getMeditationAssistant().showSessionDialog(new SessionSQL(), ProgressActivity.this);

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
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
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

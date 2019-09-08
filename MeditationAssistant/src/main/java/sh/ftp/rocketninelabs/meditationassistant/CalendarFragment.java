package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public class CalendarFragment extends Fragment {
    GridView gridCalendar = null;
    TextView txtCurrentMonth = null;
    TextView txtCurrentYear = null;
    MonthAdapter monthadapter = null;
    Calendar mCalendar = null;
    ImageButton nextMonth = null;
    private MeditationAssistant ma = null;
    SharedPreferences.OnSharedPreferenceChangeListener sharedPrefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("sessionsupdate")) {
                Log.d("MeditationAssistant", "Got sessions update, refreshing CalendarFragment");
                refreshMonthDisplay();
            }
        }
    };

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) getActivity().getApplication();
        }
        return ma;
    }

    private MonthAdapter getMonthAdapter() {
        final DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay()
                .getMetrics(metrics);
        updatecurrentDate();

        return new MonthAdapter(getActivity(), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.YEAR), metrics, (ProgressActivity) getActivity(), getMeditationAssistant());
    }

    public void refreshMonthDisplay() {
        gridCalendar.setAdapter(getMonthAdapter());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMeditationAssistant().getPrefs().registerOnSharedPreferenceChangeListener(sharedPrefslistener);

        int color_primary = getResources().getColor(getMeditationAssistant().getMATextColor(true));
        int color_primary_disabled = getResources().getColor(getMeditationAssistant().getMATextColor(false));

        /* Getting resource not found errors here */
        try {
            color_primary = getResources().getColor(
                    getMeditationAssistant()
                            .getTheme()
                            .obtainStyledAttributes(
                                    getMeditationAssistant().getMATheme(),
                                    new int[]{android.R.attr.textColorPrimary})
                            .getResourceId(0, 0)
            );
            color_primary_disabled = getResources()
                    .getColor(
                            getMeditationAssistant()
                                    .getTheme()
                                    .obtainStyledAttributes(
                                            getMeditationAssistant()
                                                    .getMATheme(),
                                            new int[]{android.R.attr.textColorPrimaryDisableOnly}
                                    )
                                    .getResourceId(0, 0)
                    );
        } catch (Exception e) {
            Log.d("MeditationAssistant", "Unable to get color resources in CalendarFragment:");
            e.printStackTrace();
        }

        txtCurrentMonth = new TextView(getActivity());
        txtCurrentYear = new TextView(getActivity());
        mCalendar = Calendar.getInstance();

        txtCurrentMonth.setTextSize(22);
        txtCurrentYear.setTextSize(12);

        txtCurrentMonth.setTextColor(color_primary);
        txtCurrentMonth
                .setTextColor(color_primary_disabled);

        txtCurrentMonth.setSingleLine(true);
        txtCurrentMonth.setEllipsize(TextUtils.TruncateAt.END);

        txtCurrentYear.setSingleLine(true);
        txtCurrentYear.setEllipsize(TextUtils.TruncateAt.END);

        gridCalendar = new GridView(getActivity());
        gridCalendar.setNumColumns(7);
        gridCalendar.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        gridCalendar.setVerticalSpacing(2);
        gridCalendar.setHorizontalSpacing(2);
        gridCalendar.setAdapter(getMonthAdapter());
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context ctx = getActivity().getApplicationContext();

        LinearLayout lcontainer = new LinearLayout(ctx);
        lcontainer.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        lcontainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout lmain = new LinearLayout(ctx);
        lmain.setOrientation(LinearLayout.VERTICAL);
        lmain.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));

        lmain.setWeightSum(0.5f);

        LinearLayout lhoriz = new LinearLayout(ctx);
        lhoriz.setOrientation(LinearLayout.HORIZONTAL);
        lhoriz.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));

        lmain.addView(gridCalendar);

        ImageButton prevMonth = new ImageButton(getActivity());
        prevMonth.setImageDrawable(getResources().getDrawable(
                getMeditationAssistant()
                        .getTheme()
                        .obtainStyledAttributes(
                                getMeditationAssistant().getMATheme(),
                                new int[]{R.attr.actionIconPreviousItem})
                        .getResourceId(0, 0)
        ));

        nextMonth = new ImageButton(getActivity());
        nextMonth.setImageDrawable(getResources().getDrawable(
                getMeditationAssistant()
                        .getTheme()
                        .obtainStyledAttributes(
                                getMeditationAssistant().getMATheme(),
                                new int[]{R.attr.actionIconNextItem})
                        .getResourceId(0, 0)
        ));

        /*TableLayout.LayoutParams monthbuttonslp = new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f);*/
        TableLayout.LayoutParams monthbuttonslp = new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f);
        monthbuttonslp.setMargins(0, getMeditationAssistant().dpToPixels(7), 0,
                getMeditationAssistant().dpToPixels(7));
        prevMonth.setLayoutParams(monthbuttonslp);
        nextMonth.setLayoutParams(monthbuttonslp);

        prevMonth.setBackgroundColor(getActivity().getResources().getColor(
                android.R.color.transparent));
        nextMonth.setBackgroundColor(getActivity().getResources().getColor(
                android.R.color.transparent));

        prevMonth.setBackgroundResource(getMeditationAssistant()
                .getTheme()
                .obtainStyledAttributes(
                        getMeditationAssistant().getMATheme(),
                        new int[]{android.R.attr.selectableItemBackground})
                .getResourceId(0, 0));
        nextMonth.setBackgroundResource(getMeditationAssistant()
                .getTheme()
                .obtainStyledAttributes(
                        getMeditationAssistant().getMATheme(),
                        new int[]{android.R.attr.selectableItemBackground})
                .getResourceId(0, 0));

        prevMonth.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCalendar.add(Calendar.MONTH, -1);
                gridCalendar.setAdapter(getMonthAdapter());

                updateMonthScroll();
            }
        });
        prevMonth.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mCalendar.add(Calendar.YEAR, -1);
                gridCalendar.setAdapter(getMonthAdapter());

                updateMonthScroll();
                return true;
            }
        });

        nextMonth.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCalendar.add(Calendar.MONTH, 1);
                gridCalendar.setAdapter(getMonthAdapter());

                updateMonthScroll();
            }
        });
        nextMonth.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Calendar midnightToday = Calendar.getInstance();
                midnightToday.set(Calendar.HOUR, 0);
                midnightToday.set(Calendar.MINUTE, 0);
                midnightToday.set(Calendar.SECOND, 0);

                mCalendar.add(Calendar.YEAR, 1);
                if (mCalendar.after(midnightToday)) {
                    mCalendar = midnightToday;
                }
                gridCalendar.setAdapter(getMonthAdapter());

                updateMonthScroll();
                return true;
            }
        });

        LinearLayout ldate = new LinearLayout(ctx);
        ldate.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams lp_horiz = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f);
        prevMonth.setLayoutParams(lp_horiz);
        ldate.setLayoutParams(lp_horiz);
        nextMonth.setLayoutParams(lp_horiz);

        ldate.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
        txtCurrentMonth.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
        txtCurrentYear.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);

        ldate.addView(txtCurrentMonth);
        ldate.addView(txtCurrentYear);

        lhoriz.setMinimumHeight(getMeditationAssistant().dpToPixels(48));
        lhoriz.addView(prevMonth);
        lhoriz.addView(ldate);
        lhoriz.addView(nextMonth);

        LinearLayout lbottom = new LinearLayout(ctx);
        lmain.setOrientation(LinearLayout.VERTICAL);
        lbottom.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT));
        lbottom.setWeightSum(0.5f);
        lbottom.setMinimumHeight(getMeditationAssistant().dpToPixels(48));
        lbottom.addView(lhoriz);

        lmain.setGravity(Gravity.BOTTOM);

        View divBottom = new View(ctx);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT,
                getMeditationAssistant().dpToPixels(1));
        lp.setMargins(getMeditationAssistant().dpToPixels(7), 0,
                getMeditationAssistant().dpToPixels(7), 0);
        divBottom.setLayoutParams(lp);

        divBottom.setPadding(7, 0, 7, 0);
        divBottom.setBackgroundDrawable(getResources().getDrawable(
                getMeditationAssistant()
                        .getTheme()
                        .obtainStyledAttributes(
                                getMeditationAssistant().getMATheme(),
                                new int[]{android.R.attr.listDividerAlertDialog})
                        .getResourceId(0, 0)
        ));

        lcontainer.addView(lbottom);
        lcontainer.addView(divBottom);
        lcontainer.addView(lmain);

        updateMonthScroll();
        return lcontainer;
    }

    public void updatecurrentDate() {
        txtCurrentMonth.setText(String.format(Locale.getDefault(), "%tB", mCalendar));
        txtCurrentYear.setText(String.format(Locale.getDefault(), "%tY", mCalendar));
    }

    private void updateMonthScroll() {
        Calendar nowCalendar = Calendar.getInstance();
        if (mCalendar.get(Calendar.MONTH) == nowCalendar.get(Calendar.MONTH)
                && mCalendar.get(Calendar.YEAR) == nowCalendar
                .get(Calendar.YEAR)) {
            nextMonth.setClickable(false);
            nextMonth.setVisibility(View.INVISIBLE);
        } else {
            nextMonth.setClickable(true);
            nextMonth.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        getMeditationAssistant().getPrefs().unregisterOnSharedPreferenceChangeListener(sharedPrefslistener);
        super.onPause();
    }

    @Override
    public void onResume() {
        getMeditationAssistant().getPrefs().registerOnSharedPreferenceChangeListener(sharedPrefslistener);
        super.onResume();
    }
}
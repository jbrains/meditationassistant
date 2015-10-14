package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

public class StatsFragment extends Fragment {
    private MeditationAssistant ma = null;
    private View fragment_progress_stats = null;
    SharedPreferences.OnSharedPreferenceChangeListener sharedPrefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("sessionsupdate")) {
                Log.d("MeditationAssistant", "Got sessions update, refreshing StatsFragment");
                refreshStatsDisplay();
            }
        }
    };

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) getActivity().getApplication();
        }
        return ma;
    }

    public void refreshStatsDisplay() {
        if (fragment_progress_stats == null) {
            return;
        }

        Locale currentLocale = getResources().getConfiguration().locale;

        int time_spent_meditating = getMeditationAssistant().db.getTotalTimeSpentMeditating();
        int time_spent_meditating_days = time_spent_meditating / 86400;
        int abovedays = time_spent_meditating % 86400;
        int time_spent_meditating_hours = abovedays / 3600;
        int abovehours = abovedays % 3600;
        int time_spent_meditating_minutes = abovehours / 60;

        TextView txtTimespentMeditatingDays = (TextView) fragment_progress_stats.findViewById(R.id.txtTimeSpentMeditatingDays);
        TextView txtTimespentMeditatingHours = (TextView) fragment_progress_stats.findViewById(R.id.txtTimeSpentMeditatingHours);
        TextView txtTimespentMeditatingMinutes = (TextView) fragment_progress_stats.findViewById(R.id.txtTimeSpentMeditatingMinutes);

        txtTimespentMeditatingDays.setText(String.valueOf(time_spent_meditating_days));
        txtTimespentMeditatingHours.setText(String.valueOf(time_spent_meditating_hours));
        txtTimespentMeditatingMinutes.setText(String.valueOf(time_spent_meditating_minutes));

        TextView txtOtherStatisticsSessions = (TextView) fragment_progress_stats.findViewById(R.id.txtOtherStatisticsSessions);
        TextView txtOtherStatisticsLongestDuration = (TextView) fragment_progress_stats.findViewById(R.id.txtOtherStatisticsLongestDuration);
        TextView txtOtherStatisticsAverageDuration = (TextView) fragment_progress_stats.findViewById(R.id.txtOtherStatisticsAverageDuration);
        TextView txtOtherStatisticsLongestStreak = (TextView) fragment_progress_stats.findViewById(R.id.txtOtherStatisticsLongestStreak);

        int numSessions = getMeditationAssistant().db.getNumSessions();
        txtOtherStatisticsSessions.setText(NumberFormat.getNumberInstance(currentLocale).format(numSessions));

        double secondsPerSession = 0;
        if (numSessions > 0) {
            secondsPerSession = time_spent_meditating / numSessions;
            secondsPerSession = Math.floor(secondsPerSession);
        }
        int secondsPerSessionHours = (int) secondsPerSession / 3600;
        int secondsPerSessionRemainder = (int) secondsPerSession % 3600;
        int secondsPerSessionMinutes = secondsPerSessionRemainder / 60;
        int secondsPerSessionSeconds = secondsPerSessionRemainder % 60;

        txtOtherStatisticsAverageDuration.setText(String.valueOf(secondsPerSessionHours) + ":" + String.format("%02d", secondsPerSessionMinutes) + ":" + String.format("%02d", secondsPerSessionSeconds));

        // Re-use variables
        secondsPerSession = getMeditationAssistant().db.getLongestSessionLength();
        secondsPerSessionHours = (int) secondsPerSession / 3600;
        secondsPerSessionRemainder = (int) secondsPerSession % 3600;
        secondsPerSessionMinutes = secondsPerSessionRemainder / 60;
        secondsPerSessionSeconds = secondsPerSessionRemainder % 60;

        txtOtherStatisticsLongestDuration.setText(String.valueOf(secondsPerSessionHours) + ":" + String.format("%02d", secondsPerSessionMinutes) + ":" + String.format("%02d", secondsPerSessionSeconds));

        int longestStreak = getMeditationAssistant().getLongestMeditationStreak();
        txtOtherStatisticsLongestStreak.setText(String.format(getResources().getQuantityString(R.plurals.daysOfMeditationMinimal, longestStreak, longestStreak), NumberFormat.getNumberInstance(currentLocale).format(longestStreak)));
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
            Log.d("MeditationAssistant", "Unable to get color resources in StatsFragment:");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context ctx = getActivity().getApplicationContext();
        fragment_progress_stats = inflater.inflate(R.layout.fragment_progress_stats, null);

        refreshStatsDisplay();

        return fragment_progress_stats;
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
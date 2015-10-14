package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SessionsFragment extends ListFragment {
    public MeditationAssistant ma = null;
    AlertDialog sessionDialog = null;
    AlertDialog sessionDetailsDialog = null;
    SessionSQL selected_session = null;
    String session_title = null;
    String session_started = null;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPrefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("sessionsupdate")) {
                Log.d("MeditationAssistant", "Got sessions update, refreshing SessionsFragment");
                refreshSessionList();
            }
        }
    };

    public MeditationAssistant getMeditationAssistant() {
        if (ma == null) {
            ma = (MeditationAssistant) getActivity().getApplication();
        }
        return ma;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMeditationAssistant().getPrefs().registerOnSharedPreferenceChangeListener(sharedPrefslistener);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(android.widget.AdapterView<?> adapterView, View view, int position, long id) {
                if (sessionDetailsDialog != null) {
                    try {
                        if (sessionDetailsDialog.isShowing()) {
                            sessionDetailsDialog.dismiss();
                        }
                    } catch (WindowManager.BadTokenException e) {
                        // Activity is not in the foreground
                    }
                }

                if (sessionDialog != null) {
                    try {
                        if (sessionDialog.isShowing()) {
                            sessionDialog.dismiss();
                        }
                    } catch (WindowManager.BadTokenException e) {
                        // Activity is not in the foreground
                    }
                }

                selected_session = (SessionSQL) getListView().getItemAtPosition(position);
                setSessionDialogDetails();

                sessionDialog = new AlertDialog.Builder(getActivity())
                        .setIcon(
                                getActivity().getResources().getDrawable(
                                        getMeditationAssistant().getTheme().obtainStyledAttributes(getMeditationAssistant().getMATheme(true),
                                                new int[]{R.attr.actionIconGoToToday})
                                                .getResourceId(0, 0)
                                )
                        )
                        .setTitle(session_title)
                        .setItems(R.array.session_actions,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        if (which == 0) {
                                            if (getMeditationAssistant()
                                                    .getTimeStartMeditate() > 0) {
                                                getActivity().runOnUiThread(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                getMeditationAssistant()
                                                                        .shortToast(
                                                                                getString(R.string.sessionNotPostedMeditating));
                                                            }
                                                        }
                                                );
                                            } else {
                                                getMeditationAssistant().getMediNET().session.started = selected_session._started;
                                                getMeditationAssistant().getMediNET().session.length = selected_session._length;
                                                getMeditationAssistant().getMediNET().session.message = selected_session._message;
                                                getMeditationAssistant().getMediNET().session.streakday = selected_session._streakday;
                                                getMeditationAssistant().getMediNET()
                                                        .postSession(true,
                                                                getActivity());
                                            }
                                        } else {
                                            AlertDialog deleteDialog = new AlertDialog.Builder(
                                                    getActivity())
                                                    .setIcon(
                                                            getActivity().getResources().getDrawable(
                                                                    getMeditationAssistant().getTheme().obtainStyledAttributes(getMeditationAssistant().getMATheme(true),
                                                                            new int[]{R.attr.actionIconGoToToday})
                                                                            .getResourceId(0, 0)
                                                            )
                                                    )
                                                    .setTitle(session_title)
                                                    .setItems(
                                                            R.array.session_delete_actions,
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface dialog,
                                                                        int which) {
                                                                    if (which == 0
                                                                            || which == 1) {
                                                                        // Delete
                                                                        // locally
                                                                        getMeditationAssistant().db
                                                                                .deleteSession(selected_session);
                                                                        getMeditationAssistant()
                                                                                .shortToast(
                                                                                        getString(R.string.sessionDeletedLocally));
                                                                    }
                                                                    if (which == 0
                                                                            || which == 2) {
                                                                        // Delete on
                                                                        // Medinet
                                                                        getMeditationAssistant()
                                                                                .getMediNET()
                                                                                .deleteSessionByStarted(
                                                                                        selected_session._started);
                                                                    }

                                                                    if (which == 0
                                                                            || which == 1) {
                                                                        refreshSessionList();
                                                                    }
                                                                }
                                                            }
                                                    ).create();
                                            deleteDialog.show();
                                        }
                                    }
                                }
                        ).create();

                sessionDialog.show();

                return true;
            }
        });

        refreshSessionList();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (sessionDetailsDialog != null) {
            try {
                if (sessionDetailsDialog.isShowing()) {
                    sessionDetailsDialog.dismiss();
                }
            } catch (WindowManager.BadTokenException e) {
                // Activity is not in the foreground
            }
        }

        if (sessionDialog != null) {
            try {
                if (sessionDialog.isShowing()) {
                    sessionDialog.dismiss();
                }
            } catch (WindowManager.BadTokenException e) {
                // Activity is not in the foreground
            }
        }

        selected_session = (SessionSQL) l.getItemAtPosition(position);
        setSessionDialogDetails();

        View detailsView = LayoutInflater.from(getActivity()).inflate(
                R.layout.session_details,
                (ViewGroup) getActivity().findViewById(R.id.sessionDetails_root));

        TextView txtSessionDetailsStarted = (TextView) detailsView.findViewById(R.id.txtSessionDetailsStarted);
        TextView txtSessionDetailsMessage = (TextView) detailsView.findViewById(R.id.txtSessionDetailsMessage);

        txtSessionDetailsStarted.setText(String.format(getString(R.string.sessionStartedAt), session_started));

        if (!selected_session._message.trim().equals("")) {
            txtSessionDetailsMessage.setText(selected_session._message.trim());
        } else {
            View divSessionDetailsMessage = detailsView.findViewById(R.id.divSessionDetailsMessage);

            divSessionDetailsMessage.setVisibility(View.GONE);
            txtSessionDetailsMessage.setVisibility(View.GONE);
        }

        sessionDetailsDialog = new AlertDialog.Builder(getActivity())
                .setIcon(
                        getActivity().getResources().getDrawable(
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

    private void setSessionDialogDetails() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy h:mm a",
                Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());

        SimpleDateFormat sdf2 = new SimpleDateFormat("h:mm a",
                Locale.getDefault());
        sdf2.setTimeZone(TimeZone.getDefault());

        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selected_session._completed * 1000);
        Date date = cal.getTime();

        session_title = String.valueOf(selected_session._length / 3600) + ":"
                + String.format("%02d", (selected_session._length % 3600) / 60)
                + " - " + sdf.format(date);

        cal.setTimeInMillis(selected_session._started * 1000);
        session_started = sdf2.format(cal.getTime());
    }

    public void refreshSessionList() {
        setListAdapter(new SessionAdapter(getActivity(),
                getMeditationAssistant().db.getAllSessions()));
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
package sh.ftp.rocketninelabs.meditationassistant;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;

public class SessionsFragment extends ListFragment {
    public MeditationAssistant ma = null;
    AlertDialog sessionDialog = null;
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
                                        if (which == 0) { // Post
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
                                                getMeditationAssistant().getMediNET().session.modified = selected_session._modified;
                                                getMeditationAssistant().getMediNET().postSession(0, null, null);
                                            }
                                        } else { // Delete
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
        getMeditationAssistant().showSessionDialog(selected_session, getActivity());
    }

    public void refreshSessionList() {
        setListAdapter(new SessionAdapter(getActivity(), getMeditationAssistant().db.getAllSessions()));
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
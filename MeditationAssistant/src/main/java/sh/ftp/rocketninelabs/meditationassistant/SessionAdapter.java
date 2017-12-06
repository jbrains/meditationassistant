package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SessionAdapter extends ArrayAdapter<SessionSQL> {
    public Map<String, Integer> sessions_map = new HashMap<String, Integer>();
    // private final ArrayList<MeditationSession> sessions;
    private Context context = null;
    private ArrayList<SessionSQL> sessions = null;

    public SessionAdapter(Context context, ArrayList<SessionSQL> sess) {
        super(context, R.layout.activity_sessions_item, sess);
        this.context = context;
        this.sessions = sess;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.activity_sessions_item,
                parent, false);
        TextView txtSessionLength = (TextView) rowView
                .findViewById(R.id.session_length);
        TextView txtSessionStarted = (TextView) rowView
                .findViewById(R.id.session_started);
        TextView txtSessionStartedTime = (TextView) rowView
                .findViewById(R.id.session_started_time);

        txtSessionLength
                .setText(String.valueOf(sessions.get(position)._length / 3600)
                        + ":"
                        + String.format("%02d",
                        (sessions.get(position)._length % 3600) / 60));
        // txtSessionLength.setText(String.format("%d:%02d",
        // sessions.get(position).length/3600,
        // (sessions.get(position).length%3600)/60));

        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((sessions.get(position)._started + sessions
                .get(position)._length) * 1000);
        Date date = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        SimpleDateFormat sdf2 = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdf3 = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        sdf2.setTimeZone(TimeZone.getDefault());
        sdf3.setTimeZone(TimeZone.getDefault());

        // sessions.get(position).date = sdf.format(date);
        // sessions.get(position).time = sdf2.format(date);

        txtSessionStartedTime.setText(sdf.format(date));
        txtSessionStarted.setText(sdf2.format(date));

        // Change icon based on name
        // String s = values[position];

        // Log.d("MeditationAssistant", "LV: " + s);

        /*if (!sessions_map.containsKey(sdf3.format(date))) {
            Log.d("MeditationAssistant", "Session position: " + sdf3.format(date) + " - " + String.valueOf(position));
            sessions_map.put(sdf3.format(date), position);
        }*/

        return rowView;
    }
}
package sh.ftp.rocketninelabs.meditationassistant;

import org.json.JSONException;
import org.json.JSONObject;

public class MeditationSession {
    public long id = 0;
    public long length = 0;
    public long started = 0;
    public long completed = 0;
    public long streakday = 0;
    public String date = "";
    public String time = "";
    public String message = "";

    public JSONObject export() {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("id", id);
            jobj.put("length", length);
            jobj.put("started", started);
            jobj.put("completed", completed);
            jobj.put("streakday", streakday);
            jobj.put("message", message);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        return jobj;
    }
}

package sh.ftp.rocketninelabs.meditationassistant;

import org.json.JSONException;
import org.json.JSONObject;

public class MeditationSession {
    public long id = 0;
    public long started = 0;
    public long completed = 0;
    public long length = 0;
    public String message = "";
    public long streakday = 0;
    public long modified = 0;

    public JSONObject export() {
        JSONObject jobj = new JSONObject();
        try {
            jobj.put("id", id);
            jobj.put("started", started);
            jobj.put("completed", completed);
            jobj.put("length", length);
            jobj.put("message", message);
            jobj.put("streakday", streakday);
            jobj.put("modified", modified);
        } catch (JSONException e1) {
            e1.printStackTrace();
        }
        return jobj;
    }
}

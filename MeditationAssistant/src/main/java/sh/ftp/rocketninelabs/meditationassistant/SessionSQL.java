package sh.ftp.rocketninelabs.meditationassistant;

public class SessionSQL {
    public Long _id;
    public Long _started;
    public Long _completed;
    public Long _length;
    public String _message;
    public Long _isposted;
    public Long _streakday;
    public Long _modified;

    public SessionSQL() {
        this._id = (long) 0;
        this._started = (long) 0;
        this._completed = (long) 0;
        this._length = (long) 0;
        this._message = "";
        this._isposted = (long) 0;
        this._streakday = (long) 0;
        this._modified = (long) 0;
    }

    public SessionSQL(Long id, Long started, Long completed, Long length, String message,
                      Long isposted, Long streakday, Long modified) {
        this._id = id;
        this._started = started;
        this._completed = completed;
        this._length = length;
        this._message = message;
        this._isposted = isposted;
        this._streakday = streakday;
        this._modified = modified;
    }

    public SessionSQL(Long started, Long completed, Long length, String message, Long isposted, Long streakday, Long modified) {
        this._started = started;
        this._completed = completed;
        this._length = length;
        this._message = message;
        this._isposted = isposted;
        this._streakday = streakday;
        this._modified = modified;
    }

    public Long getID() {
        return this._id;
    }

    public void setID(Long id) {
        this._id = id;
    }
}
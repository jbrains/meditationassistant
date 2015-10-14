package sh.ftp.rocketninelabs.meditationassistant;

public class SessionSQL {
    public Long _id;
    public Long _started;
    public Long _completed;
    public Long _length;
    public String _message;
    public Long _isposted;
    public Long _streakday;

    public SessionSQL() {
    }

    public SessionSQL(Long id, Long started, Long completed, Long length, String message,
                      Long isposted, Long streakday) {
        this._id = id;
        this._started = started;
        this._completed = completed;
        this._length = length;
        this._message = message;
        this._isposted = isposted;
        this._streakday = streakday;
    }

    public SessionSQL(Long started, Long completed, Long length, String message, Long isposted, Long streakday) {
        this._started = started;
        this._completed = completed;
        this._length = length;
        this._message = message;
        this._isposted = isposted;
        this._streakday = streakday;
    }

    public Long getID() {
        return this._id;
    }

    public void setID(Long id) {
        this._id = id;
    }
}
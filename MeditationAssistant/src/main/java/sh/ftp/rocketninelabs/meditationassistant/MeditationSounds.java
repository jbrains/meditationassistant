package sh.ftp.rocketninelabs.meditationassistant;

public class MeditationSounds {
    public static int getMeditationSound(String sound) {
        if (sound.equals("gong")) {
            return R.raw.gong;
        } else if (sound.equals("gong_burmese")) {
            return R.raw.gong_burmese;
        } else if (sound.equals("gong_metal")) {
            return R.raw.gong_metal;
        } else if (sound.equals("gong_heavy")) {
            return R.raw.gong_heavy;
        } else if (sound.equals("bell_indian")) {
            return R.raw.bell_indian;
        } else if (sound.equals("bell_temple")) {
            return R.raw.bell_temple;
        } else if (sound.equals("tinsha")) {
            return R.raw.tinsha;
        } else if (sound.equals("None")) {
            return 0;
        }
        return R.raw.gong;
    }

    public static String getMeditationSoundName(String sound) { // TODO: Localize
        if (sound.equals("gong")) {
            return "Gong";
        } else if (sound.equals("gong_burmese")) {
            return "Burmese gong";
        } else if (sound.equals("gong_metal")) {
            return "Metal gong";
        } else if (sound.equals("gong_heavy")) {
            return "Heavy gong";
        } else if (sound.equals("bell_indian")) {
            return "Indian bell";
        } else if (sound.equals("bell_temple")) {
            return "Temple bell";
        } else if (sound.equals("tinsha")) {
            return "Three Tinsha";
        } else if (sound.equals("none")) {
            return "";
        }
        return "Gong";
    }
}
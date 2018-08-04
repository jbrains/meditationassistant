package sh.ftp.rocketninelabs.meditationassistant;

public class MeditationSounds {
    public static int getMeditationSound(String sound) {
        switch (sound) {
            case "gong_burmese":
                return R.raw.gong_burmese;
            case "gong_metal":
                return R.raw.gong_metal;
            case "gong_heavy":
                return R.raw.gong_heavy;
            case "bell_indian":
                return R.raw.bell_indian;
            case "bell_temple":
                return R.raw.bell_temple;
            case "tinsha":
                return R.raw.tinsha;
            case "None":
                return 0;
            default:
                return R.raw.gong;
        }
    }

    public static String getMeditationSoundName(String sound) { // TODO: Localize
        switch (sound) {
            case "gong_burmese":
                return "Burmese gong";
            case "gong_metal":
                return "Metal gong";
            case "gong_heavy":
                return "Heavy gong";
            case "bell_indian":
                return "Indian bell";
            case "bell_temple":
                return "Temple bell";
            case "tinsha":
                return "Three Tinsha";
            case "none":
                return "";
            default:
                return "Gong";
        }
    }
}
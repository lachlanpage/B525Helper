package me.lachlanpage.b525helper;

import java.util.Locale;

// Utility class converts modem Data stats into more readable form
public class DataConversion {
    public String convertElapsedTime(int totalSecs) {
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        return String.format(Locale.ENGLISH,"%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String convertBytesToMegabits(float bytes) {return String.valueOf(bytes*(8e-6f));}

    public String convertBytesToGigabytes(float bytes) {
        return String.valueOf(bytes*(1e-9f));
    }
}

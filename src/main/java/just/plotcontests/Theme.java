package just.plotcontests;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class Theme {

    private static List<String[]> CSV_DATA = null;

    private static HashMap<Long, String> themes = new HashMap<>();
    private static HashMap<String, String> themesByWeek = new HashMap<>();

    public static void loadThemes() {
        // File format: week,theme
        File lastThemes = new File(Contests.contests.getDataFolder(), "last_themes.csv");
        if (lastThemes.isFile()) {
            try {
                List<String> l = Files.readAllLines(lastThemes.toPath(), StandardCharsets.UTF_8);
                for (String s : l) {
                    String[] a = s.split(",");
                    if (a.length >= 2) {
                        themes.put(Long.parseLong(a[0]), a[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Load both this week's and last week's theme
        Theme.theme();
        Theme.lastWeekTheme();
    }

    private static void saveThemes() {
        File lastThemes = new File(Contests.contests.getDataFolder(), "last_themes.csv");
        lastThemes.getParentFile().mkdirs();

        ArrayList<String> csv = new ArrayList<>();
        for (Long key : themes.keySet()) {
            csv.add(key + "," + themes.get(key));
        }

        try {
            Files.write(lastThemes.toPath(), csv, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCsv() {
        CSV_DATA = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new URL(Contests.contests.themes_csv_url).openStream())) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] a = line.split(",");
                if (a.length < 2 || a[0].isEmpty())
                    continue;
                CSV_DATA.add(a);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long lastWeek() {
        return thisWeek() - 1;
    }

    public static String lastWeekStr() {
        return Long.toString(lastWeek());
    }

    public static long thisWeek() {
        return System.currentTimeMillis() / 1000L / 60 / 60 / 24 / 7;
    }

    public static String thisWeekStr() {
        return Long.toString(thisWeek());
    }

    public static String calculateTheme(long week) {
        if (themes.containsKey(week))
            return themes.get(week);

        if (CSV_DATA == null)
            loadCsv();

        long t = System.currentTimeMillis();
        t -= t % (7 * 24 * 60 * 60 * 1000L);

        ArrayList<String> possibleThemes = new ArrayList<>();
        int lowestCount = Integer.MAX_VALUE;
        for (String[] a : CSV_DATA) {
            String theme = a[0];
            boolean inDate = false;
            int count = 0;
            if (a.length >= 2) {
                if (a[1].contains("/")) {
                    // Date
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(t);
                    for (int i = 0; i < 7; i++) {
                        String date = c.get(Calendar.DATE) + "/" + (c.get(Calendar.MONTH) + 1);
                        if (a[1].equalsIgnoreCase(date))
                            inDate = true;
                        date = c.get(Calendar.DATE) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR);
                        if (a[1].equalsIgnoreCase(date))
                            inDate = true;
                        c.add(Calendar.DATE, 1);
                    }
                    if (!inDate)
                        continue; // Not this time
                }
            }
            if (inDate) {
                count = -1;
            } else {
                for (String oldTheme : themes.values()) {
                    if (oldTheme.equalsIgnoreCase(theme)) {
                        count++;
                    }
                }
            }
            if (count < lowestCount) {
                possibleThemes.clear();
                lowestCount = count;
            }
            if (count == lowestCount) {
                possibleThemes.add(theme);
            }
        }

        if (possibleThemes.isEmpty()) {
            return "There are no themes, contact the system administrator";
        }

        String theme = possibleThemes.get((int) (Math.random() * possibleThemes.size()));
        themes.put(week, theme);
        saveThemes();
        return theme;
    }

    public static String theme() {
        if (!themesByWeek.containsKey(thisWeekStr()))
            themesByWeek.put(thisWeekStr(), calculateTheme(thisWeek()));
        return themesByWeek.get(thisWeekStr());
    }

    public static String lastWeekTheme() {
        if (!themesByWeek.containsKey(lastWeekStr()))
            themesByWeek.put(lastWeekStr(), calculateTheme(lastWeek()));
        return themesByWeek.get(lastWeekStr());
    }

    public static String timeRemaining() {
        long aweekinmilliseconds = 7 * 24 * 60 * 60 * 1000L;
        long t = aweekinmilliseconds - System.currentTimeMillis() % aweekinmilliseconds;
        t /= 1000;
        if (t >= 48 * 60 * 60)
            return t / 24 / 60 / 60 + " days";
        if (t >= 24 * 60 * 60)
            return t / 24 / 60 / 60 + " day";
        if (t >= 2 * 60 * 60)
            return t / 60 / 60 + " hours";
        if (t >= 60 * 60)
            return t / 60 / 60 + " hour";
        if (t >= 2 * 60)
            return t / 60 + " minutes";
        if (t >= 60)
            return t / 60 + " minute";
        return t + " seconds";
    }

    public static void main(String[] a) {
        Calendar c = Calendar.getInstance();
        System.out.println(c.get(Calendar.DATE) + "/" + (c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.YEAR));
        System.out.println(lastWeek());
        System.out.println(thisWeek());
        System.out.println("Next week: " + (thisWeek() + 1));
        System.out.println((int) (thisWeek() % 33));
        System.out.println(calculateTheme(lastWeek()));
        System.out.println(calculateTheme(thisWeek()));
        System.out.println(timeRemaining());
    }
}

package just.plotcontests;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Entry {
    private static HashMap<String, ArrayList<Entry>> entries = new HashMap<String, ArrayList<Entry>>();
    public UUID uuid;
    public String world;
    public String plotid;
    public int votes = 0;
    public String[] votelist = new String[0];

    public static ArrayList<Entry> getEntries(String week) {
        if (!entries.containsKey(week)) {
            load(week);
        }
        return entries.get(week);
    }

    public static Entry getLastWeekWinner() {
        if (!entries.containsKey(Theme.lastWeekStr()))
            load(Theme.lastWeekStr());
        ArrayList<Entry> es = entries.get(Theme.lastWeekStr());
        for (Entry e : es)
            if (e.votes >= 1000)
                return e;
        return null;
    }

    public static Entry get(UUID uuid) {
        if (!entries.containsKey(Theme.thisWeekStr()))
            load(Theme.thisWeekStr());
        ArrayList<Entry> es = entries.get(Theme.thisWeekStr());
        for (Entry e : es)
            if (e.uuid.equals(uuid))
                return e;
        return null;
    }

    public static Entry get(int index) {
        if (!entries.containsKey(Theme.thisWeekStr()))
            load(Theme.thisWeekStr());
        ArrayList<Entry> es = entries.get(Theme.thisWeekStr());
        if (index == -1)
            return es.get((int) (Math.random() * es.size()));
        return es.get(index);
    }

    public static Entry get(String world, String plotid) {
        if (!entries.containsKey(Theme.thisWeekStr()))
            load(Theme.thisWeekStr());
        ArrayList<Entry> es = entries.get(Theme.thisWeekStr());
        for (Entry e : es)
            if (e.world.equalsIgnoreCase(world) && e.plotid.equalsIgnoreCase(plotid))
                return e;
        return null;
    }

    public static Entry pop(UUID uuid) {
        if (!entries.containsKey(Theme.thisWeekStr()))
            load(Theme.thisWeekStr());
        ArrayList<Entry> es = entries.get(Theme.thisWeekStr());
        for (Entry e : es)
            if (e.uuid.equals(uuid)) {
                es.remove(e);
                return e;
            }
        return null;
    }

    /**
     * @return <code>true</code> if a previous entry was remove, <code>false</code> if no entry exists already
     */
    public static boolean add(UUID uuid, String world, String plotid) {
        if (!entries.containsKey(Theme.thisWeekStr()))
            load(Theme.thisWeekStr());
        int v = 0;
        String[] vl = new String[0];
        ArrayList<Entry> es = entries.get(Theme.thisWeekStr());
        boolean r = false;
        for (Entry e : es)
            if (e.uuid.equals(uuid)) {
                v = e.votes;
                vl = e.votelist;
                es.remove(e);
                r = true;
                break;
            }
        Entry e = new Entry();
        e.uuid = uuid;
        e.world = world;
        e.plotid = plotid;
        e.votes = v;
        e.votelist = vl;
        es.add(e);
        return r;
    }

    public static Entry getLastWeek(UUID uuid) {
        if (!entries.containsKey(Theme.lastWeekStr()))
            load(Theme.lastWeekStr());
        ArrayList<Entry> es = entries.get(Theme.lastWeekStr());
        for (Entry e : es)
            if (e.uuid.equals(uuid))
                return e;
        return null;
    }

    public static Entry getLastWeek(int index) {
        if (!entries.containsKey(Theme.lastWeekStr()))
            load(Theme.lastWeekStr());
        ArrayList<Entry> es = entries.get(Theme.lastWeekStr());
        if (index == -1)
            return es.get((int) (Math.random() * es.size()));
        return es.get(index);
    }

    public static Entry getLastWeek(String world, String plotid) {
        if (!entries.containsKey(Theme.lastWeekStr()))
            load(Theme.lastWeekStr());
        ArrayList<Entry> es = entries.get(Theme.lastWeekStr());
        for (Entry e : es)
            if (e.world.equalsIgnoreCase(world) && e.plotid.equalsIgnoreCase(plotid))
                return e;
        return null;
    }

    public static void saveThisWeek() {
        save(Theme.thisWeekStr());
    }

    public static void saveLastWeek() {
        save(Theme.lastWeekStr());
    }

    public static void save(String week) {
        if (entries.containsKey(week)) {
            File f = new File(Contests.contests.getDataFolder(), "entries." + week + ".csv");
            ArrayList<String> l = new ArrayList<String>();
            for (Entry e : entries.get(week))
                l.add(StringUtils.join(new Object[]{e.uuid, e.world, e.plotid, e.votes, StringUtils.join(e.votelist, ';')}, ','));
            try {
                f.getParentFile().mkdirs();
                Files.write(f.toPath(), l, StandardCharsets.UTF_8);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private static void load(String week) {
        ArrayList<Entry> a = new ArrayList<Entry>();
        entries.put(week, a);
        try {
            File f = new File(Contests.contests.getDataFolder(), "entries." + week + ".csv");
            if (f.isFile()) {
                List<String> l = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                Contests.contests.getLogger().info(week + ") Loading " + l.size() + " entries.");
                for (String s : l) {
                    String[] sa = s.split(",");
                    if (sa.length > 3) {
                        Entry e = new Entry();
                        e.uuid = UUID.fromString(sa[0]);
                        e.world = sa[1];
                        e.plotid = sa[2];
                        e.votes = Integer.parseInt(sa[3]);
                        if (sa.length > 4)
                            e.votelist = sa[4].split(";");
                        a.add(e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Contests.contests.getLogger().info(week + ") " + a.size() + " entries were loaded.");
        orderByVotes(a);
    }

    private static void orderByVotes(ArrayList<Entry> a) {
        l:
        for (int i = 1; i < a.size(); i++) {
            if (i <= 0) continue;
            while (a.get(i).votes > a.get(i - 1).votes) {
                Entry e = a.get(i);
                a.set(i, a.get(i - 1));
                a.set(i - 1, e);
                if ((i -= 1) <= 0)
                    break;
            }
        }
    }

    /**
     * @return <code>true</code> if vote was added, <code>false</code> if vote was removed
     */
    public boolean toggleVote(UUID uuid, int votes) {
        String u = uuid.toString().toLowerCase();
        for (int i = 0; i < votelist.length; i++)
            if (votelist[i].equalsIgnoreCase(u)) {
                this.votes -= votes;
                String[] l = new String[votelist.length - 1];
                System.arraycopy(votelist, 0, l, 0, i);
                System.arraycopy(votelist, i + 1, l, i, votelist.length - i - 1);
                votelist = l;
                return false;
            }
        this.votes += votes;
        String[] l = new String[votelist.length + 1];
        System.arraycopy(votelist, 0, l, 0, votelist.length);
        votelist = l;
        votelist[votelist.length - 1] = u;
        return true;
    }
}

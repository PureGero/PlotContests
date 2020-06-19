package just.plotcontests;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Contests extends JavaPlugin {

    public static Contests contests = null;

    public String themes_csv_url = null;

    public UUID winner = null;

    private final ArrayList<UUID> winnings = new ArrayList<>();

    /**
     * Get the number of contests a certain player has won
     * @param uuid The uuid of the player
     * @return The number of contests won, or 0 if no contests have been won.
     */
    public int getWinnings(UUID uuid) {
        int c = 0;
        synchronized (winnings) {
            for (UUID u : winnings) {
                if (u.equals(uuid))
                    c += 1;
            }
        }
        return c;
    }

    public void onEnable() {
        contests = this;

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        themes_csv_url = getConfig().getString("themes_csv_url");
        saveConfig();

        new ContestJudgeCommand(this);
        new ContestCommand(this);

        new WinnerRankListener(this);

        Entry e = Entry.getLastWeekWinner();
        if (e != null) {
            winner = e.uuid;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Theme.loadThemes();
            for (File f : getDataFolder().listFiles()) {
                if (!(f.isFile() && f.getName().endsWith(".csv") && f.getName().startsWith("entries."))) {
                    continue;
                }
                try {
                    List<String> l = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
                    for (String s : l) {
                        String[] sa = s.split(",");
                        if (sa.length > 3) {
                            int votes = Integer.parseInt(sa[3]);
                            if (votes >= 1000) {
                                synchronized (winnings) {
                                    winnings.add(UUID.fromString(sa[0]));
                                }
                            }
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDisable() {

    }
}

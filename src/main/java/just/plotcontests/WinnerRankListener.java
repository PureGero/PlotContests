package just.plotcontests;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class WinnerRankListener implements Listener {

    private final Contests contests;

    public WinnerRankListener(Contests contests) {
        this.contests = contests;

        contests.getServer().getPluginManager().registerEvents(this, contests);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().getUniqueId().equals(contests.winner)) {
            int i, j;
            String f = e.getFormat();
            if ((i = f.indexOf('[')) >= 0 && (j = f.indexOf(']')) > 0) {
                e.setFormat((f.substring(0, i + 1) + "Winner" + f.substring(j)).replaceAll("\\%1\\$s", ChatColor.GOLD + "\\%1\\$s" + ChatColor.WHITE));
            } else
                e.setFormat(e.getFormat().replaceAll("\\%1\\$s", ChatColor.GOLD + "[Winner] \\%1\\$s" + ChatColor.WHITE));
        }
    }
}

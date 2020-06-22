package just.plotcontests;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.PlotId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ContestJudgeCommand implements CommandExecutor, TabCompleter {

    private final Contests contests;

    public ContestJudgeCommand(Contests contests) {
        this.contests = contests;

        PluginCommand contestJudge = contests.getCommand("contestjudge");

        if (contestJudge == null) {
            throw new RuntimeException("Command /contestjudge is not registered in the plugin.yml");
        }

        contestJudge.setExecutor(this);
        contestJudge.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = "help";

        if (args.length > 0) {
            cmd = args[0].toLowerCase();
        }

        if (cmd.equalsIgnoreCase("help")) {
            sender.sendMessage(new String[]{
                    ChatColor.GOLD + " ---- " + ChatColor.YELLOW + "Help: PlotContest" + ChatColor.GOLD + " ---- ",
                    ChatColor.YELLOW + "/" + label + " theme" + ChatColor.GOLD + ": See last week's theme.",
                    ChatColor.YELLOW + "/" + label + " winner" + ChatColor.GOLD + ": Make this plot the winner.",
                    ChatColor.YELLOW + "/" + label + " list [page]" + ChatColor.GOLD + ": List last week's entries.",
                    ChatColor.YELLOW + "/" + label + " <index/player>" + ChatColor.GOLD + ": Teleport to a certain player's entry.",
            });
        } else if (cmd.equalsIgnoreCase("theme")) {
            sender.sendMessage(ChatColor.GOLD + "Last week's theme was " + ChatColor.YELLOW + Theme.lastWeekTheme());
        } else if (cmd.equalsIgnoreCase("winner")) {
            Plot t = JustPlots.getPlotAt((Entity) sender);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "You must be standing over a plot to do that!");
                return false;
            }
            Entry e = Entry.getLastWeek(t.getWorldName(), t.getId().toString());
            if (e == null) {
                sender.sendMessage(ChatColor.RED + "This plot has not been entered into last week's contest");
                return false;
            }
            Entry w = Entry.getLastWeekWinner();
            if (e.votes < 1000) {
                e.votes += 1000;
                contests.winner = e.uuid;
                if (w != null && !w.uuid.equals(e.uuid)) {
                    w.votes -= 1000;
                    sender.sendMessage(ChatColor.RED + Bukkit.getOfflinePlayer(w.uuid).getName() + "'s plot is no longer the winner.");
                }
                sender.sendMessage(ChatColor.GREEN + "Successfully made this plot the winner");
            } else {
                e.votes -= 1000;
                sender.sendMessage(ChatColor.GREEN + "Removed win for this plot");
            }
            Bukkit.getScheduler().runTaskAsynchronously(contests, Entry::saveLastWeek);
        } else if (cmd.equalsIgnoreCase("list")) {
            int p = 0;
            try {
                p = Integer.parseInt(args[1]) - 1;
            } catch (Exception ignored) {}
            sender.sendMessage(ChatColor.GOLD + " ---- " + ChatColor.YELLOW + "Entries (Page " + (p + 1) + ")" + ChatColor.GOLD + " ---- ");
            for (int i = p * 10; i < p * 10 + 10; i++) {
                try {
                    Entry e = Entry.getLastWeek(i);
                    sender.sendMessage(ChatColor.GOLD + " " + (i + 1) + ") " + ChatColor.YELLOW + e.votes
                            + ChatColor.GOLD + " " + e.world + " " + e.plotid
                            + ChatColor.YELLOW + " " + Bukkit.getOfflinePlayer(e.uuid).getName());
                } catch (Exception ignored) {}
            }
        } else {
            try {
                int i = Integer.parseInt(cmd) - 1;
                Entry e = Entry.getLastWeek(i);
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.uuid);
                Player tp = (Player) sender;
                Plot t = JustPlots.getPlot(e.world, new PlotId(e.plotid));
                if (t == null) {
                    tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                    return false;
                }
                tp.teleport(t.getHome());
                tp.sendMessage(ChatColor.GREEN + "Teleported to " + p.getName() + "'s entry!");
            } catch (NumberFormatException | IndexOutOfBoundsException | NullPointerException e2) {
                OfflinePlayer p = Bukkit.getPlayerExact(cmd);
                if (p == null)
                    p = Bukkit.getOfflinePlayer(cmd);
                Entry e = Entry.getLastWeek(p.getUniqueId());
                if (e == null) {
                    sender.sendMessage(ChatColor.RED + "Player has not entered this week's contest!");
                    return false;
                }
                Player tp = (Player) sender;
                Plot t = JustPlots.getPlot(e.world, new PlotId(e.plotid));
                if (t == null) {
                    tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                    return false;
                }
                tp.teleport(t.getHome());
                tp.sendMessage(ChatColor.GREEN + "Teleported to " + p.getName() + "'s entry!");
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        ArrayList<String> ret = new ArrayList<>();

        if (args.length == 1) {
            for (String s : new String[]{
                    "theme", "winner", "list"
            }) {
                if (s.startsWith(args[0].toLowerCase())) {
                    ret.add(s);
                }
            }

            for (Entry entry : Entry.getEntries(Theme.lastWeekStr())) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.uuid);

                if (player.getName() != null && player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    ret.add(player.getName());
                }
            }
        }

        return ret;
    }
}

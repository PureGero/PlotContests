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

public class ContestCommand implements CommandExecutor, TabCompleter {

    private final Contests contests;

    public ContestCommand(Contests contests) {
        this.contests = contests;

        PluginCommand contest = contests.getCommand("contest");

        if (contest == null) {
            throw new RuntimeException("Command /contestjudge is not registered in the plugin.yml");
        }

        contest.setExecutor(this);
        contest.setTabCompleter(this);
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
                    ChatColor.YELLOW + "/" + label + " enter" + ChatColor.GOLD + ": Enter your plot into this week's contest.",
                    ChatColor.YELLOW + "/" + label + " theme" + ChatColor.GOLD + ": See this week's theme.",
                    ChatColor.YELLOW + "/" + label + " vote" + ChatColor.GOLD + ": Vote for another player's entry.",
                    ChatColor.YELLOW + "/" + label + " winner" + ChatColor.GOLD + ": Who won last week's contest?",
                    ChatColor.YELLOW + "/" + label + " random" + ChatColor.GOLD + ": Teleport to a random player's entry.",
                    ChatColor.YELLOW + "/" + label + " <player>" + ChatColor.GOLD + ": Teleport to a certain player's entry.",
            });
        } else if (cmd.equalsIgnoreCase("enter") || cmd.equalsIgnoreCase("submit")) {
            Plot t = JustPlots.getPlotAt((Entity) sender);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "You must be standing over a plot to do that!");
                return false;
            }

            if (!t.isOwner((Player) sender)) {
                sender.sendMessage(ChatColor.RED + "You must be the owner of the plot to enter!");
                return false;
            }

            Entry old = Entry.pop(((Player) sender).getUniqueId());
            if (old != null) {
                sender.sendMessage(ChatColor.YELLOW + "Your previous entry for this week's build contest has been withdrawn.");
            }

            if (old == null || !old.world.equals(t.getWorldName()) || !old.plotid.equals(t.getId().toString())) {
                Entry.add(((Player) sender).getUniqueId(), t.getWorldName(), t.getId().toString());
                sender.sendMessage(ChatColor.GREEN + "Plot " + t.getId() + " has been entered into this week's build contest.");
            }
            Bukkit.getScheduler().runTaskAsynchronously(contests, Entry::saveThisWeek);
        } else if (cmd.equalsIgnoreCase("theme")) {
            sender.sendMessage(ChatColor.GOLD + "This week's theme is " + ChatColor.YELLOW + Theme.theme());
            sender.sendMessage(ChatColor.GOLD + "You have " + ChatColor.YELLOW + Theme.timeRemaining() + ChatColor.GOLD + " to enter!");
        } else if (cmd.equalsIgnoreCase("vote")) {
            Plot t = JustPlots.getPlotAt((Entity) sender);
            if (t == null) {
                sender.sendMessage(ChatColor.RED + "You must be standing over a plot to do that!");
                return false;
            }
            Entry e = Entry.get(t.getWorldName(), t.getId().toString());
            if (e == null) {
                sender.sendMessage(ChatColor.RED + "This plot has not been entered into this week's contest");
                return false;
            }
            if (e.toggleVote(((Player) sender).getUniqueId(), sender.hasPermission("contest.highvote") ? 15 : 1)) {
                sender.sendMessage(ChatColor.GREEN + "Successfully voted for this plot");
            } else
                sender.sendMessage(ChatColor.GREEN + "Removed vote for this plot");
            Bukkit.getScheduler().runTaskAsynchronously(contests, Entry::saveThisWeek);
        } else if (cmd.equalsIgnoreCase("winner")) {
            Entry e = Entry.getLastWeekWinner();
            if (e == null)
                sender.sendMessage(ChatColor.RED + "Last week's winner has not been decided yet!");
            else {
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.uuid);
                sender.sendMessage(ChatColor.GREEN + p.getName() + " won last week's build contest with a theme of " + Theme.lastWeekTheme());
            }
        } else if (cmd.equalsIgnoreCase("random")) {
            try {
                Entry e = Entry.get(-1);
                OfflinePlayer p = Bukkit.getOfflinePlayer(e.uuid);
                Player tp = (Player) sender;
                Plot t = JustPlots.getPlot(e.world, new PlotId(e.plotid));
                if (t == null) {
                    tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                    return false;
                }
                tp.teleport(t.getHome());
                tp.sendMessage(ChatColor.GREEN + "Teleported to " + p.getName() + "'s entry!");
            } catch (IndexOutOfBoundsException e) {
                sender.sendMessage(ChatColor.RED + "No one has entered yet!");
            }
        } else {
            try {
                int i = Integer.parseInt(cmd) - 1;
                Entry e = Entry.get(i);
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
                if (p == null) {
                    p = Bukkit.getOfflinePlayer(cmd);
                }
                Entry e = Entry.get(p.getUniqueId());
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
                    "enter", "theme", "vote", "winner", "random"
            }) {
                if (s.startsWith(args[0].toLowerCase())) {
                    ret.add(s);
                }
            }

            for (Entry entry : Entry.getEntries(Theme.thisWeekStr())) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.uuid);

                if (player.getName() != null && player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    ret.add(player.getName());
                }
            }
        }

        return ret;
    }
}

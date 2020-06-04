package just.plotcontests;

import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotId;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Contests extends JavaPlugin implements TabCompleter, Listener {

    public static Contests contests = null;

    public String themes_csv_url = null;

    public UUID winner = null;

    private ArrayList<UUID> winnings = new ArrayList<>();

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
        getCommand("contest").setTabCompleter(this);
        getCommand("contestjudge").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        themes_csv_url = getConfig().getString("themes_csv_url");
        saveConfig();

        Entry e = Entry.getLastWeekWinner();
        if (e != null)
            winner = e.uuid;

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Theme.loadThemes();
            for (File f : getDataFolder().listFiles()) {
                if (!(f.isFile() && f.getName().endsWith(".csv") && f.getName().startsWith("entries."))) {
                    break;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().getUniqueId().equals(winner)) {
            int i, j;
            String f = e.getFormat();
            if ((i = f.indexOf('[')) >= 0 && (j = f.indexOf(']')) > 0) {
                e.setFormat((f.substring(0, i + 1) + "Winner" + f.substring(j)).replaceAll("\\%1\\$s", ChatColor.GOLD + "\\%1\\$s" + ChatColor.WHITE));
            } else
                e.setFormat(e.getFormat().replaceAll("\\%1\\$s", ChatColor.GOLD + "[Winner] \\%1\\$s" + ChatColor.WHITE));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String l, String[] args) {
        try {
            String c = command.getName();
            if (c.equalsIgnoreCase("contest")) {
                if (System.currentTimeMillis() < 1507161600000L) {
                    sender.sendMessage("Unknown command. Type \"/help\" for help.");
                    return false;
                }
                String cmd = "help";
                if (args.length > 0) cmd = args[0].toLowerCase();
                if (cmd.equalsIgnoreCase("help")) {
                    sender.sendMessage(new String[]{
                            ChatColor.GOLD + " ---- " + ChatColor.YELLOW + "Help: PlotContest" + ChatColor.GOLD + " ---- ",
                            ChatColor.YELLOW + "/" + l + " enter" + ChatColor.GOLD + ": Enter your plot into this week's contest.",
                            ChatColor.YELLOW + "/" + l + " theme" + ChatColor.GOLD + ": See this week's theme.",
                            ChatColor.YELLOW + "/" + l + " vote" + ChatColor.GOLD + ": Vote for another player's entry.",
                            ChatColor.YELLOW + "/" + l + " winner" + ChatColor.GOLD + ": Who won last week's contest?",
                            ChatColor.YELLOW + "/" + l + " random" + ChatColor.GOLD + ": Teleport to a random player's entry.",
                            ChatColor.YELLOW + "/" + l + " <player>" + ChatColor.GOLD + ": Teleport to a certain player's entry.",
                    });
                } else if (cmd.equalsIgnoreCase("enter") || cmd.equalsIgnoreCase("submit")) {
                    Plot t = new BukkitLocation((Player) sender).getOwnedPlot();
                    if (t == null) {
                        sender.sendMessage(ChatColor.RED + "You must be standing over a plot to do that!");
                        return false;
                    }

                    if (!t.isOwner(((Player) sender).getUniqueId())) {
                        sender.sendMessage(ChatColor.RED + "You must be the owner of the plot to enter it!");
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
                    Bukkit.getScheduler().runTaskAsynchronously(this, Entry::saveThisWeek);
                } else if (cmd.equalsIgnoreCase("theme")) {
                    sender.sendMessage(ChatColor.GOLD + "This week's theme is " + ChatColor.YELLOW + Theme.theme());
                    sender.sendMessage(ChatColor.GOLD + "You have " + ChatColor.YELLOW + Theme.timeRemaining() + ChatColor.GOLD + " to enter!");
                } else if (cmd.equalsIgnoreCase("vote")) {
                    Plot t = new BukkitLocation((Player) sender).getOwnedPlot();
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
                    Bukkit.getScheduler().runTaskAsynchronously(this, Entry::saveThisWeek);
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
                        Plot t = getPlotById(e.world, e.plotid);
                        if (t == null) {
                            tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                            return false;
                        }
                        t.getHome(home -> PlotPlayer.wrap(tp).teleport(home));
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
                        Plot t = getPlotById(e.world, e.plotid);
                        if (t == null) {
                            tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                            return false;
                        }
                        t.getHome(home -> PlotPlayer.wrap(tp).teleport(home));
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
                        Plot t = getPlotById(e.world, e.plotid);
                        if (t == null) {
                            tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                            return false;
                        }
                        t.getHome(home -> PlotPlayer.wrap(tp).teleport(home));
                        tp.sendMessage(ChatColor.GREEN + "Teleported to " + p.getName() + "'s entry!");
                    }
                }
            } else if (c.equalsIgnoreCase("contestjudge")) {
                String cmd = "help";
                if (args.length > 0) cmd = args[0].toLowerCase();
                if (cmd.equalsIgnoreCase("help")) {
                    sender.sendMessage(new String[]{
                            ChatColor.GOLD + " ---- " + ChatColor.YELLOW + "Help: PlotContest" + ChatColor.GOLD + " ---- ",
                            ChatColor.YELLOW + "/" + l + " theme" + ChatColor.GOLD + ": See last week's theme.",
                            ChatColor.YELLOW + "/" + l + " winner" + ChatColor.GOLD + ": Make this plot the winner.",
                            ChatColor.YELLOW + "/" + l + " list [page]" + ChatColor.GOLD + ": List last week's entries.",
                            ChatColor.YELLOW + "/" + l + " <index/player>" + ChatColor.GOLD + ": Teleport to a certain player's entry.",
                    });
                } else if (cmd.equalsIgnoreCase("theme")) {
                    sender.sendMessage(ChatColor.GOLD + "Last week's theme was " + ChatColor.YELLOW + Theme.lastWeekTheme());
                } else if (cmd.equalsIgnoreCase("winner")) {
                    Plot t = new BukkitLocation((Player) sender).getOwnedPlot();
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
                        winner = e.uuid;
                        if (w != null && !w.uuid.equals(e.uuid)) {
                            w.votes -= 1000;
                            sender.sendMessage(ChatColor.RED + Bukkit.getOfflinePlayer(w.uuid).getName() + "'s plot is no longer the winner.");
                        }
                        sender.sendMessage(ChatColor.GREEN + "Successfully made this plot the winner");
                    } else {
                        e.votes -= 1000;
                        sender.sendMessage(ChatColor.GREEN + "Removed win for this plot");
                    }
                    Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                        public void run() {
                            Entry.saveLastWeek();
                        }
                    });
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
                        Plot t = getPlotById(e.world, e.plotid);
                        if (t == null) {
                            tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                            return false;
                        }
                        t.getHome(home -> PlotPlayer.wrap(tp).teleport(home));
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
                        Plot t = getPlotById(e.world, e.plotid);
                        if (t == null) {
                            tp.sendMessage(ChatColor.RED + "Failed to find " + p.getName() + "'s plot");
                            return false;
                        }
                        t.getHome(home -> PlotPlayer.wrap(tp).teleport(home));
                        tp.sendMessage(ChatColor.GREEN + "Teleported to " + p.getName() + "'s entry!");
                    }
                }
            }
            return true;
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().equalsIgnoreCase("invalid usage"))
                throw e;
        }
        return false;
    }

    public TextComponent clickable(String text, String cmd, ChatColor cc) {
        TextComponent c = new TextComponent(text);
        c.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{new TextComponent(cmd)}));
        c.setColor(net.md_5.bungee.api.ChatColor.getByChar(cc.getChar()));
        c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, cmd));
        return c;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String l, String[] args) {
        if (command.getName().equals("minigame")) {
            ArrayList<String> ret = new ArrayList<>();
            if (args.length == 1) {
                for (String s : new String[]{
                        "enter", "theme", "vote", "winner", "random"
                })
                    if (s.startsWith(args[0].toLowerCase()))
                        ret.add(s);
                for (Player p : Bukkit.getOnlinePlayers())
                    if (Entry.get(p.getUniqueId()) != null)
                        if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                            ret.add(p.getName());
            }
            return ret;
        }
        return null;
    }

    private static Plot getPlotById(String world, String pid) {
        for (PlotArea plotArea : PlotSquared.get().getPlotAreas(world)) {
            Plot plot = plotArea.getOwnedPlot(PlotId.fromString(pid));

            if (plot != null) {
                return plot;
            }
        }

        return null;
    }
}

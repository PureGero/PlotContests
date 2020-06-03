package just.plotcontests;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class BukkitLocation extends com.plotsquared.core.location.Location {
    public BukkitLocation(Location location) {
        super(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getYaw(), location.getPitch());
    }

    public BukkitLocation(Block block) {
        this(block.getLocation());
    }

    public BukkitLocation(Entity entity) {
        this(entity.getLocation());
    }
}

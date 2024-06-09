package dev.oddbyte;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;

public class Region {
    private String name;
    private Location pos1;
    private Location pos2;
    private Map<String, Boolean> flags = new HashMap<>();

    public Region(String name, Location pos1, Location pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setFlag(String flag, Boolean value) {
        if (value == null) {
            flags.remove(flag);
        } else {
            flags.put(flag, value);
        }
    }

    public Boolean getFlag(String flag) {
        return flags.get(flag);
    }

    public boolean isInRegion(Location loc) {
        if (pos1 == null || pos2 == null) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return (loc.getX() >= minX && loc.getX() <= maxX) &&
                (loc.getY() >= minY && loc.getY() <= maxY) &&
                (loc.getZ() >= minZ && loc.getZ() <= maxZ);
    }

    public String listFlags() {
        StringBuilder sb = new StringBuilder();
        sb.append("Region '").append(name).append("' - Positions: [")
                .append("Pos1: (").append(pos1.getX()).append(", ").append(pos1.getY()).append(", ").append(pos1.getZ()).append("), ")
                .append("Pos2: (").append(pos2.getX()).append(", ").append(pos2.getY()).append(", ").append(pos2.getZ()).append(")]\n");
        sb.append("Flags:\n");
        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue() == null ? "unset" : entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    public Location getCenter() {
        if (pos1 == null || pos2 == null) return null;
        double centerX = (pos1.getX() + pos2.getX()) / 2;
        double centerY = (pos1.getY() + pos2.getY()) / 2;
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2;
        return new Location(pos1.getWorld(), centerX, centerY, centerZ);
    }

    @Override
    public String toString() {
        return listFlags();
    }
}

package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityPortalEnterEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.ChangeDimensionPacket;
import cn.nukkit.utils.BlockColor;

/**
 * Nether Portal block - handles teleportation between Overworld and Nether
 */
public class BlockNetherPortal extends BlockFlowable {

    public BlockNetherPortal() {
        this(0);
    }

    public BlockNetherPortal(int meta) {
        super(0);
    }

    @Override
    public String getName() {
        return "Nether Portal Block";
    }

    @Override
    public int getId() {
        return NETHER_PORTAL;
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    @Override
    public double getHardness() {
        return -1;
    }

    @Override
    public int getLightLevel() {
        return 11;
    }

    @Override
    public boolean onBreak(Item item) {
        boolean result = super.onBreak(item);
        for (int side = 0; side <= 5; side++) {
            Block b = this.getSide(side);
            if (b != null) {
                if (b instanceof BlockNetherPortal) {
                    result &= b.onBreak(item);
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.inPortalTicks++;

        if (entity.inPortalTicks >= 80) {
            EntityPortalEnterEvent ev = new EntityPortalEnterEvent(entity, EntityPortalEnterEvent.TYPE_NETHER);
            this.level.getServer().getPluginManager().callEvent(ev);

            if (ev.isCancelled()) {
                return;
            }

            // Teleport entity to the Nether or back to Overworld
            if (entity instanceof Player) {
                Player player = (Player) entity;
                teleportPlayer(player);
            }

            entity.inPortalTicks = 0;
        }
    }

    /**
     * Teleport a player between Overworld and Nether
     */
    private void teleportPlayer(Player player) {
        Server server = this.level.getServer();

        // Determine target dimension
        boolean inOverworld = this.level.getDimension() == Level.DIMENSION_OVERWORLD;
        String netherName = server.getPropertyString("nether-world-name", "nether");
        String targetLevelName = inOverworld ? netherName : server.getDefaultLevel().getName();
        Level targetLevel = server.getLevelByName(targetLevelName);

        if (targetLevel == null) {
            // Nether not loaded, try to load
            if (inOverworld) {
                if (!server.loadLevel(netherName)) {
                    server.generateLevel(netherName, server.getDefaultLevel().getSeed(),
                            cn.nukkit.level.generator.Nether.class);
                    targetLevel = server.getLevelByName(netherName);
                    if (targetLevel != null) {
                        targetLevel.setDimension(Level.DIMENSION_NETHER);
                    }
                } else {
                    targetLevel = server.getLevelByName(netherName);
                    if (targetLevel != null && targetLevel.getDimension() == Level.DIMENSION_OVERWORLD) {
                        targetLevel.setDimension(Level.DIMENSION_NETHER);
                    }
                }
            }
            if (targetLevel == null) {
                return;
            }
        }

        // Calculate target position: divide/multiply by 8 for Nether scale
        double targetX = player.getFloorX();
        double targetY = player.getFloorY();
        double targetZ = player.getFloorZ();

        if (inOverworld) {
            // Going to Nether: divide coordinates by 8
            targetX = player.getFloorX() / 8.0;
            targetZ = player.getFloorZ() / 8.0;
            targetY = Math.min(player.getFloorY(), 100); // Nether is shorter
        } else {
            // Going to Overworld: multiply coordinates by 8
            targetX = player.getFloorX() * 8.0;
            targetZ = player.getFloorZ() * 8.0;
            targetY = Math.min(player.getFloorY(), 100);
        }

        // Try to find or create a portal at the target location
        Position targetPos = findOrCreatePortal(targetLevel, targetX, targetY, targetZ);
        if (targetPos == null) {
            targetPos = new Position(targetX, targetY, targetZ, targetLevel);
        }

        // Ensure the target Y is safe
        targetPos = findSafeY(targetLevel, targetPos);

        Location targetLocation = new Location(
                targetPos.x + 0.5, targetPos.y, targetPos.z + 0.5,
                player.yaw, player.pitch, targetLevel
        );

        // Send dimension change packet first for the client
        if (player.getLevel().getDimension() != targetLevel.getDimension()) {
            ChangeDimensionPacket pk = new ChangeDimensionPacket();
            pk.dimension = (byte) (targetLevel.getDimension() & 0xff);
            pk.x = (float) targetLocation.x;
            pk.y = (float) targetLocation.y;
            pk.z = (float) targetLocation.z;
            player.dataPacket(pk);
        }

        // Teleport the player
        player.teleport(targetLocation);
    }

    /**
     * Try to find an existing portal near the target position, or return null
     */
    private Position findOrCreatePortal(Level targetLevel, double x, double y, double z) {
        int searchRadius = 16;

        // Search for existing portal
        for (int searchX = (int) x - searchRadius; searchX <= (int) x + searchRadius; searchX++) {
            for (int searchZ = (int) z - searchRadius; searchZ <= (int) z + searchRadius; searchZ++) {
                for (int searchY = 5; searchY < 120; searchY++) {
                    if (targetLevel.getBlockIdAt(searchX, searchY, searchZ) == NETHER_PORTAL) {
                        return new Position(searchX, searchY, searchZ, targetLevel);
                    }
                }
            }
        }

        // No existing portal found - create one
        return createPortal(targetLevel, (int) x, (int) y, (int) z);
    }

    /**
     * Create a nether portal at the specified location
     */
    private Position createPortal(Level level, int x, int y, int z) {
        // Find a suitable Y position
        int portalY = y;
        for (int testY = 100; testY > 5; testY--) {
            int bid = level.getBlockIdAt(x, testY, z);
            int bidBelow = level.getBlockIdAt(x, testY - 1, z);
            if (bid == Block.AIR && bidBelow != Block.AIR && bidBelow != Block.STILL_LAVA && bidBelow != Block.LAVA) {
                portalY = testY;
                break;
            }
        }

        // Build obsidian frame (4 wide x 5 tall)
        for (int frameY = portalY - 1; frameY <= portalY + 4; frameY++) {
            // Left and right pillars
            level.setBlock(new Vector3(x - 1, frameY, z), Block.get(Block.OBSIDIAN));
            level.setBlock(new Vector3(x + 2, frameY, z), Block.get(Block.OBSIDIAN));
        }
        // Top and bottom bars
        for (int frameX = x - 1; frameX <= x + 2; frameX++) {
            level.setBlock(new Vector3(frameX, portalY - 1, z), Block.get(Block.OBSIDIAN));
            level.setBlock(new Vector3(frameX, portalY + 4, z), Block.get(Block.OBSIDIAN));
        }

        // Fill inside with portal blocks
        for (int portalY2 = portalY; portalY2 <= portalY + 3; portalY2++) {
            level.setBlock(new Vector3(x, portalY2, z), new BlockNetherPortal());
            level.setBlock(new Vector3(x + 1, portalY2, z), new BlockNetherPortal());
        }

        // Clear area above portal for safety
        for (int clearX = x - 2; clearX <= x + 3; clearX++) {
            for (int clearY = portalY + 5; clearY < portalY + 8 && clearY < 127; clearY++) {
                if (level.getBlockIdAt(clearX, clearY, z) != Block.OBSIDIAN && level.getBlockIdAt(clearX, clearY, z) != NETHER_PORTAL) {
                    level.setBlockIdAt(clearX, clearY, z, Block.AIR);
                }
            }
        }

        return new Position(x, portalY, z, level);
    }

    /**
     * Find a safe Y coordinate at the given position
     */
    private Position findSafeY(Level level, Position pos) {
        int x = pos.getFloorX();
        int z = pos.getFloorZ();

        // Check from top down for two consecutive air blocks with solid ground
        for (int y = 126; y > 1; y--) {
            int bid = level.getBlockIdAt(x, y, z);
            int bidBelow = level.getBlockIdAt(x, y - 1, z);
            if (bid == Block.AIR && (bidBelow != Block.AIR && bidBelow != Block.STILL_LAVA && bidBelow != Block.LAVA)) {
                return new Position(x, y, z, level);
            }
        }

        // Fallback: just use the original Y
        return pos;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.AIR_BLOCK_COLOR;
    }
}

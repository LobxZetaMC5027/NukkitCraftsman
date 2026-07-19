package cn.nukkit.item;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockFire;
import cn.nukkit.block.BlockNetherPortal;
import cn.nukkit.block.BlockSolid;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;

/**
 * Flint and Steel - creates fire and nether portals
 * Updated to support portal creation in both X and Z directions
 */
public class ItemFlintSteel extends ItemTool {

    public ItemFlintSteel() {
        this(0, 1);
    }

    public ItemFlintSteel(Integer meta) {
        this(meta, 1);
    }

    public ItemFlintSteel(Integer meta, int count) {
        super(FLINT_STEEL, meta, count, "Flint and Steel");
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Level level, Player player, Block block, Block target, int face, double fx, double fy, double fz) {
        if (block.getId() == AIR && (target instanceof BlockSolid)) {
            if (target.getId() == OBSIDIAN) {
                // Try creating portal in Z direction (north-south)
                if (tryCreatePortal(level, target, true)) {
                    return true;
                }
                // Try creating portal in X direction (east-west)
                if (tryCreatePortal(level, target, false)) {
                    return true;
                }
            }
            BlockFire fire = new BlockFire();
            fire.x = block.x;
            fire.y = block.y;
            fire.z = block.z;
            fire.level = level;

            if (fire.isBlockTopFacingSurfaceSolid(fire.getSide(Vector3.SIDE_DOWN)) || fire.canNeighborBurn()) {
                level.setBlock(fire, fire, true);
                level.scheduleUpdate(fire, fire.tickRate() + level.rand.nextInt(10));
                return true;
            }
            if ((player.gamemode & 0x01) == 0 && this.useOn(block)) {
                if (this.getDamage() >= this.getMaxDurability()) {
                    player.getInventory().setItemInHand(new Item(Item.AIR, 0, 0));
                } else {
                    this.meta++;
                    player.getInventory().setItemInHand(this);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Try to create a nether portal frame
     * @param level The level
     * @param target The obsidian block that was clicked
     * @param zAxis True to try Z-axis portal, false for X-axis
     * @return True if portal was successfully created
     */
    private boolean tryCreatePortal(Level level, Block target, boolean zAxis) {
        int targetX = target.getFloorX();
        int targetY = target.getFloorY();
        int targetZ = target.getFloorZ();

        // Find the horizontal extent of obsidian
        int min1, max1;
        if (zAxis) {
            min1 = targetZ;
            max1 = targetZ;
            for (int z = targetZ + 1; level.getBlock(new Vector3(targetX, targetY, z)).getId() == OBSIDIAN; z++) {
                max1 = z;
            }
            for (int z = targetZ - 1; level.getBlock(new Vector3(targetX, targetY, z)).getId() == OBSIDIAN; z--) {
                min1 = z;
            }
        } else {
            min1 = targetX;
            max1 = targetX;
            for (int x = targetX + 1; level.getBlock(new Vector3(x, targetY, targetZ)).getId() == OBSIDIAN; x++) {
                max1 = x;
            }
            for (int x = targetX - 1; level.getBlock(new Vector3(x, targetY, targetZ)).getId() == OBSIDIAN; x--) {
                min1 = x;
            }
        }

        int width = max1 - min1 + 1;
        if (width < 4 || width > 23) return false;

        // Find the top of the frame by checking both sides
        int minTop = 127, maxTop = 0;
        if (zAxis) {
            for (int y = targetY; level.getBlock(new Vector3(targetX, y, min1)).getId() == OBSIDIAN; y++) {
                minTop = y;
            }
            for (int y = targetY; level.getBlock(new Vector3(targetX, y, max1)).getId() == OBSIDIAN; y++) {
                maxTop = y;
            }
        } else {
            for (int y = targetY; level.getBlock(new Vector3(min1, y, targetZ)).getId() == OBSIDIAN; y++) {
                minTop = y;
            }
            for (int y = targetY; level.getBlock(new Vector3(max1, y, targetZ)).getId() == OBSIDIAN; y++) {
                maxTop = y;
            }
        }

        int topY = Math.min(minTop, maxTop);
        int height = topY - targetY + 1;
        if (height < 5 || height > 23) return false;

        // Check top row of obsidian
        int topCount = 0;
        if (zAxis) {
            for (int z = min1; level.getBlock(new Vector3(targetX, topY, z)).getId() == OBSIDIAN && z <= max1; z++) {
                topCount++;
            }
        } else {
            for (int x = min1; level.getBlock(new Vector3(x, topY, targetZ)).getId() == OBSIDIAN && x <= max1; x++) {
                topCount++;
            }
        }

        if (topCount != width) return false;

        // Fill inside with portal blocks
        if (zAxis) {
            for (int z = min1 + 1; z < max1; z++) {
                for (int y = targetY + 1; y < topY; y++) {
                    level.setBlock(new Vector3(targetX, y, z), new BlockNetherPortal());
                }
            }
        } else {
            for (int x = min1 + 1; x < max1; x++) {
                for (int y = targetY + 1; y < topY; y++) {
                    level.setBlock(new Vector3(x, y, targetZ), new BlockNetherPortal());
                }
            }
        }

        return true;
    }

    @Override
    public int getMaxDurability() {
        return ItemTool.DURABILITY_FLINT_STEEL;
    }
}

package cn.nukkit.level.generator.populator;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.NukkitRandom;

/**
 * Populator that creates soul sand patches and gravel patches in the Nether
 */
public class PopulatorNetherSoulSand extends Populator {

    private int soulSandBase = 6;
    private int gravelBase = 4;

    @Override
    public void populate(ChunkManager level, int chunkX, int chunkZ, NukkitRandom random) {
        // Soul sand patches
        if (random.nextBoundedInt(10) < this.soulSandBase) {
            int cx = NukkitMath.randomRange(random, chunkX << 4, (chunkX << 4) + 15);
            int cz = NukkitMath.randomRange(random, chunkZ << 4, (chunkZ << 4) + 15);

            // Find surface
            int surfaceY = -1;
            for (int y = 100; y > 1; y--) {
                int bid = level.getBlockIdAt(cx, y, cz);
                if (bid == Block.NETHERRACK) {
                    surfaceY = y;
                    break;
                }
            }
            if (surfaceY < 0) return;

            int radius = random.nextBoundedInt(4) + 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist <= radius) {
                        int bx = cx + dx;
                        int bz = cz + dz;
                        int bid = level.getBlockIdAt(bx, surfaceY, bz);
                        if (bid == Block.NETHERRACK) {
                            level.setBlockIdAt(bx, surfaceY, bz, Block.SOUL_SAND);
                        }
                    }
                }
            }
        }

        // Gravel patches
        if (random.nextBoundedInt(10) < this.gravelBase) {
            int cx = NukkitMath.randomRange(random, chunkX << 4, (chunkX << 4) + 15);
            int cz = NukkitMath.randomRange(random, chunkZ << 4, (chunkZ << 4) + 15);

            int surfaceY = -1;
            for (int y = 100; y > 1; y--) {
                int bid = level.getBlockIdAt(cx, y, cz);
                if (bid == Block.NETHERRACK || bid == Block.SOUL_SAND) {
                    surfaceY = y;
                    break;
                }
            }
            if (surfaceY < 0) return;

            int radius = random.nextBoundedInt(3) + 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist <= radius) {
                        int bx = cx + dx;
                        int bz = cz + dz;
                        int bid = level.getBlockIdAt(bx, surfaceY, bz);
                        if (bid == Block.NETHERRACK || bid == Block.SOUL_SAND) {
                            level.setBlockIdAt(bx, surfaceY, bz, Block.GRAVEL);
                        }
                    }
                }
            }
        }
    }

    public void setSoulSandBase(int amount) {
        this.soulSandBase = amount;
    }

    public void setGravelBase(int amount) {
        this.gravelBase = amount;
    }
}

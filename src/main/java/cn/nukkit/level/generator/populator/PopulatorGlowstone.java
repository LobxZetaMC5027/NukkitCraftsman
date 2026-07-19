package cn.nukkit.level.generator.populator;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.NukkitRandom;

/**
 * Populator that generates glowstone clusters hanging from the Nether ceiling
 */
public class PopulatorGlowstone extends Populator {

    private int baseAmount = 8;

    @Override
    public void populate(ChunkManager level, int chunkX, int chunkZ, NukkitRandom random) {
        int amount = random.nextBoundedInt(10) < this.baseAmount ? 1 : 0;

        for (int i = 0; i < amount; i++) {
            int x = NukkitMath.randomRange(random, chunkX << 4, (chunkX << 4) + 15);
            int z = NukkitMath.randomRange(random, chunkZ << 4, (chunkZ << 4) + 15);

            // Find the ceiling
            int ceilingY = -1;
            for (int y = 127; y > 50; y--) {
                if (level.getBlockIdAt(x, y, z) == Block.NETHERRACK) {
                    ceilingY = y;
                    break;
                }
            }

            if (ceilingY < 0) continue;

            // Place glowstone hanging from ceiling
            int startY = ceilingY - 1;
            int length = random.nextBoundedInt(5) + 2;

            for (int dy = 0; dy < length; dy++) {
                int by = startY - dy;
                if (by < 1) break;

                // Main column
                level.setBlockIdAt(x, by, z, Block.GLOWSTONE_BLOCK);

                // Random branches
                if (dy > 0 && random.nextBoundedInt(3) == 0) {
                    int branchDx = random.nextBoundedInt(3) - 1;
                    int branchDz = random.nextBoundedInt(3) - 1;
                    int bx = x + branchDx;
                    int bz = z + branchDz;
                    if (level.getBlockIdAt(bx, by, bz) == Block.AIR) {
                        level.setBlockIdAt(bx, by, bz, Block.GLOWSTONE_BLOCK);
                    }
                }
            }
        }
    }

    public void setBaseAmount(int amount) {
        this.baseAmount = amount;
    }
}

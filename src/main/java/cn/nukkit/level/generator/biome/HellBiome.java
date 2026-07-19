package cn.nukkit.level.generator.biome;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockNetherrack;

/**
 * Nether biome for MCPE 0.15.10
 * Represents the Hell/Nether dimension biome
 */
public class HellBiome extends NormalBiome {

    public HellBiome() {
        this.setElevation(0, 30);
        this.temperature = 2.0;
        this.rainfall = 0.0;

        this.setGroundCover(new Block[]{
                new BlockNetherrack(),
                new BlockNetherrack(),
                new BlockNetherrack(),
                new BlockNetherrack(),
                new BlockNetherrack()
        });
    }

    @Override
    public String getName() {
        return "Hell";
    }

    @Override
    public int getColor() {
        return 0x700200;
    }
}

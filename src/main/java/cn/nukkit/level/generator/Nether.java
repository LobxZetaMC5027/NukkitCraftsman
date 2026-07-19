package cn.nukkit.level.generator;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.generator.biome.Biome;
import cn.nukkit.level.generator.noise.Simplex;
import cn.nukkit.level.generator.object.ore.OreType;
import cn.nukkit.level.generator.populator.Populator;
import cn.nukkit.level.generator.populator.PopulatorGlowstone;
import cn.nukkit.level.generator.populator.PopulatorNetherLava;
import cn.nukkit.level.generator.populator.PopulatorNetherSoulSand;
import cn.nukkit.level.generator.populator.PopulatorOre;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nether world generator for MCPE 0.15.10
 * Generates Nether terrain with caves, lava seas, glowstone, soul sand and gravel
 */
public class Nether extends Generator {

    public static final int TYPE_NETHER = 3;

    private ChunkManager level;
    private NukkitRandom random;
    private Simplex noiseBase;
    private Simplex noiseCave1;
    private Simplex noiseCave2;
    private Simplex noiseCave3;

    private final List<Populator> populators = new ArrayList<>();
    private final List<Populator> generationPopulators = new ArrayList<>();

    // Nether terrain constants
    private final int lavaSeaHeight = 32;
    private final int bedrockDepth = 5;
    private final int baseHeight = 64;
    private final int heightVariation = 20;

    public Nether() {
        this(new HashMap<>());
    }

    public Nether(Map<String, Object> options) {
        // Options reserved for future use
    }

    @Override
    public int getId() {
        return TYPE_NETHER;
    }

    @Override
    public ChunkManager getChunkManager() {
        return level;
    }

    @Override
    public String getName() {
        return "nether";
    }

    @Override
    public Map<String, Object> getSettings() {
        return new HashMap<>();
    }

    @Override
    public void init(ChunkManager level, NukkitRandom random) {
        this.level = level;
        this.random = random;
        this.random.setSeed(this.level.getSeed());

        this.noiseBase = new Simplex(this.random, 4F, 1F / 4F, 1F / 64F);
        this.noiseCave1 = new Simplex(this.random, 2F, 1F, 1F / 64F);
        this.noiseCave2 = new Simplex(this.random, 2F, 1F, 1F / 64F);
        this.noiseCave3 = new Simplex(this.random, 2F, 1F, 1F / 64F);

        // Nether ores (quartz is the main nether ore)
        PopulatorOre ores = new PopulatorOre();
        ores.setOreTypes(new OreType[]{
                new OreType(new cn.nukkit.block.BlockOreQuartz(), 20, 8, 10, 118),
                new OreType(new cn.nukkit.block.BlockNetherrack(), 20, 32, 5, 30),
        });
        this.populators.add(ores);

        // Nether lava lakes and waterfalls
        PopulatorNetherLava lava = new PopulatorNetherLava();
        lava.setBaseAmount(5);
        this.populators.add(lava);

        // Glowstone clusters hanging from ceiling
        PopulatorGlowstone glowstone = new PopulatorGlowstone();
        glowstone.setBaseAmount(8);
        this.populators.add(glowstone);

        // Soul sand and gravel patches
        PopulatorNetherSoulSand soulSand = new PopulatorNetherSoulSand();
        soulSand.setSoulSandBase(6);
        soulSand.setGravelBase(4);
        this.populators.add(soulSand);
    }

    @Override
    public void generateChunk(int chunkX, int chunkZ) {
        this.random.setSeed(0xdeadbeef ^ (chunkX << 8) ^ chunkZ ^ this.level.getSeed());

        double[][] baseNoise = Generator.getFastNoise2D(this.noiseBase, 16, 16, 4, chunkX * 16, 0, chunkZ * 16);

        FullChunk chunk = this.level.getChunk(chunkX, chunkZ);

        for (int genx = 0; genx < 16; genx++) {
            for (int genz = 0; genz < 16; genz++) {
                // Calculate terrain height using noise
                double baseValue = baseNoise[genx][genz];
                int surfaceHeight = baseHeight + (int) (heightVariation * baseValue);
                if (surfaceHeight < 40) surfaceHeight = 40;
                if (surfaceHeight > 100) surfaceHeight = 100;

                // Set biome to Hell
                chunk.setBiomeId(genx, genz, Biome.HELL);
                chunk.setBiomeColor(genx, genz, 0x70, 0x02, 0x00);

                // Generate terrain column
                for (int geny = 0; geny < 128; geny++) {
                    // Bedrock at bottom and top
                    if (geny < bedrockDepth && (geny == 0 || random.nextRange(1, 5) == 1)) {
                        chunk.setBlock(genx, geny, genz, Block.BEDROCK);
                        continue;
                    }
                    if (geny > 122 && (geny == 127 || random.nextRange(1, 5) == 1)) {
                        chunk.setBlock(genx, geny, genz, Block.BEDROCK);
                        continue;
                    }

                    // Check if this should be a cave using 3D noise
                    double cave1 = this.noiseCave1.noise3D(chunkX * 16 + genx, geny, chunkZ * 16 + genz, true);
                    double cave2 = this.noiseCave2.noise3D(chunkX * 16 + genx, geny, chunkZ * 16 + genz, true);
                    double cave3 = this.noiseCave3.noise3D(chunkX * 16 + genx, geny, chunkZ * 16 + genz, true);

                    // Cave threshold - creates Swiss cheese-like Nether terrain
                    boolean isCave = (cave1 * cave2 + cave3) / 2.0 > 0.15;

                    if (isCave && geny > bedrockDepth && geny < 122) {
                        // Below lava sea level, fill with lava
                        if (geny <= lavaSeaHeight) {
                            chunk.setBlock(genx, geny, genz, Block.STILL_LAVA);
                        }
                        // Otherwise it's air (cave)
                        continue;
                    }

                    // Solid terrain: fill with netherrack
                    if (geny <= surfaceHeight) {
                        chunk.setBlock(genx, geny, genz, Block.NETHERRACK);
                    }
                    // Below lava sea and below surface, fill gaps with lava
                    else if (geny <= lavaSeaHeight) {
                        chunk.setBlock(genx, geny, genz, Block.STILL_LAVA);
                    }
                    // Above surface and above lava sea, it's air
                }
            }
        }

        // Run generation populators
        for (Populator populator : this.generationPopulators) {
            populator.populate(this.level, chunkX, chunkZ, this.random);
        }
    }

    @Override
    public void populateChunk(int chunkX, int chunkZ) {
        this.random.setSeed(0xdeadbeef ^ (chunkX << 8) ^ chunkZ ^ this.level.getSeed());
        for (Populator populator : this.populators) {
            populator.populate(this.level, chunkX, chunkZ, this.random);
        }
    }

    @Override
    public Vector3 getSpawn() {
        return new Vector3(128, 64, 128);
    }
}

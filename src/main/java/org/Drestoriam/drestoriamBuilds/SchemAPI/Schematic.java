package org.Drestoriam.drestoriamBuilds.SchemAPI;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import me.lucko.helper.scheduler.Task;
import me.lucko.helper.scheduler.builder.TaskBuilder;
import org.Drestoriam.drestoriamBuilds.DrestoriamBuilds;
import org.Drestoriam.drestoriamBuilds.SchemAPI.Util.MathsUtil;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A utility class that previews and pastes schematics block-by-block with asynchronous support.
 * <br></br>
 * @version 2.0.6
 * @author SamB440 - Schematic previews, centering and pasting block-by-block, class itself
 * @author brainsynder - 1.13+ Palette Schematic Reader
 * @author Math0424 - Rotation calculations
 * @author Jojodmo - Legacy (< 1.12) Schematic Reader
 */
public class Schematic {

    private final DrestoriamBuilds plugin;

    private final File schematic;
    private final List<BaseBlock> blocks;
    private Clipboard clipboard;
    private int pace;

    /**
     * @param plugin your plugin instance
     * @param schematic file to the schematic
     */
    public Schematic(final DrestoriamBuilds plugin, final File schematic, int pace) {
        this.plugin = plugin;
        this.schematic = schematic;
        this.blocks = new ArrayList<>();
        this.pace = pace;

        // Read and cache
        try (FileInputStream inputStream = new FileInputStream(schematic)) {
            ClipboardFormat format = ClipboardFormats.findByFile(schematic);
            ClipboardReader reader = format.getReader(inputStream);

            if(reader == null){

                System.out.println("[DB] Reader null");
                return;

            }

            this.clipboard = reader.read();

            // Get all blocks in the schematic
            final BlockVector3 minimumPoint = clipboard.getMinimumPoint();
            final BlockVector3 maximumPoint = clipboard.getMaximumPoint();
            final int minX = minimumPoint.x();
            final int maxX = maximumPoint.x();
            final int minY = minimumPoint.y();
            final int maxY = maximumPoint.y();
            final int minZ = minimumPoint.z();
            final int maxZ = maximumPoint.z();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        final BlockVector3 at = BlockVector3.at(x, y, z);
                        BaseBlock block = clipboard.getFullBlock(at);
                        blocks.add(block);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Pastes a schematic, with a specified time
     * @param paster player pasting
     * @param time time in ticks to paste blocks
     * @return collection of locations where schematic blocks will be pasted, null if schematic locations will replace blocks
     */
    @Nullable
    public Collection<Location> pasteSchematic(final Location loc, final Player paster, final int time, final Options... option) {
        final Map<Location, BaseBlock> pasteBlocks = new LinkedHashMap<>();
        final List<Options> options = Arrays.asList(option);
        try {
            final Data tracker = new Data();

            // Rotate based off the player's facing direction
            double yaw = paster.getEyeLocation().getYaw();
            // So unfortunately, WorldEdit doesn't support anything other than multiples of 90.
            // Here we round it to the nearest multiple of 90.
            yaw = MathsUtil.roundHalfUp((int) yaw, 90);
            System.out.println("Yaw: " + yaw);
            System.out.println("Origin: " + clipboard.getOrigin().toString());
            // Apply the rotation to the clipboard
            this.clipboard = clipboard.transform(new AffineTransform().rotateY(yaw * -1));


            // Max/Min referencing the schematics position in relation to the world origin
            final BlockVector3 minimumPoint = clipboard.getMinimumPoint();
            final BlockVector3 maximumPoint = clipboard.getMaximumPoint();
            final int minX = minimumPoint.x();
            final int maxX = maximumPoint.x();
            final int minY = minimumPoint.y();
            final int maxY = maximumPoint.y();
            final int minZ = minimumPoint.z();
            final int maxZ = maximumPoint.z();
            System.out.println("Max: " + maxX + " " + maxY + " " + maxZ);
            System.out.println("Min: " + minX + " " + minY + " " + minZ);

            final int width = clipboard.getRegion().getWidth();
            final int height = clipboard.getRegion().getHeight();
            final int length = clipboard.getRegion().getLength();
            final int widthCentre = width / 2;
            final int heightCentre = height / 2;
            final int lengthCentre = length / 2;

            int minBlockY = loc.getWorld().getMaxHeight();

            // Loops through to each block in the schematic, starting from front-most bottom left block
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        // Vector of block location, then getting the block itself
                        final BlockVector3 at = BlockVector3.at(x, y, z);
                        BaseBlock block = clipboard.getFullBlock(at);

                        // Ignore air blocks, change if you want
                        if (block.getBlockType().getMaterial().isAir()) continue;

                        // Here we find the relative offset based off the current location.
                        // Relative offset gotten by taking the absolute value of the difference between the furthest block (max) and current block locations
                        // i.e. maxX = 150, x = 12, |150-12| = 138, so the offset of the x location is 138 blocks
                        final int offsetX = Math.abs(maxX - x); //-436 - -436 = 0
                        final int offsetY = Math.abs(maxY - y); //66 - 63 = 3
                        final int offsetZ = Math.abs(maxZ - z); //-5 - -10 = 5

                        // Create the offset location by taking the provided location and subtracting the difference between the offset and the corresponding dimension
                        // i.e. locX - (offsetX - width) = offsetLocX, so if locX = 50, offsetX = 138, width = 75, then 50 - (138 - 75) = 50 - 63 = -13
                        // which would mean the location would be offset 13 blocks to the left of the origin
                                                                        // 0-6=-6              3-3=0               5-7=-2

                        Location offsetLoc = loc.clone();

                        switch((int) yaw){

                            case 0:

                                offsetLoc = loc.clone().subtract(offsetX - width, offsetY - height, offsetZ + 1);

                                break;

                            case 90:

                                offsetLoc = loc.clone().subtract(offsetX - width, offsetY - height, offsetZ - length);

                                break;

                            case -90:

                                offsetLoc = loc.clone().subtract(offsetX + 1, offsetY - height, offsetZ + 1);

                                break;

                            case -180:

                                offsetLoc = loc.clone().subtract(offsetX + 1, offsetY - height, offsetZ - length);

                                break;
                        }

                        // If the y value of the offset location is less than the current minimum y value, set the minimum value to the offset location
                        if (offsetLoc.getBlockY() < minBlockY) minBlockY = offsetLoc.getBlockY();

                        // Finally, put the block in the offset location
                        pasteBlocks.put(offsetLoc, block);
                    }
                }
            }

            /*
             * Verify location of pasting
             */
            boolean validated = true;
            for (Location validate : pasteBlocks.keySet()) {
                final BaseBlock baseBlock = pasteBlocks.get(validate);
                final boolean isWater = validate.clone().subtract(0, 1, 0).getBlock().getType() == Material.WATER;
                final boolean isAir = minBlockY == validate.getBlockY() && validate.clone().subtract(0, 1, 0).getBlock().getType().isAir();
                final boolean isSolid = validate.getBlock().getType().isSolid();
                final boolean isTransparent = options.contains(Options.IGNORE_TRANSPARENT) && validate.getBlock().isPassable() && !validate.getBlock().getType().isAir();

                if (!options.contains(Options.PLACE_ANYWHERE) && (isWater || isAir || isSolid) && !isTransparent) {
                    // Show fake block where block is interfering with schematic
                    paster.sendBlockChange(validate, Material.RED_STAINED_GLASS.createBlockData());
                    validated = false;
                } else {
                    // Show fake block for air
                    if (options.contains(Options.USE_FAKE_BLOCKS)) {
                        paster.sendBlockChange(validate, BukkitAdapter.adapt(baseBlock));
                    } else paster.sendBlockChange(validate, Material.GREEN_STAINED_GLASS.createBlockData());
                }

                if (!options.contains(Options.PREVIEW)) {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        if (validate.getBlock().getType() == Material.AIR) paster.sendBlockChange(validate.getBlock().getLocation(), Material.AIR.createBlockData());
                    }, 60);
                }
            }

            if (options.contains(Options.PREVIEW)) return new ArrayList<>();
            if (!validated) return null;

            if (options.contains(Options.REALISTIC)) {
                Map<Location, BaseBlock> sorted
                        = pasteBlocks.entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(i -> i.getKey().getBlockY()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1, LinkedHashMap::new));
                pasteBlocks.clear();
                pasteBlocks.putAll(sorted);
            }

            // Start pasting each block every tick
            final AtomicReference<Task> task = new AtomicReference<>();

            tracker.trackCurrentBlock = 0;

            Runnable pasteTask = () -> {
                // Get the block, set the type, data, and then update the state.
                Location key = (Location) pasteBlocks.keySet().toArray()[tracker.trackCurrentBlock];
                final BlockData data = BukkitAdapter.adapt(pasteBlocks.get(key));
                final Block block = key.getBlock();
                block.setType(data.getMaterial(), false);
                block.setBlockData(data);

                block.getState().update(true, false);

                // Play block effects. Change to what you want.
                block.getLocation().getWorld().spawnParticle(Particle.CLOUD, block.getLocation(), 6);
                block.getLocation().getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());

                tracker.trackCurrentBlock++;

                if (tracker.trackCurrentBlock >= pasteBlocks.size()) {
                    task.get().stop();
                    tracker.trackCurrentBlock = 0;
                }
            };

            task.set(TaskBuilder.newBuilder().sync().every(time).run(pasteTask));
            return pasteBlocks.keySet();
        } catch (final Exception e) {
            e.printStackTrace();
        } return null;
    }

    /**
     * Pastes a schematic, with the time defaulting to 1 block per second
     * @param location location to paste from
     * @param paster player pasting
     * @param options options to apply to this paste
     * @return list of locations where schematic blocks will be pasted, null if schematic locations will replace blocks
     */
    public Collection<Location> pasteSchematic(final Location location, final Player paster, final Options... options) {
        return pasteSchematic(location, paster, 20, options);
    }

    /**
     * Creates a constant preview of this schematic for the player
     * @param player player
     */
    /*public void previewSchematic(Player player) {
        plugin.getPlayerManagement().setBuilding(player.getUniqueId(), this);
        new BuildTask(plugin, player).start();
    }*/

    /**
     * Returns a list containing every block in the schematic.
     * @return list of every block in the schematic
     */
    public List<BaseBlock> getBlocks() {
        return blocks;
    }

    /**
     * Returns a material-count map of the materials present in this schematic.
     *
     * The key corresponds to the Bukkit {@link Material} and the value is the
     * amount present in the schematic.
     *
     * @return material-count map of materials in the schematic
     */
    public Map<Material, Integer> getSchematicMaterialData() {
        Map<Material, Integer> materialValuesMap = new HashMap<>();
        for (BaseBlock baseBlock : blocks) {
            final Material material = BukkitAdapter.adapt(baseBlock.getBlockType());
            int count = materialValuesMap.getOrDefault(material, 0);
            materialValuesMap.put(material, count + 1);
        }
        return materialValuesMap;
    }

    /**
     * Hacky method to avoid "final".
     */
    protected static class Data {
        int trackCurrentBlock;
    }

    /**
     * An enum of options to apply whilst previewing/pasting a schematic.
     */
    public enum Options {
        /**
         * Previews schematic
         */
        PREVIEW,
        /**
         * A realistic building method. Builds from the ground up, instead of in the default slices.
         */
        REALISTIC,
        /**
         * Bypasses the verification check and allows placing anywhere.
         */
        PLACE_ANYWHERE,
        /**
         * Ignores transparent blocks in the placement check
         */
        IGNORE_TRANSPARENT,
        /**
         * Instead of game markers or glass blocks,
         * uses the actual block types of the schematic to show valid build areas.
         * <hr></hr>
         * Note that this will still use red glass for invalid build areas.
         */
        USE_FAKE_BLOCKS
    }

    public int getPacing() {
        return pace;
    }

    public void setPacing(int pace) {
        this.pace = pace;
    }
}

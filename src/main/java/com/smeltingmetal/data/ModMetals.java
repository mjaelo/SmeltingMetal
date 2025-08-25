package com.smeltingmetal.data;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.MetalsConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Manages the properties and configurations of all metals in the Smelting Metal mod.
 * Handles loading metal configurations, applying overrides, and providing access to metal properties.
 * This includes default metal definitions and any custom metals defined in the configuration.
 */
public class ModMetals {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, MetalProperties> METAL_PROPERTIES_MAP = new HashMap<>();
    private static final Map<String, MetalProperties> DEFAULT_METALS = createDefaultMetals();
    private static boolean initialized = false;

    private static Map<String, MetalProperties> createDefaultMetals() {
        Map<String, MetalProperties> defaults = new HashMap<>();

        // Add default metals - we know these exist in vanilla
        defaults.put("minecraft:iron", new MetalProperties(
                "minecraft:iron",
                new ResourceLocation("minecraft:iron_ingot"),
                new ResourceLocation("minecraft:raw_iron"),
                new ResourceLocation("minecraft:raw_iron_block"),
                new ResourceLocation("minecraft:iron_nugget"),
                null, null, null
        ));

        defaults.put("minecraft:gold", new MetalProperties(
                "minecraft:gold",
                new ResourceLocation("minecraft:gold_ingot"),
                new ResourceLocation("minecraft:raw_gold"),
                new ResourceLocation("minecraft:raw_gold_block"),
                new ResourceLocation("minecraft:gold_nugget"),
                null, null, null
        ));

        defaults.put("minecraft:copper", new MetalProperties(
                "minecraft:copper",
                new ResourceLocation("minecraft:copper_ingot"),
                new ResourceLocation("minecraft:raw_copper"),
                new ResourceLocation("minecraft:raw_copper_block"),
                null, null, null, null
        ));

        return defaults;
    }

    public static void init() {
        if (initialized) {
            return;
        }

        METAL_PROPERTIES_MAP.clear();

        try {
            // First check if we can safely access the config
            if (MetalsConfig.CONFIG == null) {
                LOGGER.warn("Config not loaded yet, using default metals");
                METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                initialized = true;
                return;
            }

            // Safely get config values
            if (MetalsConfig.CONFIG.metalDefinitions != null) {
                try {
                    List<? extends String> metalDefs = MetalsConfig.CONFIG.metalDefinitions.get();

                    if (metalDefs.isEmpty()) {
                        LOGGER.warn("No metal definitions found in config, using default metals");
                        METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                    } else {
                        int registeredMetals = 0;
                        // Parse metal definitions from config
                        for (String metalId : metalDefs) {
                            try {
                                metalId = metalId.trim();
                                // Expecting format: modid:metal_name (e.g., minecraft:iron)
                                String[] parts = metalId.split(":");
                                if (parts.length != 2) {
                                    LOGGER.error("Invalid metal ID format: {}. Expected format: modid:metal_name", metalId);
                                    continue;
                                }

                                String namespace = parts[0];
                                String metalName = parts[1];

                                // Generate the item IDs
                                ResourceLocation ingotId = new ResourceLocation(namespace, metalName + "_ingot");
                                ResourceLocation rawId = new ResourceLocation(namespace, "raw_" + metalName);
                                ResourceLocation rawBlockId = new ResourceLocation(namespace, "raw_" + metalName + "_block");

                                // If required items exist, check for optional items
                                if (ForgeRegistries.ITEMS.containsKey(ingotId) &&
                                        ForgeRegistries.ITEMS.containsKey(rawId) &&
                                        ForgeRegistries.ITEMS.containsKey(rawBlockId)) {
                                    // Find items in any namespace
                                    ResourceLocation nuggetId = findItemInAnyNamespace(metalName + "_nugget");
                                    ResourceLocation crushedId = findItemInAnyNamespace("crushed_raw_" + metalName);
                                    ResourceLocation bucketId = findItemInAnyNamespace("molten_" + metalName + "_bucket");
                                    // Check for molten fluid in any namespace
                                    ResourceLocation moltenId = findFluidInAnyNamespace("molten_" + metalName);

                                    // Create the metal properties with the new IDs
                                    MetalProperties properties = new MetalProperties(
                                            metalId,
                                            ingotId,
                                            rawId,
                                            rawBlockId,
                                            nuggetId,
                                            crushedId,
                                            bucketId,
                                            moltenId
                                    );

                                    METAL_PROPERTIES_MAP.put(metalId, properties);
                                    registeredMetals++;

                                    LOGGER.debug("Registered metal: {} with items - ingot: {}, raw: {}, raw block: {}, nugget: {}, crushed: {}, bucket: {}, molten: {}",
                                            metalId, ingotId, rawId, rawBlockId,
                                            nuggetId != null ? nuggetId : "not found",
                                            crushedId != null ? crushedId : "not found",
                                            bucketId != null ? bucketId : "not found",
                                            moltenId != null ? moltenId : "not found");
                                } else {
                                    LOGGER.warn("Skipping default metal {}: Could not find required items (ingot: {}, raw: {}, raw block: {})",
                                            metalId, ingotId, rawId, rawBlockId);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Failed to process metal definition: " + metalId, e);
                            }
                        }

                        if (registeredMetals == 0) {
                            LOGGER.warn("No valid metal definitions found, using default metals");
                            METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                        } else {
                            LOGGER.info("Successfully registered {} metals from config", registeredMetals);
                        }
                    }
                } catch (IllegalStateException e) {
                    LOGGER.warn("Config not ready yet, using default metals");
                    METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                }
            } else {
                LOGGER.warn("Metal definitions config not available, using default metals");
                METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load metals from config, using defaults", e);
            METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
        }

        LOGGER.info("Total registered metals: {}", METAL_PROPERTIES_MAP.size());
        initialized = true;
    }

    public static Optional<MetalProperties> getMetalProperties(String id) {
        return Optional.ofNullable(METAL_PROPERTIES_MAP.get(id));
    }

    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(METAL_PROPERTIES_MAP);
    }

    public static boolean doesMetalExist(String id) {
        return METAL_PROPERTIES_MAP.containsKey(id);
    }

    /**
     * Finds a resource location for an item with the given path in any namespace.
     *
     * @param path The path of the item to find (e.g., "molten_iron_bucket")
     * @return The first matching ResourceLocation found, or null if not found
     */
    @Nullable
    private static ResourceLocation findItemInAnyNamespace(String path) {
        return ForgeRegistries.ITEMS.getKeys().stream()
                .filter(rl -> rl.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a resource location for a fluid with the given path in any namespace.
     *
     * @param path The path of the fluid to find (e.g., "molten_iron")
     * @return The first matching ResourceLocation found, or null if not found
     */
    @Nullable
    private static ResourceLocation findFluidInAnyNamespace(String path) {
        return ForgeRegistries.FLUIDS.getKeys().stream()
                .filter(rl -> rl.getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    public static String getMetalId(ResourceLocation ingotId) {
        return METAL_PROPERTIES_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().ingotId().equals(ingotId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
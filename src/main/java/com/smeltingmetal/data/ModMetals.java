package com.smeltingmetal.data;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.MetalsConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

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
        // Add default metals with required ingot, raw, and raw block items
        addMetalWithVariants(defaults, "minecraft:iron", "minecraft:iron_ingot", "minecraft:raw_iron", "minecraft:raw_iron_block", "minecraft:iron_nugget");
        addMetalWithVariants(defaults, "minecraft:gold", "minecraft:gold_ingot", "minecraft:raw_gold", "minecraft:raw_gold_block", "minecraft:gold_nugget");
        addMetalWithVariants(defaults, "minecraft:copper", "minecraft:copper_ingot", "minecraft:raw_copper", "minecraft:raw_copper_block", "minecraft:copper_nugget");
        return defaults;
    }

    private static void addMetalWithVariants(Map<String, MetalProperties> map, String id, String ingot, String raw, String rawBlock, String nugget) {
        ResourceLocation ingotLoc = new ResourceLocation(ingot);
        ResourceLocation rawLoc = new ResourceLocation(raw);
        ResourceLocation rawBlockLoc = new ResourceLocation(rawBlock);
        ResourceLocation nuggetLoc = new ResourceLocation(nugget);

        // Check if all required items exist
        boolean hasIngot = ForgeRegistries.ITEMS.containsKey(ingotLoc);
        boolean hasRaw = ForgeRegistries.ITEMS.containsKey(rawLoc);
        boolean hasRawBlock = ForgeRegistries.ITEMS.containsKey(rawBlockLoc);
        boolean hasNugget = ForgeRegistries.ITEMS.containsKey(nuggetLoc);

        if (hasIngot && hasRaw && hasRawBlock) {
            // Create properties with all required IDs
            map.put(id, new MetalProperties(id, ingotLoc, rawLoc, rawBlockLoc, hasNugget ? nuggetLoc : null, null));
        } else {
            LOGGER.warn("Skipping default metal {}: Could not find required items (ingot: {}: {}, raw: {}: {}, raw block: {}: {})",
                    id, ingot, hasIngot, raw, hasRaw, rawBlock, hasRawBlock);
        }
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

                                if (ForgeRegistries.ITEMS.containsKey(ingotId) &&
                                        ForgeRegistries.ITEMS.containsKey(rawId) &&
                                        ForgeRegistries.ITEMS.containsKey(rawBlockId)) {

                                    // Check for optional items
                                    ResourceLocation crushedId = null;

                                    // Check for crushed item in the create mod's namespace
                                    ResourceLocation possibleCrushedId = new ResourceLocation("create", "crushed_raw_" + metalName);
                                    if (ForgeRegistries.ITEMS.containsKey(possibleCrushedId)) {
                                        crushedId = possibleCrushedId;
                                    }

                                    // Check for nugget in the mod's namespace
                                    ResourceLocation nuggetId = new ResourceLocation(namespace, metalName + "_nugget");
                                    if (!ForgeRegistries.ITEMS.containsKey(nuggetId)) {
                                        nuggetId = null;
                                    }

                                    // Register the metal with all required and optional items
                                    MetalProperties props = new MetalProperties(metalId, ingotId, rawId, rawBlockId,
                                            nuggetId, crushedId);
                                    registerMetal(props);
                                    registeredMetals++;

                                    LOGGER.debug("Registered metal: {} with items - ingot: {}, raw: {}, raw block: {}, nugget: {}, crushed: {}",
                                            metalId, ingotId, rawId, rawBlockId,
                                            nuggetId != null ? nuggetId : "not found",
                                            crushedId != null ? crushedId : "not found");
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

    private static void registerMetal(MetalProperties metalProperties) {
        if (METAL_PROPERTIES_MAP.containsKey(metalProperties.id())) {
            LOGGER.warn("Duplicate metal ID registered: {}", metalProperties.id());
        }
        METAL_PROPERTIES_MAP.put(metalProperties.id(), metalProperties);
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

    public static String getMetalId(ResourceLocation ingotId) {
        return METAL_PROPERTIES_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().ingotId().equals(ingotId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
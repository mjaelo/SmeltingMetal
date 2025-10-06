package com.smeltingmetal.init;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.config.ModConfig;
import com.smeltingmetal.data.GemProperties;
import com.smeltingmetal.data.MetalProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages the properties and configurations of all metals in the Smelting Metal mod.
 * Handles loading metal configurations, applying overrides, and providing access to metal properties.
 * This includes default metal definitions and any custom metals defined in the configuration.
 */
public class ModData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;

    private static final Map<String, MetalProperties> METAL_PROPERTIES_MAP = new HashMap<>();
    private static final Map<String, GemProperties> GEM_PROPERTIES_MAP = new HashMap<>();
    private static Map<String, List<String>> ITEM_SHAPE_MAP;
    private static Map<String, List<String>> BLOCK_SHAPE_MAP;
    public static final List<String> DEFAULT_ITEM_SHAPES = List.of("ingot", "axe", "pickaxe", "shovel", "sword", "hoe");
    public static final List<String> DEFAULT_BLOCK_SHAPES = List.of("block", "helmet", "armor", "pants", "boots");
    public static final String CONTENT_KEY = "content";
    public static final String DEFAULT_METAL = "metal";
    public static final String DEFAULT_GEM = "gem";
    public static final String DEFAULT_CONTENT = "metal";
    public static final String SHAPE_KEY = "shape";
    public static final String DEFAULT_ITEM_SHAPE = "ingot";
    public static final String DEFAULT_BLOCK_SHAPE = "block";
    public static final int DEFAULT_COLOR = 0xFFFFFF;


    public static void init() {
        if (initialized) {
            return;
        }

        METAL_PROPERTIES_MAP.clear();

        try {
            if (ModConfig.CONFIG == null) {
                LOGGER.warn("Failed to get MetalsConfig instance.");
                return;
            }

            if (ModConfig.CONFIG.metalDefinitions == null) {
                LOGGER.warn("Config not loaded yet, skipping metal initialization.");
                return;
            }

            List<? extends String> metalDefs = ModConfig.CONFIG.metalDefinitions.get();
            List<? extends String> gemDefs = ModConfig.CONFIG.gemDefinitions.get();

            ITEM_SHAPE_MAP = processResultDefinitions(ModConfig.CONFIG.itemResultDefinitions.get());
            BLOCK_SHAPE_MAP = processResultDefinitions(ModConfig.CONFIG.blockResultDefinitions.get());

            if (metalDefs.isEmpty()) {
                LOGGER.warn("No metal definitions found in config.");
            } else {
                for (String metalName : metalDefs) {
                    parseMetalProperties(metalName.trim());
                }
            }

            if (gemDefs.isEmpty()) {
                LOGGER.warn("No gem definitions found in config.");
            } else {
                for (String gemName : gemDefs) {
                    parseGemProperties(gemName.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ModMetals", e);
        }

        initialized = true;
    }

    private static Map<String, List<String>> processResultDefinitions(List<? extends String> definitions) {
        Map<String, List<String>> shapeMap = new HashMap<>();
        for (String def : definitions) {
            String[] parts = def.split("=", 2);
            if (parts.length == 1) {
                // No synonyms, use the same value as key and only value
                shapeMap.put(parts[0].trim(), List.of(parts[0].trim()));
            } else {
                // Has synonyms, split the values and add to map
                String key = parts[0].trim();
                String[] values = parts[1].split(",");
                shapeMap.put(key, Arrays.stream(values).map(String::trim).toList());
            }
        }
        return shapeMap;
    }

    private static void parseMetalProperties(String metalDef) {
        // Parse the metal definition string
        String[] parts = metalDef.split(",");
        String metalName = parts[0].trim();
        if (metalName.contains(":")) {
            metalName = metalName.substring(metalName.indexOf(':') + 1);
        }

        // Default values
        String ingotPath = metalName + "_ingot";
        String blockPath = metalName + "_block";
        String rawPath = "raw_" + metalName;
        String rawBlockPath = "raw_" + metalName + "_block";
        String nuggetPath = metalName + "_nugget";
        String crushedPath = "crushed_raw_" + metalName;
        String bucketPath = "molten_" + metalName + "_bucket";
        String moltenFluidPath = "molten_" + metalName;
        int color = DEFAULT_COLOR;

        // Create maps to store item and block results
        Map<String, ResourceLocation> itemResults = new HashMap<>();
        Map<String, ResourceLocation> blockResults = new HashMap<>();

        populateResults(metalName, ITEM_SHAPE_MAP, itemResults);
        populateResults(metalName, BLOCK_SHAPE_MAP, blockResults);

        // Parse custom paths if provided
        for (int i = 1; i < parts.length; i++) {
            String[] keyValue = parts[i].split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "ingot" -> ingotPath = value;
                    case "block" -> blockPath = value;
                    case "raw" -> rawPath = value;
                    case "raw_block" -> rawBlockPath = value;
                    case "nugget" -> nuggetPath = value;
                    case "crushed" -> crushedPath = value;
                    case "bucket" -> bucketPath = value;
                    case "molten_fluid" -> moltenFluidPath = value;
                    case "color" -> color = Integer.parseInt(value, 16);
                    default -> LOGGER.warn("Unknown property '{}' for metal '{}'", key, metalName);
                }
            }
        }

        // Find the actual resources
        ResourceLocation ingot = findInRegistry(ForgeRegistries.ITEMS, ingotPath);
        ResourceLocation block = findInRegistry(ForgeRegistries.BLOCKS, blockPath);

        if (ingot == null && block == null) {
            LOGGER.error("Missing required items for metal '{}'. Failed to create MetalProperties. (ingot: {}, block: {})",
                    metalName, ingotPath, blockPath);
            return;
        }

        // Find optional items or use default fallbacks
        ResourceLocation raw = ingot == null ? null : findInRegistry(ForgeRegistries.ITEMS, rawPath);
        ResourceLocation rawBlock = block == null ? null : findInRegistry(ForgeRegistries.BLOCKS, rawBlockPath);
        ResourceLocation nugget = ingot == null ? null : findInRegistry(ForgeRegistries.ITEMS, nuggetPath);
        ResourceLocation crushed = ingot == null ? null : findInRegistryOrUseDefault(ForgeRegistries.ITEMS, crushedPath, raw); // Fallback for Create compat
        ResourceLocation bucket = block == null ? null : findInRegistryOrUseDefault(ForgeRegistries.ITEMS, bucketPath, ModItems.MOLTEN_METAL_BUCKET.getId());
        ResourceLocation moltenFluid = block == null ? null : findInRegistry(ForgeRegistries.FLUIDS, moltenFluidPath);

        // Create MetalProperties with both item and block results
        MetalProperties properties = new MetalProperties(metalName, ingot, block, raw, rawBlock, nugget,
                crushed, bucket, moltenFluid, itemResults, blockResults, color);
        METAL_PROPERTIES_MAP.put(metalName, properties);
        LOGGER.info("Created MetalProperties for metal: {}", metalName);
    }

    private static void parseGemProperties(String gemDef) {
        String[] parts = gemDef.split(",");
        String gemName = parts[0].trim();
        if (gemName.contains(":")) {
            gemName = gemName.substring(gemName.indexOf(':') + 1);
        }
        int color = DEFAULT_COLOR;

        // Default values
        String gemPath = gemName;
        String blockPath = gemName + "_block";
        String shardPath = gemName + "_shard";

        // Create maps to store item and block results
        Map<String, ResourceLocation> itemResults = new HashMap<>();
        Map<String, ResourceLocation> blockResults = new HashMap<>();

        populateResults(gemName, ITEM_SHAPE_MAP, itemResults);
        populateResults(gemName, BLOCK_SHAPE_MAP, blockResults);

        // Parse custom paths if provided
        for (int i = 1; i < parts.length; i++) {
            String[] keyValue = parts[i].split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                switch (key) {
                    case "gem" -> gemPath = value;
                    case "block" -> blockPath = value;
                    case "shard" -> shardPath = value;
                    case "color" -> color = Integer.parseInt(value, 16);
                    default -> LOGGER.warn("Unknown property '{}' for gem '{}'", key, gemName);
                }
            }
        }

        ResourceLocation gem = findInRegistry(ForgeRegistries.ITEMS, gemPath);
        ResourceLocation block = findInRegistry(ForgeRegistries.BLOCKS, blockPath);
        ResourceLocation shard = findInRegistry(ForgeRegistries.ITEMS, shardPath);

        if (gem == null && block == null) {
            LOGGER.error("Missing required items for gem '{}'. Failed to create GemProperties. (gem: {}, block: {})",
                    gemName, gemPath, blockPath);
            return;
        }

        GemProperties properties = new GemProperties(gemName, gem, block, shard, itemResults, blockResults, color);
        GEM_PROPERTIES_MAP.put(gemName, properties);
        LOGGER.info("Created GemProperties for gem: {}", gemName);
    }

    private static <T> ResourceLocation findInRegistryContaining(IForgeRegistry<T> registry, List<String> keywords) {
        return registry.getValues().stream()
                .map(registry::getKey)
                .filter(Objects::nonNull)
                .filter(key -> keywords.stream().allMatch(word -> key.getPath().contains(word)))
                .findFirst()
                .orElse(null);
    }

    private static <T> ResourceLocation findInRegistry(IForgeRegistry<T> registry, String suffix) {
        return registry.getValues().stream()
                .map(registry::getKey)
                .filter(Objects::nonNull)
                .filter(key -> key.getPath().equals(suffix))
                .findFirst()
                .orElse(null);
    }

    private static <T> ResourceLocation findInRegistryOrUseDefault(IForgeRegistry<T> registry, String suffix, ResourceLocation defaultItem) {
        ResourceLocation foundItem = findInRegistry(registry, suffix);
        if (foundItem != null) {
            return foundItem;
        }

        return defaultItem;
    }

    private static void populateResults(String metalName, Map<String, List<String>> shapeMap, Map<String, ResourceLocation> results) {
        shapeMap.entrySet().stream()
                .filter(shapeSet -> !results.containsKey(shapeSet.getKey()))
                .forEach(shapeSet -> shapeSet.getValue().stream()
                        .map(shapeValue -> {
                            ResourceLocation item = findInRegistry(ForgeRegistries.ITEMS, metalName + "_" + shapeValue);
                            return item != null ? item : findInRegistryContaining(ForgeRegistries.ITEMS, List.of(metalName, "_" + shapeValue));
                        })
                        .filter(Objects::nonNull)
                        .findFirst()
                        .ifPresent(loc -> results.put(shapeSet.getKey(), loc)));
    }

    public static Map<String, List<String>> getItemShapeMap() {
        return ITEM_SHAPE_MAP;
    }

    public static Map<String, List<String>> getBlockShapeMap() {
        return BLOCK_SHAPE_MAP;
    }

    public static Map<String, MetalProperties> getMetalPropertiesMap() {
        return METAL_PROPERTIES_MAP;
    }

    public static Map<String, GemProperties> getGemPropertiesMap() {
        return GEM_PROPERTIES_MAP;
    }
}
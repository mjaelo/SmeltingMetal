package com.smeltingmetal.init;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.items.generic.MetalBlockItem;
import com.smeltingmetal.items.generic.MetalItem;
import com.smeltingmetal.items.mold.ItemMold;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    private static boolean initialized = false;

    public static final String METAL_TYPE_KEY = "MetalType";
    public static final String DEFAULT_METAL_TYPE = "Metal";
    public static final String FILLED = "smeltingmetal:filled";

    public static void init() {
        if (initialized) {
            return;
        }

        METAL_PROPERTIES_MAP.clear();

        try {
            if (MetalsConfig.CONFIG == null || MetalsConfig.CONFIG.metalDefinitions == null) {
                LOGGER.warn("Config not loaded yet, skipping metal initialization.");
                return;
            }

            List<? extends String> metalDefs = MetalsConfig.CONFIG.metalDefinitions.get();

            if (metalDefs.isEmpty()) {
                LOGGER.warn("No metal definitions found in config.");
            } else {
                for (String metalName : metalDefs) {
                    parseMetalProperties(metalName.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ModMetals", e);
        }

        initialized = true;
    }

    private static void parseMetalProperties(String metalDef) {
        // Parse the metal definition string
        String[] parts = metalDef.split(",");
        String metalName = parts[0].trim();

        // Default values
        String ingotPath = metalName + "_ingot";
        String blockPath = metalName + "_block";
        String rawPath = "raw_" + metalName;
        String rawBlockPath = "raw_" + metalName + "_block";
        String nuggetPath = metalName + "_nugget";
        String crushedPath = "crushed_raw_" + metalName;
        String bucketPath = "molten_" + metalName + "_bucket";
        String moltenFluidPath = "molten_" + metalName;

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
                    default -> LOGGER.warn("Unknown property '{}' for metal '{}'", key, metalName);
                }
            }
        }

        // Find the actual resources
        ResourceLocation ingot = findItem(ingotPath);
        ResourceLocation block = findBlock(blockPath);

        if (ingot == null && block == null) {
            LOGGER.error("Missing required items for metal '{}'. Failed to create MetalProperties. (ingot: {}, block: {})",
                    metalName, ingotPath, blockPath);
            return;
        }

        // Find optional items or use default fallbacks
        ResourceLocation raw = ingot == null ? null : findOrUseDefaultItem(rawPath, ModItems.RAW_METAL_ITEM.getId());
        ResourceLocation rawBlock = block == null ? null : findOrUseDefaultBlock(rawBlockPath, ModBlocks.RAW_METAL_BLOCK.getId());
        ResourceLocation nugget = ingot == null ? null : findOrUseDefaultItem(nuggetPath, ModItems.METAL_NUGGET.getId());
        ResourceLocation crushed = ingot == null ? null : findOrUseDefaultItem(crushedPath, ModItems.RAW_METAL_ITEM.getId()); // Fallback for Create compat
        ResourceLocation bucket = block == null ? null : findOrUseDefaultItem(bucketPath, ModItems.MOLTEN_METAL_BUCKET.getId());
        ResourceLocation moltenFluid = block == null ? null : findFluid(moltenFluidPath);

        MetalProperties properties = new MetalProperties(metalName, ingot, block, raw, rawBlock, nugget, crushed, bucket, moltenFluid);
        METAL_PROPERTIES_MAP.put(metalName, properties);
        LOGGER.info("Created MetalProperties for metal: {}", metalName);
    }

    private static ResourceLocation findItem(String suffix) {
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(ForgeRegistries.ITEMS::getKey)
                .filter(Objects::nonNull)
                .filter(key -> key.getPath().equals(suffix))
                .findFirst()
                .orElse(null);
    }

    private static ResourceLocation findBlock(String suffix) {
        return ForgeRegistries.BLOCKS.getValues().stream()
                .map(ForgeRegistries.BLOCKS::getKey)
                .filter(Objects::nonNull)
                .filter(key -> key.getPath().equals(suffix))
                .findFirst()
                .orElse(null);
    }

    private static ResourceLocation findFluid(String suffix) {
        return ForgeRegistries.FLUIDS.getValues().stream()
                .map(ForgeRegistries.FLUIDS::getKey)
                .filter(Objects::nonNull)
                .filter(key -> key.getPath().equals(suffix))
                .findFirst()
                .orElse(null);
    }

    private static ResourceLocation findOrUseDefaultItem(String suffix, ResourceLocation defaultItem) {
        ResourceLocation foundItem = findItem(suffix);
        if (foundItem != null) {
            return foundItem;
        }

        return defaultItem;
    }

    private static ResourceLocation findOrUseDefaultBlock(String suffix, ResourceLocation defaultBlock) {
        ResourceLocation foundBlock = findBlock(suffix);
        if (foundBlock != null) {
            return foundBlock;
        }

        return defaultBlock;
    }

    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(METAL_PROPERTIES_MAP);
    }

    @Nullable
    public static MetalProperties getMetalProperties(String metalId) {
        return METAL_PROPERTIES_MAP.get(metalId);
    }

    public static boolean doesMetalExist(String metalId) {
        return METAL_PROPERTIES_MAP.containsKey(metalId);
    }

    public static String getMetalTypeFromStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(METAL_TYPE_KEY)) {
            return stack.getTag().getString(METAL_TYPE_KEY);
        }
        return DEFAULT_METAL_TYPE;
    }

    public static void setMetalTypeToStack(ItemStack stack, String metalType) {
        Item item = stack.getItem();
        if (!(item instanceof MetalItem || item instanceof MetalBlockItem)) return;
        boolean isDefaultMetal = Objects.equals(metalType, DEFAULT_METAL_TYPE);

        if (isDefaultMetal) {
            stack.removeTagKey(METAL_TYPE_KEY);
            stack.removeTagKey(FILLED);
        } else if (doesMetalExist(metalType)) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(METAL_TYPE_KEY, metalType);
            if (item instanceof ItemMold) {
                tag.putInt(FILLED, 1);
            }
        } else {
            SmeltingMetalMod.LOGGER.error("Invalid metal type: " + metalType);
        }
    }

    public static MetalProperties getMetalPropertiesFromStack(ItemStack stack) {
        return getMetalProperties(getMetalTypeFromStack(stack));
    }


}
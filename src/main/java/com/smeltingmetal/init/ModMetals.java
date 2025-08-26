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

    private static void parseMetalProperties(String metalName) {
        ResourceLocation ingot = findItem(metalName + "_ingot");
        ResourceLocation block = findItem(metalName + "_block");

        if (ingot == null || block == null) {
            LOGGER.error("Missing ingot or block for metal '{}'. Failed to create MetalProperties.", metalName);
            return;
        }

        // Find optional items or use default fallbacks
        ResourceLocation raw = findOrUseDefaultItem("raw_" + metalName, ModItems.RAW_METAL_ITEM.getId());
        ResourceLocation rawBlock = findOrUseDefaultBlock("raw_" + metalName + "_block", ModBlocks.RAW_METAL_BLOCK.getId());
        ResourceLocation nugget = findOrUseDefaultItem(metalName + "_nugget", ModItems.METAL_NUGGET.getId());
        ResourceLocation crushed = findOrUseDefaultItem("crushed_raw_" + metalName, ModItems.RAW_METAL_ITEM.getId()); // Fallback for Create compat
        ResourceLocation bucket = findOrUseDefaultItem(metalName + "_bucket", ModItems.MOLTEN_METAL_BUCKET.getId());
        ResourceLocation moltenItem = findOrUseDefaultItem("molten_" + metalName, ModItems.MOLTEN_METAL_ITEM.getId());
        // TODO should be a liquid not a block
        ResourceLocation moltenBlock = findOrUseDefaultBlock("molten_" + metalName + "_block", ModBlocks.MOLTEN_METAL_BLOCK.getId());

        MetalProperties properties = new MetalProperties(metalName, ingot, block, raw, rawBlock, nugget, crushed, bucket, moltenItem, moltenBlock);
        METAL_PROPERTIES_MAP.put(metalName, properties);
        LOGGER.info("Created MetalProperties for metal: {}", metalName);
    }

    @Nullable
    private static ResourceLocation findItem(String suffix) {
        return ForgeRegistries.ITEMS.getValues().stream()
                .map(ForgeRegistries.ITEMS::getKey)
                .filter(Objects::nonNull)
                .filter(key -> key.getPath().equals(suffix))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static ResourceLocation findBlock(String suffix) {
        return ForgeRegistries.BLOCKS.getValues().stream()
                .map(ForgeRegistries.BLOCKS::getKey)
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

        if (isDefaultMetal || doesMetalExist(metalType)) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(METAL_TYPE_KEY, metalType);
            if (item instanceof ItemMold) {
                tag.putInt("smeltingmetal:filled", isDefaultMetal ? 0 : 1);
            }
        } else {
            SmeltingMetalMod.LOGGER.error("Invalid metal type: " + metalType);
        }
    }

    public static MetalProperties getMetalPropertiesFromStack(ItemStack stack) {
        return getMetalProperties(getMetalTypeFromStack(stack));
    }


}
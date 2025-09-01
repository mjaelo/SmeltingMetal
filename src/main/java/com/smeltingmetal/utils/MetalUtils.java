package com.smeltingmetal.utils;

import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import com.smeltingmetal.objects.generic.MetalItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetalUtils {
    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(ModMetals.getMetalPropertiesMap());
    }

    public static MetalProperties getMetalProperties(String metalId) {
        return ModMetals.getMetalPropertiesMap().get(metalId);
    }

    public static String getMetalTypeFromStack(ItemStack stack) {
        return getTagValue(stack, ModMetals.METAL_KEY, ModMetals.DEFAULT_METAL);
    }

    public static MetalProperties getMetalPropertiesFromStack(ItemStack stack) {
        return getMetalProperties(getMetalTypeFromStack(stack));
    }

    public static void setMetalToStack(ItemStack stack, String metalType) {
        if (!ModMetals.getMetalPropertiesMap().containsKey(metalType)) return;
        setTagValue(stack, metalType, ModMetals.METAL_KEY, ModMetals.DEFAULT_METAL);
    }

    public static String getShapeFromStack(ItemStack stack) {
        String defaultShape = stack.getItem() instanceof MetalItem ? ModMetals.DEFAULT_ITEM_SHAPE : ModMetals.DEFAULT_BLOCK_SHAPE;
        return getTagValue(stack, ModMetals.SHAPE_KEY, defaultShape);
    }

    public static void setShapeToStack(ItemStack stack, String shapeValue, boolean isBlock) {
        if (!isBlock && ModMetals.getItemShapeMap().containsKey(shapeValue)) {
            setTagValue(stack, shapeValue, ModMetals.SHAPE_KEY, ModMetals.DEFAULT_ITEM_SHAPE);
        } else if (isBlock && ModMetals.getBlockShapeMap().containsKey(shapeValue)) {
            setTagValue(stack, shapeValue, ModMetals.SHAPE_KEY, ModMetals.DEFAULT_BLOCK_SHAPE);
        }
    }

    private static String getTagValue(ItemStack stack, String tagKey, String defaultValue) {
        if (stack.hasTag() && stack.getTag().contains(tagKey)) {
            return stack.getTag().getString(tagKey);
        }
        return defaultValue;
    }

    private static void setTagValue(ItemStack stack, String tagValue, String tagKey, String defaultValue) {
        Item item = stack.getItem();
        if (!(item instanceof MetalItem || item instanceof MetalBlockItem)) return;

        if (Objects.equals(tagValue, defaultValue)) {
            stack.removeTagKey(tagKey);
        } else {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(tagKey, tagValue);
        }
    }

    public static String getMetalKeyFromString(String path) {
        String pathLower = path.toLowerCase();
        for (String bad : MetalsConfig.CONFIG.blacklistKeywords.get()) {
            if (pathLower.contains(bad)) return null;
        }
        for (String metalKey : getAllMetalProperties().keySet()) {
            String keyName = metalKey.contains(":") ? metalKey.split(":")[1] : metalKey;
            if (pathLower.contains(keyName.toLowerCase())) return metalKey;
        }

        return null;
    }

    public static String getShapeKeyFromString(String path, boolean isBlock) {
        String pathLower = path.toLowerCase();
        if (MetalsConfig.CONFIG.blacklistKeywords.get().stream().anyMatch(pathLower::contains)) {
            return "";
        }
        Map<String, List<String>> itemShapeMap = isBlock ? ModMetals.getBlockShapeMap() :ModMetals.getItemShapeMap();
        return itemShapeMap.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(shapeValue -> pathLower.contains(shapeValue.toLowerCase())))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    public static String capitalizeString(String metalType) {
        return metalType.substring(0, 1).toUpperCase() + metalType.substring(1);
    }

    public static boolean isItemBlock(ResourceLocation itemId) {
        if (itemId == null) return false;
        String path = itemId.getPath();
        return MetalsConfig.CONFIG.blockKeywords.get().stream().anyMatch(path::contains);
    }
}

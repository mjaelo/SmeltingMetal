package com.smeltingmetal.utils;

import com.smeltingmetal.config.ModConfig;
import com.smeltingmetal.data.GemProperties;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.objects.generic.MetalBlockItem;
import com.smeltingmetal.objects.generic.MetalItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModUtils {
    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(ModData.getMetalPropertiesMap());
    }
    public static Map<String, GemProperties> getAllGemProperties() {
        return Collections.unmodifiableMap(ModData.getGemPropertiesMap());
    }

    public static MetalProperties getMetalProperties(String metalId) {
        return ModData.getMetalPropertiesMap().get(metalId);
    }

    public static GemProperties getGemProperties(String gemId) {
        return ModData.getGemPropertiesMap().get(gemId);
    }


    public static MetalProperties getMetalPropertiesFromStack(ItemStack stack) {
        return getMetalProperties(getContentFromStack(stack));
    }

    public static GemProperties getGemPropertiesFromStack(ItemStack stack) {
        return getGemProperties(getContentFromStack(stack));
    }

    public static String getContentFromStack(ItemStack stack) {
        return getTagValue(stack, ModData.CONTENT_KEY, ModData.DEFAULT_CONTENT);
    }

    public static void setContentToStack(ItemStack stack, String contentName) {
        if (ModData.getMetalPropertiesMap().containsKey(contentName) || ModData.getGemPropertiesMap().containsKey(contentName)) {
            setTagValue(stack, contentName, ModData.CONTENT_KEY);
        }  else {
            stack.removeTagKey(ModData.CONTENT_KEY); // both for default and invalid values
        }
    }

    public static String getShapeFromStack(ItemStack stack) {
        String defaultShape = stack.getItem() instanceof MetalItem ? ModData.DEFAULT_ITEM_SHAPE : ModData.DEFAULT_BLOCK_SHAPE;
        return getTagValue(stack, ModData.SHAPE_KEY, defaultShape);
    }

    public static void setShapeToStack(ItemStack stack, String shapeValue, boolean isBlock) {
        if (!isBlock && ModData.getItemShapeMap().containsKey(shapeValue)) {
            setTagValue(stack, shapeValue, ModData.SHAPE_KEY);
        } else if (isBlock && ModData.getBlockShapeMap().containsKey(shapeValue)) {
            setTagValue(stack, shapeValue, ModData.SHAPE_KEY);
        } else {
            stack.removeTagKey(ModData.SHAPE_KEY); // both for default and invalid values
        }
    }

    private static String getTagValue(ItemStack stack, String tagKey, String defaultValue) {
        if (stack.hasTag() && stack.getTag().contains(tagKey)) {
            return stack.getTag().getString(tagKey);
        }
        return defaultValue;
    }

    private static void setTagValue(ItemStack stack, String tagValue, String tagKey) {
        Item item = stack.getItem();
        if (!(item instanceof MetalItem || item instanceof MetalBlockItem)) return;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(tagKey, tagValue);
    }

    public static String getContentKeyFromString(String path) {
        String pathLower = path.toLowerCase();
        for (String bad : ModConfig.CONFIG.blacklistKeywords.get()) {
            if (pathLower.contains(bad)) return null;
        }
        for (String metalKey : getAllMetalProperties().keySet()) {
            String keyName = metalKey.contains(":") ? metalKey.split(":")[1] : metalKey;
            if (pathLower.contains(keyName.toLowerCase())) return metalKey;
        }
        for (String gemKey : getAllGemProperties().keySet()) {
            String keyName = gemKey.contains(":") ? gemKey.split(":")[1] : gemKey;
            if (pathLower.contains(keyName.toLowerCase())) return gemKey;
        }

        return null;
    }

    public static String getShapeKeyFromString(String path, boolean isBlock) {
        String pathLower = path.toLowerCase();
        if (ModConfig.CONFIG.blacklistKeywords.get().stream().anyMatch(pathLower::contains)) {
            return "";
        }
        Map<String, List<String>> itemShapeMap = isBlock ? ModData.getBlockShapeMap() : ModData.getItemShapeMap();
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
        return ModConfig.CONFIG.blockKeywords.get().stream().anyMatch(path::contains);
    }

    public static int getContentId(String content) {
        return ModData.getMetalPropertiesMap().containsKey(content) ? 1
                : ModData.getGemPropertiesMap().containsKey(content) ? 2 : 0;
    }


    public static int getBlockShapeId(String shapeType) {
        return switch (shapeType) {
            case "helmet" -> 1;
            case "armor" -> 2;
            case "pants" -> 3;
            case "boots" -> 4;
            default -> 0; // block
        };
    }

    public static int getItemShapeId(String shape) {
        return switch (shape) {
            case "ingot" -> 0;
            case "axe" -> 1;
            case "pickaxe" -> 2;
            case "shovel" -> 3;
            case "sword" -> 4;
            case "hoe" -> 5;
            default -> 0; // Default to ingot shape (0.0)
        };
    }
}

package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Represents the properties of a gem in the Smelting Metal mod.
 * This immutable record stores the essential identification and registration data for gems.
 * 
 * @param name The unique identifier for the gem (e.g., "diamond")
 * @param gem The ResourceLocation of the corresponding gem item (e.g., "diamond")
 * @param block The ResourceLocation of the corresponding gem block (e.g., "diamond_block")
 * @param itemResults The map of item results for the gem (e.g., "diamond_sword")
 * @param blockResults The map of block results for the gem (e.g., "diamond_chestplate")
 */
public record GemProperties(
    String name,
    ResourceLocation gem,
    ResourceLocation block,
    Map<String, ResourceLocation> itemResults,
    Map<String, ResourceLocation> blockResults,
    int color
) {
}
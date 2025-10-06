package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Represents the properties of a gem in the Smelting Metal mod.
 * This immutable record stores the essential identification and registration data for gems.
 * 
 * @param name The unique identifier for the gem (e.g., "diamond")
 * @param gem The ResourceLocation of the main gem item (e.g., "diamond")
 * @param block The ResourceLocation of gem block, created by 9 gem items (e.g., "diamond_block")
 * @param shard The ResourceLocation of gem shards, 9 gem shards create gem item (e.g., "diamond_shards")
 * @param itemResults The map of item results for the gem (e.g., "diamond_sword")
 * @param blockResults The map of block results for the gem (e.g., "diamond_chestplate")
 */
public record GemProperties(
    String name,
    ResourceLocation gem,
    ResourceLocation block,
    ResourceLocation shard,
    Map<String, ResourceLocation> itemResults,
    Map<String, ResourceLocation> blockResults,
    int color
) {
}
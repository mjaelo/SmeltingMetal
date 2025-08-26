package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents the properties of a metal in the Smelting Metal mod.
 * This immutable record stores the essential identification and registration data for metals.
 * 
 * @param name The unique identifier for the metal (e.g., "iron")
 * @param ingot The ResourceLocation of the corresponding ingot item (e.g., "minecraft:iron_ingot")
 * @param block The ResourceLocation of the corresponding metal block (e.g., "minecraft:iron_block")
 * @param raw The ResourceLocation of the corresponding raw metal item (e.g., "minecraft:raw_iron")
 * @param rawBlock The ResourceLocation of the corresponding raw metal block (e.g., "minecraft:raw_iron_block")
 * @param nugget The ResourceLocation of the corresponding nugget item (e.g., "minecraft:iron_nugget")
 * @param crushed The ResourceLocation of the corresponding crushed item (e.g., "create:crushed_raw_iron")
 * @param bucket The ResourceLocation of the corresponding bucket item (e.g., "smeltingmetal:iron_bucket")
 * @param moltenItem The ResourceLocation of the corresponding molten item
 * @param moltenBlock The ResourceLocation of the corresponding liquid/fluid
 */
public record MetalProperties(
    String name,
    ResourceLocation ingot,
    ResourceLocation block,
    ResourceLocation raw,
    ResourceLocation rawBlock,
    ResourceLocation nugget,
    ResourceLocation crushed,
    ResourceLocation bucket,
    ResourceLocation moltenItem,
    ResourceLocation moltenBlock
) {
}
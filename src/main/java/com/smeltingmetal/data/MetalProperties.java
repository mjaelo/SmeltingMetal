package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents the properties of a metal in the Smelting Metal mod.
 * This immutable record stores the essential identification and registration data for metals.
 * 
 * @param id The unique identifier for the metal (e.g., "iron", "gold", "copper")
 * @param ingotId The ResourceLocation of the corresponding ingot item (e.g., "minecraft:iron_ingot")
 */
public record MetalProperties(String id, ResourceLocation ingotId) {
}
package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;
import javax.annotation.Nullable;

/**
 * Represents the properties of a metal in the Smelting Metal mod.
 * This immutable record stores the essential identification and registration data for metals.
 * 
 * @param id The unique identifier for the metal (e.g., "minecraft:iron")
 * @param ingotId The ResourceLocation of the corresponding ingot item (e.g., "minecraft:iron_ingot")
 * @param rawId The ResourceLocation of the corresponding raw metal item (e.g., "minecraft:raw_iron")
 * @param rawBlockId The ResourceLocation of the corresponding raw metal block (e.g., "minecraft:raw_iron_block")
 * @param nuggetId The ResourceLocation of the corresponding nugget item (e.g., "minecraft:iron_nugget"), or null if not available
 * @param crushedId The ResourceLocation of the corresponding crushed item (e.g., "create:crushed_raw_iron"), or null if not available
 */
public record MetalProperties(
    String id,
    ResourceLocation ingotId,
    ResourceLocation rawId,
    ResourceLocation rawBlockId,
    @Nullable ResourceLocation nuggetId,
    @Nullable ResourceLocation crushedId
) {
    public MetalProperties(String id, ResourceLocation ingotId, ResourceLocation rawId, ResourceLocation rawBlockId) {
        this(id, ingotId, rawId, rawBlockId, null, null);
    }
}
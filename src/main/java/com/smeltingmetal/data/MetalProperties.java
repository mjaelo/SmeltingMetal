package com.smeltingmetal.data;

import net.minecraft.resources.ResourceLocation;

// This record holds properties for a specific metal type.
public record MetalProperties(String id, ResourceLocation ingotId) {
    // 'id' will be like "iron", "gold", "copper"
    // 'ingotId' will be like new ResourceLocation("minecraft", "iron_ingot")
}
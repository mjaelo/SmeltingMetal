package com.smeltingmetal; // Assuming this is in the base package

import com.mojang.logging.LogUtils; // Import for Logger
import com.smeltingmetal.data.MetalProperties;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger; // Import for Logger

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Central registry for all metal types in your mod
public class ModMetals {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, MetalProperties> METAL_PROPERTIES_MAP = new HashMap<>();

    static {
        // Vanilla Metals
        registerMetal(new MetalProperties("iron", new ResourceLocation("minecraft", "iron_ingot")));
        registerMetal(new MetalProperties("gold", new ResourceLocation("minecraft", "gold_ingot")));
        registerMetal(new MetalProperties("copper", new ResourceLocation("minecraft", "copper_ingot")));

        // Example for a custom metal from another mod (replace 'mymod' and 'tin_ingot' accordingly)
        // registerMetal(new MetalProperties("tin", new ResourceLocation("mymod", "tin_ingot")));
    }

    // Helper method to add metals to the map
    private static void registerMetal(MetalProperties metalProperties) {
        if (METAL_PROPERTIES_MAP.containsKey(metalProperties.id())) {
            LOGGER.warn("Duplicate metal ID registered: " + metalProperties.id());
        }
        METAL_PROPERTIES_MAP.put(metalProperties.id(), metalProperties);
    }

    // Public method to retrieve metal properties by ID
    public static Optional<MetalProperties> getMetalProperties(String id) {
        return Optional.ofNullable(METAL_PROPERTIES_MAP.get(id));
    }

    // Public getter for the unmodifiable map (if you ever need to iterate all metals)
    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(METAL_PROPERTIES_MAP);
    }

    // You might also want a method to check if a metal ID exists
    public static boolean doesMetalExist(String id) {
        return METAL_PROPERTIES_MAP.containsKey(id);
    }
}
package com.smeltingmetal;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.config.MetalsConfig;
import com.smeltingmetal.data.MetalProperties;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;

public class ModMetals {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, MetalProperties> METAL_PROPERTIES_MAP = new HashMap<>();
    private static final Map<String, MetalProperties> DEFAULT_METALS = createDefaultMetals();
    private static boolean initialized = false;

    private static Map<String, MetalProperties> createDefaultMetals() {
        Map<String, MetalProperties> defaults = new HashMap<>();
        defaults.put("iron", new MetalProperties("iron", new ResourceLocation("minecraft", "iron_ingot")));
        defaults.put("gold", new MetalProperties("gold", new ResourceLocation("minecraft", "gold_ingot")));
        defaults.put("copper", new MetalProperties("copper", new ResourceLocation("minecraft", "copper_ingot")));
        return defaults;
    }

    public static void init() {
        if (initialized) {
            return;
        }

        METAL_PROPERTIES_MAP.clear();
        
        try {
            // Try to load from config if available
            if (MetalsConfig.CONFIG != null && MetalsConfig.CONFIG.metalDefinitions != null) {
                List<? extends String> metalDefs = MetalsConfig.CONFIG.metalDefinitions.get();
                for (String definition : metalDefs) {
                    try {
                        String[] parts = definition.split("=", 2);
                        if (parts.length != 2) {
                            LOGGER.error("Invalid metal definition format: {}", definition);
                            continue;
                        }
                        
                        String metalId = parts[0].trim();
                        String[] metalParts = metalId.split(":", 2);
                        if (metalParts.length != 2) {
                            LOGGER.error("Invalid metal ID format: {}", metalId);
                            continue;
                        }
                        
                        String[] ingotParts = parts[1].trim().split(":", 2);
                        if (ingotParts.length != 2) {
                            LOGGER.error("Invalid ingot format: {}", parts[1].trim());
                            continue;
                        }
                        
                        ResourceLocation ingotId = new ResourceLocation(ingotParts[0], ingotParts[1]);
                        registerMetal(new MetalProperties(metalParts[1], ingotId));
                        LOGGER.debug("Registered metal: {} -> {}", metalId, ingotId);
                        
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse metal definition '{}': {}", definition, e.getMessage());
                    }
                }
            } else {
                LOGGER.warn("Config not loaded, using default metals");
                METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load metals from config, using defaults", e);
            METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
        }
        
        LOGGER.info("Registered {} metals", METAL_PROPERTIES_MAP.size());
        initialized = true;
    }

    private static void registerMetal(MetalProperties metalProperties) {
        if (METAL_PROPERTIES_MAP.containsKey(metalProperties.id())) {
            LOGGER.warn("Duplicate metal ID registered: {}", metalProperties.id());
        }
        METAL_PROPERTIES_MAP.put(metalProperties.id(), metalProperties);
    }

    public static Optional<MetalProperties> getMetalProperties(String id) {
        return Optional.ofNullable(METAL_PROPERTIES_MAP.get(id));
    }

    public static Map<String, MetalProperties> getAllMetalProperties() {
        return Collections.unmodifiableMap(METAL_PROPERTIES_MAP);
    }

    public static boolean doesMetalExist(String id) {
        return METAL_PROPERTIES_MAP.containsKey(id);
    }
}
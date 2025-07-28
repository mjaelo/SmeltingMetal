package com.smeltingmetal.data;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.MetalsConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages the properties and configurations of all metals in the Smelting Metal mod.
 * Handles loading metal configurations, applying overrides, and providing access to metal properties.
 * This includes default metal definitions and any custom metals defined in the configuration.
 */
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
            // First check if we can safely access the config
            if (MetalsConfig.CONFIG == null) {
                LOGGER.warn("Config not loaded yet, using default metals");
                METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                initialized = true;
                return;
            }
            
            // Safely get config values
            if (MetalsConfig.CONFIG.metalDefinitions != null) {
                try {
                    List<? extends String> metalDefs = MetalsConfig.CONFIG.metalDefinitions.get();
                    
                    if (metalDefs.isEmpty()) {
                        LOGGER.warn("No metal definitions found in config, using default metals");
                        METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                    } else {
                        // Parse metal definitions from config
                        for (String def : metalDefs) {
                            try {
                                String[] parts = def.split("=\\s*", 2);
                                if (parts.length == 2) {
                                    String id = parts[0].trim();
                                    ResourceLocation ingotId = new ResourceLocation(parts[1].trim());
                                    registerMetal(new MetalProperties(id, ingotId));
                                }
                            } catch (Exception e) {
                                LOGGER.error("Failed to parse metal definition: " + def, e);
                            }
                        }
                        
                        if (METAL_PROPERTIES_MAP.isEmpty()) {
                            LOGGER.warn("No valid metal definitions found, using default metals");
                            METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                        }
                    }
                } catch (IllegalStateException e) {
                    LOGGER.warn("Config not ready yet, using default metals");
                    METAL_PROPERTIES_MAP.putAll(DEFAULT_METALS);
                }
            } else {
                LOGGER.warn("Metal definitions config not available, using default metals");
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

    public static String getMetalId(ResourceLocation ingotId) {
        return METAL_PROPERTIES_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().ingotId().equals(ingotId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
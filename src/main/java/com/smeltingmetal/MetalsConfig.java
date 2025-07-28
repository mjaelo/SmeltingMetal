package com.smeltingmetal;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Predicate;

/**
 * Handles configuration for the Smelting Metal mod, including metal definitions,
 * blacklist keywords, and feature toggles. This class manages the mod's runtime
 * configuration using Forge's configuration system.
 */
public class MetalsConfig {
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final Config CONFIG;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        CONFIG_SPEC = builder.build();
    }

    /**
     * Inner configuration class that holds all configurable values for the mod.
     * These values are loaded from the mod's configuration file and can be
     * modified by server admins or through the config GUI.
     */
    public static class Config {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> metalDefinitions;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistKeywords;
        public final ForgeConfigSpec.BooleanValue enableMetalMelting;
        public final ForgeConfigSpec.BooleanValue enableNuggetRecipeReplacement;
        
        private static final Predicate<Object> METAL_DEFINITION_VALIDATOR = 
            obj -> obj instanceof String && ((String)obj).matches("^[a-z0-9_.-]+:[a-z0-9_.-]+=[a-z0-9_.-]+:[a-z0-9_.-]+$");
        
        private static final Predicate<Object> KEYWORD_VALIDATOR =
            obj -> obj instanceof String && ((String)obj).matches("^[a-z0-9_.-]+$");
        
        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Metal processing configuration")
                  .push("metals");
            
            // Default metal definitions
            List<String> defaultMetals = List.of(
                "minecraft:iron=minecraft:iron_ingot",
                "minecraft:gold=minecraft:gold_ingot",
                "minecraft:copper=minecraft:copper_ingot"
            );
            
            metalDefinitions = builder
                .comment("List of metal definitions in format: modid:metal_id=ingot_modid:ingot_id")
                .defineList(
                    "metal_definitions", 
                    defaultMetals, 
                    METAL_DEFINITION_VALIDATOR
                );
            
            // Blacklisted substrings for ingredients / items (e.g., block, nugget)
            List<String> defaultBlacklist = List.of("block", "nugget");
            blacklistKeywords = builder
                .comment("List of substrings; if an ingredient's registry path contains any of these, the recipe is skipped from molten replacement")
                .defineList("blacklist_keywords", defaultBlacklist, KEYWORD_VALIDATOR);
            
            builder.pop();
            
            // Feature toggles
            builder.comment("Feature toggles")
                  .push("features");
            
            enableMetalMelting = builder
                .comment("Enable replacement of smelting/blasting recipes with molten metal recipes")
                .define("enable_metal_melting", true);
                
            enableNuggetRecipeReplacement = builder
                .comment("Enable replacement of nugget->ingot crafting recipes with nugget->raw_metal")
                .define("enable_nugget_recipe_replacement", true);
                
            builder.pop();
        }
    }
}

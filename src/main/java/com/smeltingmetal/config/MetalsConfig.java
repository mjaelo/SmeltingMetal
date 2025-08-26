package com.smeltingmetal.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockKeywords;
        public final ForgeConfigSpec.BooleanValue enableMeltingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableCrushingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableNuggetRecipeReplacement;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Metal processing configuration")
                    .push("metals");

            // Default metal definitions
            List<String> defaultMetals = List.of("iron", "gold", "copper");

            metalDefinitions = builder
                    .comment("List of base metal names to be processed (e.g., iron, gold, tin).")
                    .defineList(
                            "metal_definitions",
                            defaultMetals,
                            obj -> obj instanceof String
                    );

            // Blacklist keywords for items that should not be processed
            List<String> defaultBlacklist = List.of("nugget");
            blacklistKeywords = builder
                    .comment("List of keywords to blacklist from processing. Any item containing these strings in its registry name will be skipped from both melting and Create crushing recipes.")
                    .defineList("blacklist_keywords", defaultBlacklist, obj -> true);

            // Block keywords for identifying block items
            List<String> defaultBlockKeywords = List.of("block", "slab", "stairs", "wall", "bricks", "tiles");
            blockKeywords = builder
                    .comment("List of keywords that identify block items. Any item containing these strings in its registry name will be treated as a block.")
                    .defineList("block_keywords", defaultBlockKeywords, obj -> true);

            builder.pop();

            // Feature toggles
            builder.comment("Feature toggles")
                    .push("features");

            enableMeltingRecipeReplacement = builder
                    .comment("Enable replacement of smelting/blasting recipes with molten metal recipes")
                    .define("enable_melting_recipe_replacement", true);

            enableCrushingRecipeReplacement = builder
                    .comment("Enable replacement of crushing recipes with crushed metal recipes")
                    .define("enable_crushing_recipe_replacement", true);

            enableNuggetRecipeReplacement = builder
                    .comment("Enable replacement of nugget->ingot crafting recipes with nugget->raw_metal")
                    .define("enable_nugget_recipe_replacement", true);

            builder.pop();
        }
    }
}

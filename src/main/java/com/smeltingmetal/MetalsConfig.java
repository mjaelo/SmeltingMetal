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
        public final ForgeConfigSpec.BooleanValue enableMeltingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableCrushingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableNuggetRecipeReplacement;

        private static final Predicate<Object> METAL_DEFINITION_VALIDATOR =
                obj -> obj instanceof String && ((String) obj).matches("^[a-z0-9_.-]+:[a-z0-9_.-]+$");

        private static final Predicate<Object> KEYWORD_VALIDATOR =
                obj -> obj instanceof String && ((String) obj).matches("^[a-z0-9_.-]+$");

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Metal processing configuration")
                    .push("metals");

            // Default metal definitions (simple format: modid:metal_name)
            List<String> defaultMetals = List.of(
                    "minecraft:iron",
                    "minecraft:gold",
                    "minecraft:copper"
            );

            metalDefinitions = builder
                    .comment("List of metal definitions in format: modid:metal_name (e.g., minecraft:iron)")
                    .defineList(
                            "metal_definitions",
                            defaultMetals,
                            METAL_DEFINITION_VALIDATOR
                    );

            // Blacklisted substrings for ingredients / items (e.g., block, nugget)
            // Blacklist keywords for items that should not be processed
            List<String> defaultBlacklist = List.of("nugget");
            blacklistKeywords = builder
                    .comment("List of keywords to blacklist from processing. Any item containing these strings in its registry name will be skipped from both melting and Create crushing recipes.")
                    .defineList("blacklist_keywords", defaultBlacklist, KEYWORD_VALIDATOR);

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

package com.smeltingmetal.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Handles configuration for the Smelting Metal mod, including metal definitions,
 * blacklist keywords, and feature toggles. This class manages the mod's runtime
 * configuration using Forge's configuration system.
 */
public class ModConfig {
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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> gemDefinitions;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> itemResultDefinitions;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockResultDefinitions;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklistKeywords;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blockKeywords;
        public final ForgeConfigSpec.BooleanValue enableMeltingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableGemRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableCrushingRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableNuggetRecipeReplacement;
        public final ForgeConfigSpec.BooleanValue enableResultRecipeRemoval;

        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Metal processing configuration")
                    .push("metals");

            // Default metal definitions
            List<String> defaultMetals = List.of("iron,color=b9835f", "gold,color=fbeb2e", "copper,color=e97b53", "netherite,color=5d342c");
            metalDefinitions = builder
                    .comment("List of base metal names to be processed (e.g., iron, gold, tin).")
                    .defineList(
                            "metal_definitions",
                            defaultMetals,
                            obj -> obj instanceof String
                    );

            // Default gem definitions
            List<String> defaultGems = List.of("diamond,color=B0FFFF");
            gemDefinitions = builder
                    .comment("List of base gem names to be processed (e.g., diamond, emerald, ruby, sapphire).")
                    .defineList(
                            "gem_definitions",
                            defaultGems,
                            obj -> obj instanceof String
                    );

            // Default item result definitions
            List<String> defaultItemResults = List.of("ingot", "pickaxe", "axe", "shovel", "sword", "hoe");
            itemResultDefinitions = builder
                    .comment("List of item result types that can be produced from metals with their keywords.")
                    .defineList("item_result_definitions", defaultItemResults, obj -> obj instanceof String);

            // Default block result definitions
            List<String> defaultBlockResults = List.of("block", "helmet=helmet,cap", "armor=chestplate,tunic,armor", "pants=pants,leggings", "boots=boots,shoes");
            blockResultDefinitions = builder
                    .comment("List of block result types that can be produced from metals with their keywords.")
                    .defineList("block_result_definitions", defaultBlockResults, obj -> obj instanceof String);

            // Blacklist keywords for items that should not be processed
            List<String> defaultBlacklist = List.of("nugget", "scrap", "mold", "template", "shard");
            blacklistKeywords = builder
                    .comment("List of keywords to blacklist from processing. Any item containing these strings in its name will be skipped from both melting and Create crushing recipes.")
                    .defineList("blacklist_keywords", defaultBlacklist, obj -> true);

            // Block keywords for identifying block items
            List<String> defaultBlockKeywords = List.of("block", "slab", "stairs", "wall", "bricks", "tiles");
            blockKeywords = builder
                    .comment("List of keywords that identify block items. Any metal item containing these strings in its name will output a molten metal block when melted.")
                    .defineList("block_keywords", defaultBlockKeywords, obj -> true);

            builder.pop();

            // Feature toggles
            builder.comment("Feature toggles")
                    .push("features");

            enableMeltingRecipeReplacement = builder
                    .comment("Enable replacement of smelting/blasting recipes with molten metal recipes")
                    .define("enable_melting_recipe_replacement", true);

            enableGemRecipeReplacement = builder
                    .comment("Enable replacement of gem recipes with gem dust recipes")
                    .define("enable_gem_recipe_replacement", true);

            enableCrushingRecipeReplacement = builder
                    .comment("Enable replacement of crushing recipes with crushed metal recipes")
                    .define("enable_crushing_recipe_replacement", true);

            enableNuggetRecipeReplacement = builder
                    .comment("Enable replacement of nugget->ingot crafting recipes with nugget->raw_metal")
                    .define("enable_nugget_recipe_replacement", true);

            enableResultRecipeRemoval = builder
                    .comment("Enable removal of recipes for items in result lists.")
                    .define("enable_result_recipe_removal", true);

            builder.pop();
        }
    }
}

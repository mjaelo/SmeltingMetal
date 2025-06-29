package com.smeltingmetal.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;
import java.util.function.Predicate;

public class MetalsConfig {
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final Config CONFIG;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CONFIG = new Config(builder);
        CONFIG_SPEC = builder.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CONFIG_SPEC, "smeltingmetal-metals.toml");
    }
    
    public static class Config {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> metalDefinitions;
        private static final Predicate<Object> METAL_DEFINITION_VALIDATOR = 
            obj -> obj instanceof String && ((String)obj).matches("^[a-z0-9_.-]+:[a-z0-9_.-]+=[a-z0-9_.-]+:[a-z0-9_.-]+$");
        
        public Config(ForgeConfigSpec.Builder builder) {
            builder.comment("Metal definitions in format: modid:metal_id=ingot_modid:ingot_id")
                  .push("metals");
            
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
            
            builder.pop();
        }
    }
}

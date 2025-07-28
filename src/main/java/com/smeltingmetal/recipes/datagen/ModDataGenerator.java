package com.smeltingmetal.recipes.datagen;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles the registration and initialization of all data generators for the Smelting Metal mod.
 * This class is responsible for setting up data generation during mod initialization.
 * It automatically registers itself to Forge's event bus to listen for the data generation event.
 */
@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));
    }
}
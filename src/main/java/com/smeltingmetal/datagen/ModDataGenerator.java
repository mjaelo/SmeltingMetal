package com.smeltingmetal.datagen;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper(); // Still needed for other data providers

        // Pass only PackOutput to ModRecipeProvider
        generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));
        // Add other providers here if you have them, e.g., new ModItemModelProvider(packOutput, existingFileHelper)
    }
}
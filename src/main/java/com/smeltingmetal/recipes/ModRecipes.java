package com.smeltingmetal.recipes;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles the registration of all custom recipe serializers for the Smelting Metal mod.
 * This class is responsible for registering the serializers that define how custom recipes
 * are saved to and loaded from JSON files.
 */
public class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "smeltingmetal");

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
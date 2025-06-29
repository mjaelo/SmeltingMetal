package com.smeltingmetal.recipes;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    // DeferredRegister for RecipeSerializers (already correct)
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SmeltingMetalMod.MODID);

    // Register your custom serializer (already correct)
    public static final RegistryObject<RecipeSerializer<MoltenMetalSmeltingRecipe>> MOLTEN_METAL_SMELTING_SERIALIZER =
            SERIALIZERS.register("molten_metal_smelting", () -> MoltenMetalSmeltingSerializer.INSTANCE);

    // --------------------------------------------------------------------------------
    // NEW: DeferredRegister for RecipeTypes
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, SmeltingMetalMod.MODID);

    // NEW: Register your custom RecipeType using the DeferredRegister
    public static final RegistryObject<RecipeType<MoltenMetalSmeltingRecipe>> MOLTEN_METAL_SMELTING_TYPE =
            RECIPE_TYPES.register("molten_metal_smelting", () -> new RecipeType<MoltenMetalSmeltingRecipe>() {});
    // --------------------------------------------------------------------------------


    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        // NEW: Register the RecipeTypes DeferredRegister to the event bus
        RECIPE_TYPES.register(eventBus);
    }
}
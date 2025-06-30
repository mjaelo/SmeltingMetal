package com.smeltingmetal.recipes;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    // DeferredRegister for RecipeSerializers
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SmeltingMetalMod.MODID);

    // Register our custom serializer
    public static final RegistryObject<RecipeSerializer<MoltenMetalSmeltingRecipe>> MOLTEN_METAL_SMELTING =
            SERIALIZERS.register("molten_metal_smelting", () -> MoltenMetalSmeltingSerializer.INSTANCE);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
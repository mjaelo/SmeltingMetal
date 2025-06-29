package com.smeltingmetal.recipes;

import com.smeltingmetal.items.MoltenMetalItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;

public class MoltenMetalSmeltingRecipe extends SmeltingRecipe {

    private final String metalType;

    public MoltenMetalSmeltingRecipe(ResourceLocation pId, String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime, String metalType) {
        // Pass pCategory to the super constructor
        super(pId, pGroup, pCategory, pIngredient, pResult, pExperience, pCookingTime);
        this.metalType = metalType;
    }

    public String getMetalType() {
        return metalType;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container pContainer, net.minecraft.core.RegistryAccess pRegistryAccess) {
        return MoltenMetalItem.createMoltenMetal(this.metalType);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MOLTEN_METAL_SMELTING_SERIALIZER.get();
    }
}
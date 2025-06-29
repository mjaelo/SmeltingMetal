package com.smeltingmetal.recipes;

import com.google.gson.JsonObject;
import com.smeltingmetal.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

public class MoltenMetalSmeltingSerializer implements RecipeSerializer<MoltenMetalSmeltingRecipe> {

    public static final MoltenMetalSmeltingSerializer INSTANCE = new MoltenMetalSmeltingSerializer();

    @Override
    public MoltenMetalSmeltingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
        String group = GsonHelper.getAsString(pJson, "group", "");
        CookingBookCategory category = CookingBookCategory.CODEC.byName(GsonHelper.getAsString(pJson, "category", null), CookingBookCategory.MISC);
        Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(pJson, "ingredient"));
        float experience = GsonHelper.getAsFloat(pJson, "experience");
        int cookingTime = GsonHelper.getAsInt(pJson, "cookingtime");

        // Keep this as is; the serializer's job is just to read the string
        String metalType = GsonHelper.getAsString(pJson, "metalType");

        ItemStack resultStack = new ItemStack(ModItems.MOLTEN_METAL.get());

        return new MoltenMetalSmeltingRecipe(pRecipeId, group, category, ingredient, resultStack, experience, cookingTime, metalType);
    }

    @Override
    public @Nullable MoltenMetalSmeltingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        String group = pBuffer.readUtf();
        CookingBookCategory category = pBuffer.readEnum(CookingBookCategory.class);
        Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
        float experience = pBuffer.readFloat();
        int cookingTime = pBuffer.readVarInt();

        String metalType = pBuffer.readUtf();

        ItemStack resultStack = new ItemStack(ModItems.MOLTEN_METAL.get());

        return new MoltenMetalSmeltingRecipe(pRecipeId, group, category, ingredient, resultStack, experience, cookingTime, metalType);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, MoltenMetalSmeltingRecipe pRecipe) {
        pBuffer.writeUtf(pRecipe.getGroup());
        pBuffer.writeEnum(pRecipe.category());
        pRecipe.getIngredients().get(0).toNetwork(pBuffer);
        pBuffer.writeFloat(pRecipe.getExperience());
        pBuffer.writeVarInt(pRecipe.getCookingTime());

        pBuffer.writeUtf(pRecipe.getMetalType());
    }
}
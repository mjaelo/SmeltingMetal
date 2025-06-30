package com.smeltingmetal.recipes;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class MoltenMetalSmeltingSerializer implements RecipeSerializer<MoltenMetalSmeltingRecipe> {
    public static final MoltenMetalSmeltingSerializer INSTANCE = new MoltenMetalSmeltingSerializer();

    @Override
    public MoltenMetalSmeltingRecipe fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
        String group = GsonHelper.getAsString(pJson, "group", "");
        CookingBookCategory category = CookingBookCategory.CODEC.byName(GsonHelper.getAsString(pJson, "category", null), CookingBookCategory.MISC);
        
        Ingredient ingredient = Ingredient.fromJson(pJson.get("ingredient"));
        ItemStack resultStack = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(GsonHelper.getAsString(pJson, "result"))));
        
        float experience = GsonHelper.getAsFloat(pJson, "experience", 0.0F);
        int cookingTime = GsonHelper.getAsInt(pJson, "cookingtime", 200);
        
        return new MoltenMetalSmeltingRecipe(pRecipeId, group, category, ingredient, resultStack, experience, cookingTime);
    }

    @Override
    public @Nullable MoltenMetalSmeltingRecipe fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
        String group = pBuffer.readUtf();
        CookingBookCategory category = pBuffer.readEnum(CookingBookCategory.class);
        Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
        ItemStack resultStack = pBuffer.readItem();
        float experience = pBuffer.readFloat();
        int cookingTime = pBuffer.readVarInt();
        
        return new MoltenMetalSmeltingRecipe(pRecipeId, group, category, ingredient, resultStack, experience, cookingTime);
    }

    @Override
    public void toNetwork(FriendlyByteBuf pBuffer, MoltenMetalSmeltingRecipe pRecipe) {
        pBuffer.writeUtf(pRecipe.getGroup());
        pBuffer.writeEnum(pRecipe.category());
        pRecipe.getIngredients().get(0).toNetwork(pBuffer);
        pBuffer.writeItem(pRecipe.getResultItem(null));
        pBuffer.writeFloat(pRecipe.getExperience());
        pBuffer.writeVarInt(pRecipe.getCookingTime());
    }
}
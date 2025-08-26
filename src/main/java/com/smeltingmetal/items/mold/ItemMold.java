package com.smeltingmetal.items.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.items.EntityEventsUtils;
import com.smeltingmetal.items.generic.MetalItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class ItemMold extends MetalItem {
    private MaterialType materialType;
    private String resultType = "ingot";

    public ItemMold(Properties pProperties, MaterialType materialType) {
        super(pProperties);
        this.materialType = materialType;
        
        // Register item property overrides on the client side
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ItemProperties.register(this, 
                new ResourceLocation("smeltingmetal", "filled"),
                (stack, level, entity, seed) -> 
                    stack.hasTag() && stack.getTag().contains("smeltingmetal:filled") ? 
                        stack.getTag().getInt("smeltingmetal:filled") : 0);
        });
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }
}

package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.objects.generic.MetalItem;
import com.smeltingmetal.utils.EntityEventsUtils;
import com.smeltingmetal.utils.ModUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import static com.smeltingmetal.SmeltingMetalMod.MODID;

public class ItemMold extends MetalItem {
    private final MaterialType materialType;
    private String shape;

    public ItemMold(Properties pProperties, MaterialType materialType) {
        super(pProperties);
        this.materialType = materialType;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerItemProperties);
    }

    public ItemMold(Properties pProperties, MaterialType materialType, String shape) {
        super(pProperties);
        this.materialType = materialType;
        this.shape = shape;

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerItemProperties);
    }

    private void registerItemProperties() {
        ItemProperties.register(this,
                new ResourceLocation(MODID, ModData.CONTENT_KEY),
                (stack, level, entity, seed) -> {
                    String content = ModUtils.getContentFromStack(stack);
                    return ModUtils.getContentId(content);
                });

        ItemProperties.register(this,
                new ResourceLocation(MODID, ModData.SHAPE_KEY),
                (stack, level, entity, seed) -> {
                    String shape = this.shape != null ? this.shape : ModUtils.getShapeFromStack(stack);
                    return ModUtils.getItemShapeId(shape);
                });
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return EntityEventsUtils.handleStackAutoCooling(stack, entity);
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public String getShape() {
        return shape;
    }
}

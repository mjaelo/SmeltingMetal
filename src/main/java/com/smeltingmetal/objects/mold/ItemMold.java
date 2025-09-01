package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.objects.generic.MetalItem;
import com.smeltingmetal.utils.EntityEventsUtils;
import com.smeltingmetal.utils.MetalUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

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
                new ResourceLocation("smeltingmetal", "has_metal"),
                (stack, level, entity, seed) -> {
                    String metalType = MetalUtils.getMetalTypeFromStack(stack);
                    return metalType.equals(ModMetals.DEFAULT_METAL) ? 0.0F : 1.0F;
                });

        ItemProperties.register(this,
                new ResourceLocation("smeltingmetal", "shape"),
                (stack, level, entity, seed) -> {
                    String shape = this.shape != null ? this.shape : MetalUtils.getShapeFromStack(stack);
                    return switch (shape) {
                        case "ingot" -> 0.0f;
                        case "axe" -> 1.0f;
                        case "pickaxe" -> 2.0f;
                        case "shovel" -> 3.0f;
                        case "sword" -> 4.0f;
                        case "hoe" -> 5.0f;
                        default -> 0.0f; // Default to ingot shape (0.0)
                    };
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

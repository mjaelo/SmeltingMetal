package com.smeltingmetal.init;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.objects.mold.BlockMoldEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SmeltingMetalMod.MODID);

    public static final RegistryObject<BlockEntityType<BlockMoldEntity>> BLOCK_MOLD_BE = BLOCK_ENTITIES.register("block_mold_be", () ->
            BlockEntityType.Builder.of(
                    BlockMoldEntity::new,
                    ModBlocks.BLOCK_MOLD_CLAY.get(),
                    ModBlocks.BLOCK_MOLD_HARDENED.get(),
                    ModBlocks.BLOCK_MOLD_NETHERITE.get()
            ).build(null));
}

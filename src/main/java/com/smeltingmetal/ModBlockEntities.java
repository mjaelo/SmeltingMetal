package com.smeltingmetal;

import com.smeltingmetal.blockentity.MetalCaseBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SmeltingMetalMod.MODID);

    public static final RegistryObject<BlockEntityType<MetalCaseBlockEntity>> hardened_case = BLOCK_ENTITIES.register(
            "hardened_case", () -> BlockEntityType.Builder.of(MetalCaseBlockEntity::new, ModBlocks.HARDENED_CASE.get(), ModBlocks.NETHERITE_CASE.get()).build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}

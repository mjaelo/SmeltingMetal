package com.smeltingmetal.blocks;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.blocks.blockentity.MetalCaseBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handles registration and management of all custom block entities in the Smelting Metal mod.
 * This includes block entities like the metal case which require special processing.
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SmeltingMetalMod.MODID);

    public static final RegistryObject<BlockEntityType<MetalCaseBlockEntity>> hardened_case = BLOCK_ENTITIES.register(
            "hardened_case", () -> BlockEntityType.Builder.of(MetalCaseBlockEntity::new, ModBlocks.HARDENED_CASE.get(), ModBlocks.NETHERITE_CASE.get()).build(null));

}

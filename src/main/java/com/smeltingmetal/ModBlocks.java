package com.smeltingmetal;

import com.smeltingmetal.blocks.MetalCaseBlock;
import com.smeltingmetal.blocks.NetheriteCaseBlock;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Central registry class for all custom blocks added by the mod.
 */
@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SmeltingMetalMod.MODID);

    // ---------------------- CASE BLOCKS --------------------------
    public static final RegistryObject<Block> CLAY_CASE = BLOCKS.register("clay_case",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.TERRACOTTA)
                    .strength(1.0F, 4.0F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> METAL_CASE = BLOCKS.register("metal_case",
            () -> new MetalCaseBlock(BlockBehaviour.Properties.copy(Blocks.TERRACOTTA)
                    .strength(1.5F, 6.0F) // similar to terracotta but slightly stronger
                    .sound(SoundType.DECORATED_POT) // still plays metal sound
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistryObject<Block> NETHERITE_METAL_CASE = BLOCKS.register("netherite_metal_case",
            () -> new NetheriteCaseBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                    .strength(50.0F, 2400.0F) // stronger than metal version
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    // ---------------------- BLOCK ITEM REGISTRATION --------------
    private static void registerBlockItems(IEventBus bus) {
        ModItems.ITEMS.register(bus);
        // We purposefully attach the block-items inside an enqueueWork call in ModItems
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        registerBlockItems(bus);
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CLAY_CASE.get());
            event.accept(METAL_CASE.get());
            event.accept(NETHERITE_METAL_CASE.get());
        }
    }

}

package com.smeltingmetal;

import com.smeltingmetal.blocks.MetalCaseBlock;
import com.smeltingmetal.blocks.NetheriteCaseBlock;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)
                    .strength(0.1F, .5F)
                    .sound(SoundType.MUD)
                    .noOcclusion()
                    .dropsLike(Blocks.CLAY)
                    ));

    public static final RegistryObject<Block> METAL_CASE = BLOCKS.register("metal_case",
            () -> new MetalCaseBlock(BlockBehaviour.Properties.copy(Blocks.DIRT)
                    .strength(.5F, 1.0F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    ));

    public static final RegistryObject<Block> NETHERITE_METAL_CASE = BLOCKS.register("netherite_metal_case",
            () -> new NetheriteCaseBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                    .strength(1.0F, 2.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    ));

    // ---------------------- BLOCK ITEM REGISTRATION --------------
    private static void registerBlockItems(IEventBus bus) {
        ModItems.ITEMS.register(bus);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        registerBlockItems(bus);
    }

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.CLAY_CASE.get());
            event.accept(ModItems.METAL_CASE.get());
            event.accept(ModItems.NETHERITE_METAL_CASE.get());
        }
    }

}

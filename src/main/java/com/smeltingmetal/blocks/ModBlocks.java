package com.smeltingmetal.blocks;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.blocks.blocks.HardenedCaseBlock;
import com.smeltingmetal.blocks.blocks.NetheriteCaseBlock;
import com.smeltingmetal.items.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

    public static final RegistryObject<Block> HARDENED_CASE = BLOCKS.register("hardened_case",
            () -> new HardenedCaseBlock(BlockBehaviour.Properties.copy(Blocks.DIRT)
                    .strength(.5F, 1.0F)
                    .sound(SoundType.DECORATED_POT)
                    .noOcclusion()
                    ));

    public static final RegistryObject<Block> NETHERITE_CASE = BLOCKS.register("netherite_case",
            () -> new NetheriteCaseBlock(BlockBehaviour.Properties.copy(Blocks.NETHERITE_BLOCK)
                    .strength(1.0F, 2.0F)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
                    ));

    @SubscribeEvent
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.CLAY_CASE.get());
            event.accept(ModItems.HARDENED_CASE.get());
            event.accept(ModItems.NETHERITE_CASE.get());
        }
    }

}

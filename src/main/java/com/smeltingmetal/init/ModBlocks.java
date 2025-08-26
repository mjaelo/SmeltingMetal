package com.smeltingmetal.init;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.items.generic.MetalBlockItem;
import com.smeltingmetal.items.mold.BlockMold;
import com.smeltingmetal.items.molten.MoltenMetalBlock;
import com.smeltingmetal.items.molten.MoltenMetalBlockItem;
import com.smeltingmetal.items.raw.RawMetalBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SmeltingMetalMod.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    // Mold block
    public static final RegistryObject<Block> BLOCK_MOLD_CLAY = BLOCKS.register("block_mold_clay",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.CLAY));

    public static final RegistryObject<Item> BLOCK_MOLD_CLAY_ITEM = BLOCK_ITEMS.register("block_mold_clay",
            () -> new MetalBlockItem(BLOCK_MOLD_CLAY.get(), new Item.Properties()));

    public static final RegistryObject<Block> BLOCK_MOLD_HARDENED = BLOCKS.register("block_mold_hardened",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.HARDENED));

    public static final RegistryObject<Item> BLOCK_MOLD_HARDENED_ITEM = BLOCK_ITEMS.register("block_mold_hardened",
            () -> new MetalBlockItem(BLOCK_MOLD_HARDENED.get(), new Item.Properties()));

    public static final RegistryObject<Block> BLOCK_MOLD_NETHERITE = BLOCKS.register("block_mold_netherite",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.NETHERITE));

    public static final RegistryObject<Item> BLOCK_MOLD_NETHERITE_ITEM = BLOCK_ITEMS.register("block_mold_netherite",
            () -> new MetalBlockItem(BLOCK_MOLD_NETHERITE.get(), new Item.Properties()));

    // Molten metal block
    public static final RegistryObject<Block> MOLTEN_METAL_BLOCK = BLOCKS.register("molten_metal_block",
            () -> new MoltenMetalBlock(BlockBehaviour.Properties.of()));

    public static final RegistryObject<Item> MOLTEN_METAL_BLOCK_ITEM = BLOCK_ITEMS.register("molten_metal_block",
            () -> new MoltenMetalBlockItem(MOLTEN_METAL_BLOCK.get(), new Item.Properties().stacksTo(1)));

    // Raw metal block
    public static final RegistryObject<Block> RAW_METAL_BLOCK = BLOCKS.register("raw_metal_block",
            () -> new RawMetalBlock(BlockBehaviour.Properties.of()));

    public static final RegistryObject<Item> RAW_METAL_BLOCK_ITEM = BLOCK_ITEMS.register("raw_metal_block",
            () -> new MetalBlockItem(RAW_METAL_BLOCK.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
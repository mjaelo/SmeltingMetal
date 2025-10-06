package com.smeltingmetal.init;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.objects.mold.BlockMold;
import com.smeltingmetal.objects.mold.BlockMoldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SmeltingMetalMod.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SmeltingMetalMod.MODID);

    public static Map<String, RegistryObject<Item>> BLOCK_MOLDS_CLAY = registerBlockMoldShapes();

    public static Map<String, RegistryObject<Item>> registerBlockMoldShapes() {
        Map<String, RegistryObject<Item>> blockMolds = new HashMap<>();
        for (String shape : ModData.DEFAULT_BLOCK_SHAPES) {
            blockMolds.put(shape, BLOCK_ITEMS.register("block_mold_shape/clay_" + shape,
                    () -> new BlockMoldItem(ModBlocks.BLOCK_MOLD_CLAY.get(), new Item.Properties(), MaterialType.CLAY, shape)));
        }
        return blockMolds;
    }

    // Mold block
    public static final RegistryObject<Block> BLOCK_MOLD_CLAY = BLOCKS.register("block_mold_clay",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.CLAY));

    public static final RegistryObject<Item> BLOCK_MOLD_CLAY_ITEM = BLOCK_MOLDS_CLAY.get(ModData.DEFAULT_BLOCK_SHAPE);

    public static final RegistryObject<Block> BLOCK_MOLD_HARDENED = BLOCKS.register("block_mold_hardened",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.HARDENED));

    public static final RegistryObject<Item> BLOCK_MOLD_HARDENED_ITEM = BLOCK_ITEMS.register("block_mold_hardened",
            () -> new BlockMoldItem(BLOCK_MOLD_HARDENED.get(), new Item.Properties(), MaterialType.HARDENED));

    public static final RegistryObject<Block> BLOCK_MOLD_NETHERITE = BLOCKS.register("block_mold_netherite",
            () -> new BlockMold(BlockBehaviour.Properties.of().strength(2.0f), MaterialType.NETHERITE));

    public static final RegistryObject<Item> BLOCK_MOLD_NETHERITE_ITEM = BLOCK_ITEMS.register("block_mold_netherite",
            () -> new BlockMoldItem(BLOCK_MOLD_NETHERITE.get(), new Item.Properties(), MaterialType.NETHERITE));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
package com.smeltingmetal.events;

import com.smeltingmetal.SmeltingMetalMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    private static final Map<String, Integer> METAL_COLORS = new HashMap<>();

    static {
        // Metal colors for rendering molten metal
        // These could be moved to a config file in the future
        METAL_COLORS.put("iron", 0xC0C0C0);      // Silver
        METAL_COLORS.put("gold", 0xFFD700);      // Gold
        METAL_COLORS.put("copper", 0xB87333);    // Copper
        METAL_COLORS.put("tin", 0xD3D4D5);       // Light gray
        METAL_COLORS.put("bronze", 0xCD7F32);    // Bronze
        METAL_COLORS.put("silver", 0xC0C0C0);    // Silver (same as iron for now)
        METAL_COLORS.put("lead", 0x575D6B);      // Dark gray-blue
        METAL_COLORS.put("nickel", 0xA1B1A8);    // Light gray-green
        METAL_COLORS.put("zinc", 0x7B8C7D);      // Gray-green
        METAL_COLORS.put("brass", 0xE1C16E);     // Gold-yellow
        METAL_COLORS.put("steel", 0x888888);     // Medium gray
        METAL_COLORS.put("electrum", 0xFFFFA0);  // Pale yellow
        METAL_COLORS.put("invar", 0xA0A0A0);     // Light gray
        METAL_COLORS.put("constantan", 0xD99D73); // Copper-brown
        METAL_COLORS.put("signalum", 0xFF7E0A);  // Orange-red
        METAL_COLORS.put("lumium", 0xFFF3B4);    // Pale yellow
        METAL_COLORS.put("enderium", 0x0F696B);  // Teal
        METAL_COLORS.put("aluminum", 0xD6D6D6);  // Light gray
        METAL_COLORS.put("uranium", 0x4E5B43);   // Green-gray
        METAL_COLORS.put("osmium", 0xB5BDC6);    // Light blue-gray
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
//        event.enqueueWork(() -> {
//            // Register item properties for all item molds
//            registerItemMoldProperties(ModItems.ITEM_MOLD_CLAY.get());
//
//            // Register item properties for block molds
//            registerBlockMoldItemProperties(ModBlocks.BLOCK_MOLD_CLAY_ITEM.get());
//
//            // Register block entity renderer for block molds
//            BlockEntityRenderers.register(ModBlockEntities.BLOCK_MOLD_BE.get(), BlockMoldBlockEntityRenderer::new);
//        });
    }

    @SubscribeEvent
    public static void onRegisterColorHandlers(RegisterColorHandlersEvent.Item event) {
        // Color handler for item molds (tint index 1 = molten metal layer)
//        event.getItemColors().register((stack, tintIndex) -> {
//            if (tintIndex == 1) { // Only color the molten metal layer
//                if (stack.hasTag() && stack.getTag().contains("MetalType")) {
//                    String metalType = stack.getTag().getString("MetalType");
//                    return METAL_COLORS.getOrDefault(metalType, 0xFFFFFF);
//                }
//            }
//            return 0xFFFFFF; // Default color (no tint) for other layers
//        }, ModItems.ITEM_MOLD.get());
//
//        // Color handler for block mold items (tint index 1 = molten metal layer)
//        event.getItemColors().register((stack, tintIndex) -> {
//            if (tintIndex == 1) { // Only color the molten metal layer
//                if (stack.hasTag() && stack.getTag().contains("MetalType")) {
//                    String metalType = stack.getTag().getString("MetalType");
//                    return METAL_COLORS.getOrDefault(metalType, 0xFFFFFF);
//                }
//            }
//            return 0xFFFFFF; // Default color (no tint) for other layers
//        }, ModBlocks.BLOCK_MOLD_CLAY_ITEM.get());
    }

    private static void registerItemMoldProperties(Item moldItem) {
//        ItemProperties.register(moldItem,
//                new ResourceLocation(SmeltingMetalMod.MODID, "material_type"),
//                (stack, level, entity, seed) -> {
//                    MaterialType material = stack.getOrCreateTag().contains("MaterialType")
//                            ? MaterialType.valueOf(stack.getOrCreateTag().getString("MaterialType"))
//                            : MaterialType.CLAY;
//                    return material.ordinal();
//                });
//
//        ItemProperties.register(moldItem,
//                new ResourceLocation(SmeltingMetalMod.MODID, "filled"),
//                (stack, level, entity, seed) ->
//                        stack.getOrCreateTag().contains("MetalType") ? 1.0f : 0.0f
//        );
    }

    private static void registerBlockMoldItemProperties(Item moldItem) {
//        ItemProperties.register(moldItem,
//                new ResourceLocation(SmeltingMetalMod.MODID, "material_type"),
//                (stack, level, entity, seed) -> {
//                    MaterialType material = stack.getOrCreateTag().contains("MaterialType")
//                            ? MaterialType.valueOf(stack.getOrCreateTag().getString("MaterialType"))
//                            : MaterialType.CLAY;
//                    return material.ordinal();
//                });
//
//        ItemProperties.register(moldItem,
//                new ResourceLocation(SmeltingMetalMod.MODID, "filled"),
//                (stack, level, entity, seed) ->
//                        stack.getOrCreateTag().contains("MetalType") ? 1.0f : 0.0f
//        );
    }
}

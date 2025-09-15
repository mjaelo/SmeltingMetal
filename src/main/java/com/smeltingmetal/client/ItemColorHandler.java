package com.smeltingmetal.client;

import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.GemProperties;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModBlocks;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.utils.ModUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.smeltingmetal.init.ModData.DEFAULT_COLOR;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ItemColorHandler {
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            GemProperties gemProps = ModUtils.getGemPropertiesFromStack(stack);
            return tintIndex == 0 && gemProps != null
                    ? gemProps.color()
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModItems.GEM_DUST_ITEM.get());
        event.register((stack, tintIndex) -> {
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(stack);
            return tintIndex == 0 && metalProps != null
                    ? metalProps.color()
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModItems.MOLTEN_METAL_ITEM.get());
        event.register((stack, tintIndex) -> {
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(stack);
            return tintIndex == 0 && metalProps != null
                    ? metalProps.color()
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModBlocks.MOLTEN_METAL_BLOCK.get());
        event.register((stack, tintIndex) -> {
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(stack);
            return tintIndex == 1 && metalProps != null
                    ? metalProps.color()
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModItems.MOLTEN_METAL_BUCKET.get());
        event.register((stack, tintIndex) -> {
            GemProperties gemProps = ModUtils.getGemPropertiesFromStack(stack);
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(stack);
            return tintIndex == 1 && (gemProps != null || metalProps != null)
                    ? (gemProps != null ? gemProps.color() : metalProps.color())
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModItems.ITEM_MOLD_NETHERITE.get());
        event.register((stack, tintIndex) -> {
            GemProperties gemProps = ModUtils.getGemPropertiesFromStack(stack);
            MetalProperties metalProps = ModUtils.getMetalPropertiesFromStack(stack);
            return tintIndex == 1 && (gemProps != null || metalProps != null)
                    ? (gemProps != null ? gemProps.color() : metalProps.color())
                    : DEFAULT_COLOR; // Default white (no tint) for other layers
        }, ModItems.ITEM_MOLD_HARDENED.get());

    }
}

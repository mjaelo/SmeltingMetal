package com.smeltingmetal.events;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModMetals;
import com.smeltingmetal.items.ServerEventsUtils;
import com.smeltingmetal.recipes.RecipeProcessor;
import com.smeltingmetal.recipes.RecipeReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModMetals::init);
    }

    @Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            MinecraftServer server = event.getServer();
            SmeltingMetalMod.setServer(server);
            SmeltingMetalMod.setRecipeManager(server.getRecipeManager());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onServerStarted(ServerStartedEvent event) {
            var server = event.getServer();
            SmeltingMetalMod.setServer(server);
            SmeltingMetalMod.setRecipeManager(server.getRecipeManager());
            RecipeProcessor.process(server.getRecipeManager(), server.registryAccess());
        }

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            LOGGER.info("Registering recipe reload listener …");
            event.addListener(new RecipeReloadListener(event.getRegistryAccess()));
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.@NotNull RightClickBlock event) {
            ItemStack heldItemStack = event.getItemStack();
            ServerEventsUtils.handleIteractionCooling(event, heldItemStack);
        }

        @SubscribeEvent
        public static void onItemPickup(EntityItemPickupEvent event) {
            Player player = event.getEntity();
            ItemEntity itemEntity = event.getItem();
            ItemStack itemStack = itemEntity.getItem();
            Class<?> containerClass = ServerEventsUtils.getContainerClass(itemStack);
            if (containerClass == null) return; // not molten item

            List<ItemStack> containerStacks = ServerEventsUtils.findInInventory(player, containerClass);
            String metalType = ModMetals.getMetalTypeFromStack(itemStack);
            MetalProperties metalProps = ModMetals.getMetalProperties(metalType);

            if (containerStacks.isEmpty() || metalProps == null) {
                player.hurt(player.damageSources().lava(), 4f);
            } else {
                ServerEventsUtils.fillContainer(metalProps, player, containerStacks.get(0));
                itemEntity.discard(); // remove ground item
            }
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onContainerClose(PlayerContainerEvent.Close event) {
            ServerEventsUtils.handleInventoryMoltenMetal(event.getEntity());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onDepotInteract(PlayerInteractEvent.RightClickBlock event) {
            String createName = "create";
            if (!ModList.get().isLoaded(createName)) return;
            Level level = event.getLevel();
            if (level.isClientSide()) return;

            ResourceLocation clickedBlockId = ForgeRegistries.BLOCKS.getKey(level.getBlockState(event.getPos()).getBlock());
            if (clickedBlockId != null && createName.equals(clickedBlockId.getNamespace()) && "depot".equals(clickedBlockId.getPath())) {
                MinecraftServer server = level.getServer();
                if (server != null) { // add delay to execute after create processes
                    server.tell(new TickTask(server.getTickCount() + 1, () -> ServerEventsUtils.handleInventoryMoltenMetal(event.getEntity())));
                }
            }
        }

    }
}
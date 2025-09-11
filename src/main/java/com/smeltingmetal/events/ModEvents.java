package com.smeltingmetal.events;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.SmeltingMetalMod;
import com.smeltingmetal.config.ModConfig;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.init.ModItems;
import com.smeltingmetal.recipes.RecipeProcessor;
import com.smeltingmetal.recipes.RecipeReloadListener;
import com.smeltingmetal.utils.ModUtils;
import com.smeltingmetal.utils.ServerEventsUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SmeltingMetalMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Track the last time a mold transfer was performed (tick time)
    private static final Map<UUID, Long> lastTransferTimes = new HashMap<>();
    private static final int TRANSFER_COOLDOWN = 5; // 5 ticks cooldown

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModData::init);
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
            String metalType = ModUtils.getContentFromStack(itemStack);
            MetalProperties metalProps = ModUtils.getMetalProperties(metalType);

            if (containerStacks.isEmpty() || metalProps == null) {
                player.hurt(player.damageSources().lava(), 4f);
            } else {
                ServerEventsUtils.fillContainer(metalProps, player, containerStacks.get(0));
                itemEntity.discard(); // remove ground item
            }
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRightClickWithMolds(PlayerInteractEvent.RightClickItem event) {
            if (event.getLevel().isClientSide()) return;

            Player player = event.getEntity();
            long currentTick = player.level().getGameTime();

            // Check cooldown to prevent multiple triggers
            Long lastTransfer = lastTransferTimes.get(player.getUUID());
            if (lastTransfer != null && currentTick - lastTransfer < TRANSFER_COOLDOWN) {
                return;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            if (!ServerEventsUtils.pourContentBetweenItemMolds(mainHand, offHand, player)
                    && !ServerEventsUtils.putGemDustIntoMold(mainHand, offHand, player)
                    && !ServerEventsUtils.printItemIntoItemMold(mainHand, offHand, player)
                    && !ServerEventsUtils.printItemIntoBlockMold(mainHand, offHand, player))
                return;

            // Update the cooldown
            lastTransferTimes.put(player.getUUID(), currentTick);

            // Cancel the event to prevent any other interactions
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onContainerClose(PlayerContainerEvent.Close event) {
            ServerEventsUtils.checkInventoryForMoltenMetal(event.getEntity());
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
                    server.tell(new TickTask(server.getTickCount() + 1, () -> ServerEventsUtils.checkInventoryForMoltenMetal(event.getEntity())));
                }
            }
        }

        @SubscribeEvent
        public static void onGrindstoneRightClick(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide()) return;
            if (event.getLevel().getBlockState(event.getPos()).is(Blocks.GRINDSTONE)) {
                Player player = event.getEntity();
                ItemStack mainHandItem = player.getMainHandItem();

                String itemName = mainHandItem.getItem().getDescriptionId();
                String gemName = ModData.getGemPropertiesMap().keySet().stream()
                        .filter(itemName::contains)
                        .findFirst()
                        .orElse(null);
                if (gemName != null) {
                    boolean isBlock = ModConfig.CONFIG.blockKeywords.get().stream().anyMatch(itemName::contains);
                    mainHandItem.shrink(1);
                    ItemStack dustStack = new ItemStack(ModItems.GEM_DUST_ITEM.get(), isBlock ? 9 : 1);
                    ModUtils.setContentToStack(dustStack, gemName);
                    if (!player.addItem(dustStack)) {
                        player.drop(dustStack, false);
                    }
                    event.setCanceled(true);
                }
            }
        }

    }
}
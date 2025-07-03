package com.smeltingmetal;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.items.FilledMoldItem;
import com.smeltingmetal.items.MoltenMetalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            Level level = event.getLevel();
            BlockPos pos = event.getPos().above();
            Player player = event.getEntity();
            ItemStack heldItem = event.getItemStack();

            if (level.isClientSide()) {
                return;
            }

            boolean isWaterBlock = level.getBlockState(pos).is(Blocks.WATER);
            boolean isWaterFluid = level.getFluidState(pos).is(FluidTags.WATER);

            if (heldItem.getItem() instanceof FilledMoldItem && (isWaterBlock || isWaterFluid)) {
                String metalType = FilledMoldItem.getMetalType(heldItem);

                // Fetch ingot ResourceLocation from ModMetals
                Optional<MetalProperties> metalProps = ModMetals.getMetalProperties(metalType);

                if (metalProps.isPresent()) {
                    Item ingotItem = ForgeRegistries.ITEMS.getValue(metalProps.get().ingotId());

                    if (ingotItem != null && ingotItem != Items.AIR) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            if (!serverPlayer.getAbilities().instabuild) {
                                heldItem.shrink(1);
                            }

                            if (!serverPlayer.getInventory().add(new ItemStack(ingotItem))) {
                                serverPlayer.drop(new ItemStack(ingotItem), false);
                            }

                            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.5F + level.random.nextFloat() * 0.5F);

                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.5, 0.1, 0.5, 0.1);
                            }
                        }
                    } else {
                        LOGGER.error("SmeltingMetalMod: Could not find ingot item for metal type: {} (ResourceLocation: {})", metalType, metalProps.get().ingotId());
                    }
                } else {
                    LOGGER.warn("SmeltingMetalMod: Unrecognized or unregistered metal type in FilledMold: {}", metalType);
                }
                event.setCanceled(true);
                event.setResult(Event.Result.ALLOW);
            }
        }

        /*
         * Utility: handle molten metal in player's inventory.
         * Converts using hardened molds, otherwise drops and damages.
         */
        private static void processMoltenMetal(Player player) {
            Level level = player.level();
            if (level.isClientSide()) return;

            int moldCount = 0;
            List<Integer> moltenSlots = new ArrayList<>();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == ModItems.HARDENED_MOLD.get()) {
                    moldCount += stack.getCount();
                } else if (stack.getItem() instanceof MoltenMetalItem) {
                    moltenSlots.add(i);
                }
            }

            if (moltenSlots.isEmpty()) return;

            // Convert as many as possible
            if (moldCount > 0) {
                for (int slot : moltenSlots) {
                    if (moldCount <= 0) break;
                    ItemStack molten = player.getInventory().getItem(slot);
                    if (molten.isEmpty()) continue;

                    String metalType = MoltenMetalItem.getMetalId(molten);
                    if (metalType == null || !ModMetals.doesMetalExist(metalType)) continue;

                    int toConvert = Math.min(moldCount, molten.getCount());

                    if (!player.getAbilities().instabuild) {
                        molten.shrink(toConvert);
                        int shrinkLeft = toConvert;
                        // shrink molds across inventory
                        for (int i = 0; i < player.getInventory().getContainerSize() && shrinkLeft > 0; i++) {
                            ItemStack moldStack = player.getInventory().getItem(i);
                            if (moldStack.getItem() == ModItems.HARDENED_MOLD.get() && !moldStack.isEmpty()) {
                                int s = Math.min(shrinkLeft, moldStack.getCount());
                                moldStack.shrink(s);
                                shrinkLeft -= s;
                            }
                        }
                    }

                    ItemStack filled = FilledMoldItem.createFilledMold(metalType);
                    filled.setCount(toConvert);
                    if (!player.getInventory().add(filled)) {
                        player.drop(filled, false);
                    }

                    moldCount -= toConvert;
                }
            }

            // Drop any remaining molten metal and damage
            boolean moltenLeft = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof MoltenMetalItem) {
                    moltenLeft = true;
                    ItemStack copy = stack.copy();
                    player.getInventory().removeItem(stack);
                    player.drop(copy, false);
                }
            }

            if (moltenLeft) {
                player.hurt(level.damageSources().lava(), 4.0F);
            }
        }

        @SubscribeEvent
        public static void onItemPickup(EntityItemPickupEvent event) {
            ItemEntity itemEntity = event.getItem();
            ItemStack pickedStack = itemEntity.getItem();

            if (!(pickedStack.getItem() instanceof MoltenMetalItem)) {
                return; // Not molten metal, let vanilla handle
            }

            Player player = event.getEntity();

            // Count hardened molds in player inventory
            int moldCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (!slot.isEmpty() && slot.getItem() == ModItems.HARDENED_MOLD.get()) {
                    moldCount += slot.getCount();
                }
            }

            if (moldCount > 0) {
                // Convert up to mold count
                String metalType = MoltenMetalItem.getMetalId(pickedStack);
                if (metalType != null && ModMetals.doesMetalExist(metalType)) {
                    int convert = Math.min(moldCount, pickedStack.getCount());

                    // Shrink molds
                    if (!player.getAbilities().instabuild) {
                        int shrinkLeft = convert;
                        for (int i = 0; i < player.getInventory().getContainerSize() && shrinkLeft > 0; i++) {
                            ItemStack moldStack = player.getInventory().getItem(i);
                            if (moldStack.getItem() == ModItems.HARDENED_MOLD.get()) {
                                int s = Math.min(shrinkLeft, moldStack.getCount());
                                moldStack.shrink(s);
                                shrinkLeft -= s;
                            }
                        }
                    }

                    // Give filled molds
                    ItemStack filled = FilledMoldItem.createFilledMold(metalType);
                    filled.setCount(convert);
                    if (!player.getInventory().add(filled)) {
                        player.drop(filled, false);
                    }

                    // Reduce or remove picked stack
                    if (pickedStack.getCount() > convert) {
                        pickedStack.shrink(convert);
                        // leave remaining molten metal entity in world with pickup delay
                        itemEntity.setPickUpDelay(40);
                    } else {
                        itemEntity.discard();
                    }

                    // Cancel default pickup handling
                    event.setCanceled(true);
                }
            } else {
                // No molds – damage player and keep item in world
                player.hurt(player.level().damageSources().lava(), 4.0F);
                itemEntity.setPickUpDelay(40);
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void onContainerClose(PlayerContainerEvent.Close event) {
            processMoltenMetal(event.getEntity());
        }
    }
}
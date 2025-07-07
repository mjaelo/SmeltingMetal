package com.smeltingmetal;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.items.FilledMoldItem;
import com.smeltingmetal.items.FilledNetheriteMoldItem;
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
import org.jetbrains.annotations.NotNull;
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
        public static void onRightClickBlock(PlayerInteractEvent.@NotNull RightClickBlock event) {
            ItemStack heldItem = event.getItemStack();
            Level level = event.getLevel();
            boolean isFilledMold = heldItem.getItem() == ModItems.FILLED_MOLD.get();
            boolean isFilledNetherite = heldItem.getItem() == ModItems.FILLED_NETHERITE_MOLD.get();

            if (level.isClientSide() || (!isFilledMold && !isFilledNetherite)) {
                return;
            }

            BlockPos pos = event.getPos().above();
            Player player = event.getEntity();

            boolean isWaterBlock = level.getBlockState(pos).is(Blocks.WATER);
            boolean isWaterFluid = level.getFluidState(pos).is(FluidTags.WATER);

            if (!isWaterBlock && !isWaterFluid) {
                return;
            }
            String metalType = isFilledMold ? FilledMoldItem.getMetalType(heldItem) : com.smeltingmetal.items.FilledNetheriteMoldItem.getMetalType(heldItem);
            Optional<MetalProperties> metalProps = ModMetals.getMetalProperties(metalType);

            if (metalProps.isPresent()) {
                Item ingotItem = ForgeRegistries.ITEMS.getValue(metalProps.get().ingotId());

                if (ingotItem != null && ingotItem != Items.AIR) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (!serverPlayer.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }

                        // give ingot
                        if (!serverPlayer.getInventory().add(new ItemStack(ingotItem))) {
                            serverPlayer.drop(new ItemStack(ingotItem), false);
                        }

                        // if netherite variant, also give back empty mold
                        if (isFilledNetherite) {
                            ItemStack moldStack = new ItemStack(ModItems.NETHERITE_MOLD.get());
                            if (!serverPlayer.getInventory().add(moldStack)) {
                                serverPlayer.drop(moldStack, false);
                            }
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
                if (!slot.isEmpty() && (slot.getItem() == ModItems.HARDENED_MOLD.get() || slot.getItem() == ModItems.NETHERITE_MOLD.get())) {
                    moldCount += slot.getCount();
                }
            }

            if (moldCount > 0) {
                // Convert up to mold count
                String metalType = MoltenMetalItem.getMetalId(pickedStack);
                if (metalType != null && ModMetals.doesMetalExist(metalType)) {
                    int convert = Math.min(moldCount, pickedStack.getCount());

                    int shrinkLeft = convert;
                    int netheriteUsed = 0;
                    for (int i = 0; i < player.getInventory().getContainerSize() && shrinkLeft > 0; i++) {
                        ItemStack moldStack = player.getInventory().getItem(i);
                        if (moldStack.isEmpty()) continue;
                        if (moldStack.getItem() == ModItems.NETHERITE_MOLD.get()) {
                            int s = Math.min(shrinkLeft, moldStack.getCount());
                            if (!player.getAbilities().instabuild) {
                                moldStack.shrink(s);
                            }
                            shrinkLeft -= s;
                            netheriteUsed += s;
                        } else if (moldStack.getItem() == ModItems.HARDENED_MOLD.get()) {
                            int s = Math.min(shrinkLeft, moldStack.getCount());
                            if (!player.getAbilities().instabuild) {
                                moldStack.shrink(s);
                            }
                            shrinkLeft -= s;
                        }
                    }

                    // Give filled molds according to mold type used
                    if (netheriteUsed > 0) {
                        ItemStack filledNeth = FilledNetheriteMoldItem.createFilled(metalType);
                        filledNeth.setCount(netheriteUsed);
                        if (!player.getInventory().add(filledNeth)) {
                            player.drop(filledNeth, false);
                        }
                    }
                    int hardenedUsed = convert - netheriteUsed;
                    if (hardenedUsed > 0) {
                        ItemStack filledHard = FilledMoldItem.createFilledMold(metalType);
                        filledHard.setCount(hardenedUsed);
                        if (!player.getInventory().add(filledHard)) {
                            player.drop(filledHard, false);
                        }
                    }

                    // Reduce or remove picked stack
                    if (pickedStack.getCount() > convert) {
                        pickedStack.shrink(convert);
                        itemEntity.setPickUpDelay(40);
                    } else {
                        itemEntity.discard();
                    }

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
            Player player = event.getEntity();
            Level level = player.level();
            if (level.isClientSide()) return;

            int moldCount = 0;
            List<Integer> moltenSlots = new ArrayList<>();

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                if (stack.getItem() == ModItems.HARDENED_MOLD.get() || stack.getItem() == ModItems.NETHERITE_MOLD.get()) {
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

                    molten.shrink(toConvert);
                    int shrinkLeft2 = toConvert;
                    int netheriteUsed2 = 0;
                    for (int i = 0; i < player.getInventory().getContainerSize() && shrinkLeft2 > 0; i++) {
                        ItemStack moldStack = player.getInventory().getItem(i);
                        if (moldStack.isEmpty()) continue;
                        if (moldStack.getItem() == ModItems.NETHERITE_MOLD.get()) {
                            int s = Math.min(shrinkLeft2, moldStack.getCount());
                            if (!player.getAbilities().instabuild) {
                                moldStack.shrink(s);
                            }
                            shrinkLeft2 -= s;
                            netheriteUsed2 += s;
                        } else if (moldStack.getItem() == ModItems.HARDENED_MOLD.get()) {
                            int s = Math.min(shrinkLeft2, moldStack.getCount());
                            if (!player.getAbilities().instabuild) {
                                moldStack.shrink(s);
                            }
                            shrinkLeft2 -= s;
                        }
                    }

                    if (netheriteUsed2 > 0) {
                        ItemStack filledNeth = FilledNetheriteMoldItem.createFilled(metalType);
                        filledNeth.setCount(netheriteUsed2);
                        if (!player.getInventory().add(filledNeth)) {
                            player.drop(filledNeth, false);
                        }
                    }
                    int hardenedUsed2 = toConvert - netheriteUsed2;
                    if (hardenedUsed2 > 0) {
                        ItemStack filledHard = FilledMoldItem.createFilledMold(metalType);
                        filledHard.setCount(hardenedUsed2);
                        if (!player.getInventory().add(filledHard)) {
                            player.drop(filledHard, false);
                        }
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

    }
}
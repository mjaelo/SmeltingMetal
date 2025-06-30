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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

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
        public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
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

        @SubscribeEvent
        public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
            Level level = event.getLevel();
            Player player = event.getEntity();
            InteractionHand hand = event.getHand();

            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            if (level.isClientSide()) {
                return;
            }

            ItemStack moldStack = ItemStack.EMPTY;
            ItemStack moltenMetalStack = ItemStack.EMPTY;

            if (mainHandItem.getItem() == ModItems.HARDENED_MOLD.get() && offHandItem.getItem() instanceof MoltenMetalItem) {
                moldStack = mainHandItem;
                moltenMetalStack = offHandItem;
            } else if (mainHandItem.getItem() instanceof MoltenMetalItem && offHandItem.getItem() == ModItems.HARDENED_MOLD.get()) {
                moldStack = offHandItem;
                moltenMetalStack = mainHandItem;
            } else {
                return;
            }

            if (!moldStack.isEmpty() && !moltenMetalStack.isEmpty()) {
                String metalType = MoltenMetalItem.getMetalId(moltenMetalStack);

                // Optional: You could add a check here to ensure the metalType is known by ModMetals
                if (ModMetals.doesMetalExist(metalType) && !"unknown".equals(metalType)) {
                    if (!player.getAbilities().instabuild) {
                        moldStack.shrink(1);
                        moltenMetalStack.shrink(1);
                    }

                    ItemStack filledMold = FilledMoldItem.createFilledMold(metalType);

                    if (!player.getInventory().add(filledMold)) {
                        player.drop(filledMold, false);
                    }

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 1.0F, 1.0F);

                    event.setCanceled(true);
                    event.setResult(Event.Result.ALLOW);
                } else {
                    LOGGER.warn("SmeltingMetalMod: Attempted to fill mold with unrecognized or unknown molten metal type: {}", metalType);
                }
            }
        }
    }
}
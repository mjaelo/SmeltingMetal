package com.smeltingmetal;

import com.mojang.logging.LogUtils;
import com.smeltingmetal.data.MetalProperties;
import com.smeltingmetal.data.ModMetals;
import com.smeltingmetal.items.ModItems;
import com.smeltingmetal.items.metalBlock.MoltenMetalBlockItem;
import com.smeltingmetal.items.metalBlock.MoltenMetalBucketItem;
import com.smeltingmetal.items.metalIngot.FilledMoldItem;
import com.smeltingmetal.items.metalIngot.FilledNetheriteMoldItem;
import com.smeltingmetal.items.metalIngot.MoltenMetalItem;
import com.smeltingmetal.recipes.replacer.RecipeProcessor;
import com.smeltingmetal.recipes.replacer.RecipeReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

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
        public static void onServerStarted(ServerStartedEvent event) {
            var server = event.getServer();
            SmeltingMetalMod.setServer(server);
            SmeltingMetalMod.setRecipeManager(server.getRecipeManager());
            RecipeProcessor.process(server.getRecipeManager(), server.registryAccess());
        }

        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            // Register our recipe reload listener so recipe modifications run on every datapack reload.
            LOGGER.info("Registering recipe reload listener …");
            event.addListener(new RecipeReloadListener(event.getRegistryAccess()));
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.@NotNull RightClickBlock event) {
            // Check if the item is a filled mold
            ItemStack heldItem = event.getItemStack();
            boolean isFilledMold = heldItem.getItem() == ModItems.FILLED_MOLD.get();
            boolean isFilledNetherite = heldItem.getItem() == ModItems.FILLED_NETHERITE_MOLD.get();

            Level level = event.getLevel();
            if (!isFilledMold && !isFilledNetherite) {
                return;
            }

            // Check if the block is water
            BlockPos pos = event.getPos().above();
            boolean isWaterBlock = level.getBlockState(pos).is(Blocks.WATER);
            boolean isWaterFluid = level.getFluidState(pos).is(FluidTags.WATER);

            if (!isWaterBlock && !isWaterFluid) {
                return;
            }

            // Get metal properties from mold
            String metalType = isFilledMold ? FilledMoldItem.getMetalType(heldItem) : FilledNetheriteMoldItem.getMetalType(heldItem);
            Optional<MetalProperties> metalProps = ModMetals.getMetalProperties(metalType);

            if (metalProps.isPresent()) {
                Item ingotItem = ForgeRegistries.ITEMS.getValue(metalProps.get().ingotId());
                if (ingotItem != null && ingotItem != Items.AIR) {
                    Player serverPlayer = event.getEntity();
                    // consume mold
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

            boolean isMoltenMetalBlock = pickedStack.getItem() instanceof MoltenMetalBlockItem;
            boolean isMoltenMetalItem = pickedStack.getItem() instanceof MoltenMetalItem;
            if (!isMoltenMetalBlock && !isMoltenMetalItem) return;

            // Search inventory for first required item
            Player player = event.getEntity();
            ItemStack metalContainerStack = findRequiredItem(isMoltenMetalBlock, player);

            // If no required item found, hurt player and keep on ground (can't pick up)
            if (metalContainerStack.isEmpty()) {
                player.hurt(player.damageSources().lava(), 4f);
            }

            // If item found, try to create new filled item
            else {
                createNewFilledItem(isMoltenMetalBlock, pickedStack, metalContainerStack, player);
                itemEntity.discard(); // remove ground item
            }

            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onContainerClose(PlayerContainerEvent.Close event) {
            Player player = event.getEntity();
            Level level = player.level();
            if (level.isClientSide()) return;

            for (ItemStack stack : player.getInventory().items) {
                boolean isMoltenMetalBlock = stack.getItem() instanceof MoltenMetalBlockItem;
                boolean isMoltenMetalItem = stack.getItem() instanceof MoltenMetalItem;
                if (!isMoltenMetalBlock && !isMoltenMetalItem) continue;

                // Search inventory for first required item
                ItemStack metalContainerStack = findRequiredItem(isMoltenMetalBlock, player);

                // If no required item found, hurt player and drop the molten item on the ground
                if (metalContainerStack.isEmpty()) {
                    player.hurt(player.damageSources().lava(), 4f);

                    ItemEntity item = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack.copy());
                    item.setNoPickUpDelay();
                    level.addFreshEntity(item);
                    player.getInventory().removeItem(stack);
                }

                // If item found, try to create new filled item
                else {
                    createNewFilledItem(isMoltenMetalBlock, stack, metalContainerStack, player);
                    stack.shrink(1);
                }
            }
        }

        private static void createNewFilledItem(boolean isMoltenMetalBlock, ItemStack moltenItemStack, ItemStack metalContainerStack, Player player) {
            String metalId = isMoltenMetalBlock ? MoltenMetalBlockItem.getMetalId(moltenItemStack) : MoltenMetalItem.getMetalId(moltenItemStack);
            if (metalId != null) {
                // create new item stack
                ItemStack newItemStack = isMoltenMetalBlock
                        ? MoltenMetalBucketItem.createStack(metalId)
                        : metalContainerStack.getItem() == ModItems.HARDENED_MOLD.get()
                        ? FilledMoldItem.createFilledMold(metalId)
                        : FilledNetheriteMoldItem.createFilled(metalId);

                newItemStack.setCount(1);
                player.getInventory().add(newItemStack);

                // consume required item and remove ground item
                metalContainerStack.shrink(1);
            }
        }

        private static @NotNull ItemStack findRequiredItem(boolean isMoltenMetalBlock, Player player) {
            ItemStack metalContainerStack = ItemStack.EMPTY;
            List<Item> requiredItems = isMoltenMetalBlock ? List.of(Items.BUCKET) : List.of(ModItems.HARDENED_MOLD.get(), ModItems.NETHERITE_MOLD.get());
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (!slot.isEmpty() && requiredItems.contains(slot.getItem())) {
                    metalContainerStack = slot;
                    break;
                }
            }
            return metalContainerStack;
        }
    }
}
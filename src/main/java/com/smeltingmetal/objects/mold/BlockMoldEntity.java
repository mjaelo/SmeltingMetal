package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModBlockEntities;
import com.smeltingmetal.init.ModMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMoldEntity extends BlockEntity {
    private String shapeType = "";
    private String metalType = "";

    public BlockMoldEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.BLOCK_MOLD_BE.get(), pPos, pBlockState);
    }

    // This constructor is required by Forge
    public BlockMoldEntity(BlockEntityType<?> type, BlockPos pPos, BlockState pBlockState) {
        super(type, pPos, pBlockState);
    }

    public MaterialType getMaterialType() {
        if (getBlockState().getBlock() instanceof BlockMold blockMold) {
            return blockMold.getMaterialType();
        }
        return MaterialType.CLAY; // Default fallback
    }

    public boolean hasMetal() {
        return !metalType.isEmpty();
    }

    public String getMetalType() {
        return metalType;
    }

    public String getShapeType() {
        return shapeType;
    }
    
    public int getShapeIndex() {
        return switch (shapeType) {
            case "helmet" -> 1;
            case "chestplate" -> 2;
            case "leggings" -> 3;
            case "boots" -> 4;
            default -> 0; // block
        };
    }
    
    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void fill(String metalType) {
        this.metalType = metalType;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void removeMetal() {
        this.metalType = "";
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(ModMetals.METAL_KEY, metalType);
        tag.putString(ModMetals.SHAPE_KEY, shapeType);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.metalType = tag.getString(ModMetals.METAL_KEY);
        this.shapeType = tag.contains(ModMetals.SHAPE_KEY) ? tag.getString(ModMetals.SHAPE_KEY) : "";
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

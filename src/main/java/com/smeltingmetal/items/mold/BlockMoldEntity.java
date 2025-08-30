package com.smeltingmetal.items.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMoldEntity extends BlockEntity {
    private final String resultType = "block";
    private String metalType = "";
    private boolean isFilled = false;
    
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
    
    public boolean isFilled() {
        return isFilled;
    }
    
    public String getMetalType() {
        return metalType;
    }
    
    public void fill(String metalType) {
        this.metalType = metalType;
        this.isFilled = true;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            if (!state.getValue(BlockMold.FILLED)) {
                level.setBlock(worldPosition, state.setValue(BlockMold.FILLED, true), 3);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    }
    
    public void empty() {
        this.metalType = "";
        this.isFilled = false;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            if (state.getValue(BlockMold.FILLED)) {
                level.setBlock(worldPosition, state.setValue(BlockMold.FILLED, false), 3);
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("MetalType", metalType);
        tag.putBoolean("IsFilled", isFilled);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.metalType = tag.getString("MetalType");
        this.isFilled = tag.getBoolean("IsFilled");
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

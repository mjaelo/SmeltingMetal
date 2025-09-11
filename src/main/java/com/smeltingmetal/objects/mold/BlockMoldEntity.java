package com.smeltingmetal.objects.mold;

import com.smeltingmetal.data.MaterialType;
import com.smeltingmetal.init.ModBlockEntities;
import com.smeltingmetal.init.ModData;
import com.smeltingmetal.utils.ModUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMoldEntity extends BlockEntity {
    private String shape = "";
    private String content = "";

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

    public String getShape() {
        return shape;
    }
    
    public void setShape(String shape) {
        this.shape = shape;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getContent() {
        return content;
    }

    public int getContentType() {
        return ModUtils.getContentId(content);
    }

    public void setContent(String content) {
        this.content = content;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void removeContent() {
        this.content = "";
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }


    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(ModData.CONTENT_KEY, content);
        tag.putString(ModData.SHAPE_KEY, shape);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.content = tag.getString(ModData.CONTENT_KEY);
        this.shape = tag.contains(ModData.SHAPE_KEY) ? tag.getString(ModData.SHAPE_KEY) : "";
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

    public boolean hasContent() {
        return !content.isEmpty();
    }
}

package com.smeltingmetal.blocks;

/**
 * Thin wrapper that sets returnsCase=true so that the block entity gives the empty case back.
 */
public class NetheriteCaseBlock extends MetalCaseBlock {

    public NetheriteCaseBlock(Properties props) {
        super(props, true);
    }
}

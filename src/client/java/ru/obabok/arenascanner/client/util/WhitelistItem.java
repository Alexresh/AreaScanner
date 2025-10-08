package ru.obabok.arenascanner.client.util;

import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

public class WhitelistItem {
    public Block block;
    public String waterlogged;
    public String blastResistance;
    public String pistonBehavior;


    public WhitelistItem(@Nullable Block _block, @Nullable String _waterlogged, @Nullable String _blastResistance, @Nullable String _pistonBehavior){
        if(_block != null){
            this.block = _block;
        }
        if(_waterlogged != null){
            this.waterlogged = _waterlogged;
        }
        if(_blastResistance != null){
            this.blastResistance = _blastResistance;
        }
        if(_pistonBehavior != null){
            this.pistonBehavior = _pistonBehavior;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        if(block != null){
            builder.append("block:").append(block);
        }
        builder.append(" or ");
        if(waterlogged != null){
            builder.append("waterlogged:").append(waterlogged);
        }
        builder.append(" or ");
        if(blastResistance != null){
            builder.append("blastResistance:").append(blastResistance);
        }
        builder.append(" or ");
        if(pistonBehavior != null){
            builder.append("pistonBehavior:").append(pistonBehavior);
        }
        builder.append("]");
        return builder.toString();
    }
}

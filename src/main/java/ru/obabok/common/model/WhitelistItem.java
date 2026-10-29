package ru.obabok.common.model;


import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.StringJoiner;

public class WhitelistItem {
    public String block;
    public String waterlogged;
    public String blastResistance;
    public String pistonBehavior;


    public WhitelistItem(@Nullable String _block, @Nullable String _waterlogged, @Nullable String _blastResistance, @Nullable String _pistonBehavior){
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
        String nl = System.lineSeparator();

        StringJoiner joiner = new StringJoiner(
                "," + nl + "  ",
                "{" + nl + "  ",
                nl + "}"
        ).setEmptyValue("{}");

        if (block != null) {
            joiner.add("block: " + block);
        }

        if (waterlogged != null) {
            joiner.add("waterlogged: " + waterlogged);
        }

        if (pistonBehavior != null) {
            joiner.add("pistonBehavior: " + pistonBehavior);
        }

        if (blastResistance != null) {
            joiner.add("blastResistance: " + blastResistance);
        }

        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistItem that = (WhitelistItem) o;
        return Objects.equals(block, that.block) &&
                Objects.equals(waterlogged, that.waterlogged) &&
                Objects.equals(blastResistance, that.blastResistance) &&
                Objects.equals(pistonBehavior, that.pistonBehavior);
    }
}

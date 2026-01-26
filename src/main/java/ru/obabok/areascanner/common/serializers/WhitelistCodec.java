package ru.obabok.areascanner.common.serializers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import ru.obabok.areascanner.common.model.Whitelist;

public final class WhitelistCodec {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Block.class, new BlockSerializer())
            .setPrettyPrinting()
            .create();

    private WhitelistCodec() {
    }

    public static Whitelist fromJson(String json) {
        try {
            return GSON.fromJson(json, Whitelist.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String toJson(Whitelist whitelist) {
        return GSON.toJson(whitelist);
    }
}

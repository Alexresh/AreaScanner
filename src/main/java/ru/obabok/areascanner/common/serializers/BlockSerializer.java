package ru.obabok.areascanner.common.serializers;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class BlockSerializer implements JsonSerializer<Block>, JsonDeserializer<Block> {
    @Override
    public JsonElement serialize(Block src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(Registries.BLOCK.getId(src).toString());
    }

    @Override
    public Block deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException{
        Identifier id = Identifier.of(json.getAsString());
        return Registries.BLOCK.get(id);
    }
}

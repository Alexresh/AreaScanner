package ru.obabok.arenascanner.client.serializes;

import com.google.gson.*;
import net.minecraft.util.math.BlockBox;

import java.lang.reflect.Type;

public class BlockBoxSerializer implements JsonSerializer<BlockBox>, JsonDeserializer<BlockBox> {
    @Override
    public JsonElement serialize(BlockBox src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("minX", src.getMinX());
        obj.addProperty("minY", src.getMinY());
        obj.addProperty("minZ", src.getMinZ());
        obj.addProperty("maxX", src.getMaxX());
        obj.addProperty("maxY", src.getMaxY());
        obj.addProperty("maxZ", src.getMaxZ());
        return obj;
    }


    @Override
    public BlockBox deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException{
        JsonObject obj = json.getAsJsonObject();
        return new BlockBox(
                obj.get("minX").getAsInt(),
                obj.get("minY").getAsInt(),
                obj.get("minZ").getAsInt(),
                obj.get("maxX").getAsInt(),
                obj.get("maxY").getAsInt(),
                obj.get("maxZ").getAsInt()
        );
    }
}

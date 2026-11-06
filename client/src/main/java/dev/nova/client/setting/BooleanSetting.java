package dev.nova.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {

    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
            value = json.getAsBoolean();
        }
    }
}

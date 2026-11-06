package dev.nova.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.MathHelper;

public class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, double defaultValue, double min, double max, double step) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double newValue) {
        double clamped = MathHelper.clamp(newValue, min, max);
        if (step > 0) {
            clamped = Math.round((clamped - min) / step) * step + min;
            clamped = MathHelper.clamp(clamped, min, max);
        }
        this.value = clamped;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    /** Wert als Text – ohne Nachkommastellen, wenn die Schrittweite ganzzahlig ist. */
    public String format() {
        return step >= 1 && step % 1 == 0
                ? String.valueOf(Math.round(value))
                : String.format("%.2f", value);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement json) {
        if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
            set(json.getAsDouble());
        }
    }
}

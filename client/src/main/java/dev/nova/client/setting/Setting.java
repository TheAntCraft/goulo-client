package dev.nova.client.setting;

import com.google.gson.JsonElement;

/** Eine einstellbare Option eines Moduls. */
public abstract class Setting<T> {

    private final String name;
    private final T defaultValue;
    protected T value;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }

    public abstract JsonElement toJson();

    /** Lädt den Wert aus der Config. Ungültige Werte werden ignoriert. */
    public abstract void fromJson(JsonElement json);
}

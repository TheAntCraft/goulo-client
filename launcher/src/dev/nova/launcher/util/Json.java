package dev.nova.launcher.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimaler JSON-Parser/-Writer, damit der Launcher ohne externe Bibliotheken auskommt.
 * Objekte → Map, Arrays → List, Zahlen → Long/Double.
 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    // ---------------------------------------------------------------- Parsen

    public static Object parse(String text) {
        Json p = new Json(text);
        try {
            p.ws();
            Object value = p.value();
            p.ws();
            if (p.i != p.s.length()) throw p.error("Unerwartete Zeichen am Ende");
            return value;
        } catch (IndexOutOfBoundsException e) {
            throw p.error("Unerwartetes Ende");
        }
    }

    public static Map<String, Object> parseObject(String text) {
        return obj(parse(text));
    }

    private Object value() {
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default: return number();
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        i++; // {
        ws();
        if (s.charAt(i) == '}') { i++; return map; }
        while (true) {
            ws();
            String key = string();
            ws();
            if (s.charAt(i++) != ':') throw error("':' erwartet");
            ws();
            map.put(key, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') return map;
            if (c != ',') throw error("',' oder '}' erwartet");
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        i++; // [
        ws();
        if (s.charAt(i) == ']') { i++; return list; }
        while (true) {
            ws();
            list.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') return list;
            if (c != ',') throw error("',' oder ']' erwartet");
        }
    }

    private String string() {
        if (s.charAt(i) != '"') throw error("'\"' erwartet");
        i++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                default -> sb.append(e); // \" \\ \/
            }
        }
    }

    private Object number() {
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String n = s.substring(start, i);
        if (n.isEmpty()) throw error("Ungültiger Wert");
        if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
        try {
            return Long.parseLong(n);
        } catch (NumberFormatException e) {
            return Double.parseDouble(n);
        }
    }

    private void expect(String word) {
        if (!s.startsWith(word, i)) throw error("'" + word + "' erwartet");
        i += word.length();
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON-Fehler an Position " + i + ": " + msg);
    }

    // ---------------------------------------------------------------- Zugriff

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        return (List<Object>) o;
    }

    public static Map<String, Object> obj(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Map ? obj(v) : Map.of();
    }

    public static List<Object> arr(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof List ? arr(v) : List.of();
    }

    public static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    public static long num(Map<String, Object> m, String key, long fallback) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : fallback;
    }

    // ---------------------------------------------------------------- Schreiben

    public static String stringify(Object o) {
        StringBuilder sb = new StringBuilder();
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
        } else if (o instanceof String str) {
            sb.append('"');
            for (char c : str.toCharArray()) {
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> {
                        if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                    }
                }
            }
            sb.append('"');
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else if (o instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                write(sb, String.valueOf(e.getKey()));
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof List<?> list) {
            sb.append('[');
            for (int k = 0; k < list.size(); k++) {
                if (k > 0) sb.append(',');
                write(sb, list.get(k));
            }
            sb.append(']');
        } else {
            write(sb, o.toString());
        }
    }
}

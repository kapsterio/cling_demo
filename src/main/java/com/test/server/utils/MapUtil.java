package com.test.server.utils;

import java.util.Map;

public class MapUtil {
    public static <T> T get(Map<String, ? extends Object> map, String... keys) {
        Map<String, ?> last = map;
        for (int i = 0; i < keys.length - 1; ++i) {
            if (last == null) {
                return null;
            }
            last = (Map<String, Object>) last.get(keys[i]);
        }
        return last == null ? null : (T) last.get(keys[keys.length - 1]);
    }
}

package top.xeonwang.JudgeServer.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class InfluxUtil {

    /**
     * 实体转 Influx Field Map
     *
     * @param entity        数据实体
     * @param excludeFields 需要作为tag的字段名（排除，不加入field）
     * @return field键值对
     */
    public static Map<String, Object> entityToFieldMap(Object entity, String... excludeFields) {
        Map<String, Object> fieldMap = new HashMap<>();
        Class<?> clazz = entity.getClass();
        // 转为数组集合，方便过滤
        java.util.Set<String> tagSet = new java.util.HashSet<>(java.util.Arrays.asList(excludeFields));

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();
            // tag字段跳过，只保留普通field
            if (tagSet.contains(fieldName)) {
                continue;
            }
            try {
                Object value = field.get(entity);
                // 忽略null值，避免influx写入空字段
                if (value != null) {
                    fieldMap.put(fieldName, value);
                }
            } catch (IllegalAccessException e) {
                // 反射异常静默忽略
            }
        }
        return fieldMap;
    }
}

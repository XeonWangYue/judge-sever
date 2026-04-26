package top.xeonwang.JudgeServer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    // 全局单例 ObjectMapper（线程安全）
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    /**
     * 泛型类反序列化：Result<T>
     *
     * @param json         JSON字符串
     * @param genericClass 泛型类（如 Result.class）
     * @param actualClass  泛型实际类型（如 User.class）
     * @return 带泛型的对象
     */
    public static <T> T toGenericObject(String json, Class<?> genericClass, Class<?> actualClass) {
        try {
            // 构造泛型类型：Result<User>
            JavaType javaType = OBJECT_MAPPER.getTypeFactory()
                    .constructParametricType(genericClass, actualClass);

            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("泛型反序列化失败", e);
        }
    }

    /**
     * 1. 对象 → JSON 字符串（序列化）
     *
     * @param obj 要转换的对象
     * @return JSON 字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("对象转 JSON 失败", e);
        }
    }

    /**
     * 2. JSON 字符串 → 对象（反序列化）
     *
     * @param json  JSON 字符串
     * @param clazz 目标对象类
     * @return 转换后的对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 转对象失败", e);
        }
    }

    /**
     * 将 LinkedHashMap / Object 转为指定的实体类
     */
    public static <T> T convertToTargetObject(Object obj, Class<T> targetClass) {
        return OBJECT_MAPPER.convertValue(obj, targetClass);
    }
}

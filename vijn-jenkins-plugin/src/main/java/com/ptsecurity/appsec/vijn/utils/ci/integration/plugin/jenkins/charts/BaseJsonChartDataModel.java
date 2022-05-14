package com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.charts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ptsecurity.appsec.vijn.utils.ci.integration.plugin.jenkins.domain.Issue;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.joor.Reflect;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class BaseJsonChartDataModel {
    /**
     * Map vulnerability severity level to chart area color
     */
    public static final Map<Issue.Level, Integer> LEVEL_COLORS = new HashMap<>();

    /**
     * Map vulnerability fix state to chart area color
     */
    public static final Map<Boolean, Integer> FIX_STATE_COLORS = new HashMap<>();

    /**
     * Map vulnerability "false positive" state to chart area color
     */
    public static final Map<Boolean, Integer> FALSE_POSITIVE_COLORS = new HashMap<>();

    static {
        log.trace("Initialize vulnerability severity chart area colors map");
        LEVEL_COLORS.put(Issue.Level.HIGH, 0xf57962);
        LEVEL_COLORS.put(Issue.Level.MEDIUM, 0xf9ad37);
        LEVEL_COLORS.put(Issue.Level.LOW, 0x66cc99);
        LEVEL_COLORS.put(Issue.Level.POTENTIAL, 0x8cb5e1);
        LEVEL_COLORS.put(Issue.Level.NONE, 0xb0b0b0);

        log.trace("Initialize vulnerability fix state chart area colors map");
        FIX_STATE_COLORS.put(false, LEVEL_COLORS.get(Issue.Level.HIGH));
        FIX_STATE_COLORS.put(true, LEVEL_COLORS.get(Issue.Level.POTENTIAL));

        log.trace("Initialize vulnerability 'false positive' state chart area colors map");
        FALSE_POSITIVE_COLORS.put(false, LEVEL_COLORS.get(Issue.Level.HIGH));
        FALSE_POSITIVE_COLORS.put(true, LEVEL_COLORS.get(Issue.Level.POTENTIAL));
    }

    /*
    public static int createShade(final float index, final float total) {
        float[] hsb = new float[3];
        hsb[0] = index / total; // * 255f;
        hsb[1] = 0.55f;
        hsb[2] = 0.8f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]).getRGB() & 0xFFFFFF;
    }

    public static int changeColorShade(final int c, final float s, final float b) {
        Color color = Color.decode("#" + Integer.toHexString(c));
        float[] hsb = new float[3];
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
        hsb[1] *= s;
        if (1 < hsb[1]) hsb[1] = 1;
        hsb[2] *= b;
        if (1 < hsb[2]) hsb[2] = 1;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]).getRGB() & 0xFFFFFF;
    }
    */

    /**
     * Converts object instance to its JSON representation that may be processed by JavaScript. That
     * object format used to pass data that may be used for charts
     * @param object Object instance to be represented as JSON
     * @return JSON data representation
     */
    @SneakyThrows
    public static JSONObject convertObject(final Object object) {
        log.trace("Convert object instance to its JSON representation");
        if (null == object) return null;
        JSONObject res = new JSONObject();
        Class<?> c = object.getClass();
        log.trace("Iterate through {} class hierarchy", c.getName());
        while (null != c) {
            log.trace("Add {} instance fields", c.getName());
            for (Field field : c.getDeclaredFields()) {
                log.trace("Process {} field", field.getName());
                Object value = Reflect.on(object).field(field.getName()).get();
                if (null == value) {
                    log.trace("Skip {} field as its value is null", field.getName());
                    continue;
                }
                if (!field.isAnnotationPresent(JsonProperty.class)) {
                    log.trace("Skip {} field as it isn't annotated as @JsonProperty", field.getName());
                    continue;
                }
                log.trace("Check if JSON property name must use @JsonProperty name attribute");
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String jsonFieldName = StringUtils.isEmpty(jsonProperty.value())
                        ? field.getName() : jsonProperty.value();

                Object jsonValue = convertValue(value);
                if (null != jsonValue) {
                    log.trace("Field {} added with non-null value", jsonFieldName);
                    res.put(jsonFieldName, jsonValue);
                }
            }
            c = c.getSuperclass();
        }
        return res;
    }

    /**
     * Method converts object field value to its JSON representation
     * @param value Field value that is to be converted
     * @return JSON data representation
     */
    protected static Object convertValue(final Object value) {
        log.trace("convert field value to its JSON representation");
        if (null == value) return null;
        Class<?> c = value.getClass();
        if (Collection.class.isAssignableFrom(c))
            return convertCollection(value);
        else if (c.isArray())
            return convertCollection(value);
        else if (c.equals(String.class))
            return value;
        else if (c.equals(UUID.class))
            return value.toString();
        else if (ClassUtils.isPrimitiveOrWrapper(c))
            return value;
        else if (c.isEnum())
            return value.toString();
        else
            return convertObject(value);
    }

    /**
     * Method converts object collection field value to its JSON representation
     * @param collection Collection field value that is to be converted
     * @return JSON data representation
     */
    protected static JSONArray convertCollection(final Object collection) {
        if (null == collection) return null;

        JSONArray res = new JSONArray();
        Class<?> c = collection.getClass();
        if (c.isArray()) {
            Object[] items = (Object[]) collection;
            for (Object item : items) res.add(convertValue(item));
        } else if (Collection.class.isAssignableFrom(c)) {
            Collection<?> items = (Collection<?>) collection;
            for (Object item : items) res.add(convertValue(item));
        }
        return res;
    }
}

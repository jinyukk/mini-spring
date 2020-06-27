package com.jzy.mini.spring.utils;

/**
 * @author jinziyu
 * @date 2020/6/27 13:44
 */
public class StringUtils {
    private StringUtils() {
    }

    public static String toLowerFirstLetter(String s) {
        if (s == null || s.length() == 0) {
            return s;
        }
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

}

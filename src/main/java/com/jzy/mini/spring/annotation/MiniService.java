package com.jzy.mini.spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jinziyu
 * @date 2020/6/27 13:54
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MiniComponent
public @interface MiniService {
}

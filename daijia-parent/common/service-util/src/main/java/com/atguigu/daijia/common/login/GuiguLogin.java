package com.atguigu.daijia.common.login;

import java.lang.annotation.*;

/**
 * 登录校验注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)  // 作用范围:运行阶段
@Target(ElementType.METHOD)  // 用在方法上
public @interface GuiguLogin {

}
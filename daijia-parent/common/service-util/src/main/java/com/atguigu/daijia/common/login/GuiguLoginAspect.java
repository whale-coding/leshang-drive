package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 登录校验AOP切面类
 */
@Slf4j
@Component
@Aspect  // 表明是一个切面类
@Order(100)
public class GuiguLoginAspect {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 登录校验逻辑
     * @param joinPoint 连接点,代表一个方法执行
     * @param guiguLogin 对打了@GuiguLogin注解的方法进行增强
     * @return 执行业务方法
     * @throws Throwable
     */
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(guiguLogin)")
    public Object process(ProceedingJoinPoint joinPoint, GuiguLogin guiguLogin) throws Throwable {
        // 获取请求头中的token
        // 获取request对象
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();

        // 获取request请求头中的token
        String token = request.getHeader("token");
        // 判断token是否为空
        if(!StringUtils.hasText(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 根据token从redis中获取用户id
        String userId = (String)redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX+token);
        // 用户id不为空，则将其放到ThreadLocal中
        if(StringUtils.hasText(userId)) {
            AuthContextHolder.setUserId(Long.parseLong(userId));
        }

        // 执行业务方法
        return joinPoint.proceed();
    }

}
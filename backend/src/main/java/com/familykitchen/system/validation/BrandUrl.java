package com.familykitchen.system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BrandUrlValidator.class)
public @interface BrandUrl {
  String message() default "品牌图片地址必须为 HTTPS 或站点相对路径";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}

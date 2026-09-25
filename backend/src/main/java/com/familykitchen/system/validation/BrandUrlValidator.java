package com.familykitchen.system.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public class BrandUrlValidator implements ConstraintValidator<BrandUrl, String> {
  @Override public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null || value.isEmpty()) return true;
    if (value.length() > 500 || value.contains("\\") || value.chars().anyMatch(c -> c <= 32 || c == 127)) return false;
    try {
      URI uri = new URI(value);
      if (value.startsWith("/")) return !value.startsWith("//") && uri.getRawAuthority() == null;
      return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
    } catch (java.net.URISyntaxException exception) {
      return false;
    }
  }
}

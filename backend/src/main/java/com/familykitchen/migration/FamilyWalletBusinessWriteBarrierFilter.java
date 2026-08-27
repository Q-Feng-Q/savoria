package com.familykitchen.migration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Global compatibility-build barrier for every mutating HTTP/business request. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnExpression("${family-kitchen.instance.lease-enabled:false}"
    + " && '${family-kitchen.migration.mode:OFF}'.equalsIgnoreCase('OFF')")
public class FamilyWalletBusinessWriteBarrierFilter extends OncePerRequestFilter {
  private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
  private final FamilyCartWalletMigrationMapper mapper;

  /**
   * Creates the compatibility-build HTTP write barrier.
   *
   * @param mapper migration persistence mapper
   */
  public FamilyWalletBusinessWriteBarrierFilter(FamilyCartWalletMigrationMapper mapper) {
    this.mapper = mapper;
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    if (!SAFE.contains(request.getMethod()) && mapper.isWriteBarrierEnabled() != 0) {
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"code\":\"FAMILY_WALLET_MIGRATION_BARRIER\",\"message\":\"writes are temporarily paused\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}

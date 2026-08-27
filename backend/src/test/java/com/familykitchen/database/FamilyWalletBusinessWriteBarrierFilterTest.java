package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familykitchen.migration.FamilyCartWalletMigrationMapper;
import com.familykitchen.migration.FamilyWalletBusinessWriteBarrierFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Verifies the compatibility HTTP write barrier covers all mutating routes. */
class FamilyWalletBusinessWriteBarrierFilterTest {
  @Test void blocksMutatingRequestAndAllowsSafeRead() throws Exception {
    FamilyCartWalletMigrationMapper mapper=mock(FamilyCartWalletMigrationMapper.class);
    FilterChain chain=mock(FilterChain.class);
    when(mapper.isWriteBarrierEnabled()).thenReturn(1);
    FamilyWalletBusinessWriteBarrierFilter filter=new FamilyWalletBusinessWriteBarrierFilter(mapper);
    MockHttpServletRequest post=new MockHttpServletRequest("POST","/api/cart/items");
    MockHttpServletResponse blocked=new MockHttpServletResponse();
    filter.doFilter(post,blocked,chain);
    assertEquals(503,blocked.getStatus());
    verify(chain,never()).doFilter(post,blocked);

    MockHttpServletRequest get=new MockHttpServletRequest("GET","/api/cart");
    MockHttpServletResponse allowed=new MockHttpServletResponse();
    filter.doFilter(get,allowed,chain);
    verify(chain).doFilter(get,allowed);
  }
}
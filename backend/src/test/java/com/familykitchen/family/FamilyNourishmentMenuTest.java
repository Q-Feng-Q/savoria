package com.familykitchen.family;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.dish.model.vo.DishView;
import com.familykitchen.family.controller.FamilyController;
import com.familykitchen.family.service.FamilyApplicationService;
import com.familykitchen.family.service.FamilyMemberApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FamilyNourishmentMenuTest {
  @Test void typeFilterIntersectsCompleteFamilyMenuWithoutRequiringFeatured() throws Exception {
    var users = mock(CurrentUserProvider.class);
    var service = mock(FamilyApplicationService.class);
    var request = mock(HttpServletRequest.class);
    var user = new CurrentUserContext(1L, 2L, 4L, null, null, Set.of(), Set.of());
    when(users.require(request)).thenReturn(user);
    ObjectMapper json = new ObjectMapper();
    DishView normal = json.readValue("{\"dishId\":1,\"productType\":\"NORMAL\",\"featured\":true}", DishView.class);
    DishView nourishment = json.readValue("{\"dishId\":2,\"productType\":\"NOURISHMENT\",\"featured\":false}", DishView.class);
    when(service.menuItems(user, 3L, "汤")).thenReturn(List.of(normal, nourishment));
    var controller = new FamilyController(users, service, mock(FamilyMemberApplicationService.class));
    assertEquals(List.of(nourishment), controller.menu(request, 3L, "汤", "NOURISHMENT").data());
    assertEquals(List.of(normal, nourishment), controller.menu(request, 3L, "汤", null).data());
    assertThrows(BusinessException.class, () -> controller.menu(request, 3L, "汤", "BAD"));
  }
}

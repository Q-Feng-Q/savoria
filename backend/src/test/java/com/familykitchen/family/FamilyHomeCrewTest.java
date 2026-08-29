package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.familykitchen.common.security.CurrentUserContext;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.family.mapper.FamilyMapper;
import com.familykitchen.family.model.entity.FamilyMemberRecord;
import com.familykitchen.family.model.entity.FamilyRecord;
import com.familykitchen.family.service.impl.FamilyApplicationServiceImpl;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies deterministic homepage crew names and their empty-state fallbacks. */
class FamilyHomeCrewTest {
  private static final CurrentUserContext USER = new CurrentUserContext(
      9L, 3L, 7L, 9L, "member", Set.of(), Set.of());

  private FamilyMapper mapper;
  private FamilyApplicationServiceImpl service;

  @BeforeEach
  void setUp() {
    mapper = mock(FamilyMapper.class);
    service = new FamilyApplicationServiceImpl(mapper, mock(DishMapper.class), mock(ObjectMapper.class));
    FamilyRecord family = new FamilyRecord();
    family.setFamilyId(7L);
    family.setFamilyName("林家");
    family.setMerchantName("暖炉小馆");
    family.setMerchantResponsibleName("老祁");
    family.setActiveMenuCount(2);
    family.setAddressCount(1);
    when(mapper.selectFamily(3L, 7L)).thenReturn(family);
    when(mapper.selectMember(9L)).thenReturn(member(9L, "小林", "MEMBER"));
    when(mapper.selectFeaturedDishes(7L, 5)).thenReturn(List.of());
    when(mapper.selectRecentOrders(7L)).thenReturn(List.of());
  }

  @Test
  void mapsMerchantResponsibleOwnerAndOrdinaryMemberToCrew() {
    when(mapper.selectMembers(7L)).thenReturn(List.of(
        member(4L, "阿禾", "OWNER"), member(9L, "小林", "MEMBER")));

    var crew = service.home(USER).crew();

    assertThat(crew.chefName()).isEqualTo("老祁");
    assertThat(crew.helperName()).isEqualTo("阿禾");
    assertThat(crew.tasterName()).isEqualTo("小林");
  }

  @Test
  void fallsBackToAdministratorThenExplicitEmptyLabels() {
    when(mapper.selectMembers(7L)).thenReturn(List.of(member(4L, "阿禾", "ADMIN")));
    var crew = service.home(USER).crew();
    assertThat(crew.tasterName()).isEqualTo("阿禾");

    when(mapper.selectMembers(7L)).thenReturn(List.of());
    crew = service.home(USER).crew();
    assertThat(crew.helperName()).isEqualTo("无帮厨");
    assertThat(crew.tasterName()).isEqualTo("无试吃员");
  }

  private static FamilyMemberRecord member(long id, String name, String role) {
    FamilyMemberRecord member = new FamilyMemberRecord();
    member.setMemberId(id);
    member.setName(name);
    member.setRoleTemplate(role);
    return member;
  }
}

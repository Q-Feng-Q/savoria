package com.familykitchen.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.family.controller.FamilyController;
import com.familykitchen.family.mapper.FamilyWorkflowMapper;
import com.familykitchen.family.model.dto.UpdateFamilyInfoRequest;
import com.familykitchen.family.model.dto.OwnerTransferRequest;
import com.familykitchen.family.model.vo.OwnerCandidateView;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

/** 家庭资料与负责人路由、DTO 和响应白名单契约测试。 */
class FamilyFamilyControllerContractTest {
  @Test
  void familyInfoRoutesAndUpdateWhitelistAreExact() throws Exception {
    Method getInfo = FamilyController.class.getMethod("familyInfo", jakarta.servlet.http.HttpServletRequest.class);
    Method updateInfo = FamilyController.class.getMethod(
        "updateFamilyInfo", jakarta.servlet.http.HttpServletRequest.class, UpdateFamilyInfoRequest.class);

    assertThat(getInfo.getAnnotation(GetMapping.class).value()).containsExactly("/info");
    assertThat(updateInfo.getAnnotation(PutMapping.class).value()).containsExactly("/info");
    assertThat(Arrays.stream(UpdateFamilyInfoRequest.class.getRecordComponents())
        .map(component -> component.getName()).toList())
        .containsExactly("familyName", "note");
  }

  @Test
  void ownerRoutesAndJsonContractsAreExact() throws Exception {
    Method candidates = FamilyController.class.getMethod(
        "ownerCandidates", jakarta.servlet.http.HttpServletRequest.class);
    Method transfer = FamilyController.class.getMethod(
        "transferOwner", jakarta.servlet.http.HttpServletRequest.class, OwnerTransferRequest.class);

    assertThat(candidates.getAnnotation(GetMapping.class).value()).containsExactly("/owner-candidates");
    assertThat(transfer.getAnnotation(PutMapping.class).value()).containsExactly("/owner");
    assertThat(Arrays.stream(OwnerTransferRequest.class.getRecordComponents())
        .map(component -> component.getName()).toList()).containsExactly("targetMemberId");
    assertThat(Arrays.stream(OwnerCandidateView.class.getRecordComponents())
        .map(component -> component.getName()).toList())
        .containsExactly("memberId", "displayName", "phoneSuffix");
  }

  @Test
  void ownerCandidateSqlAndFamilyInfoDocumentationDoNotExposeUnsupportedFields() throws Exception {
    Method candidateQuery = FamilyWorkflowMapper.class.getMethod(
        "selectOwnerCandidates", Long.class, Long.class);
    String sql = String.join(" ", candidateQuery.getAnnotation(Select.class).value());
    assertThat(sql).doesNotContain("u.username");

    Method updateInfo = FamilyController.class.getMethod(
        "updateFamilyInfo", jakarta.servlet.http.HttpServletRequest.class, UpdateFamilyInfoRequest.class);
    assertThat(updateInfo.getAnnotation(Operation.class).description()).doesNotContain("联系人");
  }
}

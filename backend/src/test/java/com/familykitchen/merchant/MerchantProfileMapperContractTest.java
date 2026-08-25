package com.familykitchen.merchant;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.merchant.mapper.MerchantProfileMapper;
import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

/** 商户资料 SQL 必须在执行时重新校验负责人关系。 */
class MerchantProfileMapperContractTest {
  @Test
  void selectAndUpdateRequireActiveMerchantAdminRelation() throws Exception {
    Method select = MerchantProfileMapper.class.getMethod("selectProfile", Long.class, Long.class);
    Method update = MerchantProfileMapper.class.getMethod(
        "updateProfile", Long.class, Long.class, String.class, String.class, String.class);
    String selectSql = String.join(" ", select.getAnnotation(Select.class).value()).toUpperCase();
    String updateSql = String.join(" ", update.getAnnotation(Update.class).value()).toUpperCase();

    for (String sql : new String[] {selectSql, updateSql}) {
      assertThat(sql)
          .contains("MERCHANT_ROLE='MERCHANT_ADMIN'")
          .contains("MUR.STATUS='ACTIVE'")
          .contains("M.STATUS='ACTIVE'")
          .contains("MUR.USER_ID=#{USERID}")
          .contains("MUR.MERCHANT_ID=M.ID");
    }
  }
}

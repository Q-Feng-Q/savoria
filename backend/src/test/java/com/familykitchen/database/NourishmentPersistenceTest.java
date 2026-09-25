package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.*;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.entity.DishEntity;
import com.familykitchen.family.mapper.FamilyMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

/** Only a fresh, isolated H2 memory database; never loads application/Flyway configuration. */
class NourishmentPersistenceTest {
  @Test void sqlRoundTripFamilyProjectionAndFiltersBeforePaging() throws Exception {
    var ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:nourishment;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    try (Connection c = ds.getConnection(); var st = c.createStatement()) {
      st.execute("create table dishes(id bigint auto_increment primary key, merchant_id bigint, category_id bigint, name varchar, description varchar, image_url varchar, base_price decimal(10,2), source_template_id bigint, featured_at timestamp, status varchar, deleted_at timestamp, deleted_by bigint, product_type varchar default 'NORMAL', nourishment_description varchar, serving_advice varchar, precautions varchar)");
      st.execute("create table users(id bigint, nickname varchar, username varchar)");
      st.execute("create table families(id bigint, merchant_id bigint)");
      st.execute("create table dish_categories(id bigint, merchant_id bigint, name varchar, sort_order int)");
      st.execute("create table family_menu_items(family_id bigint, dish_id bigint, final_price decimal, enabled int, sort_order int)");
      st.execute("insert into families values(4,2)");
      st.execute("insert into dish_categories values(3,2,'汤羹',1)");
      st.execute("create table dish_templates(id bigint primary key, template_code varchar, category_id bigint, name varchar, description varchar, image_url varchar, image_source_url varchar, image_author varchar, image_license varchar, reference_price decimal(10,2), taste_tags varchar, meal_tags varchar, template_type varchar, source_type varchar, source_key varchar, source_url varchar, source_revision varchar, source_category varchar, source_yield_text varchar, data_status varchar, procurement_ready int, image_rights_status varchar, sort_order int, enabled int, version bigint, product_type varchar, nourishment_description varchar, serving_advice varchar, precautions varchar)");
      st.execute("create table dish_template_categories(id bigint, name varchar, sort_order int, enabled int)");
      st.execute("create table dish_template_ingredients(id bigint, template_id bigint)");
      st.execute("create table dish_template_cooking_steps(id bigint, template_id bigint)");
      st.execute("insert into dish_template_categories values(3,'汤羹',1,1)");
      st.execute("insert into dish_template_categories values(9,'停用分类',2,0)");
      st.execute("insert into dish_templates(id,category_id,name,template_type,enabled,product_type,version) values(10,3,'普通','DISH',1,'NORMAL',0),(11,3,'滋补一','DISH',1,'NOURISHMENT',0),(12,3,'滋补二','DISH',1,'NOURISHMENT',0),(13,3,'配料','COMPONENT',1,'NORMAL',0)");
    }
    Configuration config = new Configuration(new Environment("isolated", new JdbcTransactionFactory(), ds));
    config.setMapUnderscoreToCamelCase(true);
    for (String file : List.of("dish/DishMapper.xml", "dish/DishTemplateMapper.xml", "family/FamilyMapper.xml")) {
      String resource = "mapper/" + file;
      try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
        new XMLMapperBuilder(in, config, resource, config.getSqlFragments()).parse();
      }
    }
    try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
      DishMapper dishes = session.getMapper(DishMapper.class);
      DishEntity dish = new DishEntity(); dish.setMerchantId(2L); dish.setCategoryId(3L);
      dish.setName("滋补汤"); dish.setBasePrice(BigDecimal.TEN); dish.setStatus("active");
      dish.setProductType("NOURISHMENT"); dish.setNourishmentDescription("第一行\n第二行"); dish.setServingAdvice("温热食用");
      dishes.insertDish(dish);
      DishEntity saved = dishes.selectDish(2L, dish.getId());
      assertEquals("NOURISHMENT", saved.getProductType());
      assertEquals("第一行\n第二行", saved.getNourishmentDescription());
      assertEquals(1, dishes.selectDishesByProductType(2L, "available", "NOURISHMENT").size());
      assertEquals(0, dishes.selectDishesByProductType(9L, "available", "NOURISHMENT").size());
      assertEquals(0, dishes.selectDishesByProductType(2L, "available", "NORMAL").size());
      saved.setServingAdvice(null); dishes.updateDish(saved);
      assertNull(dishes.selectDish(2L, dish.getId()).getServingAdvice());
      var menu = session.getMapper(FamilyMapper.class).selectFamilyMenuItems(2L, 4L);
      assertEquals("NOURISHMENT", menu.get(0).productType());
      assertEquals("第一行\n第二行", menu.get(0).nourishmentDescription());
      DishTemplateMapper templates = session.getMapper(DishTemplateMapper.class);
      assertEquals(3, templates.countTemplates(2L, null, null, null));
      assertEquals(2, templates.countTemplatesByProductType(2L, 3L, "滋补", false, "NOURISHMENT"));
      var page = templates.selectTemplatesByProductType(2L, 3L, "滋补", false, 1, 1, "NOURISHMENT");
      assertEquals(1, page.size()); assertEquals(12L, page.get(0).getId());
      assertEquals("NOURISHMENT", page.get(0).getProductType());
      assertEquals(2, templates.countAdminTemplatesByProductType(null,null,null,null,null,null,null,"NOURISHMENT"));
      assertEquals(1, templates.countAdminTemplatesByProductType(null,null,null,null,null,null,null,"NORMAL"));
      var edited = templates.selectTemplateForUpdate(11L);
      edited.setNourishmentDescription("已更新"); edited.setServingAdvice(null);
      assertEquals(1, templates.updateAdminTemplate(edited, 0L));
      assertEquals("已更新", templates.selectAdminTemplate(11L).getNourishmentDescription());
      assertEquals("NOURISHMENT", templates.selectAdminTemplate(11L).getProductType());
      try (var st = session.getConnection().createStatement()) {
        st.execute("insert into dish_templates(id,category_id,name,template_type,enabled,product_type,version) values(19,9,'隐藏滋补','DISH',1,'NOURISHMENT',0)");
      }
      session.clearCache();
      assertEquals(2, templates.countTemplatesByProductType(2L, null, null, null, "NOURISHMENT"));
    }
  }
}

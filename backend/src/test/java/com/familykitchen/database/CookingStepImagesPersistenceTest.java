package com.familykitchen.database;

import static org.junit.jupiter.api.Assertions.*;
import com.familykitchen.dish.mapper.DishMapper;
import com.familykitchen.dish.mapper.DishTemplateMapper;
import com.familykitchen.dish.model.entity.DishCookingStepEntity;
import com.familykitchen.dish.model.entity.DishTemplateCookingStepEntity;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

class CookingStepImagesPersistenceTest {
  @Test void orderedImagesRoundTripViaBothRealMappersAndNullReadsEmpty() throws Exception {
    var ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:stepimages;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    try (var c = ds.getConnection(); var st = c.createStatement()) {
      st.execute("create table dish_cooking_steps(id bigint auto_increment primary key, dish_id bigint, step_no int, title varchar, content varchar, duration_seconds int, temperature_text varchar, heat_level varchar, source_template_step_id bigint, component_template_id bigint, source_note varchar, image_urls varchar)");
      st.execute("create table dish_template_cooking_steps(id bigint auto_increment primary key, item_key varchar, template_id bigint, step_no int, title varchar, content varchar, source_text varchar, duration_seconds int, temperature_text varchar, heat_level varchar, component_template_id bigint, image_urls varchar)");
      st.execute("insert into dish_cooking_steps(dish_id,step_no) values(9,1)");
    }
    var config = new Configuration(new Environment("isolated", new JdbcTransactionFactory(), ds));
    for (String file : List.of("DishMapper.xml", "DishTemplateMapper.xml")) {
      String path = "mapper/dish/" + file;
      try (var in = getClass().getClassLoader().getResourceAsStream(path)) {
        new XMLMapperBuilder(in, config, path, config.getSqlFragments()).parse();
      }
    }
    try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
      var dishes = session.getMapper(DishMapper.class);
      var templates = session.getMapper(DishTemplateMapper.class);
      var images = List.of("/uploads/images/b.png", "https://example.com/a.jpg");
      var step = new DishCookingStepEntity(); step.setDishId(1L); step.setStepNo(1); step.setImageUrls(images);
      dishes.insertCookingStep(step);
      assertEquals(images, dishes.selectCookingSteps(1L).get(0).getImageUrls());
      assertEquals(List.of(), dishes.selectCookingSteps(9L).get(0).getImageUrls());
      var template = new DishTemplateCookingStepEntity(); template.setTemplateId(2L); template.setStepNo(1); template.setImageUrls(images);
      templates.insertTemplateCookingStep(template);
      assertEquals(images, templates.selectTemplateCookingSteps(2L).get(0).getImageUrls());
    }
  }
}

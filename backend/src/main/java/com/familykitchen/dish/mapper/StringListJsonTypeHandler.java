package com.familykitchen.dish.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/** Ordered JSON image arrays; SQL NULL from pre-image rows reads as an empty array. */
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {
  private static final ObjectMapper JSON = new ObjectMapper();
  @Override public void setNonNullParameter(PreparedStatement statement, int index, List<String> value,
      JdbcType type) throws SQLException {
    try { statement.setString(index, JSON.writeValueAsString(value)); }
    catch (Exception ex) { throw new SQLException("Cannot serialize image URLs", ex); }
  }
  @Override public List<String> getNullableResult(ResultSet result, String column) throws SQLException { return parse(result.getString(column)); }
  @Override public List<String> getNullableResult(ResultSet result, int column) throws SQLException { return parse(result.getString(column)); }
  @Override public List<String> getNullableResult(CallableStatement result, int column) throws SQLException { return parse(result.getString(column)); }
  private static List<String> parse(String value) throws SQLException {
    if (value == null || "null".equals(value)) return List.of();
    try { return List.copyOf(JSON.readValue(value, new TypeReference<List<String>>() { })); }
    catch (Exception ex) { throw new SQLException("Invalid image URLs JSON", ex); }
  }
}

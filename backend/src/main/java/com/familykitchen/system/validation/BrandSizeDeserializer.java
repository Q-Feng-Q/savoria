package com.familykitchen.system.validation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/** Do not let Jackson silently round fractional logo sizes into valid integers. */
public class BrandSizeDeserializer extends JsonDeserializer<Integer> {
  @Override public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      return (Integer) context.handleUnexpectedToken(Integer.class, parser);
    }
    return parser.getIntValue();
  }
}

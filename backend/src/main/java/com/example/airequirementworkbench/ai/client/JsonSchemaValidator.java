package com.example.airequirementworkbench.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JsonSchemaValidator {
  public void validate(Map<String, Object> schema, JsonNode value) {
    List<String> errors = new ArrayList<>();
    validateNode(schema, value, "$", errors);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join("; ", errors));
    }
  }

  @SuppressWarnings("unchecked")
  private void validateNode(Map<String, Object> schema, JsonNode value, String path, List<String> errors) {
    Object type = schema.get("type");
    if (type instanceof String typeName && !matchesType(typeName, value)) {
      errors.add(path + " 类型应为 " + typeName);
      return;
    }
    if (type instanceof List<?> typeNames && typeNames.stream().map(String::valueOf).noneMatch(typeName -> matchesType(typeName, value))) {
      errors.add(path + " 类型应为 " + typeNames);
      return;
    }

    if (value.isObject()) {
      Object required = schema.get("required");
      if (required instanceof List<?> requiredFields) {
        for (Object field : requiredFields) {
          if (field != null && !value.has(String.valueOf(field))) {
            errors.add(path + "." + field + " 为必填字段");
          }
        }
      }

      Object properties = schema.get("properties");
      if (properties instanceof Map<?, ?> propertyMap) {
        Iterator<String> names = value.fieldNames();
        while (names.hasNext()) {
          String name = names.next();
          Object propertySchema = propertyMap.get(name);
          if (propertySchema instanceof Map<?, ?> childSchema) {
            validateNode((Map<String, Object>) childSchema, value.get(name), path + "." + name, errors);
          }
        }
      }
    }

    if (value.isArray()) {
      Object items = schema.get("items");
      if (items instanceof Map<?, ?> itemSchema) {
        for (int index = 0; index < value.size(); index++) {
          validateNode((Map<String, Object>) itemSchema, value.get(index), path + "[" + index + "]", errors);
        }
      }
    }
  }

  private boolean matchesType(String typeName, JsonNode value) {
    return switch (typeName) {
      case "object" -> value.isObject();
      case "array" -> value.isArray();
      case "string" -> value.isTextual();
      case "number" -> value.isNumber();
      case "integer" -> value.isIntegralNumber();
      case "boolean" -> value.isBoolean();
      case "null" -> value.isNull();
      default -> true;
    };
  }
}

package com.opencore.gdpdu.common.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.index.annotations.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(ClassRegistry.class);

  /**
   * This maps (for each model class) from field names to the setter method and annotation for each field that is annotated with the "Column" annotation.
   */
  private static final Map<Class<?>, Map<String, ColumnInfo>> COLUMN_INFO_MAP = new HashMap<>();

  private ClassRegistry() {
  }

  /**
   * This method takes a class and retrieves all fields and checks all that are annotated with the {@link Column} annotation.
   * It then builds a Map of field names to {@link Method} objects for the setters.
   */
  public static Map<String, ColumnInfo> registerClass(Class<?> clazz) throws ParsingException {
    if (COLUMN_INFO_MAP.containsKey(clazz)) {
      return null;
    }

    BeanInfo info;
    try {
      info = Introspector.getBeanInfo(clazz);
    } catch (IntrospectionException e) {
      throw new ParsingException(e);
    }

    Map<String, Field> fieldMap = getAllFields(clazz);
    Map<String, ColumnInfo> infoMap = new HashMap<>();
    for (PropertyDescriptor propertyDescriptor : info.getPropertyDescriptors()) {
      Field field = fieldMap.get(propertyDescriptor.getName());
      if (field == null) {
        continue;
      }

      // Get every field that's annotated by the Column annotation, ignore all others
      Column annotation = field.getAnnotation(Column.class);
      if (annotation == null) {
        LOG.trace("Ignoring field [{}] because it's not annotated with @Column", field.getName());
        continue;
      }

      Method writeMethod = propertyDescriptor.getWriteMethod();
      if (writeMethod.getParameterCount() != 1) {
        throw new ParsingException("We only support setters with exactly one parameter");
      }

      infoMap.put(annotation.value(), new ColumnInfo(annotation, propertyDescriptor.getWriteMethod()));
    }
    COLUMN_INFO_MAP.put(clazz, infoMap);
    return infoMap;
  }

  public static Map<String, ColumnInfo> getClassInformation(Class<?> clazz) throws ParsingException {
    Map<String, ColumnInfo> infoMap = COLUMN_INFO_MAP.get(clazz);
    if (infoMap == null) {
      infoMap = registerClass(clazz);
    }
    return infoMap;
  }

  /**
   * This method returns all fields (private as well as public) for a Class including its superclasses.
   */
  private static Map<String, Field> getAllFields(Class<?> type) {
    Objects.requireNonNull(type, "'type' can't be null");

    Map<String, Field> fieldMap = new HashMap<>();
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      for (Field declaredField : c.getDeclaredFields()) {
        fieldMap.put(declaredField.getName(), declaredField);
      }
    }
    return fieldMap;
  }

}

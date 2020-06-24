package com.opencore.gdpdu.index.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.opencore.gdpdu.index.models.DataType;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

  String value();

  /**
   * This is optional and can be used for validation purposes.
   * This way we can check whether an {@code index.xml} file actually uses the correct GDPdU data types.
   */
  DataType type() default DataType.AlphaNumeric;

}

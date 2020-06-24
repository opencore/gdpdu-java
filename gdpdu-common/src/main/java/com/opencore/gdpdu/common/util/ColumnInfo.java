package com.opencore.gdpdu.common.util;

import java.lang.reflect.Method;

import com.opencore.gdpdu.index.annotations.Column;

public class ColumnInfo {

  public Column annotation;
  public Method setter;

  public ColumnInfo(Column annotation, Method setter) {
    this.annotation = annotation;
    this.setter = setter;
  }

}

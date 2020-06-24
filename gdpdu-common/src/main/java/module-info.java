module com.opencore.gdpdu.common {
  requires com.opencore.gdpdu.models;
  requires java.desktop;
  requires transitive org.slf4j;

  exports com.opencore.gdpdu.common.exceptions;
  exports com.opencore.gdpdu.common.util;
}

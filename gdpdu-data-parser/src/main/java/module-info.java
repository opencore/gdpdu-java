module com.opencore.gdpdu.data {

  requires commons.csv;
  requires com.opencore.gdpdu.common;
  requires com.opencore.gdpdu.index;
  requires com.opencore.gdpdu.models;
  requires java.desktop;
  requires java.validation;
  requires transitive org.slf4j;

  exports com.opencore.gdpdu.data;

  opens com.opencore.gdpdu.data;

}

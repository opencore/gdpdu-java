module com.opencore.gdpdu.models {
  requires java.validation;

  exports com.opencore.gdpdu.index.models;
  exports com.opencore.gdpdu.index.annotations;

  opens com.opencore.gdpdu.index.models;
}

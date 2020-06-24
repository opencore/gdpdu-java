import com.opencore.gdpdu.index.annotations.Column;
import com.opencore.gdpdu.index.models.DataType;

public class TestModel {

  @Column(value = "foo", type = DataType.AlphaNumeric)
  private String foo;

  @Column(value = "bar", type = DataType.Numeric)
  private int bar;

  public String getFoo() {
    return foo;
  }

  public void setFoo(String foo) {
    this.foo = foo;
  }

  public int getBar() {
    return bar;
  }

  public void setBar(int bar) {
    this.bar = bar;
  }
}

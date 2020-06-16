import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.opencore.gdpdu.data.GdpduDataParser;
import com.opencore.gdpdu.data.ParsingException;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ParsingTest {

  @Test
  void testParsing() throws ParsingException {
    GdpduDataParser parser = new GdpduDataParser();

    List<TestModel> models = parser.parseTable("src/test/resources/data1/index.xml", "Testdatei Nr. 1", TestModel.class);

    assertNotNull(models);
    assertEquals(2, models.size());

    TestModel testModel = models.get(0);
    assertEquals("foo", testModel.getFoo());
    assertEquals(10, testModel.getBar());
  }
}

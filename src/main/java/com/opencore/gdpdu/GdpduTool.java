package com.opencore.gdpdu;

import com.opencore.gdpdu.models.DataSet;
import com.opencore.gdpdu.util.GdpduParser;

public class GdpduTool {

  public static void main(String[] args) {
    DataSet dataSet = GdpduParser.parseXmlFile(args[0]);
    System.out.println(dataSet);
  }

}

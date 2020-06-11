package com.opencore.gdpdu;

import com.opencore.gdpdu.models.DataSet;

public class GdpduTool {

  public static void main(String[] args) {
    DataSet dataSet = GdpduIndexParser.parseXmlFile(args[0]);
    System.out.println(dataSet);
  }

}

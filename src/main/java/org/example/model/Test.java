package org.example.model;

public class Test {

  private int abobus;
  private String name;

  public Test() {
    this.abobus = 0;
    this.name = null;
  }

  public Test(int abobus, String name) {
    this.abobus = abobus;
    this.name = name;
  }

  public int getAbobus() {
    return abobus;
  }

  public String getName() {
    return name;
  }

  public void setAbobus(int abobus) {
    this.abobus = abobus;
  }

  public void setName(String name) {
    this.name = name;
  }
}

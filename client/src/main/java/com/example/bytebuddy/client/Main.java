/* Licensed under Apache-2.0 2024. */
package com.example.bytebuddy.client;

import com.example.bytebuddy.annotation.ApplyTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplyTransformation
public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final class ShortException extends RuntimeException {

    private ShortException(String message) {
      super(message, null, true, false);
    }
  }

  public static void main(String[] args) {
    log.info("Hello World");
    new Main().existingMethod("arg", 1);
    new Main().test();
  }

  public void existingMethod(String arg, int i) {
    log.info("inside existingMethod");
  }

  @ApplyTransformation
  public void test() {
    log.info("inside test");

    if (System.currentTimeMillis() % 2 == 0) {
      throw new ShortException("planned exception");
    }
  }
}

/* Licensed under Apache-2.0 2024. */
package com.example.bytebuddy.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlatformTransactionManager {

  private PlatformTransactionManager() {}

  private static final Logger log = LoggerFactory.getLogger(PlatformTransactionManager.class);

  public static void begin() {
    log.info("begin transaction");
  }

  public static void commit() {
    log.info("commit transaction");
  }

  public static void rollback() {
    log.info("rollback transaction");
  }
}

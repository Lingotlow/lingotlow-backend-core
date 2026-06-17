package com.lingotlow.backendcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.lingotlow.backendcore"})
public class BackendCoreApplication {
  public static void main(String[] args) {
    SpringApplication.run(BackendCoreApplication.class, args);
  }
}

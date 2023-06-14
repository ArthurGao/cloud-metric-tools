package com.arthur.metrics.utils.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  public static final String RESULT_REQUEST = "TestResult_request";
  public static final String RESULT_USER = "TestResult_user";
  public static final String RESULT_IP = "TestResult_Ip";
  public static final String RESULT_COUNTRY = "TestResult_Country";

  public static final int DEFAULT_OP_DURATION_IN_MILLIS = 500;

  @GetMapping("testRequest")
  public String testRequest() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return RESULT_REQUEST;
  }

  @GetMapping("testRequestNoMetrics")
  public String testRequestNoMetrics() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return RESULT_REQUEST;
  }

  @PostMapping("testPostRequest")
  public ResponseEntity testPostRequest204() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return ResponseEntity.accepted().build();
  }

  @DeleteMapping("testDelete")
  public ResponseEntity testDelete() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return ResponseEntity.noContent().build();
  }

  @RequestMapping(value = "/testHeader", method = {RequestMethod.HEAD})
  public ResponseEntity testHeader() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return ResponseEntity.accepted().build();
  }

  @PutMapping("testPutRequestWith404")
  public ResponseEntity testPutRequestWith404() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return ResponseEntity.notFound().build();
  }

  @PatchMapping("testPatchRequestWith500")
  public ResponseEntity<String> testRequestException() throws InterruptedException {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  @PatchMapping("testRuntimeException")
  public ResponseEntity testException() throws Throwable {
    Thread.sleep(DEFAULT_OP_DURATION_IN_MILLIS);
    throw new Exception("Custom exception");
  }
}

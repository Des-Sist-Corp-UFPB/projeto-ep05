package br.ufpb.dsc.mercado.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {
  @GetMapping("/ping")
  public Map<String,Object> ping() {
    return Map.of(
      "status", "ok",
      "service", "ep05",
      "timestamp", java.time.Instant.now().toString());
  }
}
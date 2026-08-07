package br.ufpb.dsc.mercado.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

  private final JdbcTemplate jdbcTemplate;

  public PingController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // Rota pública (liberada em SecurityConfig -> webFilterChain -> permitAll)
  // Só responde 200 se o banco estiver OK.
  // Se o SELECT falhar, responde 503 (fora do ar) para o Status acusar o problema.
  @GetMapping("/ping")
  public ResponseEntity<Map<String,Object>> ping() {
    Map<String,Object> body = new LinkedHashMap<>();
    body.put("service", "ep05");
    body.put("timestamp", java.time.Instant.now().toString());

    try {
      jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      body.put("status", "ok");
      body.put("database", "up");
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      body.put("status", "error");
      body.put("database", "down");
      body.put("databaseError", e.getMessage());
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
  }
}
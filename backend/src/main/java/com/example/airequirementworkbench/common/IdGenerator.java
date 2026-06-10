package com.example.airequirementworkbench.common;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {
  private final AtomicLong sequence = new AtomicLong(new SecureRandom().nextInt(1000));
  private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

  public long nextId() {
    long now = System.currentTimeMillis();
    long seq = sequence.updateAndGet(value -> value >= 9999 ? 1 : value + 1);
    return now * 10000 + seq;
  }

  public String nextRequirementNo() {
    return "REQ-" + YEAR.format(LocalDate.now()) + "-" + String.format("%04d", sequence.incrementAndGet() % 10000);
  }

  public String nextTraceNo() {
    return "TR-" + DAY.format(LocalDate.now()) + "-" + String.format("%04d", sequence.incrementAndGet() % 10000);
  }
}

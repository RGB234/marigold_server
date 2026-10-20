package com.sns.marigold.global.validation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class ValidationPolicyJsonGenerator {

  private ValidationPolicyJsonGenerator() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expected the output path for validation-policy.json");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
    prettyPrinter.indentObjectsWith(indenter);
    prettyPrinter.indentArraysWith(indenter);

    Path output = Path.of(args[0]);
    String json =
        objectMapper.writer(prettyPrinter).writeValueAsString(ValidationPolicy.contract());
    Files.writeString(output, json + "\n", StandardCharsets.UTF_8);
    System.out.println("Generated " + output);
  }
}

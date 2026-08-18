package com.igot.cb.pores.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.demand.service.DemandServiceImpl;
import com.igot.cb.pores.exceptions.CustomException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PayloadValidation {

  public void validatePayload(String fileName, JsonNode payload) {
    try {
      // Every schema under payloadValidation/ declares "$schema": draft-07 - use that
      // version explicitly instead of the deprecated no-arg getInstance(), which silently
      // pins to draft-04 and ignores draft-07-only keywords such as if/then/else.
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(fileName);
      JsonSchema schema = schemaFactory.getSchema(schemaStream);
      if (payload.isArray()) {
        for (JsonNode objectNode : payload) {
          validateObject(schema, objectNode);
        }
      } else{
        validateObject(schema, payload);
      }
    } catch (Exception e) {
      log.error("Failed to validate payload", e);
      throw new CustomException("Failed to validate payload", e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  private void validateObject(JsonSchema schema, JsonNode objectNode) {
    Set<ValidationMessage> validationMessages = schema.validate(objectNode);
    if (!validationMessages.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder("Validation error(s): \n");
      for (ValidationMessage message : validationMessages) {
        errorMessage.append(message.getMessage()).append("\n");
      }
      log.error("Validation Error {}", errorMessage);
      throw new CustomException("Validation Error", errorMessage.toString(), HttpStatus.BAD_REQUEST);
    }
  }
}


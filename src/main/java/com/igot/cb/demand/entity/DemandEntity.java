package com.igot.cb.demand.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "demands")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class DemandEntity {
  @Id
  private String demandId;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private JsonNode data;

  private Timestamp createdOn;

  private Timestamp updatedOn;
}

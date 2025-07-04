package com.igot.cb.competencies.theme.enity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "competency_theme")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class CompetencyThemeEntity {

  @Id
  private String id;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private JsonNode data;

  private Boolean isActive;

  private Timestamp createdOn;

  private Timestamp updatedOn;

}

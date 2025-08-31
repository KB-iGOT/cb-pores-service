package com.igot.cb.contentpartner.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name ="content_partner")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
public class ContentPartnerEntity {
    @Id
    private String id;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode data;

    private Timestamp createdOn;

    private Timestamp updatedOn;

    private Boolean isActive;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode trasformContentJson;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode transformContentViaApi;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode transformProgressJson;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode transformProgressViaApi;

    private String certificateTemplateUrl;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode serviceRegistryDetails;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode contentFileValidation;
}

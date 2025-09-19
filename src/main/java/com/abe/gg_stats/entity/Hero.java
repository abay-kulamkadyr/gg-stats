package com.abe.gg_stats.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "hero")
@Data
@NoArgsConstructor
public class Hero {

	// Setter methods needed for JPA and batch processing
	@Id
	private Integer id;

	@NotBlank(message = "Hero name cannot be blank.")
	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@NotBlank(message = "Localized name cannot be blank.")
	@Column(name = "localized_name", nullable = false)
	private String localizedName;

	@NotBlank(message = "Primary attribute cannot be blank.")
	@Column(name = "primary_attr")
	private String primaryAttr;

	@NotBlank(message = "Attack type cannot be blank.")
	@Column(name = "attack_type")
	private String attackType;

	@Column(name = "roles", columnDefinition = "text[]", nullable = false)
	private List<String> roles;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	// database contains a trigger on update for this table
	@Column(name = "updated_at", insertable = false)
	private Instant updatedAt;

}

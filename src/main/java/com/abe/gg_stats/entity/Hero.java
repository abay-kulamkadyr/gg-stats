package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "hero")
@Data
@NoArgsConstructor
public class Hero {

	// Setter methods needed for JPA and batch processing
	@Id
	private Integer id;

	@NonNull
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

	@NotNull
	@Column(name = "roles", columnDefinition = "TEXT[]")
	private List<String> roles;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	// database contains a trigger on update for this table
	@Column(name = "updated_at", insertable = false)
	private Instant updatedAt;

}

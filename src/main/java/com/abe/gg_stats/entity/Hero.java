package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hero")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hero {

	@Id
	private Integer id;

	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "localized_name", nullable = false)
	private String localizedName;

	@Column(name = "primary_attr")
	private String primaryAttr;

	@Column(name = "attack_type")
	private String attackType;

	@Column(name = "roles", columnDefinition = "TEXT[]")
	private List<String> roles;

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

}

package com.abe.gg_stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

@Entity
@Table(name = "hero")
@Getter
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

	/**
	 * Get roles as an immutable list to prevent external modification
	 */
	public List<String> getRoles() {
		return roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
	}

	// Setter methods needed for JPA and batch processing
	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLocalizedName(String localizedName) {
		this.localizedName = localizedName;
	}

	public void setPrimaryAttr(String primaryAttr) {
		this.primaryAttr = primaryAttr;
	}

	public void setAttackType(String attackType) {
		this.attackType = attackType;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles != null ? new ArrayList<>(roles) : null;
	}

	@CreationTimestamp
	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

}

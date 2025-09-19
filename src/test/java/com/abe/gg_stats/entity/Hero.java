package com.abe.gg_stats.entity;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "hero")
@Data
@NoArgsConstructor
public class Hero {

	@Id
	private Integer id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "localized_name", nullable = false)
	private String localizedName;

	@Column(name = "primary_attr")
	private String primaryAttr;

	@Column(name = "attack_type")
	private String attackType;

	// H2 doesn't understand text[] → store as comma-separated string
	@Column(name = "roles", nullable = false)
	@Convert(converter = StringListConverter.class)
	private List<String> roles;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false)
	private Instant updatedAt;

}

@Converter
class StringListConverter implements AttributeConverter<List<String>, String> {

	private static final String DELIMITER = ",";

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return "";
		}
		return String.join(DELIMITER, attribute);
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.trim().isEmpty()) {
			return List.of();
		}
		return Arrays.stream(dbData.split(DELIMITER)).map(String::trim).collect(Collectors.toList());
	}

}

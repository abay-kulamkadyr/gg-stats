package com.abe.gg_stats.dto.request.opendota.mapper;

import com.abe.gg_stats.dto.request.opendota.OpenDotaPlayerDto;
import com.abe.gg_stats.dto.response.PlayerResponseDto;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OpenDotaPlayerResponseMapper {

	@Mapping(target = "accountId", expression = "java(selectAccountId(src))")
	@Mapping(target = "steamId", source = "profile.steamid")
	@Mapping(target = "avatar", source = "profile.avatar", qualifiedByName = "toNullIfBlank")
	@Mapping(target = "avatarMedium", source = "profile.avatarmedium")
	@Mapping(target = "avatarFull", source = "profile.avatarfull")
	@Mapping(target = "profileUrl", source = "profile.profileurl", qualifiedByName = "toNullIfBlank")
	@Mapping(target = "personName", source = "profile.personaname")
	@Mapping(target = "lastLogin", source = "profile.last_login", qualifiedByName = "toInstant")
	@Mapping(target = "fullHistoryTime", source = "profile.full_history_time", qualifiedByName = "toInstant")
	@Mapping(target = "cheese", source = "profile.cheese")
	@Mapping(target = "fhUnavailable", source = "profile.fh_unavailable")
	@Mapping(target = "locCountryCode", source = "profile.loccountrycode", qualifiedByName = "toNullIfBlank")
	@Mapping(target = "lastMatchTime", source = "profile.last_match_time", qualifiedByName = "toInstant")
	@Mapping(target = "plus", source = "profile.plus")
	@Mapping(target = "rankTier", source = "rankTier")
	@Mapping(target = "leaderboardRank", source = "leaderboardRank")
	OpenDotaPlayerDto toPlayerDto(PlayerResponseDto src);

	default Long selectAccountId(PlayerResponseDto src) {
		if (src == null) {
			return null;
		}
		if (src.accountId() != null) {
			return src.accountId();
		}
		return src.profile() != null ? src.profile().accountId() : null;
	}

	@Named("toInstant")
	default Instant toInstant(String value) {
		if (value == null) {
			return null;
		}
		String v = value.trim();
		if (v.isEmpty()) {
			return null;
		}
		try {
			return Instant.parse(v);
		}
		catch (Exception e) {
			try {
				return Instant.ofEpochSecond(Long.parseLong(v));
			}
			catch (Exception ignored) {
				return null;
			}
		}
	}

	@Named("toNullIfBlank")
	default String toNullIfBlank(String value) {
		if (value == null) {
			return null;
		}
		String v = value.trim();
		return v.isEmpty() ? null : value;
	}

}

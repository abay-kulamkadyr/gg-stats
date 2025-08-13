package com.abe.gg_stats.entity;

import java.io.Serializable;
import java.util.Objects;

public class HeroRankingId implements Serializable {

	private Long accountId;

	private Integer heroId;

	public HeroRankingId() {
	}

	public HeroRankingId(Long accountId, Integer heroId) {
		this.accountId = accountId;
		this.heroId = heroId;
	}

	// getters, setters, equals, hashCode
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof HeroRankingId that))
			return false;
		return Objects.equals(accountId, that.accountId) && Objects.equals(heroId, that.heroId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(accountId, heroId);
	}

}

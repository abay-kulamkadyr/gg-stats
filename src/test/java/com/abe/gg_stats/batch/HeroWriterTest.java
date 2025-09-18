package com.abe.gg_stats.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.abe.gg_stats.batch.hero.HeroWriter;
import com.abe.gg_stats.dto.request.opendota.OpenDotaHeroDto;
import com.abe.gg_stats.dto.request.opendota.mapper.OpenDotaHeroMapper;
import com.abe.gg_stats.entity.Hero;
import com.abe.gg_stats.repository.HeroRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class HeroWriterTest {

	@Mock
	private HeroRepository heroRepository;

	@Mock
	private OpenDotaHeroMapper heroMapper;

	@InjectMocks
	private HeroWriter heroWriter;

	private OpenDotaHeroDto dto1;

	private OpenDotaHeroDto dto2;

	@BeforeEach
	void setUp() {
		dto1 = new OpenDotaHeroDto(1, "antimage", "Anti-Mage", null, null, List.of());
		dto2 = new OpenDotaHeroDto(2, "axe", "Axe", "str", "Melee", List.of("Initiator"));
	}

	@Test
	void write_ShouldMapAndPersistAllItems() throws Exception {
		Hero hero1 = new Hero();
		hero1.setId(1);
		hero1.setName("antimage");

		Hero hero2 = new Hero();
		hero2.setId(2);
		hero2.setName("axe");

		when(heroMapper.dtoToEntity(dto1)).thenReturn(hero1);
		when(heroMapper.dtoToEntity(dto2)).thenReturn(hero2);

		Chunk<OpenDotaHeroDto> chunk = Chunk.of(dto1, dto2);
		heroWriter.write(chunk);

		verify(heroMapper, times(1)).dtoToEntity(dto1);
		verify(heroMapper, times(1)).dtoToEntity(dto2);
		verify(heroRepository, times(1)).save(hero1);
		verify(heroRepository, times(1)).save(hero2);
	}

	@Test
	void write_WithEmptyChunk_ShouldDoNothing() throws Exception {
		Chunk<OpenDotaHeroDto> chunk = Chunk.of();
		heroWriter.write(chunk);
		// Nothing to verify beyond no interactions with repository
		org.mockito.Mockito.verifyNoInteractions(heroRepository);
	}

}
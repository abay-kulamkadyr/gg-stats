package com.abe.gg_stats.batch;

import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

public abstract class BaseWriter<T, E> implements ItemWriter<T> {

	protected final JpaRepository<E, ?> repository;

	@Autowired
	public BaseWriter(JpaRepository<E, ?> repository) {
		this.repository = repository;
	}

	@Override
	public void write(@NonNull Chunk<? extends T> chunk) throws Exception {
		if (chunk.isEmpty()) {
			return;
		}

		List<E> entities = chunk.getItems().stream().map(this::dtoToEntity).toList();
		repository.saveAll(entities);
	}

	protected abstract E dtoToEntity(T dto);

}

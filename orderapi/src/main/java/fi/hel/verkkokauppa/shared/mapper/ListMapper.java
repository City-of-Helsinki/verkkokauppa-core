package fi.hel.verkkokauppa.shared.mapper;

import java.util.List;
import java.util.function.BiConsumer;

public interface ListMapper {
	<S, T> List<T> mapList(List<S> sourceList, Class<T> targetType, BiConsumer<S, T> itemPostProcessor);
}

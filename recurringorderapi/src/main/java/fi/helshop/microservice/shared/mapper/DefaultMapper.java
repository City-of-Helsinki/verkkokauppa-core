package fi.helshop.microservice.shared.mapper;

import org.modelmapper.ModelMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class DefaultMapper implements Mapper {

	private final ModelMapper modelMapper;

    public DefaultMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	@Override
	public <T> T mapObject(Object sourceObject, Class<T> targetType) {
		return modelMapper.map(sourceObject, targetType);
	}

	@Override
	public void mapObject(Object sourceObject, Object targetObject) {
		modelMapper.map(sourceObject, targetObject);
	}

	@Override
	public <S, T> List<T> mapList(List<S> sourceList, Class<T> targetType, BiConsumer<S, T> itemPostProcessor) {
		final List<T> result = new ArrayList<>(sourceList.size());

		for (final S source : sourceList) {
			final T target = modelMapper.map(source, targetType);
			if (itemPostProcessor != null) {
				itemPostProcessor.accept(source, target);
			}
			result.add(target);
		}

		return result;
	}
}

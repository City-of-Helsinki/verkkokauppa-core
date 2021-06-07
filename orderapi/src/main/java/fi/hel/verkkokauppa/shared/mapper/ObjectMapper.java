package fi.hel.verkkokauppa.shared.mapper;

public interface ObjectMapper {

	public <T> T mapObject(Object sourceObject, Class<T> targetType);
	public void mapObject(Object sourceObject, Object targetObject);
}

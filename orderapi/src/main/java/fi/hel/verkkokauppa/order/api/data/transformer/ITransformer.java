package fi.hel.verkkokauppa.order.api.data.transformer;

public interface ITransformer<Entity, Dto> {

    Entity transformToEntity(Dto dto);

    Dto transformToDto(Entity entity);
}

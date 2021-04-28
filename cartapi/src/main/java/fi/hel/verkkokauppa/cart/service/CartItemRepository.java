package fi.hel.verkkokauppa.cart.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.cart.model.CartItem;

@Repository
public interface CartItemRepository extends ElasticsearchRepository<CartItem, String> {

    List<CartItem> findByCartId(String cartId);

}
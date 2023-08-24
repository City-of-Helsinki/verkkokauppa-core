package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemInvoicingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@Slf4j
public class OrderInvoicingControllerTest extends DummyData {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderItemInvoicingRepository orderItemInvoicingRepository;

    @Autowired
    private ObjectMapper mapper;

    private ArrayList<String> toBeDeletedOrderItemById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemInvoicingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderItemById.forEach(id -> orderItemRepository.deleteById(id));
            toBeDeletedOrderItemInvoicingById.forEach(id -> orderItemInvoicingRepository.deleteById(id));
            toBeDeletedOrderItemById = new ArrayList<>();
            toBeDeletedOrderItemInvoicingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderItemById = new ArrayList<>();
            toBeDeletedOrderItemInvoicingById = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void createOrderInvoicingThenReturn2xx() throws Exception {
        Order o = generateDummyOrder();
        OrderItem i1 = generateDummyOrderItem(o);
        i1 = orderItemRepository.save(i1);
        toBeDeletedOrderItemById.add(i1.getOrderItemId());
        assertNull(i1.getInvoicingStatus());

        OrderItemInvoicingDto dto1 = new OrderItemInvoicingDto();
        dto1.setOrderItemId(i1.getOrderItemId());
        dto1.setMaterial("material");
        ArrayList<OrderItemInvoicingDto> dtos = new ArrayList<>();
        dtos.add(dto1);

        MvcResult res = this.mockMvc.perform(
                        post("/order/invoicing/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dtos))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        dtos = mapper.readValue(res.getResponse().getContentAsString(), new TypeReference<ArrayList<OrderItemInvoicingDto>>(){});
        assertEquals(dtos.size(), 1);
        OrderItemInvoicingDto dto = dtos.get(0);
        assertNotNull(dto);
        toBeDeletedOrderItemInvoicingById.add(dto.getOrderItemId());
        assertEquals(dto.getMaterial(), "material");
        assertEquals(dto.getOrderItemId(), dto1.getOrderItemId());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
        assertEquals(dto.getStatus(), OrderItemInvoicingStatus.CREATED);

        OrderItemInvoicing orderItemInvoicing = orderItemInvoicingRepository.findById(dto.getOrderItemId()).orElseThrow();
        assertNotNull(orderItemInvoicing);

        i1 = orderItemRepository.findById(i1.getOrderItemId()).orElseThrow();
        assertEquals(i1.getInvoicingStatus(), OrderItemInvoicingStatus.CREATED);
    }
}

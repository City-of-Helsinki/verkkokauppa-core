package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderStatus;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemInvoicingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
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

import java.time.LocalDate;
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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderItemInvoicingRepository orderItemInvoicingRepository;

    @Autowired
    private ObjectMapper mapper;

    private ArrayList<String> toBeDeletedOrderItemById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemInvoicingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(id -> orderRepository.deleteById(id));
            toBeDeletedOrderItemById.forEach(id -> orderItemRepository.deleteById(id));
            toBeDeletedOrderItemInvoicingById.forEach(id -> orderItemInvoicingRepository.deleteById(id));
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderItemById = new ArrayList<>();
            toBeDeletedOrderItemInvoicingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
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

    @Test
    @RunIfProfile(profile = "local")
    public void exportInvoicingsThenReturn2xx() throws Exception {
        Order o1 = generateDummyOrder();
        o1 = orderRepository.save(o1);
        OrderItem o1i1 = generateDummyOrderItem(o1);
        o1i1 = orderItemRepository.save(o1i1);
        Order o2 = generateDummyOrder();
        o2.setOrderId("2");
        o2 = orderRepository.save(o2);
        OrderItem o2i1 = orderItemRepository.save(generateDummyOrderItem(o2));
        OrderItem o2i2 = orderItemRepository.save(generateDummyOrderItem(o2));
        Order o3 = generateDummyOrder();
        o3.setOrderId("3");
        o3.setStatus(OrderStatus.CANCELLED);
        o3 = orderRepository.save(o3);
        OrderItem o3i1 = orderItemRepository.save(generateDummyOrderItem(o3));

        toBeDeletedOrderById.add(o1.getOrderId());
        toBeDeletedOrderById.add(o2.getOrderId());
        toBeDeletedOrderById.add(o3.getOrderId());

        toBeDeletedOrderItemById.add(o1i1.getOrderItemId());
        toBeDeletedOrderItemById.add(o2i1.getOrderItemId());
        toBeDeletedOrderItemById.add(o2i2.getOrderItemId());
        toBeDeletedOrderItemById.add(o3i1.getOrderItemId());

        OrderItemInvoicingDto dtoo1i1 = new OrderItemInvoicingDto();
        dtoo1i1.setOrderId("1");
        dtoo1i1.setOrderIncrementId("inc1");
        dtoo1i1.setOrderItemId(o1i1.getOrderItemId());
        dtoo1i1.setMaterial("material");
        dtoo1i1.setInvoicingDate(LocalDate.now());
        dtoo1i1.setQuantity(1);
        dtoo1i1.setOrderType("ot1");
        dtoo1i1.setSalesOrg("sorg1");
        dtoo1i1.setSalesOffice("sof1");
        dtoo1i1.setCustomerYid("yid1");
        dtoo1i1.setCustomerOvt("ovt1");
        dtoo1i1.setCustomerName("cn1");
        dtoo1i1.setCustomerAddress("ca1");
        dtoo1i1.setCustomerPostcode("cpc1");
        dtoo1i1.setCustomerCity("cc1");

        OrderItemInvoicingDto dtoo2i1 = new OrderItemInvoicingDto();
        dtoo2i1.setOrderId("2");
        dtoo2i1.setOrderIncrementId("inc2");
        dtoo2i1.setOrderItemId(o2i1.getOrderItemId());
        dtoo2i1.setMaterial("material21");
        dtoo2i1.setInvoicingDate(LocalDate.now());
        dtoo2i1.setQuantity(21);
        dtoo2i1.setOrderType("ot2");
        dtoo2i1.setSalesOrg("sorg2");
        dtoo2i1.setSalesOffice("sof2");
        dtoo2i1.setCustomerYid("yid2");
        dtoo2i1.setCustomerOvt("ovt2");
        dtoo2i1.setCustomerName("cn2");
        dtoo2i1.setCustomerAddress("ca2");
        dtoo2i1.setCustomerPostcode("cpc2");
        dtoo2i1.setCustomerCity("cc2");

        OrderItemInvoicingDto dtoo2i2 = new OrderItemInvoicingDto();
        dtoo2i2.setOrderId("2");
        dtoo2i2.setOrderIncrementId("inc2");
        dtoo2i2.setOrderItemId(o2i2.getOrderItemId());
        dtoo2i2.setMaterial("material22");
        dtoo2i2.setInvoicingDate(LocalDate.now());
        dtoo2i2.setQuantity(1);
        dtoo2i2.setOrderType("ot2");
        dtoo2i2.setSalesOrg("sorg2");
        dtoo2i2.setSalesOffice("sof2");
        dtoo2i2.setCustomerYid("yid2");
        dtoo2i2.setCustomerOvt("ovt2");
        dtoo2i2.setCustomerName("cn2");
        dtoo2i2.setCustomerAddress("ca2");
        dtoo2i2.setCustomerPostcode("cpc2");
        dtoo2i2.setCustomerCity("cc2");

        OrderItemInvoicingDto dtoo3i1 = new OrderItemInvoicingDto();
        dtoo3i1.setOrderId("3");
        dtoo3i1.setOrderIncrementId("inc3");
        dtoo3i1.setOrderItemId(o3i1.getOrderItemId());
        dtoo3i1.setMaterial("material31");
        dtoo3i1.setInvoicingDate(LocalDate.now());
        dtoo3i1.setQuantity(31);
        dtoo3i1.setOrderType("ot3");
        dtoo3i1.setSalesOrg("sorg3");
        dtoo3i1.setSalesOffice("sof3");
        dtoo3i1.setCustomerYid("yid3");
        dtoo3i1.setCustomerOvt("ovt3");
        dtoo3i1.setCustomerName("cn3");
        dtoo3i1.setCustomerAddress("ca3");
        dtoo3i1.setCustomerPostcode("cpc3");
        dtoo3i1.setCustomerCity("cc3");

        ArrayList<OrderItemInvoicingDto> dtos = new ArrayList<>();
        dtos.add(dtoo1i1);
        dtos.add(dtoo2i1);
        dtos.add(dtoo2i2);
        dtos.add(dtoo3i1);

        MvcResult res = this.mockMvc.perform(
                        post("/order/invoicing/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dtos))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        toBeDeletedOrderItemInvoicingById.add(dtoo1i1.getOrderItemId());
        toBeDeletedOrderItemInvoicingById.add(dtoo2i1.getOrderItemId());
        toBeDeletedOrderItemInvoicingById.add(dtoo2i2.getOrderItemId());
        toBeDeletedOrderItemInvoicingById.add(dtoo3i1.getOrderItemId());

        this.mockMvc.perform(
                        post("/invoicing/export")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        o1i1 = orderItemRepository.findById(o1i1.getOrderItemId()).orElseThrow();
        assertEquals(o1i1.getInvoicingStatus(), OrderItemInvoicingStatus.INVOICED);
        assertNotNull(o1i1.getInvoicingIncrementId());
        o2i1 = orderItemRepository.findById(o2i1.getOrderItemId()).orElseThrow();
        assertEquals(o2i1.getInvoicingStatus(), OrderItemInvoicingStatus.INVOICED);
        assertNotNull(o2i1.getInvoicingIncrementId());
        o2i2 = orderItemRepository.findById(o2i2.getOrderItemId()).orElseThrow();
        assertEquals(o2i2.getInvoicingStatus(), OrderItemInvoicingStatus.INVOICED);
        assertNotNull(o2i2.getInvoicingIncrementId());
        o3i1 = orderItemRepository.findById(o3i1.getOrderItemId()).orElseThrow();
        assertNull(o3i1.getInvoicingIncrementId());

        OrderItemInvoicing orderItemInvoicing = orderItemInvoicingRepository.findById(dtoo1i1.getOrderItemId()).orElseThrow();
        assertEquals(orderItemInvoicing.getStatus(), OrderItemInvoicingStatus.INVOICED);
        orderItemInvoicing = orderItemInvoicingRepository.findById(dtoo2i1.getOrderItemId()).orElseThrow();
        assertEquals(orderItemInvoicing.getStatus(), OrderItemInvoicingStatus.INVOICED);
        orderItemInvoicing = orderItemInvoicingRepository.findById(dtoo2i2.getOrderItemId()).orElseThrow();
        assertEquals(orderItemInvoicing.getStatus(), OrderItemInvoicingStatus.INVOICED);
        orderItemInvoicing = orderItemInvoicingRepository.findById(dtoo3i1.getOrderItemId()).orElseThrow();
        assertEquals(orderItemInvoicing.getStatus(), OrderItemInvoicingStatus.CANCELLED);
    }
}

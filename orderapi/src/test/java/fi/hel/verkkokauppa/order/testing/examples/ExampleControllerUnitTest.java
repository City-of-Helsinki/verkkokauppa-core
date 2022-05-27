package fi.hel.verkkokauppa.order.testing.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;


/**
 * This class is used to test the controller layer of the application
 *
 * Change SubscriptionAdminController.class to controller which you want to test.
 */
//@WebMvcTest(SubscriptionAdminController.class) // Change and uncomment
//@Import(SubscriptionAdminController.class) // Change and uncomment
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
public class ExampleControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
//    @MockBean
//    private SubscriptionCardExpiredService service;

    @Test
    public void exampleControllerUnitTest() throws Exception {
//        when(mockMvc.perform(any())).thenReturn(null);
//        this.mockMvc.perform(
//                        get("/example-api")
//                )
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(content().string(mapper.writeValueAsString(new Object())));

//        verify(mockMvc, times(1)).perform(any());
        Assertions.assertTrue(true);
    }

}

package fi.hel.verkkokauppa.order.id;

import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class IncrementIdTest {
  @Autowired
  IncrementId incrementId;

  @Test
  @RunIfProfile(profile = "local")
  public void generateOrderIncrementId() throws InterruptedException {
    Integer numOfIds = 100;
    ExecutorService executor = Executors.newFixedThreadPool(numOfIds);
    CountDownLatch latch = new CountDownLatch(numOfIds);
    List<Long> ids = Collections.synchronizedList(new ArrayList<>());
    for (int i = 0; i < numOfIds; ++i) {
      executor.execute(() -> {
        Long id = incrementId.generateOrderIncrementId();
        if (id != null) {
          ids.add(id);
        }
        latch.countDown();
      });
    }
    latch.await();
    // Check all ids were generated
    Assertions.assertEquals(ids.size(), numOfIds);
    Set<Long> set = new HashSet<>(ids);
    // Check all ids are unique
    Assertions.assertEquals(ids.size(), set.size());
    // Check ids are in a consecutive sequence
    Assertions.assertEquals(Collections.max(set) - Collections.min(set) + 1, Long.valueOf(numOfIds));
  }
}
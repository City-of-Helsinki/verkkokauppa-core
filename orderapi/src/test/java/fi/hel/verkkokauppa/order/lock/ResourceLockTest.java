package fi.hel.verkkokauppa.order.lock;

import fi.hel.verkkokauppa.common.lock.ResourceLock;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.junit.Assert;
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
@RunIfProfile(profile = "local")
public class ResourceLockTest {
    @Autowired
    ResourceLock resourceLock;

    @Test
    @RunIfProfile(profile = "local")
    public void obtainLock() throws InterruptedException {
        String id = UUIDGenerator.generateType4UUID().toString();

        int numOfObtains = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numOfObtains);
        CountDownLatch latch = new CountDownLatch(numOfObtains);
        List<Boolean> obtains = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < numOfObtains; ++i) {
            executor.execute(() -> {
                try {
                    obtains.add(resourceLock.obtain(id));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        // Check all obtains were generated
        Assertions.assertEquals(obtains.size(), numOfObtains);
        // Check only one obtain obtained the lock
        Assert.assertEquals(obtains.stream().filter(x -> x).count(), 1);
        Assert.assertEquals(obtains.stream().filter(x -> !x).count(), numOfObtains - 1);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void releaseLock() throws IOException {
        String id = UUIDGenerator.generateType4UUID().toString();
        resourceLock.release(id);
        Assert.assertTrue(resourceLock.obtain(id));
        resourceLock.release(id);
        Assert.assertTrue(resourceLock.obtain(id));
        resourceLock.release(id);
    }
}

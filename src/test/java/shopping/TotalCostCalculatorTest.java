package shopping;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static shopping.TotalCostCalculator.*;

public class TotalCostCalculatorTest {
    private static final Set<CatalogItem> catalog = new HashSet<>(Arrays.asList(
            new CatalogItem("Apple", new BigDecimal("0.35")),
            new CatalogItem("Banana", new BigDecimal("0.20")),
            new CatalogItem("Melon", new BigDecimal("0.50"), TwoForOne),
            new CatalogItem("Lime", new BigDecimal("0.15"), ThreeForTwo)
    ));

    private static final TotalCostCalculator totalCostCalculator = new TotalCostCalculator(catalog);

    @Test
    public void TotalPrice_BasketContainingOneApple_Calculated() {
        final List<String> basket = Collections.singletonList("Apple");
        assertEquals(new BigDecimal("0.35"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_BasketContainingOneBanana_Calculated() {
        final List<String> basket = Collections.singletonList("Banana");
        assertEquals(new BigDecimal("0.20"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_BasketContainingTwoApplesAndOneBanana_Calculated() {
        final List<String> basket = Arrays.asList("Apple", "Apple", "Banana");
        assertEquals(new BigDecimal("0.90"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_BasketContainingTwoMelons_Calculated() {
        final List<String> basket = Arrays.asList("Melon", "Melon");
        assertEquals(new BigDecimal("0.50"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_BasketContainingThreeLimes_Calculated() {
        final List<String> basket = Arrays.asList("Lime", "Lime", "Lime");
        assertEquals(new BigDecimal("0.30"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_BasketContainingTwoApplesOneBananaTwoMelonsAndThreeLimes_Calculated() {
        final List<String> basket = Arrays.asList("Apple", "Apple", "Banana", "Melon", "Melon", "Lime", "Lime", "Lime");
        assertEquals(new BigDecimal("1.70"), totalCostCalculator.calculate(basket));
    }

    @Test
    public void TotalPrice_EmptyBasket_Calculated() {
        assertEquals(BigDecimal.ZERO, totalCostCalculator.calculate(Collections.emptyList()));
    }

    @Test
    public void TotalPrice_NullBasket_ExceptionThrown() {
        final List<String> basket = null;
        calculateTotalCostExpectingException(basket, nullBasketException());
    }

    @Test
    public void TotalPrice_BasketContainingAnUnknownItem_ExceptionThrown() {
        final List<String> basket = Collections.singletonList("Avocado");
        calculateTotalCostExpectingException(basket, unsupportedBasketException(basket));
    }

    @Test
    public void TotalPrice_MultiThreadedEachCatalogItemInIndividualBasket_Calculated() throws InterruptedException {
        List<Runnable> runnables = catalog.stream().map(toCalculateTotalCostForCatalogItemRunnable).collect(Collectors.toList());
        assertConcurrent("MultiThreadedEachCatalogItemInIndividualBasket", runnables, catalog.size());
    }

    private static final Function<CatalogItem, Runnable> toCalculateTotalCostForCatalogItemRunnable = item -> new Runnable() {
        @Override
        public void run() {
            List<String> basket = Collections.singletonList(item.getName());
            assertEquals(item.getPrice(), totalCostCalculator.calculate(basket));
        }
    };

    private void calculateTotalCostExpectingException(List<String> basket, RuntimeException expectedException) {
        boolean thrown = false;
        try {
            totalCostCalculator.calculate(basket);
        } catch (BasketException e) {
            thrown = true;
            assertEquals(expectedException.getMessage(), e.getMessage());
        }
        if (!thrown) {
            fail("Failed to throw BasketException");
        }
    }

    public static void assertConcurrent(final String message, final List<? extends Runnable> runnables, final int maxTimeoutSeconds) throws InterruptedException {
        final int numThreads = runnables.size();
        final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        try {
            final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
            final CountDownLatch afterInitBlocker = new CountDownLatch(1);
            final CountDownLatch allDone = new CountDownLatch(numThreads);
            for (final Runnable submittedTestRunnable : runnables) {
                threadPool.submit(() -> {
                    allExecutorThreadsReady.countDown();
                    try {
                        afterInitBlocker.await();
                        submittedTestRunnable.run();
                    } catch (final Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }
            assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnables to assertConcurrent", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
            afterInitBlocker.countDown();
            assertTrue(message + " timeout! More than" + maxTimeoutSeconds + "seconds", allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
        } finally {
            threadPool.shutdownNow();
        }
        assertTrue(message + "Failed with exception(s)" + exceptions, exceptions.isEmpty());
    }
}
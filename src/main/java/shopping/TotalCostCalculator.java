package shopping;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TotalCostCalculator {
    private final Map<String, CatalogItem> catalogItemsByName;

    TotalCostCalculator(final Set<CatalogItem> catalog) {
        this.catalogItemsByName = Collections.unmodifiableMap(catalog.stream().collect(Collectors.toMap(CatalogItem::getName, Function.identity())));
    }

    static final LineQuantityCalculator Normal = quantity -> quantity;

    static final LineQuantityCalculator TwoForOne = quantity -> quantity / 2 + quantity % 2;

    static final LineQuantityCalculator ThreeForTwo = quantity -> quantity / 3 * 2 + quantity % 3;

    public BigDecimal calculate(final List<String> items) {
        if (items == null) throw nullBasketException();
        final Map<String, Long> itemNamesByCount = Collections.unmodifiableList(items).stream().collect(toItemsGroupedByCount);
        final Set<String> uniqueItemNames = itemNamesByCount.keySet();
        if (!catalogItemsByName.keySet().containsAll(uniqueItemNames)) throw unsupportedBasketException(items);
        final Function<String, BigDecimal> toItemLinePrice = name -> {
            final CatalogItem item = catalogItemsByName.get(name);
            final Long quantity = itemNamesByCount.get(name);
            final LineQuantityCalculator lineQuantityCalculator = item.getLineQuantityCalculator();
            final Long lineQuantity = lineQuantityCalculator.calculate(quantity);
            return BigDecimal.valueOf(lineQuantity).multiply(item.getPrice());
        };
        return uniqueItemNames.stream().map(toItemLinePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final Collector<String, ?, Map<String, Long>> toItemsGroupedByCount = Collectors.groupingBy(Function.identity(), Collectors.counting());

    static RuntimeException nullBasketException() {
        return new BasketException("Basket is null");
    }

    static RuntimeException unsupportedBasketException(final List<String> items) {
        return new BasketException("Basket " + items + " is not fully supported by this catalog.");
    }
}
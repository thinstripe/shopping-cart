package shopping;

import java.math.BigDecimal;

import static shopping.TotalCostCalculator.Normal;

class CatalogItem {
    private final String name;
    private final BigDecimal price;
    private final LineQuantityCalculator lineQuantityCalculator;

    public CatalogItem(final String name, final BigDecimal price, final LineQuantityCalculator lineQuantityCalculator) {
        this.name = name;
        this.price = price;
        this.lineQuantityCalculator = lineQuantityCalculator;
    }

    public CatalogItem(final String name, final BigDecimal price) {
        this(name, price, Normal);
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LineQuantityCalculator getLineQuantityCalculator() {
        return lineQuantityCalculator;
    }

    @Override
    public String toString() {
        return "CatalogItem{" + "name='" + name + '\'' + ", price=" + price + ", quantityChargeCalculator=" + lineQuantityCalculator + '}';
    }
}

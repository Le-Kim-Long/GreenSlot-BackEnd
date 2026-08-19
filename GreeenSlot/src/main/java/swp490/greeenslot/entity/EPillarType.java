package swp490.greeenslot.entity;

import java.math.BigDecimal;

public enum EPillarType {
    SMALL("Trụ Nhỏ", 24, 1.0, BigDecimal.valueOf(150000)),
    MEDIUM("Trụ Vừa", 36, 1.5, BigDecimal.valueOf(200000)),
    LARGE("Trụ Lớn", 48, 2.0, BigDecimal.valueOf(300000));

    private final String displayName;
    private final int defaultHoles;
    private final double minRequiredArea;
    private final BigDecimal defaultPrice;

    EPillarType(String displayName, int defaultHoles, double minRequiredArea, BigDecimal defaultPrice) {
        this.displayName = displayName;
        this.defaultHoles = defaultHoles;
        this.minRequiredArea = minRequiredArea;
        this.defaultPrice = defaultPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultHoles() {
        return defaultHoles;
    }

    public double getMinRequiredArea() {
        return minRequiredArea;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }
}
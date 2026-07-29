package com.ryan.media.ranking;

public final class RankerException extends RuntimeException {
    private final String code;
    private final String rankerVersion;
    private final String bundleDigest;
    private final RankerPromotionStatus promotionStatus;

    public RankerException(String code, String message) {
        this(code, message, null, null, null);
    }

    public RankerException(
            String code,
            String message,
            String rankerVersion,
            String bundleDigest,
            RankerPromotionStatus promotionStatus) {
        super(message);
        this.code = code;
        this.rankerVersion = rankerVersion;
        this.bundleDigest = bundleDigest;
        this.promotionStatus = promotionStatus;
    }

    public String code() { return code; }
    public String rankerVersion() { return rankerVersion; }
    public String bundleDigest() { return bundleDigest; }
    public RankerPromotionStatus promotionStatus() { return promotionStatus; }
}

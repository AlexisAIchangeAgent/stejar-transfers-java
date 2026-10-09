package md.stejar.transfers.domain;

import java.util.Arrays;

/** Segment of the customer, with its code in the API ("retail", "premium"). */
public enum CustomerSegment {
    RETAIL("retail"),
    PREMIUM("premium");

    private final String code;

    CustomerSegment(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static CustomerSegment fromCode(String code) {
        return Arrays.stream(values())
                .filter(segment -> segment.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown customer segment: " + code));
    }
}

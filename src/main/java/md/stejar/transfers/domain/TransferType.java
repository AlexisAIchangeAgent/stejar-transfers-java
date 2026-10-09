package md.stejar.transfers.domain;

import java.util.Arrays;

/** Type of a transfer, with its code in the API ("standard", "instant"). */
public enum TransferType {
    STANDARD("standard"),
    INSTANT("instant");

    private final String code;

    TransferType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TransferType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown transfer type: " + code));
    }
}

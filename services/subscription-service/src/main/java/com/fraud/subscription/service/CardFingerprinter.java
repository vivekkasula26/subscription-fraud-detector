package com.fraud.subscription.service;

import com.fraud.subscription.config.SubscriptionProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Turns a card number into a stable, irreversible token (HMAC-SHA256) so the same card
 * can be correlated across accounts without the number ever being stored.
 */
@Component
public class CardFingerprinter {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public CardFingerprinter(SubscriptionProperties props) {
        this.key = new SecretKeySpec(props.cardFingerprintSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String fingerprint(String cardNumber) {
        String digits = cardNumber.replaceAll("[^0-9]", "");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal(digits.getBytes(StandardCharsets.UTF_8));
            return "cfp_" + HexFormat.of().formatHex(hash, 0, 8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute card fingerprint", e);
        }
    }
}

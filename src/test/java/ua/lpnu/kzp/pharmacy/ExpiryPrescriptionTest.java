package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Перевіряє record {@link ExpiryPrescription}: доступ до компонентів, рівність і валідацію. */
class ExpiryPrescriptionTest {

    @Test
    void exposesComponents() {
        ExpiryPrescription summary = new ExpiryPrescription(45, 2);

        assertEquals(45, summary.minDaysToExpire());
        assertEquals(2, summary.prescriptionCount());
    }

    /* Record порівнюється за значеннями компонентів, а не за посиланням. */
    @Test
    void usesValueEquality() {
        ExpiryPrescription first = new ExpiryPrescription(45, 2);
        ExpiryPrescription second = new ExpiryPrescription(45, 2);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new ExpiryPrescription(45, 3));
    }

    @Test
    void toStringContainsComponentNames() {
        assertEquals("ExpiryPrescription[minDaysToExpire=45, prescriptionCount=2]",
                new ExpiryPrescription(45, 2).toString());
    }

    @Test
    void acceptsZeroes() {
        assertEquals(0, new ExpiryPrescription(0, 0).minDaysToExpire());
    }

    @Test
    void rejectsNegativeComponents() {
        assertThrows(IllegalArgumentException.class, () -> new ExpiryPrescription(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ExpiryPrescription(0, -1));
    }
}

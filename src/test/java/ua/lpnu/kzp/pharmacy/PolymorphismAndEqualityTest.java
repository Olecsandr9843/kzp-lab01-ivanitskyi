package ua.lpnu.kzp.pharmacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Перевіряє поліморфну обробку через базовий тип і узгодженість equals/hashCode у колекціях. */
class PolymorphismAndEqualityTest {

    // ---------- Фабрика та поліморфізм ----------

    @Test
    void fromCsvCreatesSubtypeByPrescriptionField() {
        assertInstanceOf(PrescriptionMedicine.class, Medicine.fromCsv("Амоксицилін;капсули;128.90;180;true"));
        assertInstanceOf(FreeMedicine.class, Medicine.fromCsv("Парацетамол;таблетки;45.50;365;false"));
    }

    /* Колекція базового типу: кожен елемент сам знає, як рахувати доступність. */
    @Test
    void heterogeneousListIsProcessedThroughBaseType() {
        List<Medicine> medicines = List.of(
                Medicine.fromCsv("Парацетамол;таблетки;45.50;365;false"),
                Medicine.fromCsv("Амоксицилін;капсули;128.90;180;true"),
                Medicine.fromCsv("Ібупрофен;суспензія;96.00;90;false"),
                Medicine.fromCsv("Еналаприл;таблетки;73.60;45;true"));

        int total = 0;
        for (Medicine medicine : medicines) {
            total += medicine.availabilityScore(); // без instanceof і switch за підтипом
        }

        // 96 + 48 + 91 + 53
        assertEquals(288, total);
    }

    /* Зірочка: ієрархія sealed, тому switch за підтипами вичерпний без default. */
    @Test
    void sealedHierarchyAllowsExhaustiveSwitch() {
        Medicine medicine = Medicine.fromCsv("Еналаприл;таблетки;73.60;45;true");

        String rule = switch (medicine) {
            case PrescriptionMedicine p -> "потрібен рецепт";
            case FreeMedicine f -> "вільний продаж";
        };

        assertEquals("потрібен рецепт", rule);
    }

    // ---------- equals / hashCode ----------

    /* Логічна рівність: підтип + назва + форма; ціна й термін — властивості партії. */
    @Test
    void sameSubtypeNameAndFormAreEqual() {
        Medicine first = new FreeMedicine("Парацетамол", "таблетки", 45.50, 365);
        Medicine secondBatch = new FreeMedicine("Парацетамол", "таблетки", 39.90, 120);

        assertEquals(first, secondBatch);
        assertEquals(secondBatch, first);
        assertEquals(first.hashCode(), secondBatch.hashCode());
    }

    @Test
    void differentFormOrNameAreNotEqual() {
        Medicine tablets = new FreeMedicine("Парацетамол", "таблетки", 45.50, 365);

        assertNotEquals(tablets, new FreeMedicine("Парацетамол", "сироп", 45.50, 365));
        assertNotEquals(tablets, new FreeMedicine("Ібупрофен", "таблетки", 45.50, 365));
    }

    /* Однакова назва, але різні підтипи — різні препарати, і рівність симетрична. */
    @Test
    void differentSubtypesAreNotEqual() {
        Medicine prescription = new PrescriptionMedicine("Ліки", "таблетки", 10.0, 10);
        Medicine free = new FreeMedicine("Ліки", "таблетки", 10.0, 10);

        assertNotEquals(prescription, free);
        assertNotEquals(free, prescription);
    }

    @Test
    void equalsHandlesSelfNullAndOtherTypes() {
        Medicine medicine = new FreeMedicine("Парацетамол", "таблетки", 45.50, 365);

        assertEquals(medicine, medicine);
        assertFalse(medicine.equals(null));
        assertFalse(medicine.equals("Парацетамол"));
    }

    @Test
    void hashSetKeepsOnlyUniqueMedicines() {
        Set<Medicine> set = new HashSet<>(List.of(
                new FreeMedicine("Парацетамол", "таблетки", 45.50, 365),
                new FreeMedicine("Парацетамол", "таблетки", 39.90, 120),
                new FreeMedicine("Парацетамол", "сироп", 80.00, 200),
                new PrescriptionMedicine("Парацетамол", "таблетки", 45.50, 365)));

        assertEquals(3, set.size());
        assertTrue(set.contains(new FreeMedicine("Парацетамол", "таблетки", 1.0, 1)));
    }

    @Test
    void hashMapFindsValueByEqualKey() {
        Map<Medicine, Integer> stock = new HashMap<>();
        stock.put(new PrescriptionMedicine("Амоксицилін", "капсули", 128.90, 180), 5);

        stock.merge(new PrescriptionMedicine("Амоксицилін", "капсули", 120.00, 90), 3, Integer::sum);

        assertEquals(1, stock.size());
        assertEquals(Integer.valueOf(8), stock.get(new PrescriptionMedicine("Амоксицилін", "капсули", 1.0, 1)));
    }
}

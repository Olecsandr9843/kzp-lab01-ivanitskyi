package ua.lpnu.kzp.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке потрапляє до експорту, і задає назву його стовпця.
 *
 * <p>{@link RetentionPolicy#RUNTIME} зберігає анотацію в байт-коді й робить її доступною
 * рефлексії під час виконання; {@link ElementType#FIELD} дозволяє ставити її лише на поля
 * (зокрема на компоненти record — компілятор переносить її на відповідне приватне поле).
 *
 * <pre>{@code
 * public record Item(@CsvColumn("назва") String name, @CsvColumn("ціна") double price) { }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Назва стовпця в заголовку CSV і ключ у JSON.
     *
     * @return непорожня назва стовпця
     */
    String value();
}

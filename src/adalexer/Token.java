package adalexer;

/**
 * Класс, представляющий одну лексему (токен) в исходном коде Ada.
 * Каждый токен содержит:
 * - тип (из перечисления TokenType)
 * - значение (исходный текст лексемы)
 * - имя файла, где найден
 * - номер строки
 * - (необязательно) номер колонки - можно добавить при необходимости
 */
public class Token {

    // Тип токена (KEYWORD, IDENT, INT, и т.д.)
    private TokenType type;

    // Лексическое значение (исходный текст, например "begin", "123", "myVar")
    private String value;

    // Имя исходного файла, из которого взят токен
    private String filename;

    // Номер строки в исходном файле (начинается с 1)
    private int line;

    /**
     * Конструктор токена
     * @param type тип лексемы
     * @param value исходное текстовое представление
     * @param filename имя файла
     * @param line номер строки (от 1)
     */
    public Token(TokenType type, String value, String filename, int line) {
        this.type = type;
        this.value = value;
        this.filename = filename;
        this.line = line;
    }

    // ============ Геттеры ============
    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getFilename() {
        return filename;
    }

    public int getLine() {
        return line;
    }

    /**
     * Строковое представление токена для отладки/вывода
     * Формат: ТИП(значение) в файл:строка
     */
    @Override
    public String toString() {
        if (type == TokenType.EOF) {
            return "<EOF>";
        }
        if (type == TokenType.ERROR) {
            return String.format("<ERROR: %s at %s:%d>", value, filename, line);
        }
        return String.format("%s('%s') at %s:%d", type, value, filename, line);
    }

    /**
     * Краткое представление для статистики
     * Возвращает только тип и значение без информации о расположении
     */
    public String toShortString() {
        if (type == TokenType.EOF) return "<EOF>";
        if (type == TokenType.ERROR) return "<ERROR>";
        return type + "('" + value + "')";
    }
}
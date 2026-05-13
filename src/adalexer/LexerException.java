package adalexer;

/**
 * Класс для лексических ошибок.
 * Возникает, когда анализатор встречает недопустимый символ
 * или неправильно сформированную лексему.
 *
 * Содержит всю необходимую информацию об ошибке:
 * - сообщение (что именно неверно)
 * - имя файла
 * - номер строки
 * - номер колонки (позиция в строке)
 */
public class LexerException extends Exception {

    // Исходный файл, в котором произошла ошибка
    private String filename;

    // Номер строки (1-индексация)
    private int line;

    // Номер колонки (позиция символа в строке, 1-индексация)
    private int column;

    /**
     * Конструктор ошибки
     * @param message текст ошибки
     * @param filename имя файла
     * @param line номер строки
     * @param column номер колонки
     */
    public LexerException(String message, String filename, int line, int column) {
        super(message);      // родительский класс сохраняет сообщение
        this.filename = filename;
        this.line = line;
        this.column = column;
    }

    // ============ Геттеры для информации об ошибке ============
    public String getFilename() { return filename; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    /**
     * Форматированное сообщение об ошибке в стиле компилятора
     * Пример: [LEXICAL ERROR] Неверный символ '#' at test.ada:5:12
     */
    @Override
    public String toString() {
        return String.format("[LEXICAL ERROR] %s at %s:%d:%d",
                getMessage(), filename, line, column);
    }
}

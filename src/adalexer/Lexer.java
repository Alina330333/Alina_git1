package adalexer;

import java.io.*;
import java.util.*;

/**
 * Лексический анализатор (сканер) для языка программирования Ada.
 *
 * Основные функции:
 * 1. Чтение исходного файла
 * 2. Разбиение на токены согласно правилам лексики Ada
 * 3. Игнорирование пробелов и комментариев
 * 4. Обработка лексических ошибок
 *
 * Принцип работы:
 * - Просматриваем символы последовательно слева направо
 * - Для каждого символа определяем, что начинается (цифра, буква, оператор...)
 * - Собираем максимальную лексему, соответствующую правилу
 * - Возвращаем токен
 *
 * Алгоритм основан на ДКА (детерминированном конечном автомате)
 */
public class Lexer {

    // ==================== ПОЛЯ КЛАССА ====================

    private String filename;           // Имя обрабатываемого файла
    private String content;            // Содержимое файла (весь текст)
    private int pos;                   // Текущая позиция в строке content
    private int line;                  // Номер текущей строки (начинается с 1)
    private int lineStart;             // Индекс в content начала текущей строки

    // ==================== СТАТИЧЕСКИЕ СЛОВАРИ ДЛЯ БЫСТРОГО ПОИСКА ====================

    /**
     * Множество ключевых слов Ada (все в ВЕРХНЕМ регистре)
     * Регистронезависимость достигается приведением к верхнему регистру
     */
    private static final Set<String> KEYWORDS = new HashSet<>();

    /**
     * Соответствие строки ключевого слова и типа токена
     * "BEGIN" -> TokenType.KW_BEGIN
     */
    private static final Map<String, TokenType> KEYWORD_TO_TYPE = new HashMap<>();

    /**
     * Мультисимвольные операторы (длина 2 или 3 символа)
     * Распознаются "жадно" - берём максимально длинный оператор
     */
    private static final Map<String, TokenType> MULTI_OPS = new HashMap<>();

    /**
     * Односимвольные операторы и разделители
     */
    private static final Map<Character, TokenType> SINGLE_OPS = new HashMap<>();

    // ==================== СТАТИЧЕСКАЯ ИНИЦИАЛИЗАЦИЯ (заполнение словарей) ====================

    static {
        // ---- Инициализация ключевых слов (полный список) ----
        // Всего 79 ключевых слов согласно стандарту ISO/IEC 8652:1995/2005/2012
        String[] keywords = {
                "ABORT", "ABS", "ABSTRACT", "ACCEPT", "ACCESS", "ALL", "AND", "ARRAY",
                "AT", "BEGIN", "BODY", "CASE", "CONSTANT", "DECLARE", "DELAY", "DELTA",
                "DIGITS", "DO", "ELSE", "ELSIF", "END", "ENTRY", "EXCEPTION", "EXIT",
                "FOR", "FUNCTION", "GENERIC", "GOTO", "IF", "IN", "INTERFACE", "IS",
                "LIMITED", "LOOP", "MOD", "NEW", "NOT", "NULL", "OF", "OR", "OTHERS",
                "OUT", "OVERRIDING", "PACKAGE", "PARALLEL", "PRAGMA", "PRIVATE",
                "PROCEDURE", "PROTECTED", "RAISE", "RANGE", "RECORD", "REM", "RENAMES",
                "REQUEUE", "RETURN", "SELECT", "SEPARATE", "SOME", "SUBTYPE",
                "SYNCHRONIZED", "TAG", "TASK", "TERMINATE", "THEN", "TYPE", "UNTIL",
                "USE", "WHEN", "WHILE", "WITH", "XOR"
        };

        // Заполняем множество ключевых слов
        for (String kw : keywords) {
            KEYWORDS.add(kw);
        }

        // Заполняем маппинг ключевое_слово -> тип_токена
        // Используем рефлексию для получения константы перечисления
        // Имя константы = "KW_" + ключевое_слово (например "KW_BEGIN")
        for (String kw : KEYWORDS) {
            try {
                // Преобразуем строку "BEGIN" в TokenType.KW_BEGIN
                TokenType type = TokenType.valueOf("KW_" + kw);
                KEYWORD_TO_TYPE.put(kw, type);
            } catch (IllegalArgumentException e) {
                // Этого не должно случиться - все ключевые слова должны быть определены
                System.err.println("Ошибка: ключевое слово " + kw + " не определено в TokenType");
            }
        }

        // ---- Мультисимвольные операторы (жадное распознавание) ----
        MULTI_OPS.put(":=", TokenType.ASSIGN);   // Присваивание
        MULTI_OPS.put("=>", TokenType.ARROW);    // Ассоциация
        MULTI_OPS.put("..", TokenType.DOTDOT);   // Диапазон
        MULTI_OPS.put("**", TokenType.POW);      // Возведение в степень
        MULTI_OPS.put("<>", TokenType.BOX);      // Пустой ящик
        MULTI_OPS.put(">=", TokenType.GE);       // Больше или равно
        MULTI_OPS.put("<=", TokenType.LE);       // Меньше или равно
        MULTI_OPS.put("/=", TokenType.NEQ);      // Не равно
        MULTI_OPS.put("<<", TokenType.SHIFT_L);  // Левый сдвиг
        MULTI_OPS.put(">>", TokenType.SHIFT_R);  // Правый сдвиг

        // ---- Односимвольные операторы и разделители ----
        SINGLE_OPS.put('+', TokenType.PLUS);
        SINGLE_OPS.put('-', TokenType.MINUS);
        SINGLE_OPS.put('*', TokenType.STAR);
        SINGLE_OPS.put('/', TokenType.SLASH);
        SINGLE_OPS.put('=', TokenType.EQ);
        SINGLE_OPS.put('<', TokenType.LT);
        SINGLE_OPS.put('>', TokenType.GT);
        SINGLE_OPS.put('&', TokenType.AMP);
        SINGLE_OPS.put('\'', TokenType.TICK);
        SINGLE_OPS.put('(', TokenType.LPAREN);
        SINGLE_OPS.put(')', TokenType.RPAREN);
        SINGLE_OPS.put('[', TokenType.LBRACK);
        SINGLE_OPS.put(']', TokenType.RBRACK);
        SINGLE_OPS.put('{', TokenType.LBRACE);
        SINGLE_OPS.put('}', TokenType.RBRACE);
        SINGLE_OPS.put(';', TokenType.SEMICOL);
        SINGLE_OPS.put(':', TokenType.COLON);
        SINGLE_OPS.put(',', TokenType.COMMA);
        SINGLE_OPS.put('.', TokenType.DOT);
        SINGLE_OPS.put('|', TokenType.BAR);
    }

    // ==================== КОНСТРУКТОР ====================

    /**
     * Создаёт лексический анализатор для указанного файла
     * @param filename путь к исходному файлу Ada
     * @throws IOException если файл не найден или не может быть прочитан
     */
    public Lexer(String filename) throws IOException {
        this.filename = filename;
        this.content = readFile(filename);
        this.pos = 0;
        this.line = 1;
        this.lineStart = 0;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Читает содержимое файла в строку
     * @param filename имя файла
     * @return содержимое файла как строка
     * @throws IOException при ошибках ввода-вывода
     */
    private String readFile(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        // Используем BufferedReader для эффективного чтения
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');  // Добавляем символ перевода строки
            }
        }
        return sb.toString();
    }

    /**
     * Возвращает текущий символ или '\0' если конец файла
     */
    private char current() {
        return pos < content.length() ? content.charAt(pos) : '\0';
    }

    /**
     * Возвращает следующий символ (без смещения позиции) или '\0' если конец файла
     * Используется для операторов "заглядывания вперёд" (lookahead)
     */
    private char peek() {
        return pos + 1 < content.length() ? content.charAt(pos + 1) : '\0';
    }

    /**
     * Продвигает позицию на один символ вперёд
     * При переходе на новую строку обновляет счётчик строк
     */
    private void advance() {
        // Если текущий символ - перевод строки, увеличиваем счётчик строк
        if (current() == '\n') {
            line++;                              // Переход на следующую строку
            lineStart = pos + 1;                 // Запоминаем начало новой строки
        }
        pos++;  // Смещаем позицию вперёд
    }

    /**
     * Пропускает пробельные символы (пробел, табуляция, перевод строки, возврат каретки)
     * Эти символы не порождают токенов и просто игнорируются
     */
    private void skipWhitespace() {
        while (pos < content.length() && Character.isWhitespace(current())) {
            advance();
        }
    }

    /**
     * Возвращает строковое представление текущей позиции для отладки
     * Формат: "filename:line:column"
     */
    private String getLocation() {
        int column = pos - lineStart + 1;  // Вычисляем номер колонки (с 1)
        return String.format("%s:%d:%d", filename, line, column);
    }

    /**
     * Генерирует исключение лексической ошибки
     * @param message описание ошибки
     * @throws LexerException всегда
     */
    private void error(String message) throws LexerException {
        int column = pos - lineStart + 1;
        throw new LexerException(message, filename, line, column);
    }

    // ==================== МЕТОДЫ РАСПОЗНАВАНИЯ ЛЕКСЕМ ====================

    /**
     * Распознаёт и пропускает комментарий Ada
     * Комментарий начинается с "--" и продолжается до конца строки
     * @return всегда null (комментарии не преобразуются в токены)
     */
    private Token readComment() {
        // Пропускаем первый символ '-'
        advance();
        // Пропускаем второй символ '-'
        advance();

        // Читаем всё до конца строки
        while (pos < content.length() && current() != '\n') {
            advance();
        }

        return null;  // Комментарий не добавляется в поток токенов
    }

    /**
     * Распознаёт строковый литерал
     * Формат: " ... " где внутри " представлено как ""
     * Правила:
     * - Не может содержать перевод строки
     * - "внутри" заменяется на " (одинарный)
     * @return токен типа STRING
     * @throws LexerException при незакрытой кавычке или переводе строки внутри строки
     */
    private Token readString() throws LexerException {
        int startLine = line;
        int startPos = pos;

        // Пропускаем открывающую двойную кавычку
        advance();

        StringBuilder value = new StringBuilder();

        // Читаем символы до закрывающей кавычки
        while (pos < content.length() && current() != '"') {
            // Проверка на перевод строки (недопустимо в строковом литерале)
            if (current() == '\n') {
                error("Недопустимый перевод строки внутри строкового литерала");
            }

            // Обработка экранирования кавычки: "" внутри строки означает один символ "
            if (current() == '"' && peek() == '"') {
                value.append('"');   // Добавляем один символ кавычки
                advance();           // Пропускаем первую кавычку
                advance();           // Пропускаем вторую кавычку
            } else {
                value.append(current());
                advance();
            }
        }

        // Проверка: дошли ли до закрывающей кавычки?
        if (pos >= content.length()) {
            error("Строковый литерал не закрыт кавычкой");
        }

        // Пропускаем закрывающую кавычку
        advance();

        return new Token(TokenType.STRING, value.toString(), filename, startLine);
    }

    /**
     * Распознаёт символьный литерал
     * Формат: 'X' где X - любой печатный символ
     * Особые случаи: "''" означает символ одиночной кавычки
     * @return токен типа CHAR
     * @throws LexerException при неверном формате литерала
     */
    private Token readChar() throws LexerException {
        int startLine = line;

        // Пропускаем открывающую кавычку
        advance();

        // Проверка: литерал не может быть пустым
        if (pos >= content.length()) {
            error("Символьный литерал не содержит символа");
        }

        String value;

        // Обработка случая: '' (пустой символ) - недопустимо в Ada
        if (current() == '\'') {
            error("Пустой символьный литерал недопустим");
        }

        // Обработка случая: ''' (символ кавычки)
        if (current() == '\'' && peek() == '\'') {
            value = "'";
            advance();  // пропускаем первый '
            advance();  // пропускаем второй '
        }
        // Обычный символ
        else {
            value = String.valueOf(current());
            advance();
        }

        // Теперь должен идти закрывающий апостроф
        if (current() != '\'') {
            error("Ожидается закрывающая кавычка '\\'' в символьном литерале");
        }

        // Пропускаем закрывающий апостроф
        advance();

        return new Token(TokenType.CHAR, value, filename, startLine);
    }

    /**
     * Распознаёт числовой литерал (целый или вещественный)
     * Поддерживаемые форматы:
     * - Десятичные целые: 123, 1_000, 5E6
     * - Десятичные вещественные: 3.14, 0.5E-3
     * - На основе: 16#FF#, 2#1101_0011#
     * @return токен типа INT или REAL
     * @throws LexerException при неверном формате числа
     */
    private Token readNumber() throws LexerException {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        boolean isReal = false;

        // ========== ЧАСТЬ 1: Целая часть или основание ==========
        // Собираем цифры (и подчёркивания между ними)
        if (Character.isDigit(current())) {
            while (pos < content.length() && (Character.isDigit(current()) || current() == '_')) {
                // Проверка на некорректное использование подчёркивания
                if (current() == '_') {
                    // Подчёркивание не может быть в конце числа
                    if (peek() == '\0' || !Character.isDigit(peek())) {
                        error("Некорректное использование подчёркивания в числовом литерале");
                    }
                    // Два подчёркивания подряд запрещены
                    if (peek() == '_') {
                        error("Двойное подчёркивание в числовом литерале");
                    }
                }
                sb.append(current());
                advance();
            }
        } else {
            error("Число должно начинаться с цифры");
        }

        // ========== ЧАСТЬ 2: Литерал на основе (основание#цифры#) ==========
        if (current() == '#') {
            sb.append('#');
            advance();

            // Читаем цифры в системе счисления (0-9, A-F)
            boolean hasDigits = false;
            while (pos < content.length() &&
                    (Character.isDigit(current()) ||
                            (current() >= 'A' && current() <= 'F') ||
                            (current() >= 'a' && current() <= 'f') ||
                            current() == '_')) {
                if (current() == '_') {
                    if (peek() == '_') error("Двойное подчёркивание");
                }
                hasDigits = true;
                sb.append(current());
                advance();
            }

            if (!hasDigits) {
                error("Ожидаются цифры в числовом литерале на основе");
            }

            // Возможна дробная часть
            if (current() == '.') {
                isReal = true;
                sb.append('.');
                advance();
                hasDigits = false;
                while (pos < content.length() &&
                        (Character.isDigit(current()) ||
                                (current() >= 'A' && current() <= 'F') ||
                                (current() >= 'a' && current() <= 'f') ||
                                current() == '_')) {
                    if (current() == '_') {
                        if (peek() == '_') error("Двойное подчёркивание");
                    }
                    hasDigits = true;
                    sb.append(current());
                    advance();
                }
                if (!hasDigits) {
                    error("Ожидаются цифры после десятичной точки");
                }
            }

            // Закрывающий #
            if (current() != '#') {
                error("Ожидается '#' в конце числового литерала на основе");
            }
            sb.append('#');
            advance();

            // Экспонента (необязательно)
            if (current() == 'E' || current() == 'e') {
                sb.append(current());
                advance();
                if (current() == '+' || current() == '-') {
                    sb.append(current());
                    advance();
                }
                if (!Character.isDigit(current())) {
                    error("Ожидаются цифры в экспоненте");
                }
                while (pos < content.length() && Character.isDigit(current())) {
                    sb.append(current());
                    advance();
                }
            }

            return new Token(isReal ? TokenType.REAL : TokenType.INT,
                    sb.toString(), filename, startLine);
        }

        // ========== ЧАСТЬ 3: Десятичная точка (вещественное число) ==========
        // Особенность: нужно отличать число с точкой от оператора диапазона ".."
        if (current() == '.') {
            if (peek() == '.') {
                // Это оператор диапазона "..", значит число без точки
                return new Token(TokenType.INT, sb.toString(), filename, startLine);
            }

            // Это десятичная точка - читаем вещественное число
            isReal = true;
            sb.append('.');
            advance();

            if (!Character.isDigit(current())) {
                error("Ожидаются цифры после десятичной точки");
            }

            while (pos < content.length() && (Character.isDigit(current()) || current() == '_')) {
                if (current() == '_') {
                    if (peek() == '_' || !Character.isDigit(peek())) {
                        error("Некорректное использование подчёркивания");
                    }
                }
                sb.append(current());
                advance();
            }
        }

        // ========== ЧАСТЬ 4: Экспонента (для вещественных и целых) ==========
        if (current() == 'E' || current() == 'e') {
            sb.append(current());
            advance();
            if (current() == '+' || current() == '-') {
                sb.append(current());
                advance();
            }
            if (!Character.isDigit(current())) {
                error("Ожидаются цифры в экспоненте");
            }
            while (pos < content.length() && Character.isDigit(current())) {
                sb.append(current());
                advance();
            }
        }

        // Определяем тип токена (INT или REAL)
        TokenType type = isReal ? TokenType.REAL : TokenType.INT;
        return new Token(type, sb.toString(), filename, startLine);
    }

    /**
     * Распознаёт идентификатор или ключевое слово
     * Идентификаторы начинаются с буквы и содержат буквы, цифры и подчёркивания
     * Правила:
     * - Не может начинаться с цифры
     * - Не может содержать два подчёркивания подряд
     * - Не может заканчиваться подчёркиванием
     * - Нечувствительны к регистру (приводятся к верхнему)
     *
     * Если распознанная строка совпадает с ключевым словом (в верхнем регистре),
     * возвращается токен соответствующего ключевого слова.
     * Иначе - токен IDENT.
     * @return токен KEYWORD или IDENT
     * @throws LexerException при некорректном идентификаторе
     */
    private Token readIdentifier() throws LexerException {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        // Первый символ должен быть буквой (латиница или буквы Latin-1)
        char first = current();
        if (!isValidIdentifierStart(first)) {
            error("Идентификатор должен начинаться с буквы");
        }

        sb.append(first);
        advance();

        // Читаем остальные символы (буквы, цифры, подчёркивания)
        boolean lastWasUnderscore = false;

        while (pos < content.length() && isValidIdentifierPart(current())) {
            char ch = current();

            // Проверка на два подчёркивания подряд
            if (ch == '_') {
                if (lastWasUnderscore) {
                    error("Идентификатор не может содержать два подчеркивания подряд");
                }
                lastWasUnderscore = true;
            } else {
                lastWasUnderscore = false;
            }

            sb.append(ch);
            advance();
        }

        // Проверка: идентификатор не должен заканчиваться на подчёркивание
        if (lastWasUnderscore) {
            error("Идентификатор не может заканчиваться подчеркиванием");
        }

        // Приводим к верхнему регистру для проверки на ключевое слово
        String upperValue = sb.toString().toUpperCase();

        // Если строка является ключевым словом - возвращаем ключевое слово
        if (KEYWORDS.contains(upperValue)) {
            return new Token(KEYWORD_TO_TYPE.get(upperValue),
                    sb.toString(),  // Сохраняем оригинальный регистр для вывода
                    filename, startLine);
        }

        // Иначе - это обычный идентификатор
        return new Token(TokenType.IDENT, sb.toString(), filename, startLine);
    }

    /**
     * Проверяет, является ли символ допустимым первым символом идентификатора
     * В Ada: буквы латинского алфавита (включая диакритику из Latin-1)
     * @param c проверяемый символ
     * @return true если символ может начинать идентификатор
     */
    private boolean isValidIdentifierStart(char c) {
        // Основные латинские буквы
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
            return true;
        }
        // Буквы Latin-1 с диакритикой
        // À-Ö (192-214), Ø-ö (216-246), ø-ÿ (248-255)
        if ((c >= 192 && c <= 214) || (c >= 216 && c <= 246) || (c >= 248 && c <= 255)) {
            return true;
        }
        return false;
    }

    /**
     * Проверяет, является ли символ допустимой частью идентификатора
     * (после первого символа можно использовать буквы, цифры и подчёркивания)
     * @param c проверяемый символ
     * @return true если символ может быть частью идентификатора
     */
    private boolean isValidIdentifierPart(char c) {
        return isValidIdentifierStart(c) || Character.isDigit(c) || c == '_';
    }

    // ==================== ГЛАВНЫЙ МЕТОД АНАЛИЗА ====================

    /**
     * Главный метод лексического анализатора.
     * Последовательно возвращает токены из исходного файла.
     *
     * Принцип работы (конечный автомат):
     * 1. Пропускаем пробелы
     * 2. Смотрим на текущий символ и определяем тип лексемы:
     *    - Символ '-' с следующим '-' -> комментарий
     *    - Символ '"' -> строковый литерал
     *    - Символ "'" -> символьный литерал
     *    - Цифра -> число
     *    - Буква -> идентификатор или ключевое слово
     *    - Символ, который может начать мультисимвольный оператор -> проверяем
     *    - Иначе -> одиночный оператор
     *
     * @return следующий токен или null (для комментариев), либо EOF в конце
     * @throws LexerException при лексической ошибке
     */
    public Token nextToken() throws LexerException {
        // ---- ШАГ 1: Пропускаем пробельные символы ----
        skipWhitespace();

        // ---- ШАГ 2: Проверка на конец файла ----
        if (pos >= content.length()) {
            return new Token(TokenType.EOF, "", filename, line);
        }

        // ---- ШАГ 3: Определяем тип лексемы по первому символу ----
        char ch = current();

        // ----- 3.1: Комментарий -----
        if (ch == '-' && peek() == '-') {
            return readComment();  // Возвращает null (комментарий игнорируется)
        }

        // ----- 3.2: Строковый литерал -----
        if (ch == '"') {
            return readString();
        }

        // ----- 3.3: Символьный литерал -----
        if (ch == '\'') {
            return readChar();
        }

        // ----- 3.4: Числовой литерал -----
        if (Character.isDigit(ch)) {
            return readNumber();
        }

        // ----- 3.5: Идентификатор или ключевое слово -----
        if (isValidIdentifierStart(ch)) {
            return readIdentifier();
        }

        // ----- 3.6: Мультисимвольные операторы (жадное распознавание) -----
        // Проверяем двухсимвольные комбинации
        if (pos + 1 < content.length()) {
            String twoChars = "" + ch + peek();
            if (MULTI_OPS.containsKey(twoChars)) {
                TokenType type = MULTI_OPS.get(twoChars);
                String value = twoChars;
                advance();
                advance();
                return new Token(type, value, filename, line - (current() == '\n' ? 1 : 0));
            }
        }

        // ----- 3.7: Односимвольные операторы/разделители -----
        if (SINGLE_OPS.containsKey(ch)) {
            TokenType type = SINGLE_OPS.get(ch);
            String value = String.valueOf(ch);
            advance();
            return new Token(type, value, filename, line);
        }

        // ----- 3.8: Недопустимый символ -----
        error("Недопустимый символ: '" + ch + "' (код " + (int)ch + ")");
        return null;  // Никогда не выполнится из-за exception
    }

    /**
     * Токенизирует весь файл и возвращает список всех токенов (без комментариев)
     * @return список токенов
     * @throws LexerException при лексической ошибке
     */
    public List<Token> tokenize() throws LexerException {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while (true) {
            token = nextToken();
            if (token == null) continue;  // Пропускаем комментарии
            tokens.add(token);
            if (token.getType() == TokenType.EOF) break;
        }
        return tokens;
    }
}

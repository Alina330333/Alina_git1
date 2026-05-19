package adalexer;

import java.io.*;
import java.util.*;

/**
 * Главный класс программы.
 * Выполняет:
 * 1. Задание 1: Подсчёт общего числа лексем
 * 2. Задание 2: Подсчёт частоты (абсолютной и относительной) лексем каждого типа
 */
public class Main {

    public static void main(String[] args) {
        // Проверка аргументов командной строки
        if (args.length == 0) {
            System.err.println("Использование: java adalexer.Main <файл1> [файл2 ...]");
            System.err.println("Пример: java adalexer.Main program.ada");
            System.exit(1);
        }

        // Собираем все переданные файлы
        List<String> filenames = Arrays.asList(args);

        // Результирующая статистика (только для правильных файлов)
        int totalTokens = 0;
        Map<TokenType, Integer> typeFrequency = new HashMap<>();
        List<String> errorFiles = new ArrayList<>();  // Список файлов с ошибками

        System.out.println("=".repeat(70));
        System.out.println("ЛЕКСИЧЕСКИЙ АНАЛИЗАТОР ДЛЯ ЯЗЫКА ADA");
        System.out.println("=".repeat(70));
        System.out.println();

        // Обработка каждого файла
        for (String filename : filenames) {
            System.out.println(">>> Обработка файла: " + filename);
            System.out.println("-".repeat(50));

            try {
                // Создаём лексер и получаем токены
                Lexer lexer = new Lexer(filename);
                List<Token> tokens = lexer.tokenize();

                // Вывод всех токенов (для отладки)
                System.out.println("Токены:");
                int tokenCount = 0;
                for (Token token : tokens) {
                    if (token.getType() != TokenType.EOF) {
                        System.out.println("  " + token.toShortString());
                        tokenCount++;
                    }
                }
                System.out.println("  (Всего токенов: " + tokenCount + ")");
                System.out.println();

                // Подсчёт статистики по файлу
                int fileTokens = 0;
                Map<TokenType, Integer> fileFreq = new HashMap<>();

                for (Token token : tokens) {
                    if (token.getType() == TokenType.EOF) continue;
                    fileTokens++;
                    fileFreq.put(token.getType(),
                            fileFreq.getOrDefault(token.getType(), 0) + 1);
                }

                // Добавляем в общую статистику
                totalTokens += fileTokens;
                for (Map.Entry<TokenType, Integer> entry : fileFreq.entrySet()) {
                    typeFrequency.put(entry.getKey(),
                            typeFrequency.getOrDefault(entry.getKey(), 0) + entry.getValue());
                }

                // Вывод статистики по файлу
                System.out.println("Статистика по файлу '" + filename + "':");
                System.out.println("  Всего лексем: " + fileTokens);
                System.out.println("  Лексем по типам:");

                // Сортируем по типу токена для красоты
                List<TokenType> sortedTypes = new ArrayList<>(fileFreq.keySet());
                sortedTypes.sort(Comparator.comparing(Enum::name));

                for (TokenType type : sortedTypes) {
                    int count = fileFreq.get(type);
                    double percent = (count * 100.0) / fileTokens;
                    System.out.printf("    %-20s: %5d (%.2f%%)\n", type, count, percent);
                }
                System.out.println();

            } catch (IOException e) {
                System.err.println("  Ошибка ввода-вывода при чтении файла: " + e.getMessage());
                errorFiles.add(filename);
            } catch (LexerException e) {
                // Лексическая ошибка - файл пропускается, не добавляется в статистику
                System.err.println("  " + e);
                errorFiles.add(filename);
                System.out.println("  ВНИМАНИЕ: Файл содержит лексическую ошибку. Статистика по этому файлу НЕ учитывается.");
                System.out.println();
            }
            System.out.println();
        }

        // ===== ВЫВОД ИТОГОВОЙ СТАТИСТИКИ (ТОЛЬКО ПО КОРРЕКТНЫМ ФАЙЛАМ) =====
        System.out.println("=".repeat(70));
        System.out.println("ИТОГОВАЯ СТАТИСТИКА ПО ВСЕМ ФАЙЛАМ");
        System.out.println("=".repeat(70));
        System.out.println();

        // Если были файлы с ошибками - сообщаем, какие пропущены
        if (!errorFiles.isEmpty()) {
            System.out.println("ВНИМАНИЕ: Следующие файлы содержат лексические ошибки и НЕ учтены в статистике:");
            for (String errFile : errorFiles) {
                System.out.println("    - " + errFile);
            }
            System.out.println();
        }

        System.out.println("Задание 1: Общее число лексем");
        System.out.println("  " + totalTokens);
        System.out.println();

        System.out.println("Задание 2: Частота лексем каждого типа");
        System.out.println("  Абсолютная и относительная частота:");
        System.out.println();

        // Сортируем типы по имени
        if (!typeFrequency.isEmpty() && totalTokens > 0) {
            List<TokenType> sortedAllTypes = new ArrayList<>(typeFrequency.keySet());
            sortedAllTypes.sort(Comparator.comparing(Enum::name));

            System.out.println("  +-------------------------+----------+----------+");
            System.out.println("  | Тип лексемы             | Абс. част| Отн. част|");
            System.out.println("  +-------------------------+----------+----------+");

            for (TokenType type : sortedAllTypes) {
                int count = typeFrequency.get(type);
                double percent = (count * 100.0) / totalTokens;
                System.out.printf("  | %-23s | %8d | %7.2f%% |\n", type, count, percent);
            }

            System.out.println("  +-------------------------+----------+----------+");
        } else {
            System.out.println("  (нет корректных лексем для анализа)");
        }

        System.out.println();
        System.out.println("Анализ завершён.");
    }
}
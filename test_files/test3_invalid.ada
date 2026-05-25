-- Файл с лексическими ошибками
procedure Bad_Program is
   --X : Integer := 123_456;
   Y : Integer := "@@@";        -- ОШИБКА: недопустимый символ @
   --Z : Integer := 12_34_56;
   --S : String := "Unclosed;   -- ОШИБКА: строка не закрыта
   --C : Character := '';       -- ОШИБКА: пустой символ ??
   --W : Integer := _start;     -- ОШИБКА: идентификатор с _
   --V : Integer := 999_        -- ОШИБКА: подчёркивание в конце
begin
   null;
end Bad_Program;

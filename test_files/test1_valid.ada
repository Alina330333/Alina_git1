-- Простая программа на Ada
procedure Main is
   X : Integer := 10;
   Y : Integer := 20;
   Z : Integer;
begin
   Z := X + Y;
   if Z > 15 then
      Put_Line("Result is positive");
   end if;
end Main;
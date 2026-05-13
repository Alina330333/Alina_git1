-- Пакет с константами
package Math_Constants is
   Pi : constant Float := 3.14159;
   E  : constant Float := 2.71828;

   function Square(X : Float) return Float;
end Math_Constants;

package body Math_Constants is
   function Square(X : Float) return Float is
   begin
      return X * X;
   end Square;
end Math_Constants;
## Грамматика (псевдо-BNF):

```
expr ::= expr op expr | (expr) | identifier | { expr, expr } | number | map(expr, identifier -> expr) | reduce(expr, expr, identifier identifier -> expr)
op ::= + | - | * | / | ^
stmt ::= var identifier = expr | out expr | print "string"
program ::= stmt | program stmt
```

## Пояснения:

* __number__ - произвольное целое или вещественное число
* приоритеты операторов такие (в возрастающем порядке): __+__ и __-__, __*__ и __/__, __^__
* __{expr1, expr2}__, где expr1 и expr2 - выражения с целым результатом - последовательность чисел  { expr1, expr1 + 1, expr + 2 .. expr2 } включительно. Если результат вычисления expr1 или expr2 не целый или expr1 > expr2, результат не определен.
* __map__ - оператор над элементами последовательности, применяет отображение к элементам последовательности и получает другую последовательность. Последовательность может из целой стать вещественной. Лямбда у __map__ имеет один параметр - элемент последовательности.
* __reduce__ - свертка последовательности. Первый аргумент - последовательность, второй - нейтральный элемент, третий - операция. Свертка применяет операцию (лямбду) ко всем элементам последовательности. Например, “reduce({5, 7}, 1, x y -> x * y)” должен вычислять 1 * 5 * 6 * 7. Можно полагаться на то, что операция в reduce будет ассоциативна.
* области видимости переменных - от ее объявления (__var__) до конца файла. Переменные у лямбд в map / reduce - имеют областью видимости соответствующую лямбду. У лямбд отсутствует замыкание, к глобальным переменным обращаться нельзя
* __out__, __print__ - операторы вывода. "__string__" - произвольная строковая константа, не содержащая кавычек, без экранирования

## Пример:
```
var n = 500
var sequence = map({0, n}, i -> (-1)^i / (2 * i + 1))
var pi = 4 * reduce(sequence, 0, x y -> x + y)
print "pi = "
out pi
```

## Требования к интерпретатору:

* Интерпретатор должен выдавать ошибки парсинга и ошибки несоответствия типов (складывание чисел и последовательностей, применение map/reduce к числам, итд).
* map и reduce должны выполняться интерпретатором параллельно по разумной стратегии.
* Все, что не учтено в правилах грамматики, на твое усмотрение.
* По желанию: поддержка вычислений на длинных последовательностях (миллионы элементов).

## Требования к редактору:

* Ошибки от интерпретатора должны подсвечиваться по месту автоматически.
* Результаты работы программы должны автоматически рассчитываться в фоне и показываться.
* Долгие вычисления не должны приводить к блокировке UI и мешать работе.
* UI на твое усмотрение.

## Требования к реализации и оформлению:

* Программа должна быть написана на Java/Kotlin.
* Допускается разумное использование готовых средств и библиотек.
* Исходный код должен быть предоставлен в виде полностью настроенного самодостаточного проекта, размещенного на github.
* Должен быть предоставлен необходимый набор тестов.
* Там где это имеет смысл, API должен быть документирован на английском языке.

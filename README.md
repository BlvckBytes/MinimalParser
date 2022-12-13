# MinimalParser

## Grammar

### Numbers

There are two types of numbers, floating point as well as whole integers.

We start out by defining what a single digit has to look like:

```ebnf
Digit ::= [0-9]
```

![digit](readme_images/railroad_digit.png)

Multiple digits in combination with an optional, leading minus sign compose an integer.

```ebnf
Integer ::= "-"? Digit+
```
![integer](readme_images/railroad_integer.png)

If the number contains a decimal, it's a float. It may omit the zero right before the dot.

```ebnf
Float ::= "-"? Digit* "." Digit+
```
![float](readme_images/railroad_float.png)

### Strings

A string is always surrounded by two double-quotes `"` and may only contain characters other than
double-quotes. To add a double-quote character without closing the string, it has to be escaped `\"`.
Since YAML strings can be cumbersome when it comes to single-quotes, `\s` will be replaced by a single-quote `'`.

```ebnf
String ::= '"' ('\"' | [^"] | "\s")* '"'
```

![string](readme_images/railroad_string.png)

### Identifiers

An identifier is either a function name, a variable name or a lookup table name. These names are provided by the
evaluation context, may only start with letters and can only consist of letters, digits and underscores.

```ebnf
Letter ::= [A-Za-z]
Identifier ::= Letter (Digit | Letter | '_')*
```
![letter](readme_images/railroad_letter.png)

![identifier](readme_images/railroad_identifier.png)

### Operators

In order to not have to call functions for simple operations and to improve on readability,
some of the most used operators have been implemented:

| Operator                    | Example          | Description                                          | Precedence |
|-----------------------------|------------------|------------------------------------------------------|------------|
| **Exponentiation Operator** |
| ^                           | A ^ B            | Yields A to the power of B                           | 1          |
| **Multiplicative Operator** |
| *                           | A * B            | Yields the product of A and B                        | 2          |
| /                           | A / B            | Yields the quotient of A and B                       | 2          |
| %                           | A % B            | Yields the remainder of dividing A by B              | 2          |
| **Additive Operator**       |
| +                           | A + B            | Yields the sum of A and B                            | 3          |
| -                           | A - B            | Yields the difference of A and B                     | 3          |
| **Equality Operator**       |
| \>                          | A \> B           | Yields `true` if A is greater than B                 | 4          |
| <                           | A < B            | Yields `true` if A is less than B                    | 4          |
| \>=                         | A \>= B          | Yields `true` if A is greater than or equal to B     | 4          |
| <=                          | A <= B           | Yields `true` if A is less than or equal to B        | 4          |
| ==                          | A == B           | Yields `true` if A and B equal ignoring casing       | 4          |
| ===                         | A === B          | Yields `true` if A and B equal exactly               | 4          |
| !=                          | A != B           | Yields `true` if A and B don't equal ignoring casing | 4          |
| !==                         | A !== B          | Yields `true` if A and B don't equal exactly         | 4          |
| **Negation Operator**       |
| !                           | !A               | Yields the inverse of A                              | 5          |
| **Conjunction Operator**    |
| &&                          | A && B           | Yields `true` if both A and B yield `true`           | 6          |
| **Disjunction Operator**    |
| &#124;&#124;                | A &#124;&#124; B | Yields `true` if either A or B yields `true`         | 7          |
| **Concatenation Operator**  |
| &                           | A & B            | Concatenates the contents of A and B                 | 8          |

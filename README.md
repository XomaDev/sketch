# Sketch

![sketch](/asset/sketch.png)

Sketch is a language intended for fun.
<br>
It has a Kotlin-style syntax.
<br>

Sketch uses an AST-type parser to evaluate code.

````kotlin
val date = 07;
val goodboy = true;

fun today() {
  if (  goodboy ) {  print("apologize!");  }
  else {    delay();    };
};

fun delay() {
  date++;
  today();
};
today();
````
The above code is an example.
It'll print Apologize if `good boy` or it will go on forever because the day never comes.

## Variable

````kotlin
val text = "Hello, World!";
````

Sketch supports four types of values - strings, numbers, booleans (`true`/`false`), `null` and array.
````kotlin
val happy = true;
if (happy) {    happy = null;   };
````

## Arrays
````kotlin
val array = array(7);
array[0] = 7;

for x (1 -> len(array) - 1) {
    array[x] = array[x - 1] * 2;
}
printf(array);
````
prints `[7.0, 14.0, 28.0, 56.0, 112.0, 224.0, 448.0]
`

## Conditions

There are two types of condition checks, i.e. ternary operator and `if else`.

````kotlin
if (happy) {
  print("Happy Day :D");  
} else {
  print("Oh, No :(");  
};
````
Inline operator (ternary).
<br>
Syntax `[condition] then [expression] or [expression]`
````vertica
print(happy then "Hello, World" or "No :/");
````
## Functions

You can define a function in sketch.
<br>
`null` is the default value if you don't return anything.
````kotlin
fun hello(arg) {
    if (arg != 7 && arg + 2 == 10) {
        return "that was right";
    };
    return "Ah, No";
};

print( hello(8) );
````
## Loops

Yes, 😉 Sketch supports two types of loops - for loop and while loop.

````kotlin
fun hello(arg) {
    for x (1 <- 7) {
        print(x);
        if (x == 5) {  break;  };
    };
};

print( hello(8) );
````

It also supports `forward` statement, which can be used to forward loop x times.
````kotlin
fun hello() {
    for x (1 <- 7) {
        print(x);
        if (x == 5) {  forward 2;  };
    };
};
hello();
````

Reverse loop can be performed through `<-` symbol. `->` for a forward loop.

````kotlin
val x = 7;
while (x > 0) {
    print("value of x " + --x);
};
````

## Import

````kotlin
with Sketch.systemTime clock;

fun fib(n) {
  if (n < 2) {
    return n;
  }
  return fib(n - 1) + fib(n - 2);
}

val before = clock();
print(fib(40));
print(clock() - before);
````

Internal functions can be implemented with the use of `with` statement.
<br>
`with <from> <function> <optional_function_rename>`

## Running

You can clone the repository, run the `Main` file or use the `jar` file from the releases.

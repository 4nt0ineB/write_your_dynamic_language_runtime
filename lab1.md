
# Exercice 1

## Q1

Before starting, explain to yourself how "switch on types" in the method visit of ASTInterpreter.java works ?
How to "return" a value from a case ?
How to call it, how to do a recursive call ? 

---
We make a recusrive call in the AST expression
We use yield to return a value in a case.
We call the visit function and we pass down the environnement

## Q2

The file ASTInterpreterTest contains several unit tests for all the following questions.
Now, we want to implement the AST interpreter when there is a constant String (a literal).
What is the class of the corresponding node in the AST (you can take a look to the file smalljs.md for a summary).
How to implement it ?
Is it enough to make test marked Q2 pass ? what other node should be implemented too ? 

---
a Literal


## 4
We now want to print something.
What is the class of the node in the AST to implement ?
For now, we will suppose that the only function is the print function. How to find the function "print" which is registered in the global environment ? How to call it ?
Add the code to support the function print and verify that the test marked Q4 pass. 
---

We find the print function with lookup.
we call it with "invoke()"

## 5

What is the return value of the function print in JavaScript ?
Make sure your interpreter behave accordingly and verify that the test marked Q5 pass. 

---
it returns undefined

## 6

We now want to support other functions than just print.
Where all the builtin functions are defined ?
How to modify the code to support all builtin functions ? What is the "qualifier" component represent ?
Is the visit of a function call enough to implement the support of a function call ? What is the method in the environment env to look for a value associated to a name ?
What is the class that represent a function at runtime ?
Modify the code and verify that the test marked Q6 pass. 

-- 
Nothing more, we use the lookup in the environment.
The qualifier is what comes before the parenthesis.
FunCall

## 8

We now want to be able to declare or assign a local variable.
What is the class of the node in the AST corresponding to a local variable assignment ?
Implement the interpreter part and verify that all the tests marked Q8 pass. 

--

LocalVarAssignment
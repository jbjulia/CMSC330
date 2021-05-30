cmake_minimum_required(VERSION 3.19)
project(Project2)

set(CMAKE_CXX_STANDARD 14)

add_executable(
        Project2
        expression.h
        literal.h
        module3.cpp
        operand.cpp
        operand.h
        parse.cpp
        parse.h
        plus.h
        subexpression.cpp
        subexpression.h
        symboltable.cpp
        symboltable.h
        variable.cpp
        variable.h
        minus.h
        times.h
        divide.h
        greaterthan.h
        lessthan.h
        equal.h
        and.h
        or.h
        conditional.h
        negate.h
)

#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

using namespace std;

#include "expression.h"
#include "subexpression.h"
#include "symboltable.h"
#include "parse.h"

SymbolTable symbolTable;

void parseAssignments(stringstream &in);

int main() {
    string file;
    ifstream in_file;
    const int SIZE = 256;
    Expression *expression;
    char paren, comma, line[SIZE];
    cout << "Please enter the name of the file you wish to test (e.g. 'expression1.txt'):  ";
    cin >> file;
    file = "../expressions/" + file;
    in_file.open(file);
    if (in_file) {
        ifstream fin(file);
        while (true) {
            symbolTable.init();
            fin.getline(line, SIZE);
            if (!fin)
                break;
            stringstream in(line, ios_base::in);
            in >> paren;
            cout << line << " ";
            expression = SubExpression::parse(in);
            in >> comma;
            parseAssignments(in);
            double result = expression->evaluate();
            cout << "Value = " << result << endl;
        }
    } else {
        cout << "Error: File does not exist.";
        return 1;
    }
    return 0;
}

void parseAssignments(stringstream &in) {
    char assignop, delimiter;
    string variable;
    double value;
    do {
        variable = parseName(in);
        in >> ws >> assignop >> value >> delimiter;
        symbolTable.insert(variable, value);
    } while (delimiter == ',');
}


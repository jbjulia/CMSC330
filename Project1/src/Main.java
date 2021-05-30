/*
  Name:  Joseph Julian
  Project:  Project 1
  Date:  13 Apr 2021
  Description:  Parses input file and generates GUI.
 */

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class Main {
    final boolean debug = false;
    Lexer lexer;
    Parser parser;

    public static void main(String[] args) {
        Main test = new Main();
        test.selectFile();
    }

    /**
     * Prompts user to select existing file to be parsed. Takes file
     * and passes to lexer and parser classes to analyze and parse.
     */
    private void selectFile() {
        int option;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        do {
            option = fileChooser.showOpenDialog(null);
        } while (option != JFileChooser.APPROVE_OPTION && option != JFileChooser.CANCEL_OPTION);

        File file = fileChooser.getSelectedFile();

        lexer = new Lexer(file);
        lexer.analyzeFile();
        lexer.printTokens();
        parser = new Parser();

        if (parser.parseFile())
            System.out.println("\n\nFile successfully parsed!");
    }

    enum TokenType {
        NOT_FOUND, SEMICOLON, COMMA, STRING, WIDGET, END, WINDOW,
        LAYOUT, FLOW, BUTTON, LABEL, PANEL, TEXT_FIELD, GRID, GROUP,
        RADIO, QUOTATION, OPEN_PARENTHESIS, CLOSE_PARENTHESIS, COLON,
        PERIOD, NUMBER, END_OF_FILE
    }

    enum Status {IN_PARENTHESIS, IN_QUOTATION, DEFAULT}


    private static class Lexer {
        private final ArrayList<Token> tokens;
        private File file;
        private int i;
        private TokenType lastToken;
        private Status status;
        private String line = "";

        /**
         * Initializes private data members and opens the input file.
         *
         * @param _file User-selected file
         */
        public Lexer(File _file) {
            if (_file != null) {
                file = _file;
            }

            tokens = new ArrayList<>();
        }

        /**
         * Adds given token to <code>tokens</code>, after checking some contextual
         * information.  Even if the lexeme is marked as a special token, verify
         * that it's not in a parenthesis (a number literal) or in a quotation
         * (a string literal):
         *
         * @param token  -
         * @param lexeme -
         */
        private void addToken(TokenType token, String lexeme) {
            if (status == Status.IN_PARENTHESIS) {
                if (token == TokenType.CLOSE_PARENTHESIS) {
                    lastToken = token;
                    tokens.add(new Token(token, lexeme));
                    status = Status.DEFAULT;
                    return;
                }

                lastToken = token;
                tokens.add(new Token(token, lexeme));
                return;
            } else if (status == Status.IN_QUOTATION) {
                if (token == TokenType.QUOTATION) {
                    if (lastToken == TokenType.QUOTATION) {
                        tokens.add(new Token(TokenType.STRING, ""));
                        tokens.add(new Token(token, lexeme));
                        status = Status.DEFAULT;
                        return;
                    }

                    lastToken = token;
                    tokens.add(new Token(token, lexeme));
                    status = Status.DEFAULT;
                    return;
                }

                if (lastToken == TokenType.STRING) {
                    int x = tokens.size() - 1;
                    tokens.get(x).lexeme += " " + lexeme;
                    return;
                }

                token = TokenType.STRING;
                lastToken = token;
                tokens.add(new Token(token, lexeme));
                return;
            } else if (token == TokenType.QUOTATION) {
                lastToken = token;
                tokens.add(new Token(token, lexeme));
                status = Status.IN_QUOTATION;
                return;
            } else if (token == TokenType.OPEN_PARENTHESIS) {
                lastToken = token;
                tokens.add(new Token(token, lexeme));
                status = Status.IN_PARENTHESIS;
                return;
            }

            lastToken = token;
            tokens.add(new Token(token, lexeme));
        }

        private void analyzeFile() {
            Scanner scanner = openFile(file);

            if (scanner != null) {
                analyzeInput(scanner);
            }
        }

        /**
         * Calls analyzeLine() on each line in the Scanner parameter.
         *
         * @param scanner File data
         */
        private void analyzeInput(Scanner scanner) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                if (!(nextLine.startsWith("/") || nextLine.isEmpty()))
                    analyzeLine(nextLine);
            }
        }

        /**
         * Takes a single String and analyzes it into Token objects.
         *
         * @param nextLine String to be analyzed
         */
        private void analyzeLine(String nextLine) {
            line = nextLine;
            i = 0;
            char character = nextChar();
            String punctuations = "\"(),:;.";

            do {
                StringBuilder currentLexeme = new StringBuilder();
                while (character != 0 && (Character.isWhitespace(character)
                        || !(punctuations.contains(String.valueOf(character))
                        || Character.isAlphabetic(character)
                        || Character.isDigit(character)))) {
                    character = nextChar();
                }

                if (character == 0) return;

                if (Character.isLetterOrDigit(character)) {
                    currentLexeme.append(character);
                    character = nextChar();
                    while (Character.isLetterOrDigit(character)) {
                        currentLexeme.append(character);
                        character = nextChar();
                    }
                    TokenType token = testToken(currentLexeme.toString());
                    addToken(token, currentLexeme.toString());
                } else if (punctuations.contains(String.valueOf(character))) {
                    currentLexeme = new StringBuilder(String.valueOf(character));
                    TokenType token = testPunctuation(currentLexeme.toString());
                    addToken(token, currentLexeme.toString());
                    character = nextChar();
                }
            } while (character != 0);
        }


        /**
         * Returns next character in line. Returns 0 if end of line.
         */
        private char nextChar() {
            if (i == line.length()) {
                i = 0;
                return 0;
            } else return line.charAt(i++);
        }

        /**
         * Called by analyzeFile().
         *
         * @param file File to be opened into a Scanner object
         * @return Scanner from parameter
         */
        private Scanner openFile(File file) {
            Scanner scanner;

            try {
                scanner = new Scanner(file);
            } catch (FileNotFoundException e) {
                System.out.println("Error: File not found!");

                return null;
            }
            return scanner;
        }

        private void printTokens() {
            for (int x = 0; x < tokens.size(); x++) {
                if (x % 5 == 0)
                    System.out.println("\n");

                System.out.print(tokens.get(x) + " ");
            }
        }


        /**
         * Tests punctuation of given lexeme and returns TokenType.
         *
         * @param lexeme lexeme to be analyzed
         * @return TokenType of the lexeme
         */
        private TokenType testPunctuation(String lexeme) {
            return switch (lexeme) {
                case "\"" -> TokenType.QUOTATION;
                case "(" -> TokenType.OPEN_PARENTHESIS;
                case ")" -> TokenType.CLOSE_PARENTHESIS;
                case "," -> TokenType.COMMA;
                case ":" -> TokenType.COLON;
                case ";" -> TokenType.SEMICOLON;
                case "." -> TokenType.PERIOD;
                default -> TokenType.NOT_FOUND;
            };
        }

        /**
         * Tests non-punctuation lexemes for token type.
         *
         * @param lexeme lexeme to be analyzed
         * @return TokenType of the lexeme
         */
        private TokenType testToken(String lexeme) {
            if (status == Status.IN_QUOTATION) {
                return TokenType.STRING;
            } else if (status == Status.IN_PARENTHESIS) {
                boolean flag = true;
                for (int x = 0; x < lexeme.length(); x++) {
                    char c = lexeme.charAt(x);
                    if (!Character.isDigit(c))
                        flag = false;
                }

                if (flag) {
                    return TokenType.NUMBER;
                } else
                    return TokenType.NOT_FOUND;
            } else
                switch (lexeme.charAt(0)) {
                    case 'B':
                        if (lexeme.equals("Button")) {
                            return TokenType.BUTTON;
                        }
                    case 'E':
                        if (lexeme.equals("End")) {
                            return TokenType.END;
                        }
                    case 'F':
                        if (lexeme.equals("Flow")) {
                            return TokenType.FLOW;
                        }
                    case 'G':
                        if (lexeme.equals("Grid")) {
                            return TokenType.GRID;
                        } else if (lexeme.equals("Group")) {
                            return TokenType.GROUP;
                        }
                    case 'L':
                        if (lexeme.equals("Label")) {
                            return TokenType.LABEL;
                        } else if (lexeme.equals("Layout")) {
                            return TokenType.LAYOUT;
                        }
                    case 'P':
                        if (lexeme.equals("Panel")) {
                            return TokenType.PANEL;
                        }
                    case 'R':
                        if (lexeme.equals("Radio")) {
                            return TokenType.RADIO;
                        }
                    case 'T':
                        if (lexeme.equals("Textfield")) {
                            return TokenType.TEXT_FIELD;
                        }
                    case 'W':
                        if (lexeme.equals("Window")) {
                            return TokenType.WINDOW;
                        }
                    default:
                        return TokenType.NUMBER;
                }
        }
    }


    private static class Token {
        TokenType type;
        String lexeme;

        public Token(TokenType _type, String _lexeme) {
            type = _type;
            lexeme = _lexeme;
        }

        @Override
        public String toString() {
            return String.format("[%s, \"%s\"]", type, lexeme);
        }
    }


    private class Parser {
        ArrayList<Token> tokens;
        JFrame window;
        Container currentContainer;
        ButtonGroup group;
        int i = 0;
        TokenType token;
        String error = "";

        /**
         * Parses file and verifies syntax of each line.
         */
        private boolean parseFile() {
            tokens = lexer.tokens;
            token = nextToken();

            if (gui()) {
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setLocationRelativeTo(null);
                window.setVisible(true);
                return true;
            } else {
                System.out.printf("Error: Incorrect syntax at token %d: %s\n", i + 1, error);
                return false;
            }
        }


        private String getToken() {
            return tokens.get(i - 1).lexeme;
        }


        private boolean gui() {
            if (token == TokenType.WINDOW) {
                token = nextToken();
                if (token == TokenType.QUOTATION) {
                    token = nextToken();
                    if (token == TokenType.STRING) {
                        currentContainer = window = new JFrame(getToken());
                        token = nextToken();
                        if (token == TokenType.QUOTATION) {
                            token = nextToken();
                            if (token == TokenType.OPEN_PARENTHESIS) {
                                token = nextToken();
                                if (token == TokenType.NUMBER) {
                                    int width = Integer.parseInt(getToken());
                                    token = nextToken();
                                    if (token == TokenType.COMMA) {
                                        token = nextToken();
                                        if (token == TokenType.NUMBER) {
                                            int height = Integer.parseInt(getToken());
                                            token = nextToken();
                                            if (token == TokenType.CLOSE_PARENTHESIS) {
                                                token = nextToken();
                                                if (layout()) {
                                                    token = nextToken();
                                                    if (widgets()) {
                                                        if (token == TokenType.END) {
                                                            token = nextToken();
                                                            if (token == TokenType.PERIOD) {
                                                                window.setMinimumSize(new Dimension(width, height));
                                                                return true;
                                                            } else
                                                                error = "GUI: \"Period\" token not found.";
                                                        } else
                                                            error += "  GUI: \"End\" token not found.";
                                                    } else
                                                        error += "  GUI: \"Widgets\" pattern not found.";
                                                } else
                                                    error += "  (GUI: \"Layout\" pattern not found.)";
                                            } else
                                                error = "GUI: \"Close_Parenthesis\" token not found.";
                                        } else
                                            error += "GUI: \"Number\" token not found.";
                                    } else
                                        error = "GUI: \"Comma\" token not found.";
                                } else
                                    error += "GUI: \"Number\" token not found.";
                            } else
                                error = "GUI: \"Open_Parenthesis\" token not found.";
                        } else
                            error = "GUI: \"Quotation\" token not found.";
                    } else
                        error += "  (GUI: \"String\" token not found.)";
                } else
                    error = "GUI: \"Quotation\" token not found.";
            } else
                error = "GUI: \"Window\" token not found.";

            return false;
        }


        private boolean layout() {
            if (token == TokenType.LAYOUT) {
                token = nextToken();
                if (layout_type()) {
                    token = nextToken();
                    if (token == TokenType.COLON) {
                        return true;
                    } else
                        error = "(\"Colon\" token not found.)";
                } else
                    error += "  \"Layout_Type\" pattern not found.";
            } else
                error += "  (\"Layout\" token not found.)";

            return false;
        }

        private boolean layout_type() {
            if (token == TokenType.FLOW) {
                currentContainer.setLayout(new FlowLayout());
                return true;
            } else if (token == TokenType.GRID) {
                token = nextToken();
                if (token == TokenType.OPEN_PARENTHESIS) {
                    token = nextToken();
                    if (token == TokenType.NUMBER) {
                        int rows = Integer.parseInt(getToken());
                        token = nextToken();
                        if (token == TokenType.COMMA) {
                            token = nextToken();
                            if (token == TokenType.NUMBER) {
                                int columns = Integer.parseInt(getToken());
                                token = nextToken();
                                if (token == TokenType.CLOSE_PARENTHESIS) {
                                    currentContainer.setLayout(new GridLayout(rows, columns));
                                    return true;
                                } else if (token == TokenType.COMMA) {
                                    token = nextToken();
                                    if (token == TokenType.NUMBER) {
                                        int h_gap = Integer.parseInt(getToken());
                                        token = nextToken();
                                        if (token == TokenType.COMMA) {
                                            token = nextToken();
                                            if (token == TokenType.NUMBER) {
                                                int v_gap = Integer.parseInt(getToken());
                                                token = nextToken();
                                                if (token == TokenType.CLOSE_PARENTHESIS) {
                                                    currentContainer.setLayout(new GridLayout(rows, columns, h_gap, v_gap));
                                                    return true;
                                                } else
                                                    error = "\"Close_Parenthesis\" token not found.";
                                            } else
                                                error += "  (\"Number\" token not found.)";
                                        } else
                                            error = "\"Comma\" token not found.";
                                    } else
                                        error += "  (\"Number\" token not found.)";
                                } else
                                    error = "\"Comma\" or \"Close_Parenthesis\" token not found.";
                            } else
                                error += "  (\"Number\" token not found.)";
                        } else
                            error = "\"Comma\" token not found.";
                    } else
                        error += "  (\"Number\" token not found.)";
                } else
                    error = "\"Open_Parenthesis\" token not found.";
            } else
                error = "\"Flow\" or \"Grid\" token not found.";

            return false;
        }


        private TokenType nextToken() {
            if (i == tokens.size()) {
                return TokenType.END_OF_FILE;
            } else {
                return tokens.get(i++).type;
            }
        }

        private boolean radioButton() {
            if (token == TokenType.RADIO) {
                token = nextToken();
                if (token == TokenType.QUOTATION) {
                    token = nextToken();
                    if (token == TokenType.STRING) {
                        JRadioButton button = new JRadioButton(getToken());
                        token = nextToken();
                        if (token == TokenType.QUOTATION) {
                            token = nextToken();
                            if (token == TokenType.SEMICOLON) {
                                currentContainer.add(button);
                                group.add(button);
                                return true;
                            } else
                                error = "\"Semicolon\" token not found.";
                        } else
                            error += "  (\"Quotation\" token not found.)";
                    } else
                        error += "  (\"String\" token not found.)";
                } else
                    error += "  (\"Quotation\" token not found.)";
            } else
                error += "  (\"Radio_Button\" pattern not found.)";

            return false;
        }


        private boolean radButtons() {
            boolean flag = false;
            int temp = i;

            if (radioButton()) {
                flag = true;
                token = nextToken();
                radButtons();
            } else {
                i = temp;
            }

            return flag;
        }

        private boolean widget() {
            switch (token) {
                case BUTTON:
                    token = nextToken();
                    if (token == TokenType.QUOTATION) {
                        token = nextToken();
                        if (token == TokenType.STRING) {
                            JButton button = new JButton(getToken());
                            token = nextToken();
                            if (token == TokenType.QUOTATION) {
                                token = nextToken();
                                if (token == TokenType.SEMICOLON) {
                                    currentContainer.add(button);
                                    return true;
                                } else
                                    error = "Widget: \"Semicolon\" token not found.";
                            } else
                                error += "  (Widget: \"Quotation\" token not found.)";
                        } else
                            error += "  (Widget: \"String\" token not found.)";
                    } else
                        error += "  (Widget: \"Quotation\" token not found.)";
                case GROUP:
                    token = nextToken();
                    group = new ButtonGroup();
                    if (radButtons()) {
                        if (token == TokenType.END) {
                            token = nextToken();
                            if (token == TokenType.SEMICOLON) {
                                System.out.printf(debug ? "\nend widget(%s)" : "", tokens.get(i - 1).lexeme);
                                return true;
                            } else
                                error = "Widget: Group: \"Semicolon\" token not found.";
                        } else
                            error += "  (Widget: Group: \"End\" token not found.)";
                    } else
                        error += "  (Widget: Group: \"Radio_Buttons\" pattern not found.)";
                case LABEL:
                    token = nextToken();
                    if (token == TokenType.QUOTATION) {
                        token = nextToken();
                        if (token == TokenType.STRING) {
                            JLabel label = new JLabel(getToken());
                            token = nextToken();
                            if (token == TokenType.QUOTATION) {
                                token = nextToken();
                                if (token == TokenType.SEMICOLON) {
                                    currentContainer.add(label);
                                    System.out.printf(debug ? "\nend widget(%s)" : "", tokens.get(i - 1).lexeme);
                                    return true;
                                } else
                                    error = "Widget: \"Semicolon\" token not found.";
                            } else
                                error += "  (Widget: \"Quotation\" token not found.)";
                        } else
                            error += "  (Widget: \"String\" token not found.)";
                    } else
                        error += "  (Widget: \"Quotation\" token not found.)";
                case PANEL:
                    System.out.printf(debug ? "\nPanel Starting(%d) on \"%s\"\n" : "", i - 1, tokens.get(i - 1));
                    token = nextToken();
                    JPanel panel;
                    Container parentContainer = panel = new JPanel();
                    currentContainer.add(panel);
                    currentContainer = panel;
                    if (layout()) {
                        token = nextToken();
                        if (widgets()) {
                            currentContainer = parentContainer;
                            if (token == TokenType.END) {
                                System.out.printf(debug ? "\nPanel : END found(%d) on \"%s\"\n" : "", i - 1, tokens.get(i - 1));
                                token = nextToken();
                                if (token == TokenType.SEMICOLON) {
                                    System.out.printf(debug ? "\nPanel Complete(%d) on \"%s\"\n" : "", i - 1, tokens.get(i - 1));
                                    Border border = BorderFactory.createLineBorder(Color.black);
                                    panel.setSize(500, 500);
                                    panel.setBorder(border);
                                    if (panel.getBorder() == null) System.out.printf("\nNo Border!\n");

                                    System.out.printf(debug ? "\nend widget(%s)" : "", tokens.get(i - 1).lexeme);
                                    return true;
                                } else
                                    error = "Widget: Panel: \"Semicolon\" token not found.";
                            } else
                                error = "Widget: Panel: \"End\" token not found.";
                        } else
                            error += "  Widget: Panel: \"Widget\" pattern not found.";
                    } else
                        error += "  (Widget: Panel: \"Layout\" pattern not found.)";
                case TEXT_FIELD:
                    token = nextToken();
                    if (token == TokenType.NUMBER) {
                        JTextField textField = new JTextField(Integer.parseInt(getToken()));
                        token = nextToken();
                        if (token == TokenType.SEMICOLON) {
                            currentContainer.add(textField);
                            System.out.printf("\nend widget(%s)", tokens.get(i - 1).lexeme);
                            return true;
                        } else
                            error = "Widget: \"Semicolon\" token not found.";
                    } else
                        error += "  (Widget: \"Number\" token not found.)";
                default:
                    error += "  Widget: \"Widgets\" pattern not found.";
                    return false;
            }
        }

        private boolean widgets() {
            boolean flag = false;
            int temp = i;

            if (widget()) {
                flag = true;
                token = nextToken();
                Container parentContainer = currentContainer;
                widgets();
                currentContainer = parentContainer;
            } else {
                i = temp;
            }

            return flag;
        }
    }
}
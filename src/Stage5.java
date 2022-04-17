import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.regex.Pattern.compile;

public class Stage5 {
    public static void main(String[] args) {
        final var app = new Stage4.Application();
        app.menu();
    }
    public static class Application {
        private static final String ACTION_EXIT = "/exit";
        private static final String ACTION_HELP = "/help";

        private final Stage4.Calculator calculator;

        public Application() {
            calculator = new Stage4.PostfixCalculator();
        }

        public void menu() {
            final var sc = new Scanner(System.in);

            while (true) {
                final var line = sc.nextLine().trim().replaceAll("\\s+", " ");

                if (line.isBlank()) {
                    continue;
                }
                if (ACTION_EXIT.equals(line)) {
                    break;
                }
                if (ACTION_HELP.equals(line)) {
                    printHelp();
                    continue;
                }
                if (line.charAt(0) == '/') {
                    System.out.println(Stage4.Validation.ERR_UNKNOWN_COMMAND);
                    continue;
                }
                if (line.contains("=")) {
                    calculator.doAssignment(line);
                    continue;
                }
                if (calculator.setExpression(line)) {
                    System.out.println(calculator.calculate());
                }
            }
            System.out.println("Bye!");
            sc.close();
        }

        void printHelp() {
            System.out.println(join("\n",
                    "The program calculates the arithmetic expressions for big numbers.",
                    "The program support variables assigment and use.",
                    "Supported operations: + - * / % ^",
                    "Supported commands:",
                    "/help - shows this help",
                    "/exit - exit the program."
            ));
        }
    }
    public static class ExpressionValidation implements Stage4.Validation {
        final private Map<String, BigInteger> variables;

        public ExpressionValidation(Map<String, BigInteger> variables) {
            this.variables = variables;
        }

        @Override
        public boolean checkExpression(String expression) {
            final var isCorrectExpression = EXPRESSION.matcher(expression).matches();

            if (!isCorrectExpression) {
                System.out.println(ERR_INVALID_EXPRESSION);
            }
            return isCorrectExpression;
        }

        @Override
        public boolean checkAssignment(String expression) {
            final var isCorrectAssignment = ASSIGNMENT.matcher(expression).matches();

            if (!isCorrectAssignment) {
                System.out.println(ERR_INVALID_ASSIGNMENT);
            }
            return isCorrectAssignment;
        }

        @Override
        public boolean checkParentheses(String expression) {
            int count = 0;
            for (final var parenthesis : expression.replaceAll("[^()]", "").toCharArray()) {
                count += parenthesis == '(' ? 1 : -1;
                if (count < 0) {
                    break;
                }
            }
            if (count != 0) {
                System.out.println(ERR_INVALID_EXPRESSION);
            }
            return count == 0;
        }

        @Override
        public boolean checkVariables(String expression) {
            final var variables = Set.of(expression.split("[^\\p{Alpha}]+"));
            final var isVariablesDefined = this.variables.keySet().containsAll(variables);

            if (!isVariablesDefined) {
                System.out.println(ERR_UNKNOWN_VARIABLE);
            }
            return isVariablesDefined;

        }

        @Override
        public boolean checkIdentifier(String identifier) {
            final var isCorrectIdentifier = IDENTIFIER.matcher(identifier).matches();

            if (!isCorrectIdentifier) {
                System.out.println(ERR_INVALID_IDENTIFIER);
            }
            return isCorrectIdentifier;
        }
    }
    public interface Validation {
        Pattern IDENTIFIER = compile("\\p{Alpha}+");
        Pattern EXPRESSION = compile("[-+ ]*\\(*\\w+( *([/*%^]|[-+]+) *\\(*\\w+\\)*)*");
        Pattern ASSIGNMENT = compile("\\p{Alpha}+\\s*=[-+ ]*\\(*\\w+( *([/%*^]|[-+]+) *\\(*\\w+\\)*)*");

        String ERR_INVALID_EXPRESSION = "Invalid expression";
        String ERR_INVALID_ASSIGNMENT = "Invalid assignment";
        String ERR_INVALID_IDENTIFIER = "Invalid identifier";
        String ERR_UNKNOWN_VARIABLE = "Unknown variable";
        String ERR_UNKNOWN_COMMAND = "Unknown command";

        boolean checkExpression(String expression);

        boolean checkAssignment(String expression);

        boolean checkVariables(String expression);

        boolean checkParentheses(String expression);

        boolean checkIdentifier(String identifier);

    }
    public interface Calculator {
        Pattern OPERATOR = Pattern.compile("[-+*%/^]");
        String  LEFT_PARENTHESIS = "(";
        String  RIGHT_PARENTHESIS = ")";

        boolean setExpression(final String expression);

        BigInteger calculate();

        void doAssignment(final String expression);
    }
    public enum Operations {
        POWER(5, "^", (a, b) -> a.pow(b.intValue())),
        MODULO(4, "%", BigInteger::mod),
        MULTIPLY(4, "*", BigInteger::multiply),
        DIVIDE(4, "/", BigInteger::divide),
        ADD(2, "+", BigInteger::add),
        SUBTRACT(2, "-", BigInteger::subtract);

        private final String symbol;
        private final BinaryOperator<BigInteger> operation;
        private final int priority;

        Operations(int priority, String symbol, BinaryOperator<BigInteger> operation) {
            this.symbol = symbol;
            this.operation = operation;
            this.priority = priority;
        }

        static Stage4.Operations getOperation(String symbol) {
            return Stream.of(Stage4.Operations.values()).filter(i -> i.getSymbol().equals(symbol)).findAny().orElseThrow();
        }

        String getSymbol() {
            return symbol;
        }

        int getPriority() {
            return priority;
        }

        BigInteger calculate(BigInteger a, BigInteger b) {
            return operation.apply(a, b);
        }
    }
    public static class PostfixCalculator implements Stage4.Calculator {
        private static final String TOKENS =
                "(?<=[-+]?\\w)(?=[-+/*^])|(?<=[^-+/*^][-+/*^])(?=[-+]?\\w)|(?=[()])|(?<=[()])";

        private final Stage4.Validation validation;
        private final Map<String, BigInteger> variables;

        private List<String> postfix;

        public PostfixCalculator() {
            variables = new HashMap<>();
            validation = new Stage4.ExpressionValidation(variables);
        }

        @Override
        public boolean setExpression(final String expression) {

            final var isCorrectExpression =
                    validation.checkExpression(expression)
                            && validation.checkParentheses(expression)
                            && validation.checkVariables(expression);

            if (isCorrectExpression) {
                toPostfix(splitIntoTokens(expression));
            }

            return isCorrectExpression;
        }

        @Override
        public BigInteger calculate() {
            final var stack = new ArrayDeque<BigInteger>();

            for (final var element : postfix) {
                final var isOperator = OPERATOR.matcher(element).matches();
                if (isOperator) {
                    var b = stack.pop();
                    var a = stack.pop();
                    stack.push(Stage4.Operations.getOperation(element).calculate(a, b));
                } else {
                    stack.push(parseOperand(element));
                }
            }
            return stack.pop();
        }

        private BigInteger parseOperand(String operand) {
            final var id = operand.replaceAll("[^\\p{Alpha}]", "");
            if (!id.isBlank()) {
                operand = operand.replace(id, variables.get(id).toString());
            }
            return new BigInteger(operand.replaceAll("[\\s+]+", "").replaceAll("--", ""));
        }

        @Override
        public void doAssignment(final String line) {
            if (!validation.checkAssignment(line)) {
                return;
            }
            final var parts = line.split("=", 2);
            final var identifier = parts[0].trim();
            final var expression = parts[1].trim();

            if (validation.checkIdentifier(identifier) && setExpression(expression)) {
                variables.put(identifier, calculate());
            }
        }

        private String[] splitIntoTokens(String expression) {
            return expression
                    .replaceAll("--", "+")
                    .replaceAll("\\+\\++", "+")
                    .replaceAll("\\+-", "-")
                    .replaceAll("\\s+", "")
                    .split(TOKENS);
        }

        private void toPostfix(String[] infix) {
            postfix = new LinkedList<>();
            final var stack = new ArrayDeque<String>();

            for (var element : infix) {
                final var isOperand = !element.matches("[-+/%*^()]");

                if (isOperand) {
                    postfix.add(element);
                    continue;
                }

                if (LEFT_PARENTHESIS.equals(element)) {
                    stack.push(element);
                    continue;
                }

                if (RIGHT_PARENTHESIS.equals(element)) {
                    while (!LEFT_PARENTHESIS.equals(stack.peek())) {
                        postfix.add(stack.pop());
                    }
                    stack.pop();
                    continue;
                }

                if (stack.isEmpty() || LEFT_PARENTHESIS.equals(stack.peek())) {
                    stack.push(element);
                    continue;
                }

                if (Stage4.Operations.getOperation(element).getPriority() > Stage4.Operations.getOperation(stack.peek()).getPriority()) {
                    stack.push(element);
                    continue;
                }

                while (!stack.isEmpty() && !stack.peek().equals(LEFT_PARENTHESIS)
                        && Stage4.Operations.getOperation(element).getPriority() <= Stage4.Operations.getOperation(stack.peek()).getPriority()) {
                    postfix.add(stack.pop());
                }

                stack.push(element);
            }

            while (!stack.isEmpty()) {
                postfix.add(stack.pop());
            }
        }
    }
}


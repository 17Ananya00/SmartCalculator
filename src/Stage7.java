import java.math.BigInteger;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.regex.Pattern.compile;

public class Stage7 {
    public static void main(String[] args) {
        new Operations().start();
    }
    public static class Operations {
        private final Scanner scanner;
        private static final String HELP = "/help";
        private static final String EXIT = "/exit";
        private static final Pattern VARIABLE = Pattern.compile("(?)[a-z]+");
        private static final Pattern VARIABLES_NUMBERS = Pattern.compile("[-+\\s()^]*\\w+[() ]*([-+/*\\s() ^]*[-+/* ^] *[() ]*\\w+[() ]*)*");

        public Operations() {
            this.scanner = new Scanner(System.in);
        }

        // check if it is a valid variable, then do like variable, else fifth stage
        void start() {
            Variables variables = new Variables();
            String line;
            while (!EXIT.equals(line = scanner.nextLine().trim())) {
                if (line.isBlank()) {
                    continue;
                }
                if (HELP.equals(line)) {
                    System.out.println("This is my calculator, it can add, subtract, divide and multiply numbers");
                } else if (line.charAt(0) == '/') {
                    System.out.println("Unknown command");
                } else if (line.contains("=")) {
                    variables.operationAssignment(line);
                } else if (VARIABLE.matcher(line).matches()) {
                    variables.showValue(line);
                } else if (VARIABLES_NUMBERS.matcher(line).matches()) {
                    variables.checkAndCalc(line);
                } else {
                    System.out.println("Invalid command");
                }
            }
            System.out.println("Bye!");
        }

    }
    public static class Variables {
        private final Map<String, BigInteger> variables = new HashMap<>();

        void operationAssignment(String line) {
            String[] arguments = getRidOfExtraPMS(line).replaceAll("\\s+", "").split("=");
            if (arguments.length != 2) {
                System.out.println("Invalid assignment");
            }
            if (arguments[0].matches("(?i).*[^a-z]+.*")) {
                System.out.println("Invalid identifier");
            } else if (arguments[1].matches("(?i).*([^a-z\\d-]|-?[a-z]+\\d+|-?\\d+[a-z]+)+.*")) {
                System.out.println("Invalid assignment");
            }
            if (arguments[1].matches("-?[\\d]+")) {
                variables.put(arguments[0], new BigInteger((arguments[1])));
            } else if (arguments[1].matches("(?i)-?[a-z]+")) {
                String temp = arguments[1].charAt(0) == '-' ? arguments[1].substring(1) : arguments[1];
                if (variables.get(temp) != null) {
                    variables.put(arguments[0], arguments[1].charAt(0) == '-' ? variables.get(temp).negate() : variables.get(temp));
                } else {
                    System.out.println("Unknown variable");
                }
            }
        }

        void showValue(String line) {
            if (variables.get(line) != null) {
                System.out.println(variables.get(line));
            } else {
                System.out.println("Unknown variable");
            }
        }

        void checkAndCalc(String line) {
            String lineWithoutSpacesAndOtherGarbage = getRidOfExtraPMS(line);
            if (lineWithoutSpacesAndOtherGarbage.matches(".*(/ *[/*^]|\\* *[/*^]|[-+] *[*/^]|[*/^] *\\+|\\^ *[-+/*^]).*|.*[-+/*^]$") || lineWithoutSpacesAndOtherGarbage.matches("(?i).*(\\d+[^\\d\\s+-/*()^]+|[a-z]+[^\\s+-/*()a-z^]+).*")) {
                System.out.println("Invalid expression");
                return;
            }
//        below I use stack for check an input line for balanced brackets
            Deque<Character> stack = new ArrayDeque<>();
            boolean isBalanced = true;
            for (var i : lineWithoutSpacesAndOtherGarbage.toCharArray()) {
                if (i == '(') {
                    stack.offerLast(i);
                } else if (i == ')') {
                    if (stack.isEmpty()) {
                        isBalanced = false;
                        break;
                    }
                    stack.pop();
                }
            }
            if (!isBalanced || !stack.isEmpty()) {
                System.out.println("Invalid expression");
                return;
            }
            BigInteger result = calcString(lineWithoutSpacesAndOtherGarbage);
            System.out.println(result == null ? "Smth bad is happened" : result);
        }

        // replace unary minus to !, it cleans string of extra operations
        private String getRidOfExtraPMS(String line) {
            while (line.matches(".*(\\+ *\\+|- *-|\\+ *-|- *\\+).*")) {
                line = line.replaceAll("(\\+ *\\+)|(- *-)", "+").replaceAll("\\+ *-|- *\\+", "-");
            }
            return line.replaceAll("\\s+", "").replaceAll("\\*-", "\\*!").replaceAll("/-", "/!").replaceAll("\\( *-", "\\(!");
        }

        // this video helped me a lot https://www.youtube.com/watch?v=Vk-tGND2bfc
        private BigInteger calcString(String line) {
            Pattern number = Pattern.compile("\\d+");
            Pattern variable = Pattern.compile("(?i)[a-z]+");
            Pattern operations = Pattern.compile("[-*+/!^]");
            Deque<String> stackOperations = new ArrayDeque<>();
            Deque<BigInteger> stackOperands = new ArrayDeque<>();
            Queue<String> tokens = getQueueTokens(line);
//        process every token in tokens
            while (!tokens.isEmpty()) {
                String token = tokens.poll();
                if (number.matcher(token).matches()) {
                    stackOperands.offerLast(new BigInteger(token));
                } else if (variable.matcher(token).matches()) {
                    if (variables.get(token) != null) {
                        stackOperands.offerLast(variables.get(token));
                    } else {
                        System.out.println("unknown variable\nexecution is failed");
                        return null;
                    }
//                below it works with signs
                } else if (operations.matcher(token).matches()) {
                    if (stackOperations.isEmpty()) {
                        stackOperations.offerLast(token);
                    } else {
                        if (getPriority(token) > getPriority(stackOperations.peekLast()) || Objects.equals(stackOperations.peekLast(), "(")) {
                            stackOperations.offerLast(token);
                        } else {
                            while (getPriority(token) <= getPriority(stackOperations.peekLast()) && !Objects.equals(stackOperations.peekLast(), "(")) {
                                String currentOperation = stackOperations.pollLast();
                                if (!calcStacks(currentOperation, stackOperands)) {
                                    System.out.println("current operation is " + currentOperation);
                                    System.out.println("Can't make operations with tokens with different priority");
                                    return null;
                                }
                                if (stackOperations.isEmpty()) {
                                    break;
                                }
                            }
                            stackOperations.offerLast(token);
                        }
                    }
//                deal with brackets
                } else if (Objects.equals("(", token)) {
                    stackOperations.offerLast(token);
                } else if (Objects.equals(")", token)) {
                    while (!Objects.equals(stackOperations.peekLast(), "(")) {
                        String operation = stackOperations.pollLast();
                        calcStacks(operation, stackOperands);
                    }
                    stackOperations.pollLast();
                } else {
                    System.out.println("Unknown token, exit");
                    return null;
                }
            }
//        calc the rest numbers
            while (stackOperands.size() != 1) {
                calcStacks(stackOperations.pollLast(), stackOperands);
            }
            return stackOperands.pollLast();
        }

        // takes one or two numbers from stack and make an operation
        private boolean calcStacks(String operation, Deque<BigInteger> numbers) {
            BigInteger firstNumber = numbers.pollLast();
            if (firstNumber == null) {
                System.out.println("First number is null");
                return false;
            }
            if (Objects.equals("!", operation)) {
                numbers.offerLast(firstNumber.negate());
            } else {
                BigInteger secondNumber = numbers.pollLast();
                if (secondNumber == null) {
                    System.out.println("Second number is null");
                    return false;
                }
                BigInteger result = useOperator(operation, secondNumber, firstNumber);
                if (result == null) {
                    return false;
                } else {
                    numbers.offerLast(result);
                }
            }
            return true;
        }

        private BigInteger useOperator(String operator, BigInteger value1, BigInteger value2) {
            switch (operator) {
                case "+":
                    return value1.add(value2);
                case "-":
                    return value1.subtract(value2);
                case "/":
                    return value1.divide(value2);
                case "*":
                    return value1.multiply(value2);
                case "^":
                    BigInteger result = BigInteger.ONE;
                    for (BigInteger i = BigInteger.ZERO; i.compareTo(value2) < 0; i = i.add(BigInteger.ONE)) {
                        result = result.multiply(value1);
                    }
                    return result;
            }
            return null;
        }

        private int getPriority(String operator) {
            if (Objects.equals("+", operator) || Objects.equals("-", operator)) {
                return 1;
            } else if (Objects.equals("/", operator) || Objects.equals("*", operator)) {
                return 2;
            } else if (Objects.equals("^", operator)) {
                return 4;
            }
            return 5;
        }

        //returns tokens it is ether number/variable or sign
        private Queue<String> getQueueTokens(String line) {
            Deque<String> tokens = new ArrayDeque<>();
            Set<Character> operations = new HashSet<>(Arrays.asList('+', '-', '/', '!', '*', '(', ')', '^'));
            StringBuilder customString = new StringBuilder(line);
//        plus sign just for exit from while cycle
            customString.append('+');
            while (customString.length() > 0) {
                boolean isItNumber = false;
                int index = 0;
                char currentToken = customString.charAt(index);
                while (!operations.contains(currentToken)) {
                    currentToken = customString.charAt(++index);
                    isItNumber = true;
                }
                tokens.offerLast(customString.substring(0, isItNumber ? index : index + 1));
                customString = new StringBuilder(customString.substring(isItNumber ? index : index + 1));
            }
//       remove last + sign
            tokens.pollLast();
            return tokens;
        }
    }
}

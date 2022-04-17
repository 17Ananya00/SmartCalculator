import java.util.Scanner;

public class Stage3 {
    public static void main(String[] args) {
        int sum;
        do {
            Scanner kb = new Scanner(System.in);
            int score = 0;
            sum = 0;
            String line = kb.nextLine();
            if (line.equals("/help")) {
                System.out.println("The program calculates the sum of numbers");
            } else if (line.equals("/exit")) {
                System.out.println("bye!");
                break;
            } else {
                kb = new Scanner(line); //has to do this to make the kb.hasNexInt() work.
                while (kb.hasNextInt()) {
                    score = kb.nextInt();
                    sum += score;

                }
                if ((!line.equals(""))) {
                    System.out.println(sum);
                }
            }


        }
        while (sum != -900) ;
    }
}



package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

class QuestionCreator {

    private static String getIdentification(){
        int random = (int)(Math.random()* 1024 + 1);
        return String.valueOf(random);
    }

    static String createQuestion(int option) throws IOException {
        StringBuilder question = new StringBuilder();
        Scanner in = new Scanner(System.in);
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);

        question.append(getIdentification());
        if (option == 1)
            question.append("#0#00#");
        else
            question.append("#0#01#");

        System.out.print("Enter question count: ");
        int questionCount = in.nextInt();
        question.append(String.valueOf(questionCount));

        int i = questionCount;
        int count = 1;
        while (i > 0) {
            question.append("#");
            System.out.println(String.format("Quenstion %d:", count));
            if (option == 1){
                System.out.print(" Enter domain name: ");
                String domainName = br.readLine();
                question.append(domainName);
            }
            else {
                System.out.print(" Enter IP: ");
                String ipAddress = br.readLine();
                question.append(ipAddress);
            }

            question.append("#");

            String recordType;
            if (option == 1){
                System.out.print(" Enter record type (A): ");
                recordType = br.readLine();
                if (recordType.equals(""))
                    recordType = "A";
            }
            else {
                System.out.println(" PTR records for reverse names. So record type: PTR");
                recordType = "PTR";
            }

            question.append(recordType);
            question.append("#");

            System.out.print(" Enter record class (IN): ");
            String recordClass = br.readLine();

            if (recordClass.equals(""))
                recordClass = "IN";
            question.append(recordClass);

            count++;
            i--;
        }
        question.append("!");
        return question.toString();
    }

}

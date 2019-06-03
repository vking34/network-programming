package server;

import org.xbill.DNS.*;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class AnswerCreator {

//    public static final String question = "902#0#00#2#www.facebook.com#CNAME#IN#outlook.com#MX#IN!";

    private static SimpleResolver RESOLVER = null;

    static {
        try {
            RESOLVER = new SimpleResolver("8.8.8.8");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    static String createAnswer(Connection connection, String question) throws TextParseException, SQLException {

        StringBuilder answer = new StringBuilder();
        String addressString = null;
        String domainName = null;

        String[] fields = question.split("#");

        answer.append(fields[0]);

        if (fields[2].equals("00"))
            answer.append("#1#00");
        else
            answer.append("#1#01");

        answer.append("#");
        System.out.println(fields[3]);
        answer.append(fields[3]);
        answer.append("#");
        fields[fields.length - 1] = fields[fields.length - 1].replace("!", "");

        Integer questionCount = Integer.valueOf(fields[3]);

        int k = 0, count = 1, i = questionCount, type = Type.A, dclass = DClass.IN;
        loop: while ( i > 0){
            --i;
            System.out.println(String.format("Answer %d:", count));
            switch (fields[5 + k*3]){
                case "A":
                    type = Type.A;
                    break;
                case "AAAA":
                    type = Type.AAAA;
                    break;
                case "CNAME":
                    type = Type.CNAME;
                    break;
                case "MX":
                    type = Type.MX;
                    break;
                case "PTR":
                    type = Type.PTR;
                    break;
                default:
                    answer.replace(fields[0].length()+ 3,fields[0].length()+ 5, "11");
                    answer.delete(fields[0].length()+ 5,answer.length());
                    break loop;
            }

            switch (fields[6 + k*3]){
                case "IN":
                    dclass = DClass.IN;
                    break;
                case "CH":
                    dclass = DClass.CH;
                    break;
                default:
                    answer.replace(fields[0].length()+ 3,fields[0].length()+ 5, "11");
                    answer.delete(fields[0].length()+ 5,answer.length());
                    break loop;
            }

            // check record in DB
            PreparedStatement statement;
            if (fields[2].equals("00")){ // resolve domain name
                statement = connection.prepareStatement("SELECT address \n" +
                        "FROM record\n" +
                        "WHERE domain_name = ? \n" +
                        "AND record_type = ? " +
                        "AND class = ?");
                statement.setString(1, fields[4 + k*3].concat("."));
                statement.setInt(2, type);
                statement.setString(3, fields[6 + k*3]);
            }
            else {  // resolve IP address
                statement = connection.prepareStatement("SELECT domain_name \n" +
                        "FROM record\n" +
                        "WHERE address = ? \n" +
                        "AND record_type = ? " +
                        "AND class = ?");
                statement.setString(1, fields[4 + k*3]);
                statement.setInt(2, type);
                statement.setString(3, fields[6 + k*3]);
            }

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()){
                System.out.println(" Found in Database: ");
                if (fields[2].equals("00")) { // resolve domain name
                    addressString = resultSet.getString("address");
                    System.out.println(String.format(" Domain name: %s, record type: %s, class: %s -> IP address: %s", fields[4 + k*3], fields[5 + k*3], fields[6 + k*3], addressString));
                }
                else { // resolve IP address
                    domainName = resultSet.getString("domain_name");
                    System.out.println(String.format(" IP address: %s, record type: PTR, class: %s -> Domain name: %s", fields[4 + k*3], fields[6 + k*3], domainName));
                }
            }

            else { // query to cloud DNS
                System.out.println(" Not Found in Database: ");
                if (fields[2].equals("00")) { // resolve domain name
                    try {

                        Lookup lookup = new Lookup(fields[4 + k * 3], type);
                        lookup.setResolver(RESOLVER);
                        Record[] records = lookup.run();

                        String address = null;
                        switch (type) {
                            case Type.A:
                                ARecord aRecord = (ARecord) records[0];
                                System.out.println("Host " + aRecord.getName() + " address " + aRecord.getAddress());
                                address = aRecord.getAddress().toString();
                                break;

                            case Type.AAAA:
                                AAAARecord aaaaRecord = (AAAARecord) records[0];
                                address = aaaaRecord.getAddress().toString();
                                break;

                            case Type.MX:
                                MXRecord mxRecord = (MXRecord) records[0];
                                String host = mxRecord.getTarget().toString();

                                records = new Lookup(host, Type.A).run();
                                ARecord aRecord1 = (ARecord) records[0];

                                address = aRecord1.getAddress().toString();
                                System.out.println(address);
                                break;

                            case Type.CNAME:
                                CNAMERecord cnameRecord = (CNAMERecord) records[0];
                                String host1 = cnameRecord.getTarget().toString();

                                records = new Lookup(host1, Type.A).run();
                                ARecord aRecord2 = (ARecord) records[0];

                                address = aRecord2.getAddress().toString();
                                break;
                        }

                        String[] addresses = address.split("/");
                        addressString = addresses[1];
                        System.out.println(String.format(" Domain name: %s, record type: %s, class: %s -> IP address: %s", fields[4 + k*3], fields[5 + k*3], fields[6 + k*3], addressString));
                    }
                    catch (Exception e){
                        addressString = "unknown";
                    }
                }
                else { // resolve IP address
                    String reverseIP = reverseIp(fields[4 + k*3]);
                    Lookup lookup = new Lookup(reverseIP.concat(".in-addr.arpa."), type);
                    lookup.setResolver(RESOLVER);
                    Record[] records = lookup.run();

                    try {
                        PTRRecord ptrRecord = (PTRRecord) records[0];
                        domainName = ptrRecord.getTarget().toString();
                    }
                    catch (Exception e){
                        domainName = "unknown";
                    }

                    System.out.println(String.format(" IP address: %s, record type: PTR, class: %s -> Domain name: %s", fields[4 + k*3], fields[6 + k*3], domainName));
                }
            }


            answer.append(fields[4 + k*3]);
            answer.append("#");
            answer.append(fields[5 + k*3]);
            answer.append("#");
            answer.append(fields[6 + k*3]);
            answer.append("#");
            if (fields[2].equals("00")) {
                answer.append(addressString);
            }
            else {
                answer.append(domainName);
            }

            if (i != 0)
                answer.append("#");

            count++;
            k++;
        }
        answer.append("!");
//        System.out.println(answer.toString());
        return answer.toString();
    }


    static String reverseIp(String ip){
        String[] octets = ip.split("\\.");
        return octets[3].concat(".").concat(octets[2]).concat(".").concat(octets[1]).concat(".").concat(octets[0]);
    }
}

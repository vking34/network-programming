package server;

import org.xbill.DNS.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ReverseDNSLookup {
    static String reverseDNSLookup(InetAddress adr)
    {
        try
        {
            final Name name = ReverseMap.fromAddress(adr);

            final Lookup lookup = new Lookup(name, Type.PTR);
            lookup.setResolver(new SimpleResolver("8.8.8.8"));
            lookup.setCache(null);
            final Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL)
                for (final Record record : records)
                    if (record instanceof PTRRecord)
                    {
                        final PTRRecord ptr = (PTRRecord) record;
                        return ptr.getTarget().toString();
                    }
        }
        catch (final Exception e)
        {
        }
        return null;
    }

    static String reverseIp(String ip){
        String[] octets = ip.split("\\.");
        String reverseIP = octets[3].concat(".").concat(octets[2]).concat(".").concat(octets[1]).concat(".").concat(octets[0]);
        return reverseIP;
    }

    public static void main(String[] args) throws UnknownHostException {
//        String domain = reverseDNSLookup(InetAddress.getByName("42.113.206.26"));
//        System.out.println(domain);
        String ip = "42.58.56.12";

        String reverseIP = reverseIp(ip);

        System.out.println(reverseIP);

    }
}

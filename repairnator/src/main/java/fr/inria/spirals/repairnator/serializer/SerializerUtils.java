package fr.inria.spirals.repairnator.serializer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by urli on 02/02/2017.
 */
public class SerializerUtils {

    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();

        } catch (UnknownHostException e) {
            return "unknown host";
        }
    }
}

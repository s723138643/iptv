package com.orion.iptv.network;

import android.util.Log;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

public class HostIP {
    private static final String TAG = "HostIP";

    public static List<String> getHostIP() {
        try {
            return _getHostIP();
        } catch (SocketException e) {
            Log.e(TAG, "get host ip address failed, " + e);
        }
        return new ArrayList<>();
    }

    private static List<String> _getHostIP() throws SocketException {
        ArrayList<String> ips = new ArrayList<>();
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        while (nis.hasMoreElements()) {
            getAddress(nis.nextElement()).map(ips::add);
        }
        return ips;
    }

    private static Optional<String> getAddress(NetworkInterface inf) throws SocketException {
        if (inf.isLoopback() || inf.isVirtual() || inf.isPointToPoint() || !inf.isUp()) {
            return Optional.empty();
        }
        Enumeration<InetAddress> addresses = inf.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (address instanceof Inet6Address) {
                continue;
            }
            if (address.isAnyLocalAddress()) {
                continue;
            }
            String hostAddress = address.getHostAddress();
            if (hostAddress == null || hostAddress.equals("")) {
                continue;
            }
            Log.i(TAG, String.format("got address: %s", hostAddress));
            return Optional.of(hostAddress);
        }
        return Optional.empty();
    }
}

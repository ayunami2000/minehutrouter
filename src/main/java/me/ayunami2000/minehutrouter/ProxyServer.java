package me.ayunami2000.minehutrouter;

import java.io.*;
import java.net.*;
import java.util.*;

public class ProxyServer {
    public static HashMap<String, Integer> ipportmap=new HashMap<>();
    public static List<Map.Entry<String, Integer>> ipportlist;

    public static Map<UUID, List<Socket>> slots = new HashMap<>();

    static {
        ipportmap.put("play.kaboom.pw",25565);
        ipportlist=new ArrayList<>(ipportmap.entrySet());
    }

    public static void clearHostPort(){
        ipportmap.clear();
    }

    public static void addHostPort(String hostt, int remoteportt){
        ipportmap.put(hostt,remoteportt);
    }

    public static void saveHostPort(){
        ipportlist=new ArrayList<>(ipportmap.entrySet());
    }

    /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    public static void runServer(int localport)
            throws IOException {
        // Create a ServerSocket to listen for connections with
        ServerSocket ss = new ServerSocket(localport);

        while (true) {
            final byte[] request = new byte[1024];
            final byte[] reply = new byte[4096];

            UUID iid=UUID.randomUUID();
            while(slots.containsKey(iid))iid=UUID.randomUUID();
            final UUID id=iid;
            slots.put(id, Arrays.asList(null, null));
            try {
                // Wait for a connection on the local port
                slots.get(id).set(0, ss.accept());

                final InputStream streamFromClient = slots.get(id).get(0).getInputStream();
                final OutputStream streamToClient = slots.get(id).get(0).getOutputStream();

                //randomly choose a server :D
                Collections.shuffle(ipportlist);
                Map.Entry<String, Integer> ipportitem=ipportlist.get(0);
                String host=ipportitem.getKey();
                int remoteport=ipportitem.getValue();
                System.out.println("CONNECT: "+id+" - "+host+":"+remoteport);

                // Make a connection to the real server.
                // If we cannot connect to the server, send an error to the
                // client, disconnect, and continue waiting for connections.
                try {
                    slots.get(id).set(1, new Socket(host, remoteport));
                } catch (IOException e) {
                    System.out.println("Error fard!! (id: "+id+")");
                    e.printStackTrace();
                    /*
                    PrintWriter out = new PrintWriter(streamToClient);
                    out.print("Proxy server cannot connect to " + host + ":"
                            + remoteport + ":\n" + e + "\n");
                    out.flush();
                    */
                    slots.get(id).get(0).close();
                    slots.remove(id);
                    continue;
                }

                // Get server streams.
                final InputStream streamFromServer = slots.get(id).get(1).getInputStream();
                final OutputStream streamToServer = slots.get(id).get(1).getOutputStream();

                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                Thread t = new Thread() {
                    public void run() {
                        int bytesRead;
                        try {
                            while ((bytesRead = streamFromClient.read(request)) != -1) {
                                streamToServer.write(request, 0, bytesRead);
                                streamToServer.flush();
                            }
                        } catch (IOException e) {
                        }

                        // the client closed the connection to us, so close our
                        // connection to the server.
                        try {
                            System.out.println("DISCONNECT: "+id+" - "+host+":"+remoteport);
                            streamToServer.close();
                            try {
                                if(slots.get(id) != null){
                                    if (slots.get(id).get(1) != null)
                                        slots.get(id).get(1).close();
                                    if (slots.get(id).get(0) != null)
                                        slots.get(id).get(0).close();
                                    slots.remove(id);
                                }
                            } catch (IOException e) {
                            }
                        } catch (IOException e) {
                        }
                    }
                };

                // Start the client-to-server request thread running
                t.start();

                new Thread(() -> {
                    try {
                        // Read the server's responses
                        // and pass them back to the client.
                        int bytesRead;
                        while ((bytesRead = streamFromServer.read(reply)) != -1) {
                            streamToClient.write(reply, 0, bytesRead);
                            streamToClient.flush();
                        }

                        // The server closed its connection to us, so we close our
                        // connection to our client.
                        System.out.println("DISCONNECT: "+host+":"+remoteport);
                        streamToClient.close();
                        try {
                            if(slots.get(id) != null){
                                if (slots.get(id).get(1) != null)
                                    slots.get(id).get(1).close();
                                if (slots.get(id).get(0) != null)
                                    slots.get(id).get(0).close();
                                slots.remove(id);
                            }
                        } catch (IOException e) {
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
                if(slots.get(id) != null){
                    if (slots.get(id).get(1) != null)
                        slots.get(id).get(1).close();
                    if (slots.get(id).get(0) != null)
                        slots.get(id).get(0).close();
                    slots.remove(id);
                }
            }
        }
    }
}
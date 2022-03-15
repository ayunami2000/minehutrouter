package me.ayunami2000.minehutrouter.mixin;

import me.ayunami2000.minehutrouter.ProxyServer;
import net.minecraft.server.ServerNetworkIo;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

@Mixin(ServerNetworkIo.class)
public class ServerNetworkIoMixin {
    @Inject(method = "bind(Ljava/net/InetAddress;I)V", at = @At("HEAD"))
    private void injected(InetAddress address, int port, CallbackInfo ci) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String ln;
            while(true){
                ln=scanner.nextLine().trim();
                if(ln.equalsIgnoreCase("plsstop")){
                    System.out.println("Stopping!");
                    Runtime.getRuntime().halt(0);
                }else if(ln.equalsIgnoreCase("refresh")){
                    System.out.println("Refreshing ip:port from file!");
                    setHostPortFromFile();
                }else if(ln.toLowerCase().startsWith("kick ")){
                    String finalLn = ln;
                    UUID theUser = ProxyServer.slots.keySet().stream().filter(uuid -> uuid.toString().equals(finalLn.substring(5))).findFirst().orElse(null);
                    if(theUser==null){
                        System.out.println("A user with that UUID does not exist!");
                    }else{
                        System.out.println("Kicking user with that UUID!");
                        try {
                            ProxyServer.slots.get(theUser).get(0).close();
                            ProxyServer.slots.get(theUser).get(1).close();
                            ProxyServer.slots.remove(theUser);
                        } catch (Exception e) {}
                    }
                }else {
                    System.out.println("Invalid command: " + ln);
                    System.out.println("Valid commands are: plsstop refresh kick");
                    //Thread.onSpinWait();
                }
            }
        }).start();
        //this should hang it :D hope this works!!
        //good luck bc no validation of data is done here...
        setHostPortFromFile();
        try {
            System.out.println("Starting proxy on port " + port);
            ProxyServer.runServer(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //stop server anyways
        Runtime.getRuntime().halt(0);
        ci.cancel();
    }

    private static void setHostPortFromFile(){
        try(FileInputStream inputStream = new FileInputStream("ipport.txt")) {
            String[] ipportlist = IOUtils.toString(inputStream, StandardCharsets.US_ASCII).trim().split("\n");
            ProxyServer.clearHostPort();
            for (String s : ipportlist) {
                if(!s.trim().isEmpty()) {
                    String[] ipport = s.trim().split(":");
                    try {
                        ProxyServer.addHostPort(ipport[0], Integer.parseInt(ipport[1]));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            ProxyServer.saveHostPort();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

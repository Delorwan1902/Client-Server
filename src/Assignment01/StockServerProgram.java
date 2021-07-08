package Assignment01;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class StockServerProgram {
    private static final int port = 8888;
    private static List<StockMarket> stockMarket = new ArrayList<>();
    private static List<StockClientHandler> handlers = new ArrayList<>();
    private static Boolean hasStock = true;
    private static Integer stockOwnerID = null;

    public static synchronized void setHasStock(boolean stock) {
        hasStock = stock;
    }

    public static String getStockOwnerID() {
        return Integer.toString(stockOwnerID);
    }

    public static synchronized void setStockOwnerID(Integer clientID) {
        stockOwnerID = clientID;
    }

    public static synchronized StockMarket getClient(int clientID) throws NoSuchElementException {
        for(int i = 0; i < stockMarket.size(); i++) {
            if(stockMarket.get(i).getClientID() == clientID)
                return stockMarket.get(i);
        }
        throw new NoSuchElementException("Client could not be found");
    }

    public static synchronized StockMarket createClientID() {
        synchronized (stockMarket) {
            StockMarket sm;
            if (stockMarket.isEmpty()) {
                sm = new StockMarket(0);
                stockMarket.add(sm);
                return sm; //Return 0 as no clients have connected until now
            }
            Collections.sort(stockMarket, Comparator.comparingInt(StockMarket::getClientID)); //Sort in ascending order by IDs
            sm = new StockMarket((stockMarket.get(stockMarket.size() - 1).getClientID()+1));
            stockMarket.add(sm);
            return sm;
        }
    }

    public static String getClientIDs() {
        List<String> clientIDs = new ArrayList<>();
        for(int i = 0; i < stockMarket.size(); i++) {
            if(stockMarket.get(i).getActiveInMarket())
                clientIDs.add(Integer.toString(stockMarket.get(i).getClientID()));
        }
        String clientsToString = String.join(",", clientIDs);
        return clientsToString;
    }

    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("Waiting for connection...");
            while (true) {
                Socket socket = server.accept();
                System.out.println("\nNew client connected " + socket.getInetAddress().getHostAddress());
                StockMarket newClient = createClientID();

                synchronized (hasStock) {
                    if(hasStock == true) {
                        newClient.setHasStock(true);
                        stockOwnerID = newClient.getClientID();
                        hasStock = false;
                    }
                }
                StockClientHandler sch = new StockClientHandler(socket, newClient, handlers);
                handlers.add(sch);
                new Thread(sch).start(); //Assign the new client ID here
            }
        }
        catch(Exception e) {
            System.out.println("StockServerProgram / " + e);
        }
        finally {
            if(server != null) {
                try {
                    server.close();
                }
                catch(IOException e) {
                    System.out.println("StockServerProgram / " + e);
                }
            }
        }
    }
}

package Assignment01;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class StockClientHandler implements Runnable {
    private final Socket clientSocket;
    private StockMarket client;
    private int clientID;
    private List<StockClientHandler> handlers;

    private PrintWriter out = null; //Pushing out
    private Scanner in = null; //Taking in

    public StockClientHandler(Socket clientSocket, StockMarket client, List<StockClientHandler> handlers) {
        this.clientSocket = clientSocket;
        this.client = client;
        this.clientID = client.getClientID();
        this.handlers = handlers;
    }

    public PrintWriter getPrintWriter() {
        return out;
    }

    @Override
    public void run() {
        try {
            in = new Scanner(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            String line;

            System.out.println("Client " + clientID + " has connected");
            out.println(clientID);
            out.println(client.getHasStock());

            for(StockClientHandler sch : handlers) {
                if(sch != this && sch.getPrintWriter() != null)
                        sch.getPrintWriter().println("Client " + clientID + " has connected: [" + StockServerProgram.getClientIDs() + "]");
            }

            boolean exit = false;
            while(!exit) {
                line = in.nextLine();
                String[] substrings = line.split(" ");
                switch (substrings[0].toLowerCase()) {
                    case "clients":
                        out.println(StockServerProgram.getClientIDs());
                        break;
                    case "transfer":
                        if(client.getHasStock() == true) { //Check if client has stock
                            try {
                                StockMarket sm = StockServerProgram.getClient(Integer.parseInt(substrings[1])); //Can throw NoSuchElementException if ID cannot be found
                                client.setHasStock(false); //Client no longer has the stock
                                sm.setHasStock(true);
                                for(StockClientHandler sch : handlers) {
                                    if(sch.client.getClientID() == Integer.parseInt(substrings[1]) && sch.getPrintWriter() != null) {
                                        sch.getPrintWriter().println("You have now received the stock. You can now trade with other available clients");
                                        StockServerProgram.setStockOwnerID(Integer.parseInt(substrings[1]));
                                    }
                                }
                                out.println("SUCCESS");
                            }
                            catch(NoSuchElementException e) {
                                out.println("Client does not exist");
                            }
                        }
                        else
                            out.println("You have no stock to trade with");
                        break;
                    case "stock":
                        out.println(StockServerProgram.getStockOwnerID());
                        break;
                    case "exit":
                        out.println("exit");
                        exit = true;
                        break;
                    default:
                        out.println("Invalid argument, try again!");
                        break;
                }
            }
        }
        catch(Exception e) {
            if(client.getHasStock() == true) {
                List<StockClientHandler> temp = new ArrayList<>();
                for(StockClientHandler sch : handlers) {
                    if(sch != this && sch.getPrintWriter() != null)
                        temp.add(sch);
                }
                if(!temp.isEmpty()) {
                    StockClientHandler sch = temp.get(new Random().nextInt(temp.size())); //Pick random element from list
                    sch.client.setHasStock(true);
                    StockServerProgram.setStockOwnerID(sch.client.getClientID());
                    sch.getPrintWriter().println("You have now received the stock. You can now trade with other available clients");
                }
                else {
                    StockServerProgram.setHasStock(true);
                    StockServerProgram.setStockOwnerID(null);
                    System.out.println("No clients available, server has the stock temporarily");
                }
            }

            client.setHasStock(false);
            client.setActiveInMarket(false);

            try {
                clientSocket.close();
            }
            catch (IOException ioException) {
                System.out.println("StockClientHandler / " + ioException);
            }
        }
        finally {
            System.out.println("Client " + clientID + " has disconnected");
            if(out != null) out.close();
            if(in != null) in.close();
            out = null;
        }
    }
}

package Assignment01;

import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerConnection implements Runnable {
    private Socket server;
    private Scanner in;
    public List<Integer> clientList = new ArrayList<>();
    private boolean getList = false, showStockOwner = false;
    private Integer stockOwner = null;
    private StockClient sc;

    public ServerConnection(Socket socket, StockClient stockClient) throws Exception {
        server = socket;
        in = new Scanner(server.getInputStream());
        sc = stockClient;
    }

    public void checkForList() {
        getList = true;
    }

    public List<Integer> getClientList() {
        return clientList;
    }

    public void checkForStockOwner() {
        showStockOwner = true;
    }

    public Integer getStockOwner() {
        return stockOwner;
    }

    @Override
    public void run() {
        String serverResponse;

        try {
            while (true) {
                String line;
                serverResponse = in.nextLine();

                if (getList) {
                    synchronized (clientList) {
                        clientList = Stream.of(serverResponse.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
                        line = "Clients available: " + clientList;
                        System.out.println(line);
                        getList = false;
                    }
                }
                else if(showStockOwner) {
                    stockOwner = Integer.parseInt(serverResponse);
                    line = "\nCurrent owner of stock: " + stockOwner;
                    System.out.println(line);
                    showStockOwner = false;
                }
                else {
                    line = "Server replied: " + serverResponse;
                    System.out.println(line);
                }
            }
        }
        catch (Exception e) {

        }
        finally {
            in.close();
        }

    }
}

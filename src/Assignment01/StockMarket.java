package Assignment01;

public class StockMarket {
    private int clientID;
    public boolean activeInMarket;
    private boolean hasStock;

    public StockMarket(int clientID) {
        this.clientID = clientID;
        activeInMarket = true;
        hasStock = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Client ID: " + clientID + ", has stock: " + getHasStock());
        return sb.toString();
    }

    public int getClientID() {
        return clientID;
    }

    public boolean getHasStock() {
        return hasStock;
    }

    public void setHasStock(boolean hasStock) {
        this.hasStock = hasStock;
    }

    public boolean getActiveInMarket() {
        return activeInMarket;
    }

    public void setActiveInMarket(boolean activeInMarket) {
        this.activeInMarket = activeInMarket;
    }
}

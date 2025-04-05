package ticket;

public class Ticket {
    private final String id;
    private final String eventName;
    private final double price;

    public Ticket(String id, String eventName, double price) {
        this.id = id;
        this.eventName = eventName;
        this.price = price;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("Ticket{id='%s', event='%s', price=%.2f}",
                id, eventName, price);
    }
}
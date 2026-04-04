package com.example.bookingapp;

public class Ticket {
    private String ticketId;
    private String seatNumber;
    private double price;
    private String eventId;
    private String eventTitle;
    private String reservationId;

    /** Required empty constructor */
    public Ticket() {}

    public Ticket(String ticketId, String seatNumber, double price,
                  String eventId, String eventTitle, String reservationId) {
        this.ticketId      = ticketId;
        this.seatNumber    = seatNumber;
        this.price         = price;
        this.eventId       = eventId;
        this.eventTitle    = eventTitle;
        this.reservationId = reservationId;
    }

    /**
     * Factory method — creates and returns a Ticket for a confirmed reservation
     *
     */
    public static Ticket generateTicket(Reservation reservation) {
        // Generate a simple ticket ID from the reservation ID
        String ticketId    = "TKT-" + reservation.getReservationId()
                .substring(0, Math.min(8,
                        reservation.getReservationId().length()))
                .toUpperCase();

        // Seat number — in a real system this would be assigned from a seat map.
        // For now we auto-assign based on reservation order.
        String seatNumber = "AUTO-" + (int)(Math.random() * 1000);

        double price = 0.0;
        try {
            price = Double.parseDouble(reservation.getTotalPrice());
        } catch (NumberFormatException ignored) {}

        return new Ticket(
                ticketId,
                seatNumber,
                price,
                reservation.getEventId(),
                reservation.getEventTitle(),
                reservation.getReservationId()
        );
    }

    //  Getters
    public String getTicketId()      { return ticketId; }
    public String getSeatNumber()    { return seatNumber; }
    public double getPrice()         { return price; }
    public String getEventId()       { return eventId; }
    public String getEventTitle()    { return eventTitle; }
    public String getReservationId() { return reservationId; }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId='" + ticketId + '\'' +
                ", seatNumber='" + seatNumber + '\'' +
                ", event='" + eventTitle + '\'' +
                ", price=" + price +
                '}';
    }
}

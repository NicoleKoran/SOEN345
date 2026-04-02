package com.example.bookingapp;
import com.example.bookingapp.models.Event;

import org.junit.Before;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class EventTest {
    private Event event;
    private Date testDate;

    @Before
    public void setUp(){
        event = new Event("E001", "Daniel Caesar Concert", testDate, "Montreal", "XXX", Event.EventCategory.concert, 200, 100, Event.EventStatus.available, 150);
    }

    @Test
    public void constructorSetupTest(){
        assertEquals(event.getEventId(), "E001");
        assertEquals(event.getTitle(), "Daniel Caesar Concert");
        assertEquals(event.getDate(), testDate);
        assertEquals(event.getLocation(), "Montreal");
        assertEquals(event.getDescription(), "XXX");
        assertEquals("concert", event.getCategory());
        assertEquals(event.getTotalSeats(), 200);
        assertEquals(event.getAvailableSeats(), 100);
        assertEquals("available", event.getStatus());
        assertEquals(event.getPrice(), 150);
    }

    @Test
    public void setTitleTest(){
        event.setTitle("DC Concert");
        assertEquals(event.getTitle(), "DC Concert");
    }

    @Test
    public void setDateTest(){
        Date newDate = new Date();
        event.setDate(newDate);
        assertEquals(event.getDate(), newDate);
    }

    @Test
    public void setLocationTest(){
        event.setLocation("NYC");
        assertEquals(event.getLocation(), "NYC");
    }

    @Test
    public void setDescriptionTest(){
        event.setDescription("000");
        assertEquals(event.getDescription(), "000");
    }

    @Test
    public void getCategoryEnum_returnsCorrectEnum_forValidCategory() {
        assertEquals(Event.EventCategory.concert, event.getCategoryEnum());
    }

    @Test
    public void getCategoryEnum_returnsNull_whenCategoryIsNull() {
        event.setCategory(null);
        assertNull(event.getCategoryEnum());
    }

    @Test
    public void getCategoryEnum_returnsNull_forUnknownCategory() {
        event.setCategory("unknown_category");
        assertNull(event.getCategoryEnum());
    }

    @Test
    public void getCategoryEnum_handlesAllValidCategories() {
        for (Event.EventCategory cat : Event.EventCategory.values()) {
            event.setCategoryEnum(cat);
            assertEquals(cat, event.getCategoryEnum());
        }
    }

    @Test
    public void setCategoryEnum_storesCategoryAsString() {
        event.setCategoryEnum(Event.EventCategory.travel);
        assertEquals("travel", event.getCategory());
    }

    @Test
    public void getStatusEnum_returnsCorrectEnum_forValidStatus() {
        assertEquals(Event.EventStatus.available, event.getStatusEnum());
    }

    @Test
    public void getStatusEnum_returnsNull_whenStatusIsNull() {
        event.setStatus(null);
        assertNull(event.getStatusEnum());
    }

    @Test
    public void getStatusEnum_returnsNull_forUnknownStatus() {
        event.setStatus("invalid_status");
        assertNull(event.getStatusEnum());
    }

    @Test
    public void getStatusEnum_handlesAllValidStatuses() {
        for (Event.EventStatus status : Event.EventStatus.values()) {
            event.setStatusEnum(status);
            assertEquals(status, event.getStatusEnum());
        }
    }

    @Test
    public void setTotalSeatsTest(){
        event.setTotalSeats(400);
        assertEquals(event.getTotalSeats(), 400);
    }

    @Test
    public void setAvailableSeatsTest(){
        event.setAvailableSeats(300);
        assertEquals(event.getAvailableSeats(), 300);
    }
    @Test
    public void setPriceTest(){
        event.setPrice(25);
        assertEquals(event.getPrice(), 25);
    }

}

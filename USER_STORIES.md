# User Stories – Cloud-based Ticket Reservation Application
**SOEN 345: Software Testing, Verification and Quality Assurance | Winter 2026**

---

## 👤 Customer User Stories

### Registration & Authentication

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-01 | As a **customer**, I want to **register an account using my email address**, so that I can **access the ticket reservation system**. | Given a valid email and password, when I submit the registration form, then an account is created and a confirmation email is sent. | High |
| US-02 | As a **customer**, I want to **register an account using my phone number**, so that I can **access the system without an email address**. | Given a valid phone number, when I submit the form, then an OTP is sent for verification and the account is created upon success. | High |
| US-03 | As a **customer**, I want to **log into my account**, so that I can **manage my reservations and profile**. | Given valid credentials, when I log in, then I am redirected to the home screen with my account loaded. | High |

---

### Browsing & Searching Events

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-04 | As a **customer**, I want to **view a list of all available events**, so that I can **explore what is currently on offer**. | Given I am logged in, when I navigate to the events page, then I see a list of upcoming events with names, dates, locations, and categories. | High |
| US-05 | As a **customer**, I want to **search for events by keyword**, so that I can **quickly find a specific event**. | Given a search term, when I submit the search, then only matching events are displayed. | High |
| US-06 | As a **customer**, I want to **filter events by date**, so that I can **find events happening on a specific day**. | Given a selected date range, when I apply the filter, then only events within that range are shown. | High |
| US-07 | As a **customer**, I want to **filter events by location**, so that I can **find events near me**. | Given a city or region, when I apply the location filter, then only events in that area are displayed. | Medium |
| US-08 | As a **customer**, I want to **filter events by category** (e.g., concert, movie, sports, travel), so that I can **browse events I am interested in**. | Given a selected category, when I apply the filter, then only events of that category are listed. | Medium |

---

### Booking & Reservations

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-09 | As a **customer**, I want to **reserve a ticket for an event**, so that I can **secure my spot**. | Given an available event, when I select a ticket and confirm, then a reservation is created and the seat count is decremented from availability. | High |
| US-10 | As a **customer**, I want to **view my reservation history**, so that I can **keep track of my past and upcoming bookings**. | Given I am logged in, when I navigate to "My Reservations", then I see a list of all my reservations with status (active/cancelled). | High |
| US-11 | As a **customer**, I want to **cancel a reservation**, so that I can **free up a ticket I no longer need**. | Given an active reservation, when I request cancellation, then the reservation is marked as cancelled and the ticket becomes available again. | High |

---

### Confirmations & Notifications

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-12 | As a **customer**, I want to **receive a confirmation via email after booking**, so that I can **have a record of my reservation**. | Given a completed reservation, when the booking is confirmed, then a confirmation email is sent to the registered email address within 1 minute. | High |
| US-13 | As a **customer**, I want to **receive a confirmation via SMS after booking**, so that I can **be notified even without internet access**. | Given a completed reservation and a registered phone number, when the booking is confirmed, then an SMS is sent with the reservation details. | Medium |
| US-14 | As a **customer**, I want to **receive an SMS/email notification when an event I booked is cancelled**, so that I can **make alternative arrangements**. | Given an admin cancels an event, when the cancellation is processed, then all customers with reservations for that event are notified. | High |

---

## 🛠️ Administrator User Stories

### Event Management

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-15 | As an **administrator**, I want to **add a new event**, so that **customers can discover and book tickets for it**. | Given event details (name, date, location, category, ticket count), when I submit the form, then the event appears in the event listings. | High |
| US-16 | As an **administrator**, I want to **edit an existing event**, so that I can **correct or update event details**. | Given an existing event, when I modify details and save, then the updated information is reflected immediately in the listings. | High |
| US-17 | As an **administrator**, I want to **cancel an event**, so that I can **handle unforeseen circumstances**. | Given an active event, when I cancel it, then it is removed from listings, all reservations are cancelled, and customers are notified. | High |
| US-18 | As an **administrator**, I want to **view all reservations for an event**, so that I can **track attendance and manage capacity**. | Given an event, when I open its details, then I see a list of all customers with reservations and their statuses. | Medium |

---

## ⚙️ Non-Functional / System User Stories

| ID    | User Story | Acceptance Criteria | Priority |
|-------|-----------|---------------------|----------|
| US-19 | As a **system**, I want to **handle multiple concurrent users booking tickets simultaneously**, so that **no double-booking or data inconsistency occurs**. | Given 100+ concurrent users, when simultaneous bookings are made, then the system maintains correct ticket counts without race conditions. | High |
| US-20 | As a **system**, I want to **remain highly available via cloud deployment**, so that **users can access the application 24/7 without downtime**. | Given the app is deployed on a cloud platform, when traffic spikes, then uptime remains ≥ 99.5% with no service degradation. | High |
| US-21 | As a **customer**, I want to **navigate the app through a simple and user-friendly interface**, so that I can **complete bookings without confusion or assistance**. | Given a first-time user, when they open the app, then they can complete a reservation in under 3 steps without any help documentation. | Medium |

---

## 📋 Summary Table

| Category | # of Stories |
|---------|-------------|
| Registration & Authentication | 3 |
| Browsing & Searching Events | 5 |
| Booking & Reservations | 3 |
| Confirmations & Notifications | 3 |
| Administrator Event Management | 4 |
| Non-Functional / System | 3 |
| **Total** | **21** |

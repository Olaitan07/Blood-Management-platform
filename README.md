# Blood-Management-platform
Modular monolith for blood management — hospitals share inventory and coordinate transfers in real time. Spring Boot with Spring Modulith, event-driven, PostgreSQL.


**Blood Management Platform**

An event-driven system that connects hospitals into a single network to share blood inventory and coordinate transfers in real time — reducing both shortages and expiry waste.

Built as a modular monolith: a React frontend talks to a Spring Boot backend organised into clear, independently-developed modules (auth, donor, inventory, transfer, notification, audit, and more). Modules stay decoupled by communicating through in-process domain events rather than direct calls, with PostgreSQL for persistence — giving the structural clarity of microservices without the operational complexity of a distributed system.

Clinicians can search blood availability across hospitals and request transfers; blood bank officers approve requests and manage inventory; donors track eligibility and donation history; administrators manage hospitals, users, and reports. The transfer workflow is implemented as an event-driven saga that reserves stock on approval and releases it through compensating actions on cancellation or timeout.

Key features include role-based access secured with JWT, automatic donor eligibility tracking, blood expiry monitoring, event-driven notifications, and an immutable audit trail for healthcare accountability.

**Tech stack:** Java 17, Spring Boot 3, Spring Modulith, PostgreSQL, React.

Developed as a Master's project to demonstrate modular architecture, event-driven design, and clean module boundaries applied to a real-world healthcare information system.


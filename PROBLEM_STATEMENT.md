# 27. Enterprise Issue Escalation & Service Desk Management System

Stored from the course brief for reference.

## Business use case

The company ResolveNow Service Desk requires an automated ticket management platform to log customer grievances, assign issues to specialized departments, and track resolution lifecycles. ResolveNow must provide real-time ticket updates, ensure transparent department assignments, and deliver automated progress alerts to users. The company specifies a microservices design that decouples complaint intake, department task assignment, and user notification delivery. ResolveNow mandates JWT-secured access controls, an API Gateway for ingress routing, and Eureka service discovery with load balancing to maintain service continuity during high-volume support surges.

The system should:

- Track complaint lifecycle
- Assign issues to departments
- Notify users on updates

## Microservices

- Complaint Service
- Assignment Service
- Notification Service
- API Gateway, Auth Service, Eureka Server

## Tasks

- Implement JWT authentication
- Complaint Service logs issues
- Assignment Service assigns tasks
- Notification Service alerts users
- Inter-service communication (Complaint → Assignment → Notification)
- Register services with Eureka
- Route via API Gateway
- Enable load balancing
- Write tests
- Deploy application

## Sample database tables

**Complaint:** `complaint_id`, `user_id`, `description`, `status`

**Assignment:** `assignment_id`, `complaint_id`, `assigned_to`

**Notification:** `notification_id`, `user_id`, `message`

These are the **required names and minimum columns**. Extra columns are allowed. Extra unused tables and renaming Complaint → Request are not.

# Aggregate-Level Monolith to Service Decomposition Example

This folder contains two web applications to demostrate the decomposition of a monolithic application into 
microservices via the aggregate level service decomposition approach. There is a simple domain model, which is
composed of two entities: Club and Player, which have a one-to-many relationship.

The following design decisions are followed throughout the implementation:

* Both the service and monolithic applications have their own databases, and they store entity data in their own 
databases, but two fo those systems constitute a single logical data model. Therefore, PK values of entity instances 
need to be unique across the two databases.
* Dual write operation is prohibited. At any time in the system, only one party can write to the data model. The other 
party is responsible its own database via consuming entity change events published by the source of truth. 

## How To Build & Run

In order to run those two web applications, and their dependencies such as Kafka, H2, you need to run the following 
command:

```bash
docker-compose up --build -d
```

In order to stop the running containers, you can run the following command:

```bash
docker-compose down
```

## The Monolithic Application

**clubs-monolith** is the monolithic application. It runs on port 8080. You can access the monolithic application via 
the following URL: http://localhost:8080

## The Service Application

**clubs-service** is the decomposed service application. It runs on port 8090. You can access the decomposed service 
application via the following URL: http://localhost:8090

## Bulk Sync Operation

There is a component called BulksyncTool which is responsible for syncing the data from the monolith to the service.  
The bulk sync operation is a one-time operation, which is used to sync the data from the monolith to the service. You can 
access the bulk sync operation via sending a POST request to the /migrations/bulkSync endpoint.

## The Entity Change Events

Both monolithic and service applications publish entity change events to the Kafka topic named **entity-change-topic**. The 
monolithic application publishes entity change events when a new entity is created, updated or deleted. The service
consumes those entity change events and updates its own database accordingly. Same story happens in reverse order when
the service application takes the write responsibility to the data model.

## Id Mappings

There is an id_mappings table in the monolith and service databases. The id mappings are used to keep track of the ids 
of the entities in the monolith and the service.

## The Service Application Operation Modes

* **READ_ONLY**: It means that only read-only operations should be served from the service, all write operations should 
be forwarded to the monolith. At the stage, any changes on the monoith should be reflected to the service via entity 
change events published by the monolith and consumed by the service. This is the initial operation mode of the service.

* **READ_WRITE**: It means that all read and write operations should be served from the service, before executing the 
write operation, the entity state should be synced from the monolith if necessary. At this state, the service 
decomposition is completed and the monolith only exists to serve as legacy data contract for the data analytics.

* **DRY_RUN**: It means that all read and write operations should be forwarded to the monolith, meanwhile write 
operations should be executed on the service as well without causing any external side effects on the other parties.
The purpose of the DRY_RUN mode is to check if service produces same data with the monolith. This can be achieved by 
comparing the data in the sourceDB and targetDB. If the verification is successful, then the service can be switched to 
the READ_ONLY mode or READ_WRITE mode.

## How to Change Operation Mode

**/migrations/operation-mode** endpoint can be used to display and change the operation mode of the service. The 
operation mode can be changed by sending a PUT request to the /migrations/operation-mode endpoint with the desired 
operation mode as the request body. The operation mode can be one of the following string values: 
READ_ONLY, READ_WRITE, DRY_RUN.

## Accessing the Environment

You can inspect contents of entity-change-topic Kafka topic via the following URL: http://localhost:9001.
You can inspect contents of sourceDB and targetDB via the following URL: http://localhost:81.
After accessing the H2 console you can connect to the sourceDB and targetDB by using the following JDBC URLs.

* For source DB `jdbc:h2:tcp://localhost:1521/sourceDB` with username: `sa` and password: <empty>
* For target DB `jdbc:h2:tcp://localhost:1522/targetDB` with username: `sa` and password: <empty>

You can also inspect the contents of the id_mappings, players and clubs tables by executing the following SQL queries:
```sql
select * from id_mappings;
select * from players;
select * from clubs;
```


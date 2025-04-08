# Java gRPC Server (Maven + Spring Boot)

This repository showcases a **Java-based gRPC server** built with **Spring Boot**, **Maven**, and **Protocol Buffers**. It demonstrates a working gRPC service implementation using best practices for dependency management, server configuration, and reflection support.

> 🔧 **Tech Stack:** Java 21, Spring Boot, gRPC, Maven, Protobuf


---

## ✨ Project Highlights

- 📡 **gRPC Server Setup:** Lightweight and high-performance server setup using `grpc-netty`.
- 📜 **Protobuf Integration:** `.proto` definitions compiled via Maven for generating gRPC stubs.
- 🧩 **Spring Boot Integration:** Simplifies configuration and dependency injection.
- 🔍 **Reflection Enabled:** Server supports gRPC reflection for easy introspection and debugging.
- 🔄 **Bidirectional Communication Ready:** Base for implementing unary, server-streaming, client-streaming, and bidirectional gRPC methods.

---

## 📁 Project Structure

```bash
Java-Spring-gRPC/ 
├── src/ 
├── main/ 
│   │   ├── java/code
│   │   │   ├── Main.java 
│   │   │   ├── client/ 
│   │   │   │   └── GrpcClient.java  
│   │   │   ├── config/ 
│   │   │   │   └── GrpcServerConfig.java  
│   │   │   └── services/ 
│   │   │       └── GreetingServiceImpl.java  
│   │   └── proto/ 
│   │       └── greeting.proto 
│   └── resources/ 
│   └── application.yml 
├── pom.xml 
├── LICENSE 
└── README.md
```


---

## 🚀 Getting Started

### 1. Clone the Repository

    ```bash
    git clone https://github.com/MrDay2Day/grpc-server-java.git
    cd grpc-server-java
    ```

### 2. Build the Project
Ensure you have Maven installed. You can build the project using the following command:Make sure you have Java 21 and Maven 3.8+ installed.

   ```bash
    mvn clean install
   ```

### 3. Run the gRPC Server

   ```bash
   mvn spring-boot:run
   ```
The server will start on the default port: `3031`

## 📜 Protobuf Definition
`greeting.proto` defines the `GreetingService`:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.example.grpc";
option java_outer_classname = "GreetingServiceProto";

service GreetingService {
  rpc sayHello (HelloRequest) returns (HelloResponse);
}

message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string message = 1;
}
```

## 📦 Maven Plugin for Proto Compilation
This project uses the protobuf-maven-plugin to automatically compile .proto files during build:

```xml
<plugin>
  <groupId>org.xolstice.maven.plugins</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  ...
</plugin>
```
## 🔧 Reflection Support
The server is configured to support gRPC reflection, allowing clients to discover available services and methods dynamically. This is useful for debugging and testing.

Reflection is enabled by adding the following to `GrpcServerConfig.java`:

```java
.addService(ProtoReflectionService.newInstance())
```
This allows tools like grpcurl to discover and call methods:

```bash
grpcurl -plaintext localhost:3031 list
```

## 🧪 Testing the gRPC Server
To test the server:

```bash
grpcurl -plaintext -d '{"name": "Day2Day"}' localhost:3031 com.example.grpc.GreetingService/sayHello
```

Expected Response:
```json
{
  "message": "Hello, Day2Day!"
}
```

## 📦 Dependency Management
Dependencies are centrally managed in `pom.xml`, with specific versions pulled in via `<dependencyManagement>` for maintainability:

```xml
<dependency>
  <groupId>net.devh</groupId>
  <artifactId>grpc-server-spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>io.grpc</groupId>
  <artifactId>grpc-services</artifactId>
</dependency>
```

##  ✅ Skills Demonstrated
- ⚙️ Setting up a gRPC server with Spring Boot
- 📄 Writing and compiling Protocol Buffers with Maven
- 🧠 Understanding gRPC service design patterns (Unary RPC in this case)
- 🧩 Integrating Protobuf-generated code into business logic
- 🧪 Using testing/debugging tools like grpcurl with reflection
- 📦 Clean dependency management using Maven

## 📌 Future Enhancements
- ✅ Add support for streaming RPCs
- ✅ Implement client-side logic
- ✅ Add unit tests and integration tests
- ✅ Dockerize the application for containerized deployment
- ✅ Add observability (metrics/logging)

## 📄 License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
```



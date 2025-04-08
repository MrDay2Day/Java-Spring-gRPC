package code.services;

import com.example.grpc.GreetingServiceGrpc.GreetingServiceImplBase;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class GreetingServiceImpl extends GreetingServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(GreetingServiceImpl.class);

    @PostConstruct
    public void init() {
        logger.info("GreetingServiceImpl initialized");
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        logger.info("Received request from: {}", request.getName());
        String name = request.getName();
        String message = "Hello, " + name + "!";
        HelloResponse response = HelloResponse.newBuilder().setMessage(message).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
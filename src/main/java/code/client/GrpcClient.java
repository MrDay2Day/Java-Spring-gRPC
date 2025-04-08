package code.client;

import com.example.grpc.GreetingServiceGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 3031).usePlaintext().build();
        GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

        HelloRequest request = HelloRequest.newBuilder().setName("World").build();
        HelloResponse response = stub.sayHello(request);

        System.out.println(response.getMessage());

        channel.shutdown();
    }
}
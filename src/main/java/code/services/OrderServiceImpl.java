package code.services;

import com.example.grpc.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.example.grpc.OrderServiceGrpc;
import com.example.grpc.orderStatusResponse;

import javax.annotation.PostConstruct;

@Service
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @PostConstruct
    public void init() {
        logger.info("GreetingServiceImpl initialized");
    }

    @Override
    public void getOrder(getOrderRequest request,
                         io.grpc.stub.StreamObserver<getOrderResponse> responseObserver){
        logger.info("Received 'getOrder' request from: {}", request.getProductId());
        System.out.println(("Received request from: " + request.toString()));

        getOrderResponse response = getOrderResponse.newBuilder()
                .setOrderId(100000001)
                .setTotal((request.getUnits() * request.getUnitCost()) * 2.3)
                .setStatus(String.valueOf(OrderStatusResponse.PENDING))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getOrderStatus(orderStatusRequest request, StreamObserver<orderStatusResponse> responseObserver){
        logger.info("Received 'getOrderStatus' request from: {}", request.getOrderId());
        System.out.println(("Received request from: " + request.toString()));

        orderStatusResponse response = orderStatusResponse.newBuilder()
                .setStatus(OrderStatusResponse.SHIPPED.toString())
                .setStatus(String.valueOf(OrderStatusResponse.SHIPPED))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }




}


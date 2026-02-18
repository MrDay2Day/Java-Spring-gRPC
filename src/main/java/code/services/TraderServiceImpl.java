package code.services;

import com.google.protobuf.Timestamp;
import d2d.trader.grpc.TraderServiceGrpc.TraderServiceImplBase;
import d2d.trader.grpc.getTraderRequest;
import d2d.trader.grpc.getTraderResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;

@Service
public class TraderServiceImpl extends TraderServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(TraderServiceImpl.class);

    @PostConstruct
    public void init() {
        logger.info("TraderServiceImpl initialized");
    }

    @Override
    public void processTrade(getTraderRequest request, StreamObserver<getTraderResponse> responseObserver) {
        logger.info("Received 'processTrade' request from: {}", request.getSymbol());
        System.out.println(("Received request from: " + request.toString()));
        boolean execute = request.getPrice() > 15000.00;

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        getTraderResponse response = getTraderResponse
                .newBuilder()
                .setExecute(execute)
                .setSymbol(request.getSymbol())
                .setSide(request.getSide())
                .setTimestamp(request.getTimestamp())
                .setPrice(request.getPrice())
                .setAmount(request.getAmount())
                .setProcessedDateTime(timestamp)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

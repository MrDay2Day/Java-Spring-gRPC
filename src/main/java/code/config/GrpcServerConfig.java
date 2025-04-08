package code.config;

import code.services.GreetingServiceImpl;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import javax.annotation.PreDestroy;
import java.io.IOException;
import io.grpc.protobuf.services.ProtoReflectionService;

@Configuration
public class GrpcServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(GrpcServerConfig.class);

    @Value("${grpc.server.port}")
    private int grpcPort;

    private Server server;

    @Bean(initMethod = "start")
    public Server grpcServer(GreetingServiceImpl greetingService) throws IOException {
        server = NettyServerBuilder.forPort(grpcPort)
                .addService(greetingService)
                .addService(ProtoReflectionService.newInstance())
                .build();

        logger.info("gRPC Server will start on port: {}", grpcPort);
        return server;
    }

    public void start() throws IOException {
        server.start();
        logger.info("gRPC Server started, listening on port: {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server");
            GrpcServerConfig.this.stop();
            logger.info("gRPC server shut down complete");
        }));
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
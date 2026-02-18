package code.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class GetProtoFiles {

    public static void main(String[] args) throws InterruptedException {
        // Fetch protos from localhost:3031 and save to "downloaded_protos"
        fetchAndSaveProtos("localhost", 3031, "downloaded_protos");
    }

    public static void fetchAndSaveProtos(String host, int port, String outputDir) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            File dir = new File(outputDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + outputDir);
                }
            }

            CountDownLatch finishLatch = new CountDownLatch(1);
            Set<String> processedFiles = new HashSet<>();
            AtomicInteger pendingRequests = new AtomicInteger(0);
            AtomicReference<StreamObserver<ServerReflectionRequest>> requestObserverRef = new AtomicReference<>();

            ServerReflectionGrpc.ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);
            StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(ServerReflectionResponse response) {
                    if (response.hasListServicesResponse()) {
                        List<ServiceResponse> services = response.getListServicesResponse().getServiceList();
                        System.out.println("Found " + services.size() + " services.");

                        for (ServiceResponse service : services) {
                            pendingRequests.incrementAndGet();
                            ServerReflectionRequest request = ServerReflectionRequest.newBuilder()
                                    .setHost(host)
                                    .setFileContainingSymbol(service.getName())
                                    .build();
                            StreamObserver<ServerReflectionRequest> observer = requestObserverRef.get();
                            if (observer != null) {
                                observer.onNext(request);
                            }
                        }
                    } else if (response.hasFileDescriptorResponse()) {
                        List<ByteString> fileDescriptorProtoList = response.getFileDescriptorResponse().getFileDescriptorProtoList();
                        for (ByteString bytes : fileDescriptorProtoList) {
                            try {
                                DescriptorProtos.FileDescriptorProto fileProto = DescriptorProtos.FileDescriptorProto.parseFrom(bytes);
                                String name = fileProto.getName();
                                synchronized (processedFiles) {
                                    if (!processedFiles.contains(name)) {
                                        processedFiles.add(name);
                                        saveFile(dir, fileProto);
                                        System.out.println("Saved: " + name);
                                    }
                                }
                            } catch (InvalidProtocolBufferException e) {
                                System.err.println("Failed to parse descriptor: " + e.getMessage());
                            }
                        }
                    }

                    if (pendingRequests.decrementAndGet() == 0) {
                        StreamObserver<ServerReflectionRequest> observer = requestObserverRef.get();
                        if (observer != null) {
                            observer.onCompleted();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Error: " + t.getMessage());
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Completed fetching protos.");
                    finishLatch.countDown();
                }
            };

            StreamObserver<ServerReflectionRequest> requestObserver = stub.serverReflectionInfo(responseObserver);
            requestObserverRef.set(requestObserver);

            // Initial request to list services
            pendingRequests.incrementAndGet();
            requestObserver.onNext(ServerReflectionRequest.newBuilder()
                    .setHost(host)
                    .setListServices("")
                    .build());

            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                System.err.println("Timed out waiting for proto fetch to complete.");
            }

        } finally {
            channel.shutdownNow();
        }
    }

    private static void saveFile(File dir, DescriptorProtos.FileDescriptorProto fileProto) {
        try {
            File file = new File(dir, fileProto.getName());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean created = parent.mkdirs();
                if (!created) {
                    System.err.println("Failed to create parent directory for: " + fileProto.getName());
                }
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                String protoSource = ProtoPrinter.print(fileProto);
                fos.write(protoSource.getBytes());
            }
        } catch (IOException e) {
            System.err.println("Failed to save file " + fileProto.getName() + ": " + e.getMessage());
        }
    }

    private static class ProtoPrinter {
        public static String print(DescriptorProtos.FileDescriptorProto fd) {
            StringBuilder sb = new StringBuilder();
            String syntax = fd.hasSyntax() ? fd.getSyntax() : "proto2";
            sb.append("syntax = \"").append(syntax).append("\";\n\n");

            if (fd.hasPackage()) {
                sb.append("package ").append(fd.getPackage()).append(";\n\n");
            }

            for (String dep : fd.getDependencyList()) {
                sb.append("import \"").append(dep).append("\";\n");
            }
            if (fd.getDependencyCount() > 0) sb.append("\n");

            if (fd.hasOptions()) {
                printFileOptions(sb, fd.getOptions());
            }

            for (DescriptorProtos.EnumDescriptorProto ed : fd.getEnumTypeList()) {
                printEnum(sb, ed, "");
            }

            for (DescriptorProtos.DescriptorProto msg : fd.getMessageTypeList()) {
                printMessage(sb, msg, "", syntax);
            }

            for (DescriptorProtos.ServiceDescriptorProto sd : fd.getServiceList()) {
                printService(sb, sd);
            }

            return sb.toString();
        }

        private static void printFileOptions(StringBuilder sb, DescriptorProtos.FileOptions options) {
            if (options.hasJavaPackage()) sb.append("option java_package = \"").append(options.getJavaPackage()).append("\";\n");
            if (options.hasJavaOuterClassname()) sb.append("option java_outer_classname = \"").append(options.getJavaOuterClassname()).append("\";\n");
            if (options.hasJavaMultipleFiles()) sb.append("option java_multiple_files = ").append(options.getJavaMultipleFiles()).append(";\n");
            if (options.hasGoPackage()) sb.append("option go_package = \"").append(options.getGoPackage()).append("\";\n");
            if (options.hasObjcClassPrefix()) sb.append("option objc_class_prefix = \"").append(options.getObjcClassPrefix()).append("\";\n");
            if (options.hasCsharpNamespace()) sb.append("option csharp_namespace = \"").append(options.getCsharpNamespace()).append("\";\n");
            if (options.hasSwiftPrefix()) sb.append("option swift_prefix = \"").append(options.getSwiftPrefix()).append("\";\n");
            if (options.hasPhpClassPrefix()) sb.append("option php_class_prefix = \"").append(options.getPhpClassPrefix()).append("\";\n");
            if (options.hasPhpNamespace()) sb.append("option php_namespace = \"").append(options.getPhpNamespace()).append("\";\n");
            sb.append("\n");
        }

        private static void printEnum(StringBuilder sb, DescriptorProtos.EnumDescriptorProto ed, String indent) {
            sb.append(indent).append("enum ").append(ed.getName()).append(" {\n");
            for (DescriptorProtos.EnumValueDescriptorProto val : ed.getValueList()) {
                sb.append(indent).append("  ").append(val.getName()).append(" = ").append(val.getNumber()).append(";\n");
            }
            sb.append(indent).append("}\n\n");
        }

        private static void printMessage(StringBuilder sb, DescriptorProtos.DescriptorProto msg, String indent, String syntax) {
            sb.append(indent).append("message ").append(msg.getName()).append(" {\n");

            for (DescriptorProtos.EnumDescriptorProto ed : msg.getEnumTypeList()) {
                printEnum(sb, ed, indent + "  ");
            }

            for (DescriptorProtos.DescriptorProto nested : msg.getNestedTypeList()) {
                printMessage(sb, nested, indent + "  ", syntax);
            }

            // Group fields by oneof index
            Map<Integer, List<DescriptorProtos.FieldDescriptorProto>> oneofs = new HashMap<>();
            List<DescriptorProtos.FieldDescriptorProto> normalFields = new ArrayList<>();

            for (DescriptorProtos.FieldDescriptorProto field : msg.getFieldList()) {
                if (field.hasOneofIndex()) {
                    // Check for proto3_optional
                    if (field.hasProto3Optional() && field.getProto3Optional()) {
                        normalFields.add(field);
                    } else {
                        oneofs.computeIfAbsent(field.getOneofIndex(), k -> new ArrayList<>()).add(field);
                    }
                } else {
                    normalFields.add(field);
                }
            }

            for (DescriptorProtos.FieldDescriptorProto field : normalFields) {
                printField(sb, field, indent + "  ", syntax);
            }

            for (Map.Entry<Integer, List<DescriptorProtos.FieldDescriptorProto>> entry : oneofs.entrySet()) {
                int index = entry.getKey();
                // Check if oneof declaration exists
                if (index < msg.getOneofDeclCount()) {
                    String oneofName = msg.getOneofDecl(index).getName();
                    sb.append(indent).append("  oneof ").append(oneofName).append(" {\n");
                    for (DescriptorProtos.FieldDescriptorProto field : entry.getValue()) {
                        printField(sb, field, indent + "    ", syntax);
                    }
                    sb.append(indent).append("  }\n");
                } else {
                    // Fallback if oneof declaration is missing
                    for (DescriptorProtos.FieldDescriptorProto field : entry.getValue()) {
                        printField(sb, field, indent + "  ", syntax);
                    }
                }
            }

            sb.append(indent).append("}\n\n");
        }

        private static void printField(StringBuilder sb, DescriptorProtos.FieldDescriptorProto field, String indent, String syntax) {
            sb.append(indent);
            if (syntax.equals("proto2")) {
                if (field.hasLabel()) {
                    if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED) sb.append("required ");
                    else if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL) sb.append("optional ");
                }
            } else {
                // proto3
                if (field.hasProto3Optional() && field.getProto3Optional()) {
                    sb.append("optional ");
                } else if (field.hasLabel() && field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL && !field.hasOneofIndex()) {
                    // Standard proto3 optional (implicit) - do nothing
                }
            }
            
            if (field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
                sb.append("repeated ");
            }

            sb.append(getTypeName(field)).append(" ").append(field.getName()).append(" = ").append(field.getNumber()).append(";\n");
        }

        private static String getTypeName(DescriptorProtos.FieldDescriptorProto field) {
            if (field.hasTypeName()) {
                return field.getTypeName();
            }
            switch (field.getType()) {
                case TYPE_DOUBLE: return "double";
                case TYPE_FLOAT: return "float";
                case TYPE_INT64: return "int64";
                case TYPE_UINT64: return "uint64";
                case TYPE_INT32: return "int32";
                case TYPE_FIXED64: return "fixed64";
                case TYPE_FIXED32: return "fixed32";
                case TYPE_BOOL: return "bool";
                case TYPE_STRING: return "string";
                case TYPE_GROUP: return "group";
                case TYPE_MESSAGE: return field.getTypeName();
                case TYPE_BYTES: return "bytes";
                case TYPE_UINT32: return "uint32";
                case TYPE_ENUM: return field.getTypeName();
                case TYPE_SFIXED32: return "sfixed32";
                case TYPE_SFIXED64: return "sfixed64";
                case TYPE_SINT32: return "sint32";
                case TYPE_SINT64: return "sint64";
                default: return "unknown";
            }
        }

        private static void printService(StringBuilder sb, DescriptorProtos.ServiceDescriptorProto svc) {
            sb.append("service ").append(svc.getName()).append(" {\n");
            for (DescriptorProtos.MethodDescriptorProto method : svc.getMethodList()) {
                sb.append("  rpc ").append(method.getName())
                  .append(" (").append(method.getInputType()).append(") returns (")
                  .append(method.getOutputType()).append(");\n");
            }
            sb.append("}\n\n");
        }
    }
}

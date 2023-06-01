package narif.poc.grpc101.greetings;

import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GreetingsClient {

    private static final Logger logger = Logger.getLogger(GreetingsClient.class.getName());

    private final GreeterRPCGrpc.GreeterRPCBlockingStub blockingStub;
    private final GreeterRPCGrpc.GreeterRPCStub asyncStub;

    public GreetingsClient(Channel channel){
        blockingStub = GreeterRPCGrpc.newBlockingStub(channel);
        asyncStub = GreeterRPCGrpc.newStub(channel);
    }

    public void greetSingleUser(String username){
        GreetingRequest greetingRequest = GreetingRequest.newBuilder()
                .setUsername(username).build();
        GreetingResponse greetingResponse = null;
        try {
            greetingResponse = blockingStub.greetUser(greetingRequest);
        }catch (StatusRuntimeException e){
            logger.warning("RPC Failed: "+e.getStatus());
        }
        logger.info("Greeting from RPC: "+greetingResponse.getMessage());
    }

    public void greetUserMultipleTimes(){
        final var najeeb = GreetingRequest.newBuilder().setUsername("Najeeb").build();
        final var responseIterator = blockingStub.greetUserStream(najeeb);
        while (responseIterator.hasNext()){
            logger.info(responseIterator.next().getMessage());
        }
    }

    public void greetAllUsersAtOnce(List<String> usernames) throws InterruptedException {
        final CountDownLatch cl = new CountDownLatch(1);
        StreamObserver<GreetingResponse> responseStreamObserver = createResponseStreamObserver(cl);
        final StreamObserver<GreetingRequest> greetingRequestStreamObserver = asyncStub.greetUsersAtOnce(responseStreamObserver);
        try{
            for (String username: usernames) {
                final var build = GreetingRequest.newBuilder().setUsername(username).build();
                greetingRequestStreamObserver.onNext(build);
            }
            if(cl.getCount()==0){
                return;
            }
        }catch (StatusRuntimeException e){
            logger.warning("RPC Failed"+e.getStatus());
            throw new RuntimeException(e);
        } catch (Exception e){
            greetingRequestStreamObserver.onError(e);
            throw e;
        }
        greetingRequestStreamObserver.onCompleted();
        cl.await();
    }

    private static StreamObserver<GreetingResponse> createResponseStreamObserver(CountDownLatch cl) {
        return new StreamObserver<GreetingResponse>() {
            @Override
            public void onNext(GreetingResponse value) {
                logger.info("GREETING MESSAGE FROM RPC: " + value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                logger.log(Level.WARNING, "Greeting Failed from RPC: {0}", status);
                cl.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("RPC Server done with its response.");
                cl.countDown();
            }
        };
    }

    public static void main(String[] args) throws InterruptedException {
        final String host = "localhost";
        final String port = "50051";
        final var target = host + ":" + port;
        final ManagedChannel managedChannel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .build();
        try{
            final var greetingsClient = new GreetingsClient(managedChannel);
            greetingsClient.greetSingleUser("Najeeb");
            greetingsClient.greetUserMultipleTimes();
            greetingsClient.greetAllUsersAtOnce(List.of("Najeeb", "Arif",
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill", 
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill",
                    "John", "Doe", "Jack", "Jill",
                    "Tony", "Stark", "Bucky", "Barnes"));
        }finally {
            managedChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

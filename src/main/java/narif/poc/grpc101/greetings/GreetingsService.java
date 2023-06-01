package narif.poc.grpc101.greetings;

import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GreetingsService extends GreeterRPCGrpc.GreeterRPCImplBase {

    @Override
    public void greetUser(GreetingRequest greetingRequest, StreamObserver<GreetingResponse> greetingResponseStreamObserver){
        String userName = greetingRequest.getUsername();
        String message = "Hello Mr. "+userName+". This is greeting message.";
        final var greetingResponse = GreetingResponse.newBuilder().setMessage(message).build();
        greetingResponseStreamObserver.onNext(greetingResponse);
        greetingResponseStreamObserver.onCompleted();
    }

    @Override
    public void greetUserStream(GreetingRequest greetingRequest, StreamObserver<GreetingResponse> greetingResponseStreamObserver){
        for (int i = 0; i < 100; i++) {
            String message = "Hello Mr. "+greetingRequest.getUsername()+". This is a greeting message. Count: "+(i+1);
            final var build = GreetingResponse.newBuilder().setMessage(message).build();
            greetingResponseStreamObserver.onNext(build);
        }
        greetingResponseStreamObserver.onCompleted();
    }

    @Override
    public StreamObserver<GreetingRequest> greetUsersAtOnce(StreamObserver<GreetingResponse> greetingResponseStreamObserver){
        return new StreamObserver<GreetingRequest>() {
            List<String> users = new CopyOnWriteArrayList<>();
            @Override
            public void onNext(GreetingRequest value) {
                users.add(value.getUsername());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println(t.getMessage());
            }

            @Override
            public void onCompleted() {
                greetingResponseStreamObserver.onNext(getGreetingResponse());
                greetingResponseStreamObserver.onCompleted();
            }

            private GreetingResponse getGreetingResponse() {
                return GreetingResponse.newBuilder()
                        .setMessage("Hello " + getAllUsernames() + ". You All are welcomed to the world of gRPC.").build();
            }

            private String getAllUsernames() {
                return users.stream().map(s -> "Mr. " + s).collect(Collectors.joining(", "));
            }
        };
    }
}

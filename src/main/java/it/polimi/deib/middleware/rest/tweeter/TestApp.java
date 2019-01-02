package it.polimi.deib.middleware.rest.tweeter;

import java.net.URI;
import java.net.URISyntaxException;

public class TestApp {

    public static void main(String[] args) {
        try {
            // open websocket
            System.out.println("trying to open websocket for client side tho");
            final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(
                    new URI("ws://localhost:4242/"));
            System.out.println("is it open");
            // add listener
//            clientEndPoint.addMessageHandler(System.out::println);
            clientEndPoint.addMessageHandler(message -> System.out.println(message));
//            clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
//                public void handleMessage(String message) {
//                    System.out.println(message);
//                }
//            });

            // send message to websocket
            clientEndPoint.sendMessage("{'event':'addChannel','channel':'ok_btccny_ticker'}");

            // wait 5 seconds for messages from websocket
            Thread.sleep(5000);

        } catch (InterruptedException ex) {
            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
    }
}

# bootique-jetty11-websocket

This module provides server WebSocket functionality to your Bootique app.
It is based on Jetty WebSocket implementation and support JSR-356 compliant
API.

## Features

* Supports deploying endpoints annotated with `@ServerEndpoint` and
other JSR-356 annotations.
* Endpoints are managed via the DI container and support service injection.
* It is a user's choice whether the endpoint should be a singleton or
a new instance should be created per peer.
* Supports `WebSocketContainer` configuration via YAML.
* _Endpoints that are not using annotations and are instead subclass
`javax.websocket.Endpoint` are not yet supported._

## Example

```java
@ServerEndpoint("/e1")
public class E1 { ... }

@ServerEndpoint("/e2")
public class E2 { ... }

public MyModule implements Module {

   @Override
   public void configure(Binder b) {
       // E1 is a singleton endpoint
       b.bind(E1.class).in(Singleton.class);

       // E2 is an instance-per-peer endpoint
       b.bind(E2.class):

       // both can be registered to handle WebSocket connections
       JettyWebSocketModule.extend(b)
           .addEndpoint(E1.class)
           .addEndpoint(E2.class);
   }
}
```

Example config:

```yaml
jettywebsocket:
    asyncSendTimeout: "5s"
    maxSessionIdleTimeout: "30min"
    maxBinaryMessageBufferSize: "30kb"
    maxTextMessageBufferSize: "45kb"
```
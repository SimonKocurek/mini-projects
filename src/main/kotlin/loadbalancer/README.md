# HTTP Load Balancer

Server that distributes load to multiple downstream servers
equally using [Round-robin](https://en.wikipedia.org/wiki/Round-robin_scheduling) algorithm.

When establishing a connection to a downstream server fails, it will be timed out. 
No traffic is sent to timed out servers, giving them time to restart/recover from any potential issues.

> The implementation does not rely on any third party libraries for HTTP server, HTTP client or logging.
> These are either using very old parts of the Java standard library, or are self-made and should not be
> used in production.

```
Usage: loadbalancer [<options>] <downstream servers>...

  Distribute traffic using Round-robin algorithm equally between multiple HTTP
  downstream servers.

  Perform a simple passive healthcheck and route traffic only to healthy
  downstream servers.

Options:
  -p, --port=<int>           Port the server will be listening on. Default:
                             8000.
  --gracefulShutdownSeconds=<int>
                             After termination signal is received, the server
                             will stop receiving new requests. Before
                             forcefully terminating existing connections, it
                             will give the existing connections this specified
                             time to finish. Default: 3.
  --minThreadPoolSize=<int>  Minimum number of threads handling requests.
                             Default: 10.
  --maxThreadPoolSize=<int>  Maximum number of threads handling requests.
                             Default: 10_000.
  --threadPoolKeepAliveSeconds=<int>
                             Number of seconds after which idle threads will be
                             released from thread pool. Default: 60.
  --loadBalancingRecoveryTimeout=<int>
                             Number of seconds when no traffic will be sent to
                             an unhealthy downstream server. Downstream server
                             is marked as unhealthy if establishing a
                             connection to it fails. Default: 60.
  -h, --help                 Show this message and exit

Arguments:
  <downstream servers>  URLs in format: <scheme>://<authority><path> without
                        query params or fragment of downstream servers where
                        traffic will be forwarded.
```

## Possible improvements

- Implementing sticky sessions.
- Adding HTTPS support.
- Adding HTTP 2, HTTP 3 and Websockets support.
- Better resource utilization by using utilizing virtual threads or an asynchronous server.
- Ability to dynamically change the list of downstream servers without the need to restart the load balancer.

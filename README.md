# JGitEnvironmentRepository#fetch indefinitely hangs during network partitions
If a network partition occurs during `JGitEnvironmentRepository#fetch`, config server hangs indefinitely since the socket timeouts are not set.

### To recreate the issue

```
git clone https://github.com/fahimfarookme/config-server-stuck-thread-issue.git
cd config-server-stuck-thread-issue/scripts
./nw-partition-during-fetch.sh 7000 git issue
```
You will notice that no response is received after `Simulating a network partition for dns github.com`


### To test the fix

```
git clone https://github.com/fahimfarookme/config-server-stuck-thread-issue.git
cd config-server-stuck-thread-issue/scripts
./nw-partition-during-fetch.sh 7000 git fix
```
You will continue to receive responses after `Simulating a network partition for dns github.com` due to timeouts set. Also the following on the logs;

```
2018-05-30 20:14:50.966  WARN 11782 --- [io-7000-exec-11] .c.s.e.MultipleJGitEnvironmentRepository : Could not fetch remote for master remote: https://github.com/fahimfarookme/config-repo
2018-05-30 20:14:50.970 DEBUG 11782 --- [io-7000-exec-11] .c.s.e.MultipleJGitEnvironmentRepository : Stacktrace for: Could not fetch remote for master remote: https://github.com/fahimfarookme/config-repo

org.eclipse.jgit.api.errors.TransportException: https://github.com/fahimfarookme/config-repo: cannot open git-upload-pack
	at org.eclipse.jgit.api.FetchCommand.call(FetchCommand.java:252) ~[org.eclipse.jgit-4.11.0.201803080745-r.jar!/:4.11.0.201803080745-r]
	at org.springframework.cloud.config.server.environment.JGitEnvironmentRepository.fetch(JGitEnvironmentRepository.java:449) [spring-cloud-config-server-2.0.0.BUILD-SNAPSHOT.jar!/:2.0.0.BUILD-SNAPSHOT]
....
Caused by: java.net.SocketTimeoutException: connect timed out
	at java.net.PlainSocketImpl.socketConnect(Native Method) ~[na:1.8.0_171]
	at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:350) ~[na:1.8.0_171]
	at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:206) ~[na:1.8.0_171]
	at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:188) ~[na:1.8.0_171]
```

### Analysis
- JGit by default uses `org.eclipse.jgit.transport.http.JDKHttpConnectionFactory` and [this line](https://github.com/spring-cloud/spring-cloud-config/blob/master/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/JGitEnvironmentRepository.java#L596) sets socket the timeout.

- However [this PR](https://github.com/spring-cloud/spring-cloud-config/pull/1002)  introduced [HttpClientConfigurableHttpConnectionFactory](https://github.com/pivotal-dylan-roberts/spring-cloud-config/blob/28dc13ea08222cbdd5c55dfd60d4d28e52d08b85/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/HttpClientConfigurableHttpConnectionFactory.java) which [provides](https://github.com/pivotal-dylan-roberts/spring-cloud-config/blob/28dc13ea08222cbdd5c55dfd60d4d28e52d08b85/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/HttpClientConfigurableHttpConnectionFactory.java#L56) a custom [Apache HttpClient](https://github.com/pivotal-dylan-roberts/spring-cloud-config/blob/28dc13ea08222cbdd5c55dfd60d4d28e52d08b85/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/support/HttpClientSupport.java), however it does not set the socket timeouts. Therefore the timeout set at [this line](https://github.com/spring-cloud/spring-cloud-config/blob/master/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/JGitEnvironmentRepository.java#L596) is not effective/ used.
   - i.e. JGit [sets socket timeouts](https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit.http.apache/src/org/eclipse/jgit/transport/http/apache/HttpClientConnection.java#L145) -- from what we set [here](https://github.com/spring-cloud/spring-cloud-config/blob/master/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/JGitEnvironmentRepository.java#L596) -- if we [do not provide](https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit.http.apache/src/org/eclipse/jgit/transport/http/apache/HttpClientConnection.java#L132) any custom Apache HttpClient.
   
- Since `JGitEnvironmentRepository#getLocations` is a `synchronized` method, subsequent /refresh and /health requests BLOCKED indefinitely.

- Thread dumps [attached](https://github.com/spring-cloud/spring-cloud-config/files/2055152/thread-dumps.zip).

- Once the timeouts are set in the Apache Client [here](https://github.com/fahimfarookme/config-server-stuck-thread-issue/blob/master/config-server-fix/src/main/java/me/fahimfarook/config/server/TimeoutHttpConnectionFactory.java#L72), setting it on the `TransportCommand` [here](https://github.com/spring-cloud/spring-cloud-config/blob/master/spring-cloud-config-server/src/main/java/org/springframework/cloud/config/server/environment/JGitEnvironmentRepository.java#L596) is unnecessary.

- JGit sets the same timeout value for [both connect and read](https://github.com/eclipse/jgit/blob/master/org.eclipse.jgit/src/org/eclipse/jgit/transport/TransportHttp.java#L838). However, suggesting to have different timeouts for connect and read since we already inject JGit with a custom Apache HttpClient.





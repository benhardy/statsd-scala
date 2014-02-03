statsd-scala
============

A statsd client for scala, ships statistics to a UDP socket

See https://github.com/etsy/statsd

# Basic usage

Create a Config with a new UdpMessageSender pointing to the statsd host.
Create a new Statsd object with that config. Then reuse that to record stats

    import net.bhardy.statsd._

    val statsd = Statsd(Config(new UdpMessageSender("localhost", serverPort)))
    statsd("chickens").increment

    statsd.namespace("reptiles")("lizards").count(5)

    statsd.namespace("mammals", "primates")("chimps").increment

    // namespace is curried for reuse

    val fishStats: String=>StatOperations = statsd.namespace("vertebrates", "fishes")
    fishStats("mullet").increment
    fishStats("bream").count(5)


# Error handling

If for some reason we can't send the stat to the server, an error will be
logged to the error handler function in Config. By default this dumps to 
stderr.

# TODOS

* Sampling
* Gauges a formatted doubles

# BUGS

File 'em on github if you see them

# LICENSE

MIT


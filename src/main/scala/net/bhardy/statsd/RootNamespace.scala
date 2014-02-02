package net.bhardy.statsd

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.regex.Pattern

/**
 * Operations that can be performed on a named statistic
 */
sealed trait StatOperations {
  /**
   * Sends an increment (count = 1) for the given stat to the statsd server.
   *
   * @see #count
   */
  def increment: Unit

  /**
   * Sends a decrement (count = -1) for the given stat to the statsd server.
   *
   * @see #count
   */
  def decrement: Unit

  /**
   * Sends an arbitrary count for the given stat to the statsd server.
   */
  def count(count: Int): Unit

  /**
   * Sends an arbitary gauge value for the given stat to the statsd server.
   *
   * @example Report the current user count:
   *          statsd.gauge("user.count", User.count)
   */
  def gauge(count: Long): Unit

  /**
   * Sends a timing (in ms) for the given stat to the statsd server. The
   * sampleRate determines what percentage of the time this report is sent. The
   * statsd server then uses the sampleRate to correctly track the average
   * timing for the stat.
   *
   * @param millis timing in milliseconds
   */
  def timing(millis: Long): Unit

  /** Reports execution time of the provided block using {#timing}.
    *
    * @param block The operation to be timed
    * @see #timing
    * @example Report the time (in ms) taken to activate an account
    *          $statsd.time('account.activate') { @account.activate! }
    */
  def time[T](block: => T): T
}

/**
 * Statsd config.
 * @param messageSender - usually takes a UdpReceiver (or a mock for testing)
 * @param errorLogger - if sending messages to receiver fails, an error is logged here (defaults to stderr)
 */
case class Config(messageSender: String => Unit,
                  errorLogger: String => Unit = System.err.println(_))

object Config {
  val DEFAULT = Config(
    messageSender = new UdpMessageSender("localhost")
  )
}
/**
 * somewhere to send messages via UDP
 * <p>
 * Only does host lookup and socket creation if a message gets sent with
 * apply (lazy fields).
 *
 * @param host - the host to send messages to
 * @param port - what port to send the messages on
 */
final class UdpMessageSender(host: String,
                             port: Int = 8125) extends (String => Unit) {

  private lazy val hostAddress = InetAddress.getByName(host)
  private lazy val socket = new DatagramSocket()

  def apply(message: String): Unit = {
    val messageBytes = message.getBytes
    val packet = new DatagramPacket(messageBytes, messageBytes.length, hostAddress, port)
    socket.send(packet)
  }
}

/**
 * Statsd provides methods for the user to build a fully-qualified stat
 * name from an optional series of namespaces and a simple stat name. The
 * resulting object of type StatOperations can be used to perform statistical
 * operations for the named stat.
 * <p>
 * Measurement messages are sent to the UdpReceiver specified in the Config's
 * "receiver" field, so it's important to set that correctly.
 * <p>
 * Namespaces and stat names are always sanitized of special character so as
 * not to provide bad data to Statsd.
 *
 * @param config - Statsd configuration @see Config
 */
case class Statsd(config: Config = Config.DEFAULT) {

  /**
   * Provide Stat operations for the named stat with namespaces prepended
   * @param rootNamespace - the first namespace
   * @param subNamespaces - optional sub-namespaces (varargs)
   * @param statName - the thing to measure
   * @return measurement operations we can perform on the stat
   */
  def namespace(rootNamespace: String, subNamespaces: String*)(statName: String): StatOperations = {
    val buf = new StringBuilder(sanitized(rootNamespace))
    subNamespaces.foreach {
      subSpace =>
        buf.append(".").append(sanitized(subSpace))
    }
    val qualifiedName = buf.append(".").append(sanitized(statName)).toString
    operationsFor(qualifiedName)
  }

  /**
   * Provide Stat operations for the named stat without any namespace
   * @param statName - the thing to measure
   * @return measurement operations we can perform on the stat
   */
  def apply(statName: String): StatOperations = {
    val unqualifiedName = sanitized(statName)
    operationsFor(unqualifiedName)
  }

  /**
   * Sanitize stat names so we don't confuse statsd.
   */
  def sanitized(stat: String) = {
    Statsd.RESERVED_CHARS_PATTERN matcher stat replaceAll "_"
  }

  private def operationsFor(fullStatName: String) = new StatOperations {

    def increment = count(1)

    def decrement = count(-1)

    def count(count: Int) = send(count.toString, "c")

    def gauge(value: Long) = send(value.toString, "g")

    def timing(millis: Long) = send(millis.toString, "ms")

    def time[T](block: => T): T = {
      val start = System.currentTimeMillis
      val result: T = block
      val end = System.currentTimeMillis
      timing(end - start)
      result
    }

    private def send(delta: String, typeInfo: String): Unit = {
      val message = fullStatName + ":" + delta + "|" + typeInfo
      try {
        config.messageSender(message)
      } catch {
        case boom: IOException => config.errorLogger("Statsd: " + boom.getClass + " " + boom.getMessage)
      }
    }
  }
}

object Statsd {
  /** characters that will be replaced with _ in stat names */
  private val RESERVED_CHARS_PATTERN = Pattern compile "[:|@\\.]+"
}
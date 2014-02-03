package net.bhardy.statsd

import org.scalatest.{Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import java.io.IOException
import org.mockito.ArgumentCaptor

/**
  */
class StatsdSpec extends FunSpec with Matchers with MockitoSugar {
  describe("basic operations") {
    describe("increment") {
      it("sends a message indicating a bump of 1 for the item count to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))

        statsd("item").increment

        verify(messages).apply("item:1|c")
        verify(errors, never()).apply(anyString())
      }
    }
    describe("decrement") {
      it("sends a message indicating a bump of -1 for the item count to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))

        statsd("item").decrement

        verify(messages).apply("item:-1|c")
        verify(errors, never()).apply(anyString())
      }
    }
    describe("time") {
      it("sends a message indicating a block execution duration to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))
        val argument = ArgumentCaptor.forClass(classOf[String])

        statsd("sleeping").time {
          Thread.sleep(1)
        }
        verify(messages).apply(argument.capture())
        argument.getValue should fullyMatch regex "^sleeping:[0-9]+[|]ms$"
        verify(errors, never()).apply(anyString())
      }
    }
    describe("timing") {
      it("sends a message indicating a duration to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))

        statsd("sleeping").timing(5)

        verify(messages).apply("sleeping:5|ms")
      }
    }
  }
  describe("namespaced forms") {
    it("allows a one-time application of a series of namespace strings and a stat") {
      val messages = mock[String => Unit]
      val errors = mock[String => Unit]
      val statsd = Statsd(Config(messages, errors))

      statsd.namespace("businesses", "farm", "poultry")("roosters").increment

      verify(messages).apply("businesses.farm.poultry.roosters:1|c")
    }
  }

  describe("sanitize") {
    it("ensures that namespace and stat names are cleaned of special chars") {
      val messages = mock[String => Unit]
      val errors = mock[String => Unit]
      val statsd = Statsd(Config(messages, errors))

      statsd.namespace("my.place")("your.info").increment

      verify(messages).apply("my_place.your_info:1|c")
      verify(errors, never()).apply(anyString())
    }
    it("ensures that namespace and stat names replaces multiple instances of special chars with single underscore") {
      val messages = mock[String => Unit]
      val errors = mock[String => Unit]
      val statsd = Statsd(Config(messages, errors))

      statsd.namespace("Package::Class")("birds@|chickens").increment

      verify(messages).apply("Package_Class.birds_chickens:1|c")
      verify(errors, never()).apply(anyString())
    }
  }

  describe("namespaces operations") {
    describe("increment") {
      it("sends a message indicating a bump of 1 for the item count to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))


        statsd.namespace("interesting", "things")("item").increment

        verify(messages).apply("interesting.things.item:1|c")
        verify(errors, never()).apply(anyString())
      }
    }
    describe("curried namespace reuse") {
      it("sends a message indicating a bump of 1 for the item count to the statsd server") {
        val messages = mock[String => Unit]
        val errors = mock[String => Unit]
        val statsd = Statsd(Config(messages, errors))

        val thingsStats: String=>StatOperations = statsd.namespace("interesting", "things")
        thingsStats("item").increment
        thingsStats("other").count(5)

        verify(messages).apply("interesting.things.item:1|c")
        verify(messages).apply("interesting.things.other:5|c")
        verify(errors, never()).apply(anyString())
      }
    }
  }

  describe("error handling") {
    it("sends a message to the error log on IO Exception") {
      def failingConsumer(message: String): Unit = throw new IOException("something went wrong")
      val errors = mock[String => Unit]
      val statsd = Statsd(Config(failingConsumer, errors))

      statsd("glitches").increment

      verify(errors).apply("Statsd: class java.io.IOException something went wrong")
    }
  }

}

package net.bhardy.statsd

import org.scalatest.{Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import java.net.{DatagramPacket, DatagramSocket}

/**
 */
class FunctionalSpec extends FunSpec with Matchers with MockitoSugar {

  describe("talking to a UdpSocket") {
    val serverPort = 5555
    val serverSocket = new DatagramSocket(serverPort)

    val statsd = Statsd(Config(new UdpMessageSender("localhost", serverPort)))
    statsd("chicken").increment

    val byteReserve = new Array[Byte](200)
    val incoming = new DatagramPacket(byteReserve, byteReserve.length)
    serverSocket.receive(incoming)
    val inBytes = incoming.getData()
    val result = new String(inBytes, 0, incoming.getLength)
    result should be === "chicken:1|c"
    serverSocket.close()
  }
}

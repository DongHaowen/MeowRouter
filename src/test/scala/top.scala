import scala.util.control.Breaks._
import chisel3._
import chisel3.util._
import chisel3.iotesters.{Driver, TesterOptionsManager, PeekPokeTester}
import _root_.top.Top

class TopWrap extends Module {
  val io = IO(new Bundle {
    // Clk and rst are implicit
    val rx_clk = Input(Clock())
    val tx_clk = Input(Clock())

    val rx_tdata = Input(UInt(8.W))
    val rx_tvalid = Input(Bool())
    val rx_tlast = Input(Bool())
    // Ignore user

    val tx_tdata = Output(UInt(8.W))
    val tx_tvalid = Output(Bool())
    val tx_tlast = Output(Bool())
    val tx_tready = Input(Bool())
    val tx_tuser = Output(Bool())
  })

  val top = Module(new Top())
  top.io <> io
  top.io.rx_clk := this.clock
  top.io.tx_clk := this.clock
}

class TopTest(c: TopWrap) extends PeekPokeTester(c) {
  val packetsStr = List(
    // ARP request, local IP for port 1 is 10.0.1.1, expected to emit an ARP Response
    "FF FF FF FF FF FF 00 12 17 CD DD 12 81 00 00 01 08 06 00 01 08 00 06 04 00 01 00 12 17 CD DD 12 0A 00 01 02 00 00 00 00 00 00 0A 00 01 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 61 84 A4 4D",
    // IP ping 10.0.1.2 -> 10.0.2.3 (With incorrect FCS). This should route to 10.0.2.2, and thus cause a ARP miss. Expected to emit four ARP Request
    "00 00 00 00 00 01 00 12 17 CD DD 12 81 00 00 01 08 00 45 00 00 54 32 22 00 00 40 01 34 85 0A 00 01 02 0A 00 02 03 08 00 80 D5 7B 43 00 03 5C BF EC 2E 00 03 C7 EF 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F 30 31 32 33 34 35 36 37 26 7F 6C E4",
    // ARP response from 10.0.3.1
    "00 00 00 00 00 03 06 05 04 03 02 01 81 00 00 03 08 06 00 01 08 00 06 04 00 02 06 05 04 03 02 01 0A 00 03 02 00 00 00 00 00 03 0A 00 03 01 01 00 00 00 00 00 00 00 00 00 00 00 00 00 61 84 A4 4D",
    // IP ping 10.0.1.2 -> 10.0.3.6 (With incorrect FCS). This should route to 10.0.3.2
    "00 00 00 00 00 01 00 12 17 CD DD 12 81 00 00 01 08 00 45 00 00 54 32 22 00 00 40 01 34 85 0A 00 01 02 0A 00 03 06 08 00 80 D5 7B 43 00 03 5C BF EC 2E 00 03 C7 EF 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F 30 31 32 33 34 35 36 37 26 7F 6C E4",
    // Very large IP packet (Fake size), should drop
    "00 00 00 00 00 01 00 12 17 CD DD 12 81 00 00 01 08 00 45 00 FF FF 32 22 00 00 40 01 34 85 0A 00 01 02 0A 00 03 06 08 00 80 D5 7B 43 00 03 5C BF EC 2E 00 03 C7 EF 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F 30 31 32 33 34 35 36 37 26 7F 6C E4"
  )

  val expectedPackets = List(
    List("00 12 17 CD DD 12 00 00 00 00 00 01 81 00 00 01 08 06 00 01 08 00 06 04 00 02 00 00 00 00 00 01 0A 00 01 01 00 12 17 CD DD 12 0A 00 01 02"),
    List(
      "FF FF FF FF FF FF 00 00 00 00 00 01 81 00 00 01 08 06 00 01 08 00 06 04 00 01 00 00 00 00 00 01 0A 00 01 01 00 00 00 00 00 00 0A 00 02 02",
      "FF FF FF FF FF FF 00 00 00 00 00 02 81 00 00 02 08 06 00 01 08 00 06 04 00 01 00 00 00 00 00 02 0A 00 02 01 00 00 00 00 00 00 0A 00 02 02",
      "FF FF FF FF FF FF 00 00 00 00 00 03 81 00 00 03 08 06 00 01 08 00 06 04 00 01 00 00 00 00 00 03 0A 00 03 01 00 00 00 00 00 00 0A 00 02 02",
      "FF FF FF FF FF FF 00 00 00 00 00 04 81 00 00 04 08 06 00 01 08 00 06 04 00 01 00 00 00 00 00 04 0A 00 04 01 00 00 00 00 00 00 0A 00 02 02"
    ),
    List(),
    List("06 05 04 03 02 01 00 00 00 00 00 03 81 00 00 03 08 00 45 00 00 54 32 22 00 00 40 01 34 85 0A 00 01 02 0A 00 03 06 08 00 80 D5 7B 43 00 03 5C BF EC 2E 00 03 C7 EF 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F 30 31 32 33 34 35 36 37 26 7F 6C E4"),
    List()
  )

  var recvBuf = Vector[Int]()

  def peekData(pid: Int, rid: Int): Int = {
    val valid = peek(c.io.tx_tvalid) == BigInt(1)
    val last = peek(c.io.tx_tlast) == BigInt(1)
    val data = peek(c.io.tx_tdata).toInt
    val user = peek(c.io.tx_tuser) == BigInt(1)
    
    if (valid) {
      recvBuf = recvBuf :+ data
    }

    if (user) {
      println("User Package")
    }
    
    if (last && recvBuf.length > 0) {
      val recivedPacketStr = recvBuf.map(x => "%02x".format(x).toUpperCase).mkString(" ")
      println("recived packet: " + recivedPacketStr)
      assert(recivedPacketStr == expectedPackets(pid)(rid))
      recvBuf = Vector[Int]()
      rid + 1
    } else {
      rid
    }
  }

  for (pid <- 0 until packetsStr.length) {
    var recivedPacketCnt = 0
    val p = packetsStr(pid)
    val sendBuf = p.split(" ").map(x => Integer.parseInt(x, 16))
    println("sending packet: " + p)
    for (i <- 0 until sendBuf.length) {
      poke(c.io.rx_tdata, sendBuf(i))
      poke(c.io.rx_tvalid, true)
      poke(c.io.rx_tlast, i == sendBuf.length - 1)

      poke(c.io.tx_tready, true)
      step(1)
      recivedPacketCnt = peekData(pid, recivedPacketCnt)
    }
    breakable {
      for (i <- 0 until 1000) {
        poke(c.io.rx_tvalid, false)
        poke(c.io.tx_tready, true)

        step(1)
        recivedPacketCnt = peekData(pid, recivedPacketCnt)
        //println("recivedPacketCnt: " + recivedPacketCnt)
        if (recivedPacketCnt == expectedPackets(pid).length && expectedPackets(pid).length != 0) {
          break
        }
      }
    }
  }
}

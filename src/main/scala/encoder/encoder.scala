package encoder

import chisel3._
import chisel3.util._
import data._
import _root_.util.AsyncWriter
import _root_.util.AsyncReader

class EncoderUnit extends Bundle {
  val data = UInt(8.W)
  val last = Bool()
}

class Encoder(PORT_COUNT: Int) extends Module {
  val io = IO(new Bundle{
    val input = Input(new Packet(PORT_COUNT))
    val status = Input(UInt())

    val stall = Output(Bool())
    val pause = Input(Bool())

    val writer = Flipped(new AsyncWriter(new EncoderUnit))
    val ipReader = Flipped(new AsyncReader(new EncoderUnit))
  })

  val writing = RegInit(false.B)
  val cnt = RegInit(0.U)

  val sIDLE :: sETH :: sARP :: sIP :: sIPPIPE :: Nil = Enum(5)
  val state = RegInit(sIDLE)

  val sending = Reg(new Packet(PORT_COUNT))
  val header = sending.eth.asVec
  val arpView = sending.arp.asUInt.asTypeOf(Vec(28, UInt(8.W)))
  val ipView = sending.ip.asUInt.asTypeOf(Vec(20, UInt(8.W)))

  io.ipReader.clk := this.clock
  io.ipReader.en := false.B

  io.writer.data.last := false.B
  io.writer.data.data := 0.asUInt.asTypeOf(io.writer.data.data)
  io.writer.en := false.B
  io.writer.clk := this.clock

  switch(state) {
    is(sIDLE) {
      when(!io.pause && io.status === Status.normal) {
        state := sETH
        sending := io.input
        cnt := 17.U
      }
    }

    is(sETH) {
      // Sending ETH packet
      io.writer.data.data := header(cnt)
      io.writer.en := true.B

      when(!io.writer.full) {
        when(cnt > 0.U) {
          cnt := cnt - 1.U
        } .elsewhen(sending.eth.pactype === PacType.arp) {
          // Is ARP
          state := sARP
          cnt := 27.U
        } .otherwise {
          // Is IP
          state := sIP
          cnt := 19.U
        }
      }
    }

    is(sARP) {
      io.writer.data.data := arpView(cnt)
      io.writer.data.last := cnt === 0.U
      io.writer.en := true.B

      when(!io.writer.full) {
        when(cnt > 0.U) {
          cnt := cnt - 1.U
        } .otherwise {
          state := sIDLE
        }
      }
    }

    is(sIP) {
      io.writer.data.data := ipView(cnt)
      io.writer.data.last := false.B
      io.writer.en := true.B

      when(!io.writer.full) {
        when(cnt > 0.U) {
          cnt := cnt - 1.U
        } .otherwise {
          state := sIPPIPE
        }
      }
    }

    is(sIPPIPE) {
      io.writer.data := io.ipReader.data
      val transfer = (!io.ipReader.empty) && (!io.writer.full)
      io.writer.en := transfer
      io.ipReader.en := transfer

      when(io.ipReader.data.last && transfer) {
        state := sIDLE
      }
    }
  }

  io.stall := state =/= sIDLE
}
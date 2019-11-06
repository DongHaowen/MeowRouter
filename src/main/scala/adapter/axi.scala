package adapter

import chisel3._

trait HasAXIReader {
  val ID_WIDTH: Int
  val ADDR_WIDTH: Int
  val DATA_WIDTH: Int

  val ARID = Output(UInt(ID_WIDTH.W))
  val ARADDR = Output(UInt(ADDR_WIDTH.W))
  val ARLEN = Output(UInt(8.W))
  val ARSIZE = Output(UInt(3.W))
  val ARBURST = Output(UInt(2.W))
  val ARCACHE = Output(UInt(4.W))
  val ARPROT = Output(UInt(3.W))
  val ARQOS = Output(UInt(3.W))
  val ARREGION = Output(UInt(4.W))
  val ARVALID = Output(Bool())
  val ARREADY = Input(Bool())

  val RID = Input(UInt(ID_WIDTH.W))
  val RDATA = Input(UInt(DATA_WIDTH.W))
  val RRESP = Input(UInt(2.W))
  val RLAST = Input(Bool())
  val RVALID = Input(Bool())
  val RREADY = Output(Bool())
}

trait HasAXIWriter {
  val ID_WIDTH: Int
  val ADDR_WIDTH: Int
  val DATA_WIDTH: Int

  val AWID = Output(UInt(ID_WIDTH.W))
  val AWADDR = Output(UInt(ADDR_WIDTH.W))
  val AWLEN = Output(UInt(8.W))
  val AWSIZE = Output(UInt(3.W))
  val AWBURST = Output(UInt(2.W))
  // AXI4 removes AWLOCK
  val AWCACHE = Output(UInt(4.W))
  val AWPROT = Output(UInt(3.W))
  val AWQOS = Output(UInt(3.W))
  val AWREGION = Output(UInt(4.W))
  // We ignore user signals
  val AWVALID = Output(Bool())
  val AWREADY = Input(Bool())

  // AXI4 removes WID
  val WDATA = Output(UInt(DATA_WIDTH.W))
  val WSTRB = Output(UInt((DATA_WIDTH/8).W))
  val WLAST = Output(Bool())
  val WVALID = Output(Bool())
  val WREADY = Input(Bool())

  val BID = Input(UInt(ID_WIDTH.W))
  val BRESP = Input(UInt(2.W))
  val BVALID = Input(Bool())
  val BREADY = Output(Bool())
}

class AXI(
  val DATA_WIDTH: Int,
  val ADDR_WIDTH: Int,
  val ID_WIDTH: Int
) extends Bundle with HasAXIReader with HasAXIWriter {
  def split(): (AXIReader, AXIWriter) = {
    val r = Wire(new AXIReader(DATA_WIDTH, ADDR_WIDTH, ID_WIDTH))
    val w = Wire(new AXIWriter(DATA_WIDTH, ADDR_WIDTH, ID_WIDTH))
    r <> this
    w <> this
    (r, w)
  }
}

class AXIReader(
  val DATA_WIDTH: Int,
  val ADDR_WIDTH: Int,
  val ID_WIDTH: Int
) extends Bundle with HasAXIReader

class AXIWriter(
  val DATA_WIDTH: Int,
  val ADDR_WIDTH: Int,
  val ID_WIDTH: Int
) extends Bundle with HasAXIWriter

object AXI {
  object Constants {
    object Resp {
      val OKAY = 0
      val EXOKAY = 1
      val SLVERR = 2
      val DECERR = 3
    }

    object Size {
      val S1 = 0
      val S2 = 1
      val S4 = 2
      val S8 = 3
      val S16 = 4
      val S32 = 5
      val S64 = 6
      val S128 = 7
    }

    object Burst {
      val FIXED = 0
      val INCR = 1
      val WRAP = 2
    }
  }
}
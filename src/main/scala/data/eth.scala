package data

import chisel3._;
import chisel3.util.log2Ceil;
import chisel3.util.Cat

class Eth(val VLAN_COUNT: Int) extends Bundle {
    val dest = Output(new MACAddr)
    val sender = Output(new MACAddr)
    val pactype = Output(PacType())
    val vlan = Output(UInt(log2Ceil(VLAN_COUNT).W))

    def toBits() : UInt = Cat(
        dest.toBits,
        sender.toBits,
        0x8100.U(24.W),
        vlan.asTypeOf(UInt(8.W))
    )

    def asVec : Vec[UInt] = toBits().asTypeOf(Vec(28, UInt(8.W)))
}
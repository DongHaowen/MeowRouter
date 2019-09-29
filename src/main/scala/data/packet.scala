package data

import chisel3._

class Packet(val PORT_COUNT: Int) extends Bundle {
  val eth = new Eth(PORT_COUNT + 1) // Port = 0 should refer to localhost
  val arp = new ARP(48, 32)
  val ip = new IP
}
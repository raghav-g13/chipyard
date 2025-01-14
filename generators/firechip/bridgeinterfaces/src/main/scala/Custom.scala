// See LICENSE for license details.

package firechip.bridgeinterfaces

import chisel3._
import chisel3.util._

// Note: This file is heavily commented as it serves as a bridge walkthrough
// example in the FireSim docs

// Note: All code in this file must be isolated from target-side generators/classes/etc
// since this is also injected into the midas compiler.

// object PRV
// {
//   val SZ = 2
//   val U = 0
//   val S = 1
//   val H = 2
//   val M = 3
// }

// class MStatus extends Bundle {
//   // not truly part of mstatus, but convenient
//   val debug = Bool()
//   val cease = Bool()
//   val wfi = Bool()
//   val isa = UInt(32.W)

//   val dprv = UInt(PRV.SZ.W) // effective prv for data accesses
//   val dv = Bool() // effective v for data accesses
//   val prv = UInt(PRV.SZ.W)
//   val v = Bool()

//   val sd = Bool()
//   val zero2 = UInt(23.W)
//   val mpv = Bool()
//   val gva = Bool()
//   val mbe = Bool()
//   val sbe = Bool()
//   val sxl = UInt(2.W)
//   val uxl = UInt(2.W)
//   val sd_rv32 = Bool()
//   val zero1 = UInt(8.W)
//   val tsr = Bool()
//   val tw = Bool()
//   val tvm = Bool()
//   val mxr = Bool()
//   val sum = Bool()
//   val mprv = Bool()
//   val xs = UInt(2.W)
//   val fs = UInt(2.W)
//   val mpp = UInt(2.W)
//   val vs = UInt(2.W)
//   val spp = UInt(1.W)
//   val mpie = Bool()
//   val ube = Bool()
//   val spie = Bool()
//   val upie = Bool()
//   val mie = Bool()
//   val hie = Bool()
//   val sie = Bool()
//   val uie = Bool()
// }

case class RoCCBundleParams (
  xLen: Int,
  nPTWPorts: Int,
  nRoCCCSRs: Int,
)

class RoCCInst extends Bundle {
  val funct = Bits(7.W)
  val rs2 = Bits(5.W)
  val rs1 = Bits(5.W)
  val xd = Bool()
  val xs1 = Bool()
  val xs2 = Bool()
  val rd = Bits(5.W)
  val opcode = Bits(7.W)
}

class RoCCCmd(val params: RoCCBundleParams) extends Bundle {
  val inst = Bits(32.W)
  val rs1 = Bits(params.xLen.W)
  val rs2 = Bits(params.xLen.W)
  // val status = new MStatus
}

class RoCCResp(val params: RoCCBundleParams) extends Bundle {
  val rd = Bits(5.W)
  val data = Bits(params.xLen.W)
  // val status = new MStatus
}

class RoCCBridgeCoreIO(val params: RoCCBundleParams) extends Bundle {
  val rocccmd = Flipped(Decoupled(new RoCCCmd(params)))
  val roccresp = Decoupled(new RoCCResp(params))
}

class CustomBridgeTargetIO(val params: RoCCBundleParams) extends Bundle {
  val clock = Input(Clock())
  // TODO: IS THIS RIGHT? SHOULDN"T BE FLIPPED
  val roccio = new RoCCBridgeCoreIO(params)
  // Note this reset is optional and used only to reset target-state modeled
  // in the bridge. This reset is just like any other Bool included in your target
  // interface, simply appears as another Bool in the input token.
  val reset = Input(Bool())
}

// Out bridge module constructor argument. This captures all of the extra
// metadata we'd like to pass to the host-side BridgeModule. Note, we need to
// use a single case class to do so, even if it is simply to wrap a primitive
// type, as is the case for the div Int.
case class CustomKey(val params: RoCCBundleParams)

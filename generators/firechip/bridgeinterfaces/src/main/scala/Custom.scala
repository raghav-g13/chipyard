// See LICENSE for license details.

package firechip.bridgeinterfaces

import chisel3._

// Note: This file is heavily commented as it serves as a bridge walkthrough
// example in the FireSim docs

// Note: All code in this file must be isolated from target-side generators/classes/etc
// since this is also injected into the midas compiler.

class CustomIOIn(val w: Int) extends Bundle {
  val x = Input(UInt(w.W))
  val y = Input(UInt(w.W))
}

class CustomInToken(val w: Int) extends Bundle {
  val x = UInt(w.W)
  val y = UInt(w.W)
}

class CustomOutToken(val w: Int) extends Bundle {
  val gcd = UInt(w.W)
}

class CustomIOOut(val w: Int) extends Bundle {
  val gcd = Output(UInt(w.W))
}

class CustomIO(val w: Int) extends Bundle {
  val input_ready = Output(Bool())
  val input_valid = Input(Bool())
  val in = new CustomIOIn(w)
  val out = new CustomIOOut(w)
  val output_ready = Input(Bool())
  val output_valid = Output(Bool())
  val busy = Output(Bool())
}

class CustomBridgeTargetIO(val w: Int) extends Bundle {
  val clock = Input(Clock())
  // TODO: IS THIS RIGHT? SHOULDN"T BE FLIPPED
  val gcdio = new CustomIO(w)
  // Note this reset is optional and used only to reset target-state modeled
  // in the bridge. This reset is just like any other Bool included in your target
  // interface, simply appears as another Bool in the input token.
  val reset = Input(Bool())
}

// Out bridge module constructor argument. This captures all of the extra
// metadata we'd like to pass to the host-side BridgeModule. Note, we need to
// use a single case class to do so, even if it is simply to wrap a primitive
// type, as is the case for the div Int.
case class CustomKey(w: Int)

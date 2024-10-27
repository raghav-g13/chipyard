// See LICENSE for license details

package chipyard.example

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters

import firesim.lib.bridgeutils._

import firechip.bridgeinterfaces._

// Note: This file is heavily commented as it serves as a bridge walkthrough
// example in the FireSim docs

// DOC include start: UART Bridge Target-Side Module
class CustomBridge(w: Int)(implicit p: Parameters) extends BlackBox
    with Bridge[HostPortIO[CustomBridgeTargetIO]] {
  // Module portion corresponding to this bridge
  val moduleName = "firechip.goldengateimplementations.CustomBridgeModule"
  
  // Since we're extending BlackBox this is the port will connect to in our target's RTL
  val io = IO(new CustomBridgeTargetIO(w))
  // Implement the bridgeIO member of Bridge using HostPort. This indicates that
  // we want to divide io, into a bidirectional token stream with the input
  // token corresponding to all of the inputs of this BlackBox, and the output token consisting of
  // all of the outputs from the BlackBox
  val bridgeIO = HostPort(io)

  // And then implement the constructorArg member
  val constructorArg = Some(CustomKey(w))

  // Finally, and this is critical, emit the Bridge Annotations -- without
  // this, this BlackBox would appear like any other BlackBox to Golden Gate
  generateAnnotations()
}
// DOC include end: UART Bridge Target-Side Module

// DOC include start: UART Bridge Companion Object
object CustomBridge {
  def apply(clock: Clock, gcd: chipyard.example.GCDInnerIO, reset: Bool, w: Int)(implicit p: Parameters): CustomBridge = {
    val ep = Module(new CustomBridge(w))
    gcd.busy := ep.io.gcdio.busy
    gcd.gcd := ep.io.gcdio.out.gcd

    ep.io.gcdio.in.x := gcd.x
    ep.io.gcdio.in.y := gcd.y

    gcd.input_ready := ep.io.gcdio.input_ready
    ep.io.gcdio.input_valid := gcd.input_valid

    
    gcd.output_valid := ep.io.gcdio.output_valid 
    ep.io.gcdio.output_ready := gcd.output_ready
     
    ep.io.clock := clock
    ep.io.reset := reset
    ep
  }
}
// DOC include end: UART Bridge Companion Object

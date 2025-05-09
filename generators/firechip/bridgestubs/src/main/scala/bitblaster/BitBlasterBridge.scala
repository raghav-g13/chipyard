// package firechip.bridgestubs

// import chisel3._
// import chisel3.util._

// import org.chipsalliance.cde.config.Parameters

// import firesim.lib.bridgeutils._
// import firechip.bridgeinterfaces._

// class BitBlasterBridge(outParams: BitBlasterConfig, inParams: BitBlasterConfig) extends BlackBox
//     with Bridge[HostPortIO[BitBlasterBridgeIO]] {
//   val moduleName = "firechip.goldengateimplementations.BitBlasterBridgeModule"
//   val io = IO(new BitBlasterBridgeIO(outParams, inParams))
//   val bridgeIO = HostPort(io)
//   val constructorArg = None // TODO: Add as needed with key
//   generateAnnotations()
// }

// class BitBlasterBridgeDecoupled(outParams: BitBlasterConfig, inParams: BitBlasterConfig) extends BlackBox
//     with Bridge[HostPortIO[BitBlasterBridgeDecoupledIO]] {
//   val moduleName = "firechip.goldengateimplementations.BitBlasterBridgeDecoupledModule"
//   val io = IO(new BitBlasterBridgeDecoupledIO(outParams, inParams))
//   val bridgeIO = HostPort(io)
//   val constructorArg = None // TODO: Add as needed with key
//   generateAnnotations()
// }

// object BitBlasterBridge {
//   def apply(clock: Clock, reset: Bool, outParams: BitBlasterConfig, inParams: BitBlasterConfig, out: BitBlasterOut, in: BitBlasterIn)(implicit p: Parameters): BitBlasterBridge = {
//     val ep = Module(new BitBlasterBridge(outParams, inParams))
//     ep.io.out := out
//     in := ep.io.in 
//     ep.io.clock := clock
//     ep.io.reset := reset
//     ep
//   }
// }


// object BitBlasterBridgeDecoupled {
//   def apply(clock: Clock, reset: Bool, outParams: BitBlasterConfig, inParams: BitBlasterConfig, out: BitBlasterOutDecoupled, in: BitBlasterInDecoupled)(implicit p: Parameters): BitBlasterBridgeDecoupled = {
//     val ep = Module(new BitBlasterBridgeDecoupled(outParams, inParams))
//     ep.io.out <> out
//     in <> ep.io.in 
//     ep.io.clock := clock
//     ep.io.reset := reset
//     ep
//   }
// }
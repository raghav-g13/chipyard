// See LICENSE for license details

package firechip.goldengateimplementations

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.util._

import midas.widgets._
import firesim.lib.bridgeutils._

import firechip.bridgeinterfaces._


// class BitBlasterBridgeModule(key: BitBlasterBridgeKey)(implicit p: Parameters)
//     extends BridgeModule[HostPortIO[BitBlasterBridgeDecoupledIO]]()(p)
//     with StreamToHostCPU with StreamFromHostCPU {

//   def splitBitsIntoChunks(input: UInt, tokenWidth: Int): Vec[UInt] = {
//     val numChunks = (input.getWidth + tokenWidth - 1) / tokenWidth
//     VecInit((0 until numChunks).map { i =>
//       val highBit = ((i + 1) * tokenWidth - 1).min(input.getWidth - 1)
//       val lowBit = i * tokenWidth
//       input(highBit, lowBit)
//     })
//   }

//   // StreamToHostCPU  mixin parameters
//   // Use the legacy NIC depth
//   val toHostCPUQueueDepth = 3072
//   val fromHostCPUQueueDepth = 3072
//   val tokenWidth = BridgeStreamConstants.streamWidthBits

//   lazy val module = new BridgeModuleImp(this) {
//     val io    = IO(new WidgetIO)
//     val hPort = IO(HostPort(new BitBlasterBridgeDecoupledIO(key.outParams, key.inParams)))

//     val outBits = hPort.hBits.out
//     val outLen = key.outParams.numBits
//     val inBits = hPort.hBits.in
//     val inLen = key.inParams.numBits
//     val reset = hPort.hBits.reset

//     // Implement trigger stuff

//     // Mask-off under reset

//     // Set after trigger-dependent memory-mapped registers have been set, to
//     // prevent spurious credits
//     val initDone                 = genWORegInit(Wire(Bool()), "initDone", true.B)
//     // When unset, diables token capture to improve FMR, while still enabling the
//     // use of TracerV-based triggers
//     val traceEnable              = genWORegInit(Wire(Bool()), "traceEnable", true.B)
//     // //Program Counter trigger value can be configured externally
//     // val hostTriggerPCWidthOffset = pcWidth - p(CtrlNastiKey).dataBits
//     // val hostTriggerPCLowWidth    = if (hostTriggerPCWidthOffset > 0) p(CtrlNastiKey).dataBits else pcWidth
//     // val hostTriggerPCHighWidth   = if (hostTriggerPCWidthOffset > 0) hostTriggerPCWidthOffset else 0

//     // val hostTriggerPCStartHigh = RegInit(0.U(hostTriggerPCHighWidth.W))
//     // val hostTriggerPCStartLow  = RegInit(0.U(hostTriggerPCLowWidth.W))
//     // attach(hostTriggerPCStartHigh, "hostTriggerPCStartHigh", WriteOnly)
//     // attach(hostTriggerPCStartLow, "hostTriggerPCStartLow", WriteOnly)
//     // val hostTriggerPCStart     = Cat(hostTriggerPCStartHigh, hostTriggerPCStartLow)
//     // val triggerPCStart         = RegInit(0.U(pcWidth.W))
//     // triggerPCStart := hostTriggerPCStart

//     // val hostTriggerPCEndHigh = RegInit(0.U(hostTriggerPCHighWidth.W))
//     // val hostTriggerPCEndLow  = RegInit(0.U(hostTriggerPCLowWidth.W))
//     // attach(hostTriggerPCEndHigh, "hostTriggerPCEndHigh", WriteOnly)
//     // attach(hostTriggerPCEndLow, "hostTriggerPCEndLow", WriteOnly)
//     // val hostTriggerPCEnd     = Cat(hostTriggerPCEndHigh, hostTriggerPCEndLow)
//     // val triggerPCEnd         = RegInit(0.U(pcWidth.W))
//     // triggerPCEnd := hostTriggerPCEnd

//     //Cycle count trigger
//     val hostTriggerCycleCountWidthOffset = 64 - p(CtrlNastiKey).dataBits
//     val hostTriggerCycleCountLowWidth    = if (hostTriggerCycleCountWidthOffset > 0) p(CtrlNastiKey).dataBits else 64
//     val hostTriggerCycleCountHighWidth   =
//       if (hostTriggerCycleCountWidthOffset > 0) hostTriggerCycleCountWidthOffset else 0

//     val hostTriggerCycleCountStartHigh = RegInit(0.U(hostTriggerCycleCountHighWidth.W))
//     val hostTriggerCycleCountStartLow  = RegInit(0.U(hostTriggerCycleCountLowWidth.W))
//     attach(hostTriggerCycleCountStartHigh, "hostTriggerCycleCountStartHigh", WriteOnly)
//     attach(hostTriggerCycleCountStartLow, "hostTriggerCycleCountStartLow", WriteOnly)
//     val hostTriggerCycleCountStart     = Cat(hostTriggerCycleCountStartHigh, hostTriggerCycleCountStartLow)
//     val triggerCycleCountStart         = RegInit(0.U(cycleCountWidth.W))
//     triggerCycleCountStart := hostTriggerCycleCountStart

//     val hostTriggerCycleCountEndHigh = RegInit(0.U(hostTriggerCycleCountHighWidth.W))
//     val hostTriggerCycleCountEndLow  = RegInit(0.U(hostTriggerCycleCountLowWidth.W))
//     attach(hostTriggerCycleCountEndHigh, "hostTriggerCycleCountEndHigh", WriteOnly)
//     attach(hostTriggerCycleCountEndLow, "hostTriggerCycleCountEndLow", WriteOnly)
//     val hostTriggerCycleCountEnd     = Cat(hostTriggerCycleCountEndHigh, hostTriggerCycleCountEndLow)
//     val triggerCycleCountEnd         = RegInit(0.U(cycleCountWidth.W))
//     triggerCycleCountEnd := hostTriggerCycleCountEnd

//     val trace_cycle_counter = RegInit(0.U(cycleCountWidth.W))

//     //trigger selector
//     val triggerSelector = RegInit(0.U((p(CtrlNastiKey).dataBits).W))
//     attach(triggerSelector, "triggerSelector", WriteOnly)

//     //set the trigger
//     assert(triggerCycleCountEnd >= triggerCycleCountStart)
//     val triggerCycleCountVal = RegInit(false.B)
//     triggerCycleCountVal := (trace_cycle_counter >= triggerCycleCountStart) & (trace_cycle_counter <= triggerCycleCountEnd)

//     // val triggerPCValVec = RegInit(VecInit(Seq.fill(traces.length)(false.B)))
//     // traces.zipWithIndex.foreach { case (trace, i) =>
//     //   when(trace.valid) {
//     //     when(triggerPCStart === trace.iaddr) {
//     //       triggerPCValVec(i) := true.B
//     //     }.elsewhen((triggerPCEnd === trace.iaddr) && triggerPCValVec(i)) {
//     //       triggerPCValVec(i) := false.B
//     //     }
//     //   }
//     // }

//     val trigger = MuxLookup(
//       triggerSelector,
//       false.B,
//       Seq(
//         0.U -> true.B,
//         1.U -> triggerCycleCountVal,
//       ),
//     )

//     // divide with a ceiling round, to get the total number of arms
//     val outArmCount = (outLen + tokenWidth) / tokenWidth

//     val outVec = splitBitsIntoChunks(outBits.bits, tokenWidth)

//     // Number of bits to use for the counter, the +1 is required because the counter will count 1 past the number of arms
//     val counterBits = log2Ceil(armCount + 1)

//     // This counter acts to select the mux arm
//     val counter = RegInit(0.U(counterBits.W))

//     // The main mux where the input arms are different possible valid traces, and the output goes to streamEnq
//     val streamMux = MuxLookup(counter, outVec(0), Seq.tabulate(outArmCount)(x => x.U -> outVec(x)))

//     streamEnq.bits := streamMux

//     outBits.ready := streamEnq.ready && trigger
//     val anyValidRemainMux = outBits.valid // TODO: Implement this

//     val maybeFire = !anyValidRemainMux || (counter === (armCount - 1).U)
//     val maybeEnq  = anyValidRemainMux

//     val commonPredicates = Seq(hPort.toHost.hValid, hPort.fromHost.hReady, streamEnq.ready, initDone)
//     val do_enq_helper  = DecoupledHelper((Seq(maybeEnq, traceEnable) ++ commonPredicates):_*)
//     val do_fire_helper = DecoupledHelper((maybeFire +: commonPredicates):_*)

//     // Note, if we dequeue a token that wins out over the increment below
//     when(do_fire_helper.fire()) {
//       counter := 0.U
//     }.elsewhen(do_enq_helper.fire()) {
//       counter := counter + 1.U
//     }

//     streamEnq.valid     := do_enq_helper.fire(streamEnq.ready, trigger)
//     hPort.toHost.hReady := do_fire_helper.fire(hPort.toHost.hValid)

//     // Output token (back to hub model) handling.
//     val triggerReg = RegEnable(trigger, false.B, do_fire_helper.fire())
//     hPort.hBits.triggerDebit  := !trigger && triggerReg
//     hPort.hBits.triggerCredit := trigger && !triggerReg

//     hPort.fromHost.hValid := do_fire_helper.fire(hPort.fromHost.hReady)

//     when(hPort.toHost.fire) {
//       trace_cycle_counter := trace_cycle_counter + 1.U
//     }

//     genCRFile()

//     override def genHeader(base: BigInt, memoryRegions: Map[String, BigInt], sb: StringBuilder): Unit = {
//       genConstructor(
//         base,
//         sb,
//         "bitblaster_t",
//         "bitblaster",
//         Seq(
//           UInt32(toHostStreamIdx),
//           UInt32(toHostCPUQueueDepth),
//           UInt32(key.outParams.numBits),
//           UInt32(fromHostStreamIdx),
//           UInt32(fromHostCPUQueueDepth),
//           UInt32(key.inParams.numBits),
//           Verbatim(clockDomainInfo.toC),
//         ),
//         hasStreams = true,
//       )
//     }
//   }
// }

class BitBlasterBridgeModule(key: BitBlasterBridgeKey)(implicit p: Parameters)
    extends BridgeModule[HostPortIO[BitBlasterBridgeIO]]()(p)
    with StreamToHostCPU with StreamFromHostCPU {

  def splitBitsIntoChunks(input: UInt, tokenWidth: Int): Vec[UInt] = {
    val numChunks = (input.getWidth + tokenWidth - 1) / tokenWidth
    VecInit((0 until numChunks).map { i =>
      val highBit = ((i + 1) * tokenWidth - 1).min(input.getWidth - 1)
      val lowBit = i * tokenWidth
      input(highBit, lowBit)
    })
  }

  // StreamToHostCPU  mixin parameters
  // Use the legacy NIC depth
  val toHostCPUQueueDepth = 3072
  val fromHostCPUQueueDepth = 3072
  val tokenWidth = BridgeStreamConstants.streamWidthBits

  lazy val module = new BridgeModuleImp(this) {
    val io    = IO(new WidgetIO)
    val hPort = IO(HostPort(new BitBlasterBridgeIO(key.outParams, key.inParams)))

    val outBits = hPort.hBits.out
    val outLen = key.outParams.numBits
    val inBits = hPort.hBits.in
    val inLen = key.inParams.numBits

    // Implement trigger stuff

    // Mask-off under reset

    // Set after trigger-dependent memory-mapped registers have been set, to
    // prevent spurious credits
    val initDone                 = genWORegInit(Wire(Bool()), "initDone", true.B)
    // When unset, diables token capture to improve FMR, while still enabling the
    // use of TracerV-based triggers
    val traceEnable              = genWORegInit(Wire(Bool()), "traceEnable", true.B)
    // //Program Counter trigger value can be configured externally
    // val hostTriggerPCWidthOffset = pcWidth - p(CtrlNastiKey).dataBits
    // val hostTriggerPCLowWidth    = if (hostTriggerPCWidthOffset > 0) p(CtrlNastiKey).dataBits else pcWidth
    // val hostTriggerPCHighWidth   = if (hostTriggerPCWidthOffset > 0) hostTriggerPCWidthOffset else 0

    // val hostTriggerPCStartHigh = RegInit(0.U(hostTriggerPCHighWidth.W))
    // val hostTriggerPCStartLow  = RegInit(0.U(hostTriggerPCLowWidth.W))
    // attach(hostTriggerPCStartHigh, "hostTriggerPCStartHigh", WriteOnly)
    // attach(hostTriggerPCStartLow, "hostTriggerPCStartLow", WriteOnly)
    // val hostTriggerPCStart     = Cat(hostTriggerPCStartHigh, hostTriggerPCStartLow)
    // val triggerPCStart         = RegInit(0.U(pcWidth.W))
    // triggerPCStart := hostTriggerPCStart

    // val hostTriggerPCEndHigh = RegInit(0.U(hostTriggerPCHighWidth.W))
    // val hostTriggerPCEndLow  = RegInit(0.U(hostTriggerPCLowWidth.W))
    // attach(hostTriggerPCEndHigh, "hostTriggerPCEndHigh", WriteOnly)
    // attach(hostTriggerPCEndLow, "hostTriggerPCEndLow", WriteOnly)
    // val hostTriggerPCEnd     = Cat(hostTriggerPCEndHigh, hostTriggerPCEndLow)
    // val triggerPCEnd         = RegInit(0.U(pcWidth.W))
    // triggerPCEnd := hostTriggerPCEnd

    //Cycle count trigger
    val cycleCountWidth   = 64
    val hostTriggerCycleCountWidthOffset = 64 - p(CtrlNastiKey).dataBits
    val hostTriggerCycleCountLowWidth    = if (hostTriggerCycleCountWidthOffset > 0) p(CtrlNastiKey).dataBits else 64
    val hostTriggerCycleCountHighWidth   =
      if (hostTriggerCycleCountWidthOffset > 0) hostTriggerCycleCountWidthOffset else 0

    val hostTriggerCycleCountStartHigh = RegInit(0.U(hostTriggerCycleCountHighWidth.W))
    val hostTriggerCycleCountStartLow  = RegInit(0.U(hostTriggerCycleCountLowWidth.W))
    attach(hostTriggerCycleCountStartHigh, "hostTriggerCycleCountStartHigh", WriteOnly)
    attach(hostTriggerCycleCountStartLow, "hostTriggerCycleCountStartLow", WriteOnly)
    val hostTriggerCycleCountStart     = Cat(hostTriggerCycleCountStartHigh, hostTriggerCycleCountStartLow)
    val triggerCycleCountStart         = RegInit(0.U(cycleCountWidth.W))
    triggerCycleCountStart := hostTriggerCycleCountStart

    val hostTriggerCycleCountEndHigh = RegInit(0.U(hostTriggerCycleCountHighWidth.W))
    val hostTriggerCycleCountEndLow  = RegInit(0.U(hostTriggerCycleCountLowWidth.W))
    attach(hostTriggerCycleCountEndHigh, "hostTriggerCycleCountEndHigh", WriteOnly)
    attach(hostTriggerCycleCountEndLow, "hostTriggerCycleCountEndLow", WriteOnly)
    val hostTriggerCycleCountEnd     = Cat(hostTriggerCycleCountEndHigh, hostTriggerCycleCountEndLow)
    val triggerCycleCountEnd         = RegInit(0.U(cycleCountWidth.W))
    triggerCycleCountEnd := hostTriggerCycleCountEnd

    val trace_cycle_counter = RegInit(0.U(cycleCountWidth.W))

    //trigger selector
    val triggerSelector = RegInit(0.U((p(CtrlNastiKey).dataBits).W))
    attach(triggerSelector, "triggerSelector", WriteOnly)

    //set the trigger
    assert(triggerCycleCountEnd >= triggerCycleCountStart)
    val triggerCycleCountVal = RegInit(false.B)
    triggerCycleCountVal := (trace_cycle_counter >= triggerCycleCountStart) & (trace_cycle_counter <= triggerCycleCountEnd)

    // val triggerPCValVec = RegInit(VecInit(Seq.fill(traces.length)(false.B)))
    // traces.zipWithIndex.foreach { case (trace, i) =>
    //   when(trace.valid) {
    //     when(triggerPCStart === trace.iaddr) {
    //       triggerPCValVec(i) := true.B
    //     }.elsewhen((triggerPCEnd === trace.iaddr) && triggerPCValVec(i)) {
    //       triggerPCValVec(i) := false.B
    //     }
    //   }
    // }

    val trigger = MuxLookup(
      triggerSelector,
      false.B,
      Seq(
        0.U -> true.B,
        1.U -> triggerCycleCountVal,
      ),
    )

    // divide with a ceiling round, to get the total number of arms
    val outArmCount = (outLen + tokenWidth) / tokenWidth

    val outVec = splitBitsIntoChunks(outBits.out, tokenWidth)

    // Number of bits to use for the counter, the +1 is required because the counter will count 1 past the number of arms
    val counterBits = log2Ceil(outArmCount + 1)

    // This counter acts to select the mux arm
    val counter = RegInit(0.U(counterBits.W))

    // The main mux where the input arms are different possible valid traces, and the output goes to streamEnq
    val streamMux = MuxLookup(counter, outVec(0), Seq.tabulate(outArmCount)(x => x.U -> outVec(x)))

    streamEnq.bits := streamMux

    val anyValidRemainMux =  true.B // TODO: Implement this

    val maybeFire = !anyValidRemainMux || (counter === (outArmCount - 1).U)
    val maybeEnq  = anyValidRemainMux

    val commonPredicates = Seq(hPort.toHost.hValid, hPort.fromHost.hReady, streamEnq.ready, initDone)
    val do_enq_helper  = DecoupledHelper((Seq(maybeEnq, traceEnable) ++ commonPredicates):_*)
    val do_fire_helper = DecoupledHelper((maybeFire +: commonPredicates):_*)

    // Note, if we dequeue a token that wins out over the increment below
    when(do_fire_helper.fire()) {
      counter := 0.U
    }.elsewhen(do_enq_helper.fire()) {
      counter := counter + 1.U
    }

    streamEnq.valid     := do_enq_helper.fire(streamEnq.ready, trigger)
    hPort.toHost.hReady := do_fire_helper.fire(hPort.toHost.hValid)

    // Output token (back to hub model) handling.
    val triggerReg = RegEnable(trigger, false.B, do_fire_helper.fire())
    // hPort.hBits.triggerDebit  := !trigger && triggerReg
    // hPort.hBits.triggerCredit := trigger && !triggerReg

    hPort.fromHost.hValid := do_fire_helper.fire(hPort.fromHost.hReady)

    when(hPort.toHost.fire) {
      trace_cycle_counter := trace_cycle_counter + 1.U
    }

    // IN Logic

    inBits := DontCare
    streamDeq.ready := false.B


    genCRFile()

    override def genHeader(base: BigInt, memoryRegions: Map[String, BigInt], sb: StringBuilder): Unit = {
      genConstructor(
        base,
        sb,
        "bitblaster_t",
        "bitblaster",
        Seq(
          UInt32(toHostStreamIdx),
          UInt32(toHostCPUQueueDepth),
          UInt32(key.outParams.numBits),
          UInt32(fromHostStreamIdx),
          UInt32(fromHostCPUQueueDepth),
          UInt32(key.inParams.numBits),
          Verbatim(clockDomainInfo.toC),
        ),
        hasStreams = true,
      )
    }
  }
}

package chipyard.example

import chisel3._
import chisel3.util._
import testchipip._
import chisel3.experimental.{IO, IntParam, BaseModule}
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.regmapper.{HasRegMap, RegField}
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.UIntIsOneOf


// DOC include start: AirSimIO params
case class AirSimIOParams(
  address: BigInt = 0x2000,
  width: Int = 32,
  useAXI4: Boolean = false)
// DOC include end: AirSimIO params

// DOC include start: AirSimIO key
case object AirSimIOKey extends Field[Option[AirSimIOParams]](None)
// DOC include end: AirSimIO key

class AirSimIOIO(val w: Int) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val input_ready = Output(Bool())
  val input_valid = Input(Bool())
  val x = Input(UInt(w.W))
  val y = Input(UInt(w.W))
  val output_ready = Input(Bool())
  val output_valid = Output(Bool())
  val airsimio = Output(UInt(w.W))
  val busy = Output(Bool())
}

class AirSimPortIO() extends Bundle {
  //val enable = Input(Bool())
  val top_busy = Output(Bool())
}

trait AirSimIOTopIO extends Bundle {
  val airsimio_busy = Output(Bool())
}


trait HasAirSimIOIO extends BaseModule {
  val w: Int
  val io = IO(new AirSimIOIO(w))
}

trait HasAirSimPortIO extends BaseModule {
  val port = IO(new AirSimPortIO())
}

// DOC include start: AirSimIO chisel
class AirSimIOMMIOChiselModule(val w: Int) extends Module
  with HasAirSimIOIO
{
  val s_idle :: s_run :: s_done :: Nil = Enum(3)

  val state = RegInit(s_idle)
  val tmp   = Reg(UInt(w.W))
  val airsimio   = Reg(UInt(w.W))

  io.input_ready := state === s_idle
  io.output_valid := state === s_done
  io.airsimio := airsimio

  when (state === s_idle && io.input_valid) {
    state := s_run
  } .elsewhen (state === s_run && tmp === 0.U) {
    state := s_done
  } .elsewhen (state === s_done && io.output_ready) {
    state := s_idle
  }

  when (state === s_idle && io.input_valid) {
    airsimio := io.x
    tmp := io.y
  } .elsewhen (state === s_run) {
    when (airsimio > tmp) {
      airsimio := airsimio - tmp
    } .otherwise {
      tmp := tmp - airsimio
    }
  }

  io.busy := state =/= s_idle
}
// DOC include end: AirSimIO chisel

// DOC include start: AirSimIO instance regmap

trait AirSimIOModule extends HasRegMap {
  val io: AirSimIOTopIO

  implicit val p: Parameters
  def params: AirSimIOParams
  val clock: Clock
  val reset: Reset


  // How many clock cycles in a PWM cycle?
  val x = Reg(UInt(params.width.W))
  val y = Wire(new DecoupledIO(UInt(params.width.W)))
  val airsimio = Wire(new DecoupledIO(UInt(params.width.W)))
  val status = Wire(UInt(2.W))

  val impl = Module(new AirSimIOMMIOChiselModule(params.width))

  impl.io.clock := clock
  impl.io.reset := reset.asBool

  impl.io.x := x
  impl.io.y := y.bits
  impl.io.input_valid := y.valid
  y.ready := impl.io.input_ready

  airsimio.bits := impl.io.airsimio
  airsimio.valid := impl.io.output_valid
  impl.io.output_ready := airsimio.ready

  status := Cat(impl.io.input_ready, impl.io.output_valid)
  io.airsimio_busy := impl.io.busy


  regmap(
    0x00 -> Seq(
      RegField.r(2, status)), // a read-only register capturing current status
    0x04 -> Seq(
      RegField.w(params.width, x)), // a plain, write-only register
    0x08 -> Seq(
      RegField.w(params.width, y)), // write-only, y.valid is set on write
    0x0C -> Seq(
      RegField.r(params.width, airsimio))) // read-only, airsimio.ready is set on read
}
// DOC include end: AirSimIO instance regmap

// DOC include start: AirSimIO router
class AirSimIOTL(params: AirSimIOParams, beatBytes: Int)(implicit p: Parameters)
  extends TLRegisterRouter(
    params.address, "airsimio", Seq("ucbbar,airsimio"),
    beatBytes = beatBytes)(
      new TLRegBundle(params, _) with AirSimIOTopIO)(
      new TLRegModule(params, _, _) with AirSimIOModule)

class AirSimIOAXI4(params: AirSimIOParams, beatBytes: Int)(implicit p: Parameters)
  extends AXI4RegisterRouter(
    params.address,
    beatBytes=beatBytes)(
      new AXI4RegBundle(params, _) with AirSimIOTopIO)(
      new AXI4RegModule(params, _, _) with AirSimIOModule)
// DOC include end: AirSimIO router

// DOC include start: AirSimIO lazy trait
trait CanHavePeripheryAirSimIO { this: BaseSubsystem =>
  private val portName = "airsimio"

  println(s"[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[")
  println(s"Got here!")
  println(s"[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[")
  //val airsim_port = new AirSimPortIO()
  //airsim_port.top_busy := 1.U
  //dontTouch(airsim_port)

  // Only build if we are using the TL (nonAXI4) version
  //val airsimio = p(AirSimIOKey) match {
  //  case Some(params) => {
  //    if (params.useAXI4) {
  //      val airsimio = LazyModule(new AirSimIOAXI4(params, pbus.beatBytes)(p))
  //      pbus.toSlave(Some(portName)) {
  //        airsimio.node :=
  //        AXI4Buffer () :=
  //        TLToAXI4 () :=
  //        // toVariableWidthSlave doesn't use holdFirstDeny, which TLToAXI4() needsx
  //        TLFragmenter(pbus.beatBytes, pbus.blockBytes, holdFirstDeny = true)
  //      }
  //      Some(airsimio)
  //    } else {
  //      val airsimio = LazyModule(new AirSimIOTL(params, pbus.beatBytes)(p))
  //      pbus.toVariableWidthSlave(Some(portName)) { airsimio.node }

  //      val outer_io = InModuleBody {
  //        val outer_io = IO(new ClockedIO(new BlockDeviceIO)).suggestName(portName)
  //        outer_io.top_busy := 1.U
  //      }
  //      Some(airsimio)
  //    }
  //  }
  //  case None => None
  //}
  val airsimio = p(AirSimIOKey).map { params =>
    val airsimmod = LazyModule(new AirSimIOTL(params, pbus.beatBytes)(p))
    pbus.toVariableWidthSlave(Some(portName)) { airsimmod.node }

    val outer_io = InModuleBody {
      val outer_io = IO(new ClockedIO(new AirSimPortIO)).suggestName(portName)
      println(s"oooooooooooooooooooooooooooo");
      println(outer_io)
      println(s"oooooooooooooooooooooooooooo");
      dontTouch(outer_io)
      outer_io
    }
    outer_io
  }
}
// DOC include end: AirSimIO lazy trait

// DOC include start: AirSimIO imp trait
trait CanHavePeripheryAirSimIOModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryAirSimIO
}
// DOC include end: AirSimIO imp trait


// DOC include start: AirSimIO config fragment
class WithAirSimIO(useAXI4: Boolean) extends Config((site, here, up) => {
  case AirSimIOKey => Some(AirSimIOParams(useAXI4 = useAXI4))
})
// DOC include end: AirSimIO config fragment

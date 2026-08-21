package chipyard

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.{Config, Parameters}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.subsystem._
import testchipip.soc.{OBUS}
import chipyard.harness.BuildTop
import chipyard.iobinders._
import sifive.blocks.devices.uart._
import testchipip._
import testchipip.boot._
import scala.collection.immutable.ListMap
import constellation.channel._
import constellation.routing._
import constellation.router._
import constellation.topology._
import constellation.noc._
import constellation.soc.{GlobalNoCParams}
import shuttle.common._
import saturn.common.{VectorParams}
import freechips.rocketchip.util.{AsyncQueueParams}
import freechips.rocketchip.subsystem.WithoutTLMonitors

class KodiakFireSimConfig extends Config (

  //==================================
  // Set up buses
  //==================================
  new chipyard.config.WithSystemBusWidth(256) ++


  //==================================
  // Set up TestHarness
  //==================================
  new chipyard.harness.WithAbsoluteFreqHarnessClockInstantiator ++ // use absolute frequencies for simulations in the harness
                                                                   // NOTE: This only simulates properly in VCS
  new testchipip.soc.WithChipIdPin ++                               // Add pin to identify chips

  // Success error, tie-off doesn't work
  // new chipyard.harness.WithTiedOffJTAG ++
  // new chipyard.harness.WithTiedOffDMI ++

  // Success error, let's try WithNoDebug
  new chipyard.config.WithNoDebug ++

  // new chipyard.harness.WithSerialTLTiedOff(tieoffs=Some(Seq(1))) ++ // Tie-off the chip-to-chip link in single-chip sims
  new chipyard.harness.WithDriveChipIdPin ++
  // new chipyard.harness.WithOffchipBusSelPlusArg ++


  //==================================
  // Set up peripherals
  //==================================
  new testchipip.boot.WithNoCustomBootPin ++
  new chipyard.config.WithNoBusErrorDevices ++

  new freechips.rocketchip.subsystem.WithoutTLMonitors ++

  //==================================
  // Rocket
  //==================================
  // ICache
  new freechips.rocketchip.rocket.WithL1ICacheWays(2) ++
  new freechips.rocketchip.rocket.WithL1ICacheSets(128) ++
  new freechips.rocketchip.rocket.WithL1ICacheBlockBytes(64) ++
  new freechips.rocketchip.rocket.WithNBigCores(1) ++
  // DCache
  new freechips.rocketchip.rocket.WithL1DCacheBlockBytes(64) ++
  new freechips.rocketchip.rocket.WithL1DCacheSets(128) ++
  new freechips.rocketchip.rocket.WithL1DCacheWays(4) ++

  //==================================
  // Shuttle Tile + Saturn Cores
  //==================================
  new saturn.shuttle.WithShuttleVectorUnit(512, 256, VectorParams.genParams) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithTCM(size=256L << 10, banks=2) ++
  new shuttle.common.WithShuttleTileBoundaryBuffers() ++
  // ICache
  new shuttle.common.WithL1ICacheWays(2) ++
  new shuttle.common.WithL1ICacheSets(64) ++
  // DCache
  new shuttle.common.WithL1DCacheWays(2) ++
  new shuttle.common.WithL1DCacheBanks(1) ++
  new shuttle.common.WithL1DCacheTagBanks(1) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithNShuttleCores(2) ++

  //==================================
  // Set up I/O
  //==================================
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 4L) ++                  // 4GB max external memory
  new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++                          // 1 memory channel

  new testchipip.serdes.WithSerialTL
    (
    Seq(
    testchipip.serdes.SerialTLParams(                               // 1st serial-tl is chip-to-chip
      client = Some(testchipip.serdes.SerialTLClientParams()),      // chip-to-chip serial-tl acts as a client
      manager = Some(testchipip.serdes.SerialTLManagerParams(       // chip-to-chip serial-tl managers other chip's memor
        memParams = Seq(testchipip.serdes.ManagerRAMParams(
          address = 0,  // base of chip 2's addr space wrt chip 2
          size = 2L << 32,
        )),
        slaveWhere = OBUS,
        cacheIdBits = 4
      )),
      phyParams = testchipip.serdes.CreditedSourceSyncSerialPhyParams(phitWidth=4, flitWidth=16)     // chip-to-chip serial-tl is symmetric source-sync'd
    ))
  ) ++
  new testchipip.soc.WithOffchipBusClient(SBUS,                     // obus provides path to other chip's memory
    blockRange = Seq(AddressSet(0, (2L << 32) - 1)),                // The lower 8GB is mapped to this chip
    replicationBase = Some(2L << 32)                                // The upper 8GB goes off-chip
  ) ++

  new testchipip.soc.WithOffchipBus ++

  //==================================
  // Set up memory
  //==================================
  new chipyard.config.WithInclusiveCacheInteriorBuffer ++
  new chipyard.config.WithInclusiveCacheExteriorBuffer ++
  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays=4, capacityKB=512, outerLatencyCycles=4) ++
  new freechips.rocketchip.subsystem.WithNBanks(4) ++
  new testchipip.soc.WithNoScratchpadMonitors ++
  new testchipip.soc.WithScratchpad(base=0x580000000L,
                                    size=(1L << 17), // 128KB
                                    banks=2,
                                    partitions=1,
                                    buffer=BufferParams.default,
                                    outerBuffer=BufferParams.default) ++

  new chipyard.config.AbstractConfig
)

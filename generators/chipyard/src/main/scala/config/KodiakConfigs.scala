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

class KodiakFireSimBaseConfig extends Config (
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
  new chipyard.harness.WithSerialTLTiedOff(tieoffs=Some(Seq(1))) ++ // Tie-off the chip-to-chip link in single-chip sims
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
  // new chipyard.config.WithRocketBoundaryBuffers ++         // Add buffer to meet timing at chip boundary (from ext memory)
  // new chipyard.config.WithRocketCacheRowBits(64) ++        // Always build a 64-bit width cache, add WidthWidget to partition ext memory into 64-bit packets
  //new freechips.rocketchip.subsystem.WithAsynchronousRocketTiles(3, 3) ++ // Add async crossings between RocketTile and uncore
  //new freechips.rocketchip.subsystem.WithNBigCores(4, location=InCluster(0)) ++
  //new freechips.rocketchip.subsystem.WithCluster(0) ++

  //==================================
  // UCIe Digital 0
  //==================================
  // new edu.berkeley.cs.ucie.digital.tilelink.WithUCITLAdapter(edu.berkeley.cs.ucie.digital.tilelink.UCITLParams(
  //   protoParams = edu.berkeley.cs.ucie.digital.protocol.ProtocolLayerParams(),
  //   tlParams    = edu.berkeley.cs.ucie.digital.tilelink.TileLinkParams( address = 0x0000,
  //                                                                       addressRange = ((2L << 32)-1),
  //                                                                       configAddress = 0x5000,
  //                                                                       inwardQueueDepth = 2,
  //                                                                       outwardQueueDepth = 2,
  //                                                                       dataWidth_arg = 256),
  //   fdiParams   = edu.berkeley.cs.ucie.digital.interfaces.FdiParams(width = 64, dllpWidth = 64, sbWidth = 32),
  //   rdiParams   = edu.berkeley.cs.ucie.digital.interfaces.RdiParams(width = 64, sbWidth = 32),
  //   sbParams    = edu.berkeley.cs.ucie.digital.sideband.SidebandParams(),
  //   //myId        = 1,
  //   linkTrainingParams = edu.berkeley.cs.ucie.digital.logphy.LinkTrainingParams(),
  //   afeParams   = edu.berkeley.cs.ucie.digital.interfaces.AfeParams(),
  //   laneAsyncQueueParams = AsyncQueueParams()
  // )) ++
  // new chipyard.harness.WithUcieDigitalTiedOff ++


  // //==================================
  // // UCIe Digital 1
  // //==================================
  // new edu.berkeley.cs.ucie.digital.tilelink.WithUCITLAdapter(edu.berkeley.cs.ucie.digital.tilelink.UCITLParams(
  //   protoParams = edu.berkeley.cs.ucie.digital.protocol.ProtocolLayerParams(),
  //   tlParams    = edu.berkeley.cs.ucie.digital.tilelink.TileLinkParams( address = 0x100000000L,
  //                                                                       addressRange = ((2L << 32)-1),
  //                                                                       configAddress = 0x100005000L,
  //                                                                       inwardQueueDepth = 2,
  //                                                                       outwardQueueDepth = 2,
  //                                                                       dataWidth_arg = 256),
  //   fdiParams   = edu.berkeley.cs.ucie.digital.interfaces.FdiParams(width = 64, dllpWidth = 64, sbWidth = 32),
  //   rdiParams   = edu.berkeley.cs.ucie.digital.interfaces.RdiParams(width = 64, sbWidth = 32),
  //   sbParams    = edu.berkeley.cs.ucie.digital.sideband.SidebandParams(),
  //   //myId        = 1,
  //   linkTrainingParams = edu.berkeley.cs.ucie.digital.logphy.LinkTrainingParams(),
  //   afeParams   = edu.berkeley.cs.ucie.digital.interfaces.AfeParams(),
  //   laneAsyncQueueParams = AsyncQueueParams()
  // )) ++
  // new chipyard.harness.WithUcieDigitalTiedOff ++

  //==================================
  // Set up tiles
  //==================================
  // new chipyard.config.WithNPMPs(0) ++ 										// Eliminate potentially critical path
  // Gemmini with RoCC interfaces
  // new chipyard.config.WithMultiRoCC ++
  // new chipyard.config.WithMultiRoCCFromBuildRoCC(0, 1, 2, 3, 4) ++
  // new rerocc.WithReRoCC(rerocc.client.ReRoCCClientParams(nCfgs=16), rerocc.manager.ReRoCCTileParams(l2TLBEntries=512, l2TLBWays=1, dcacheParams=Some(DCacheParams(nWays=1, nSets=64)))) ++


  //==================================
  // Shuttle Tile + Saturn Cores
  //==================================
  // new shuttle.common.WithAsynchronousShuttleTiles(3, 3, location=InCluster(0)) ++ // Add async crossings between RocketTile and uncore
  new saturn.shuttle.WithShuttleVectorUnit(512, 256, VectorParams.genParams) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithTCM(size=256L << 10, banks=2) ++
  new shuttle.common.WithShuttleTileBoundaryBuffers() ++
  // ICache
  new shuttle.common.WithL1ICacheWays(2) ++
  new shuttle.common.WithL1ICacheSets(64) ++
  // DCache
  new shuttle.common.WithL1DCacheWays(2) ++
  // new shuttle.common.WithL1DCacheSets(256) ++
  new shuttle.common.WithL1DCacheBanks(1) ++
  new shuttle.common.WithL1DCacheTagBanks(1) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithNShuttleCores(2) ++

  //==================================
  // Set up I/O
  //==================================
  // new WithIntel4x4ChipTop(sim=sim) ++
  //new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 4L) ++                  // 4GB max external memory
  //new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++                          // 1 memory channel

  new testchipip.serdes.WithSerialTL
    (Seq(testchipip.serdes.SerialTLParams(              // 1 serial tilelink port
      manager = Some(testchipip.serdes.SerialTLManagerParams(                             // port acts as amanager of offchip memory
      	memParams = Seq(testchipip.serdes.ManagerRAMParams(                               // 4 GB of off-c`hip   memory
          address = BigInt("80000000", 16),
          size    = BigInt("100000000", 16)
        )),
        isMemoryDevice = true,
        slaveWhere = MBUS
      )),
      client = Some(testchipip.serdes.SerialTLClientParams()),                            // Allow an external manager to probe this chip
      phyParams = testchipip.serdes.DecoupledExternalSyncSerialPhyParams(phitWidth=1, flitWidth=16)              // 4-bit bidir interface, sync'd to an external clock
    ),
    testchipip.serdes.SerialTLParams(                               // 1st serial-tl is chip-to-chip
      client = Some(testchipip.serdes.SerialTLClientParams()),      // chip-to-chip serial-tl acts as a client
      manager = Some(testchipip.serdes.SerialTLManagerParams(       // chip-to-chip serial-tl managers other chip's memor
        memParams = Seq(testchipip.serdes.ManagerRAMParams(
          address = 0,
          size = 2L << 32,
        )),
        slaveWhere = OBUS,
        cacheIdBits = 4
      )),
      phyParams = testchipip.serdes.CreditedSourceSyncSerialPhyParams(phitWidth=4, flitWidth=16)     // chip-to-chip serial-tl is symmetric source-sync'd
    )
    )
  ) ++
  new testchipip.soc.WithOffchipBusClient(SBUS,                     // obus provides path to other chip's memory
    blockRange = Seq(AddressSet(0, (2L << 32) - 1)),                // The lower 8GB is mapped to this chip
    replicationBase = Some(2L << 32)                                // The upper 8GB goes off-chip
  ) ++

  new testchipip.soc.WithOffchipBus ++
  // TODO: REMOVE
  new freechips.rocketchip.subsystem.WithNoMemPort ++                                   // Remove axi4 mem port
  //new testchipip.soc.WithOffchipBusClient(MBUS) ++                                          // offchip bus connects to
  //new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++                          // 1 memory channel


  //==================================
  // Set up memory
  //==================================
  //new chipyard.config.WithBroadcastManager ++
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

  //==================================
  // Set up NoC
  //==================================
  //new rerocc.WithReRoCCNoC(rerocc.ReRoCCNoCParams(
  //  tileClientMapping = ListMap(
  //    0 -> RRXYMesh(1, 1),
  //    1 -> RRXYMesh(0, 0),
  //    2 -> RRXYMesh(1, 0),
  //    //3 -> XYMesh(0, 3)
  //  ),
  //  managerMapping = ListMap(
  //    0 -> RRXYMesh(0, 2),
  //    1 -> RRXYMesh(0, 3),
  //    2 -> RRXYMesh(1, 2),
  //    3 -> RRXYMesh(1, 3),
  //    4 -> RRXYMesh(0, 1)
  //  ),
  //  nocParams = NoCParams(
  //    topology      = Mesh2D(XYMesh.XDIM, XYMesh.YDIM),
  //    channelParamGen = (a, b) => UserChannelParams(Seq.fill(2) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
  //    routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
  //    routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 2, 1)
  //  )
  //)) ++
  //new constellation.soc.WithSbusNoC(constellation.protocol.SplitACDxBETLNoCParams(
  //  constellation.protocol.DiplomaticNetworkNodeMapping(
  //    inNodeMapping = ListMap(
  //      "serial_tl" -> XYMeshv2(3, 1),
  //      "Core 0"   -> XYMeshv2(0, 1),
  //      "Core 1"   -> XYMeshv2(1, 1),
  //      "Core 2"   -> XYMeshv2(2, 1),
  //      "ReRoCC 4"   -> XYMeshv2(2, 2),
  //      "ReRoCC 0"   -> XYMeshv2(4, 0),
  //      "ReRoCC 1"   -> XYMeshv2(4, 1),
  //      "ReRoCC 2"   -> XYMeshv2(4, 2),
  //      "ReRoCC 3"   -> XYMeshv2(4, 3),
  //      "ucie-client[0]" -> XYMeshv2(0, 3),
  //      "ucie-client[1]" -> XYMeshv2(0, 3),
  //    ),
  //    outNodeMapping = ListMap(
  //      "pbus"         -> XYMeshv2(3, 1),
  //      "serdesser[0]" -> XYMeshv2(0, 0),
  //      "serdesser[1]" -> XYMeshv2(1, 0),
  //      "serdesser[2]" -> XYMeshv2(2, 0),
  //      "serdesser[3]" -> XYMeshv2(3, 0),
  //      "Core 1 TCM"   -> XYMeshv2(0, 2),
  //      "Core 2 TCM"   -> XYMeshv2(1, 2),
  //      // "ram[0]" -> XYMeshv2(3, 0),
  //      // "ram[1]" -> XYMeshv2(3, 1),
  //      // "ram[2]" -> XYMeshv2(3, 2),
  //      // "ram[3]" -> XYMeshv2(3, 3),
  //      "uciephy[0]"   -> XYMeshv2(1, 3),
  //      "regNode[0]"   -> XYMeshv2(1, 3),
  //      "regNode[1]"   -> XYMeshv2(1, 3),
  //      "spad_rw_mgr_0[0]" -> XYMeshv2(5, 0),
  //      "spad_rw_mgr_0[1]" -> XYMeshv2(5, 1),
  //      "spad_rw_mgr_0[2]" -> XYMeshv2(5, 2),
  //      "spad_rw_mgr_0[3]" -> XYMeshv2(5, 3),
  //    )
  //  ),
  //  acdNoCParams = NoCParams(
  //    topology        = Mesh2D(XYMeshv2.XDIM, XYMeshv2.YDIM),
  //    channelParamGen = (a, b) => UserChannelParams(Seq.fill(3) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
  //    routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
  //    routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 3, 1)),
  //  beNoCParams = NoCParams(
  //    topology        = Mesh2D(XYMeshv2.XDIM, XYMeshv2.YDIM),
  //    channelParamGen = (a, b) => UserChannelParams(Seq.fill(2) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
  //    routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
  //    routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 2, 1)),
  //  beDivision = 4
  //), inlineNoC = true) ++
  //new constellation.soc.WithMbusNoC(constellation.protocol.SimpleTLNoCParams(
  //  constellation.protocol.DiplomaticNetworkNodeMapping(
  //    inNodeMapping = ListMap(
  //      "L2 InclusiveCache[0]" -> 0, "L2 InclusiveCache[1]" -> 1,
  //      "L2 InclusiveCache[2]" -> 2, "L2 InclusiveCache[3]" -> 3),
  //    outNodeMapping = ListMap(
  //      "ram[0]" -> 4,
  //      "serdesser[0]" -> 5,
  //      "tlFrontEnd[0]" -> 6)),
  //  NoCParams(
  //    topology        = BidirectionalLine(7),
  //    channelParamGen = (a, b) => UserChannelParams(Seq.fill(5) { UserVirtualChannelParams(4) }),
  //    routingRelation = NonblockingVirtualSubnetworksRouting(BidirectionalLineRouting(), 5, 1))
  //), inlineNoC = true) ++


  //==================================
  // Set up clock./reset
  //==================================
  // Create the uncore clock group
  // new chipyard.clocking.WithClockGroupsCombinedByName(
  //   ("uncore", Seq("implicit", "sbus", "mbus", "cbus",
  //                  "system_bus", "fbus", "pbus"), Nil)) ++
  new chipyard.config.AbstractConfig
)

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
  new chipyard.harness.WithSerialTLTiedOff(tieoffs=Some(Seq(1))) ++ // Tie-off the chip-to-chip link in single-chip sims
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
  // new chipyard.config.WithRocketBoundaryBuffers ++         // Add buffer to meet timing at chip boundary (from ext memory)
  // new chipyard.config.WithRocketCacheRowBits(64) ++        // Always build a 64-bit width cache, add WidthWidget to partition ext memory into 64-bit packets
  //new freechips.rocketchip.subsystem.WithAsynchronousRocketTiles(3, 3) ++ // Add async crossings between RocketTile and uncore
  //new freechips.rocketchip.subsystem.WithNBigCores(4, location=InCluster(0)) ++
  //new freechips.rocketchip.subsystem.WithCluster(0) ++

  //==================================
  // UCIe Digital 0
  //==================================
  // new edu.berkeley.cs.ucie.digital.tilelink.WithUCITLAdapter(edu.berkeley.cs.ucie.digital.tilelink.UCITLParams(
  //   protoParams = edu.berkeley.cs.ucie.digital.protocol.ProtocolLayerParams(),
  //   tlParams    = edu.berkeley.cs.ucie.digital.tilelink.TileLinkParams( address = 0x0000,
  //                                                                       addressRange = ((2L << 32)-1),
  //                                                                       configAddress = 0x5000,
  //                                                                       inwardQueueDepth = 2,
  //                                                                       outwardQueueDepth = 2,
  //                                                                       dataWidth_arg = 256),
  //   fdiParams   = edu.berkeley.cs.ucie.digital.interfaces.FdiParams(width = 64, dllpWidth = 64, sbWidth = 32),
  //   rdiParams   = edu.berkeley.cs.ucie.digital.interfaces.RdiParams(width = 64, sbWidth = 32),
  //   sbParams    = edu.berkeley.cs.ucie.digital.sideband.SidebandParams(),
  //   //myId        = 1,
  //   linkTrainingParams = edu.berkeley.cs.ucie.digital.logphy.LinkTrainingParams(),
  //   afeParams   = edu.berkeley.cs.ucie.digital.interfaces.AfeParams(),
  //   laneAsyncQueueParams = AsyncQueueParams()
  // )) ++
  // new chipyard.harness.WithUcieDigitalTiedOff ++


  // //==================================
  // // UCIe Digital 1
  // //==================================
  // new edu.berkeley.cs.ucie.digital.tilelink.WithUCITLAdapter(edu.berkeley.cs.ucie.digital.tilelink.UCITLParams(
  //   protoParams = edu.berkeley.cs.ucie.digital.protocol.ProtocolLayerParams(),
  //   tlParams    = edu.berkeley.cs.ucie.digital.tilelink.TileLinkParams( address = 0x100000000L,
  //                                                                       addressRange = ((2L << 32)-1),
  //                                                                       configAddress = 0x100005000L,
  //                                                                       inwardQueueDepth = 2,
  //                                                                       outwardQueueDepth = 2,
  //                                                                       dataWidth_arg = 256),
  //   fdiParams   = edu.berkeley.cs.ucie.digital.interfaces.FdiParams(width = 64, dllpWidth = 64, sbWidth = 32),
  //   rdiParams   = edu.berkeley.cs.ucie.digital.interfaces.RdiParams(width = 64, sbWidth = 32),
  //   sbParams    = edu.berkeley.cs.ucie.digital.sideband.SidebandParams(),
  //   //myId        = 1,
  //   linkTrainingParams = edu.berkeley.cs.ucie.digital.logphy.LinkTrainingParams(),
  //   afeParams   = edu.berkeley.cs.ucie.digital.interfaces.AfeParams(),
  //   laneAsyncQueueParams = AsyncQueueParams()
  // )) ++
  // new chipyard.harness.WithUcieDigitalTiedOff ++

  //==================================
  // Set up tiles
  //==================================
  // new chipyard.config.WithNPMPs(0) ++ 										// Eliminate potentially critical path
  // Gemmini with RoCC interfaces
  // new chipyard.config.WithMultiRoCC ++
  // new chipyard.config.WithMultiRoCCFromBuildRoCC(0, 1, 2, 3, 4) ++
  // new rerocc.WithReRoCC(rerocc.client.ReRoCCClientParams(nCfgs=16), rerocc.manager.ReRoCCTileParams(l2TLBEntries=512, l2TLBWays=1, dcacheParams=Some(DCacheParams(nWays=1, nSets=64)))) ++


  //==================================
  // Shuttle Tile + Saturn Cores
  //==================================
  // new shuttle.common.WithAsynchronousShuttleTiles(3, 3, location=InCluster(0)) ++ // Add async crossings between RocketTile and uncore
  new saturn.shuttle.WithShuttleVectorUnit(512, 256, VectorParams.genParams) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithTCM(size=256L << 10, banks=2) ++
  new shuttle.common.WithShuttleTileBoundaryBuffers() ++
  // ICache
  new shuttle.common.WithL1ICacheWays(2) ++
  new shuttle.common.WithL1ICacheSets(64) ++
  // DCache
  new shuttle.common.WithL1DCacheWays(2) ++
  // new shuttle.common.WithL1DCacheSets(256) ++
  new shuttle.common.WithL1DCacheBanks(1) ++
  new shuttle.common.WithL1DCacheTagBanks(1) ++
  new shuttle.common.WithShuttleTileBeatBytes(16) ++
  new shuttle.common.WithNShuttleCores(2) ++

  //==================================
  // Set up I/O
  //==================================
  // new WithIntel4x4ChipTop(sim=sim) ++
  //new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 4L) ++                  // 4GB max external memory
  // new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++                          // 1 memory channel

  new testchipip.serdes.WithSerialTL
    (Seq(testchipip.serdes.SerialTLParams(              // 1 serial tilelink port
      manager = Some(testchipip.serdes.SerialTLManagerParams(                             // port acts as amanager of offchip memory
      	memParams = Seq(testchipip.serdes.ManagerRAMParams(                               // 4 GB of off-c`hip   memory
          address = BigInt("80000000", 16),
          size    = BigInt("100000000", 16)
        )),
        isMemoryDevice = true,
        slaveWhere = MBUS
      )),
      client = Some(testchipip.serdes.SerialTLClientParams()),                            // Allow an external manager to probe this chip
      phyParams = testchipip.serdes.DecoupledExternalSyncSerialPhyParams(phitWidth=1, flitWidth=16)              // 4-bit bidir interface, sync'd to an external clock
    ),
    testchipip.serdes.SerialTLParams(                               // 1st serial-tl is chip-to-chip
      client = Some(testchipip.serdes.SerialTLClientParams()),      // chip-to-chip serial-tl acts as a client
      manager = Some(testchipip.serdes.SerialTLManagerParams(       // chip-to-chip serial-tl managers other chip's memor
        memParams = Seq(testchipip.serdes.ManagerRAMParams(
          address = 0,
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
  // TODO: REMOVE
  new freechips.rocketchip.subsystem.WithNoMemPort ++                                   // Remove axi4 mem port
  //new testchipip.soc.WithOffchipBusClient(MBUS) ++                                          // offchip bus connects to
  // new freechips.rocketchip.subsystem.WithNMemoryChannels(1) ++                          // 1 memory channel


  //==================================
  // Set up memory
  //==================================
  //new chipyard.config.WithBroadcastManager ++
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

  //==================================
  // Set up clock./reset
  //==================================
  // Create the uncore clock group
  // new chipyard.clocking.WithClockGroupsCombinedByName(
  //   ("uncore", Seq("implicit", "sbus", "mbus", "cbus",
  //                  "system_bus", "fbus", "pbus"), Nil)) ++
  new chipyard.config.AbstractConfig
)

class MultiSimSertlKodiakConfig extends Config(
  new chipyard.harness.WithAbsoluteFreqHarnessClockInstantiator ++
  new chipyard.harness.WithMultiChipSerialTL(chip0=0, chip1=1, chip0portId=1, chip1portId=1) ++
  new chipyard.harness.WithMultiChip(0, new KodiakFireSimBaseConfig) ++
  new chipyard.harness.WithMultiChip(1, new KodiakFireSimBaseConfig)
)




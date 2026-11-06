import chisel3._
import chisel3.util._

// 基本問題1: 4ビットRingカウンタ (9MHz分周・1秒イネーブル連携)
class RingCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val out    = Output(UInt(width.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(clkFreq)
  val cntReg     = RegInit(0.U(cntWidth.W))
  val state      = RegInit(1.U(width.W))
  val ledReg     = RegInit(false.B)

  val enRingCounter = WireDefault(false.B)

  // TODO:
  // 1. 分周カウンタ cntReg (clear時は 0.U、en時は 0 〜 clkFreq-1 を巡回) と
  //    1秒ストローブ enRingCounter := io.en && (cntReg === (fullPeriod - 1).U) を実装してください
  // 2. state レジスタ (clear時は 1.U、enRingCounter 時は Cat(state(width - 2, 0), state(width - 1))) を実装してください
  // 3. ledReg の 0.5秒反転と出力接続を実装してください

  io.out    := state
  // io.out := ~state
  io.runLED := ~ledReg
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new RingCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

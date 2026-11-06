import chisel3._
import chisel3.util._

// 発展問題: 4ビット・グレイコード・カウンタ (9MHz分周・1秒イネーブル連携)
class GrayCodeCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val gray   = Output(UInt(4.W))
    val bin    = Output(UInt(4.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(clkFreq)
  val cntReg     = RegInit(0.U(cntWidth.W))
  val binReg     = RegInit(0.U(width.W))
  val ledReg     = RegInit(false.B)

  val enGrayCodeCounter = WireDefault(false.B)

  // TODO:
  // 1. 分周カウンタ cntReg と 1秒ストローブ enGrayCodeCounter := io.en && (cntReg === (fullPeriod - 1).U) を実装してください
  // 2. バイナリカウンタ binReg (clear時は 0.U、enGrayCodeCounter 時は binReg + 1.U) を実装してください
  // 3. バイナリカウンタからグレイコードへの変換論理 (val gray = binReg ^ (binReg >> 1)) と
  //    負論理出力 (io.gray := ~gray, io.bin := ~binReg, io.runLED := ~ledReg) を実装してください

  io.gray   := 0.U
  io.bin    := ~binReg
  io.runLED := ~ledReg
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new GrayCodeCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

import chisel3._
import chisel3.util.log2Ceil

// 1秒パルスを生成するサブモジュール
class OneHzGenerator(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en         = Input(Bool())
    val clear      = Input(Bool())
    val ledBlink   = Output(Bool())
    val oneHzPulse = Output(Bool())
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(fullPeriod)

  val cntReg = RegInit(0.U(cntWidth.W))
  val ledReg = RegInit(false.B)

  when(io.clear) {
    cntReg := 0.U
    ledReg := false.B
  }.elsewhen(io.en) {
    when(cntReg === (fullPeriod - 1).U) {
      cntReg := 0.U
      ledReg := !ledReg
    }.elsewhen(cntReg === (halfPeriod - 1).U) {
      cntReg := cntReg + 1.U
      ledReg := !ledReg
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }

  io.ledBlink   := ledReg
  io.oneHzPulse := io.en && (cntReg === (fullPeriod - 1).U)
}

// トップモジュール: OneHzGenerator と FourStateFSM を呼び出し、1秒パルスをイネーブルとしてFSMを動作させる
class TeCIO(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())     // ランボタン (RUN_SW: P16)
    val clear  = Input(Bool())     // リセットスイッチ (RESET_SW: P120)
    val runLED = Output(Bool())    // 動作中点滅LED (RUN_LED: P43)
    val out    = Output(UInt(4.W)) // ワンホット状態出力 (DATA_LED[3:0]: P62, P61, P59, P58)
  })

  // TODO:
  // 1. OneHzGenerator(clkFreq) と FourStateFSM を Module(new ...) でインスタンス化
  // 2. oneHz.io.oneHzPulse を fsm.io.en に接続し、1秒パルスをイネーブルとしてFSMを動作させる
  // 3. TeCボードのLED負論理に合わせて反転出力 (~oneHz.io.ledBlink, ~fsm.io.out) を接続
  io.runLED := true.B
  io.out    := 0.U(4.W)
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

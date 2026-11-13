import chisel3._

// トップモジュール: OneHzGenerator と SequenceDetector101 を呼び出し、1秒パルスをイネーブルとしてFSMを動作させる
class FSMIO(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())  // ランボタン (RUN_SW: P16)
    val clear    = Input(Bool())  // リセットスイッチ (RESET_SW: P120)
    val in       = Input(Bool())  // シリアルビット入力 (DATA_SW[0]: P35)
    val runLED   = Output(Bool()) // 動作中点滅LED (RUN_LED: P43)
    val detected = Output(Bool()) // "101" 検出出力 (DATA_LED[0]: P58)
  })

  // TODO:
  // 1. OneHzGenerator(clkFreq) と SequenceDetector101 を Module(new ...) でインスタンス化
  // 2. oneHz.io.oneHzPulse を fsm.io.en に接続し、1秒パルスをイネーブルとしてFSMを動作させる
  // 3. TeCボードのLED負論理に合わせて反転出力 (~oneHz.io.ledBlink, ~fsm.io.detected) を接続
  io.runLED   := true.B
  io.detected := true.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new FSMIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

import chisel3._

// トップモジュール: OneHzGenerator と TrafficLightController を呼び出し、1秒パルスをイネーブルとしてFSMを動作させる
class FSMIO(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en               = Input(Bool())     // ランボタン (RUN_SW: P16)
    val clear            = Input(Bool())     // リセットスイッチ (RESET_SW: P120)
    val pedestrianButton = Input(Bool())     // 歩行者ボタン (STEP_SW: P15)
    val runLED           = Output(Bool())    // 動作中点滅LED (RUN_LED: P43)
    val mainLight        = Output(UInt(2.W)) // 車道信号出力 (DATA_LED[1:0]: P59, P58)
    val pedLight         = Output(Bool())    // 歩行者信号出力 (DATA_LED[7]: P81)
  })

  // TODO:
  // 1. OneHzGenerator(clkFreq) と TrafficLightController を Module(new ...) でインスタンス化
  // 2. oneHz.io.oneHzPulse を fsm.io.en に接続し、1秒パルスをイネーブルとしてFSMを動作させる
  // 3. TeCボードのLED負論理に合わせて反転出力 (~oneHz.io.ledBlink, ~fsm.io.mainLight, ~fsm.io.pedLight) を接続
  io.runLED    := true.B
  io.mainLight := 0.U(2.W)
  io.pedLight  := true.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new FSMIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

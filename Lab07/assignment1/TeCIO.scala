import chisel3._

// トップモジュール: OneHzGenerator と FourStateFSM を呼び出し、1秒パルスをイネーブルとしてFSMを動作させる
class FSMIO(val clkFreq: Int = 9000000) extends Module {
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
    new FSMIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

import chisel3._
import chisel3.util._

// 課題1: 4状態を遷移し続けるMoore型FSM (ワンホット出力)
class FourStateFSM(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val run    = Input(Bool())      // ランボタン (RUN_SW: P16) 押下で動作開始
    val clear  = Input(Bool())      // リセットスイッチ (RESET_SW: P120) 押下で初期状態へ復帰
    val runLED = Output(Bool())     // 動作状態インジケータ (RUN_LED: P43)
    val out    = Output(UInt(4.W))  // ワンホット状態出力 (DATA_LED[3:0]: P62, P61, P59, P58)
  })

  object State extends ChiselEnum {
    val s0, s1, s2, s3 = Value
  }

  // TODO:
  // 1. リセットスイッチ (io.clear) 押下で初期状態 State.s0 ("b0001") に戻り停止
  // 2. ランボタン (io.run) 押下で動作開始し、s0 -> s1 -> s2 -> s3 -> s0 の4状態を巡回遷移
  // 3. 状態をワンホット ("b0001", "b0010", "b0100", "b1000") として io.out に出力
  io.out    := "b0001".U(4.W)
  io.runLED := true.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new FourStateFSM,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

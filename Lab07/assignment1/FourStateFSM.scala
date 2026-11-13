import chisel3._
import chisel3.util._

// 課題1: 4状態を遷移し続けるMoore型FSM (ワンホット出力)
class FourStateFSM extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())     // イネーブル信号 (OneHzGeneratorからの1秒パルス)
    val clear = Input(Bool())     // 同期クリア (リセットスイッチ押下で初期状態 s0 へ復帰)
    val out   = Output(UInt(4.W)) // ワンホット状態出力 ("b0001", "b0010", "b0100", "b1000")
  })

  object State extends ChiselEnum {
    val s0, s1, s2, s3 = Value
  }

  // TODO:
  // 1. 状態レジスタ stateReg を初期値 State.s0 で生成
  // 2. io.clear のとき State.s0 へ初期化、io.en のとき s0 -> s1 -> s2 -> s3 -> s0 と巡回遷移
  // 3. Moore型出力として各状態をワンホット ("b0001", "b0010", "b0100", "b1000") で io.out に出力
  io.out := "b0001".U(4.W)
}

// object VerilogGenerator extends App {
//   _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
//     new FourStateFSM,
//     Array("--target-dir", "generated"),
//     Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
//   )
// }

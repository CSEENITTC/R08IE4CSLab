import chisel3._
import chisel3.util._

// 課題2: Moore型 "101" パターン検出器 (ワンホット状態出力 + 検出フラグ出力)
class SequenceDetector101 extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())     // イネーブル信号 (OneHzGeneratorからの1秒パルス)
    val clear    = Input(Bool())     // 同期クリア (初期状態 sIDLE へ復帰)
    val in       = Input(Bool())     // シリアルビット入力 (DATA_SW[0]: P35)
    val out      = Output(UInt(4.W)) // ワンホット状態出力 ("b0001", "b0010", "b0100", "b1000")
    val detected = Output(Bool())    // "101" 検出出力 (ADDR_LED[0]: P83)
  })

  object State extends ChiselEnum {
    val sIDLE, s1, s10, s101 = Value
  }

  // TODO:
  // 1. 状態レジスタ stateReg を初期値 State.sIDLE で生成
  // 2. io.clear のとき State.sIDLE へ初期化、io.en のとき io.in に応じて重複あり "101" の状態遷移を記述
  // 3. Moore型出力として各状態をワンホット ("b0001", "b0010", "b0100", "b1000") で io.out に出力し、
  //    stateReg === State.s101 のときのみ io.detected を true.B に駆動
  io.out      := "b0001".U(4.W)
  io.detected := false.B
}

// object VerilogGenerator extends App {
//   _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
//     new SequenceDetector101,
//     Array("--target-dir", "generated"),
//     Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
//   )
// }

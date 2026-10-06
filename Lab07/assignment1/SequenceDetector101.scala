import chisel3._
import chisel3.util._

// 基本問題: Moore型 "101" パターン検出器
class SequenceDetector101 extends Module {
  val io = IO(new Bundle {
    val in       = Input(Bool())
    val detected = Output(Bool())
  })

  object State extends ChiselEnum {
    val sIDLE, s1, s10, s101 = Value
  }

  // TODO: ChiselEnum を用いて状態を定義し、グリッチフリーなMoore型FSMを実装してください
  // 入力系列 "101" の重複検出 ("10101" で2回パルス) に対応してください
  io.detected := false.B
}

object SequenceDetector101Generator extends App {
  emitVerilog(new SequenceDetector101, Array("--target-dir", "generated"))
}

import chisel3._
import chisel3.util._

// 基本問題: 自己復帰回路付き 4ビットRingカウンタ
class SelfCorrectingRingCounter(val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val state = RegInit(1.U(width.W))

  // TODO: 不正状態(PopCount!=1)からの自動復帰論理を持つ1-hot循環シフトを実装してください
  // 正常時: 左循環シフト (Cat(state(width - 2, 0), state(width - 1)))
  // 異常時: 次クロックで自動的に 1.U に強制復帰
  io.out := state
}

object SelfCorrectingRingCounterGenerator extends App {
  emitVerilog(new SelfCorrectingRingCounter(4), Array("--target-dir", "generated"))
}

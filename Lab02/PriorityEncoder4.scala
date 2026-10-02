import chisel3._
import chisel3.util._

// 発展問題: 4入力優先度付きエンコーダ
class PriorityEncoder4 extends Module {
  val io = IO(new Bundle {
    val in    = Input(UInt(4.W)) // in(3) が最高優先度、in(0) が最低優先度
    val pos   = Output(UInt(2.W))
    val valid = Output(Bool())
  })

  // TODO: 最高位のアクティブビット (in(3) > in(2) > in(1) > in(0)) の位置を pos に出力してください
  // 有効な入力がある場合は valid を true.B、全ビットが0の場合は false.B としてください
  //
  // 優先度仕様:
  // in(3) == 1 -> pos = 3, valid = true
  // in(2) == 1 -> pos = 2, valid = true
  // in(1) == 1 -> pos = 1, valid = true
  // in(0) == 1 -> pos = 0, valid = true
  // in == 0    -> pos = 0 (任意), valid = false
  io.pos   := 0.U
  io.valid := false.B
}

object PriorityEncoder4Generator extends App {
  emitVerilog(new PriorityEncoder4, Array("--target-dir", "generated"))
}

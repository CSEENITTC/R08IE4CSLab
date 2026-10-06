import chisel3._

// 基本問題: ロード・イネーブル・同期クリア付き 任意進数カウンタ
class ModuloNCounter(val n: Int = 10, val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val load     = Input(Bool())
    val loadData = Input(UInt(width.W))
    val count    = Output(UInt(width.W))
    val rollover = Output(Bool())
  })

  val cntReg = RegInit(0.U(width.W))

  // TODO: clear -> load -> en の優先度でカウント動作と rollover 信号を実装してください
  // clear: 次クロックで 0 にリセット
  // load: 次クロックで loadData を格納 (n 未満にクリップ)
  // en: 通常カウント (count == n-1 のとき rollover を 1 クロック true.B にし 0 に復帰)
  io.count    := cntReg
  io.rollover := false.B
}

object ModuloNCounterGenerator extends App {
  emitVerilog(new ModuloNCounter(10, 4), Array("--target-dir", "generated"))
}

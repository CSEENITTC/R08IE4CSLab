import chisel3._

// 基本問題2: 1秒ごとにカウントアップする同期クリア付きモジュロNカウンタ
// 1HzGenerator等の 1Hz ストローブパルス (oneHzPulse) を io.en に入力することで、
// 1秒ごとに 0 から n-1 までインクリメントし、満了時に 0 に戻る
class ModuloNCounter(val n: Int = 10, val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())         // カウントイネーブル (1Hzパルス等を接続)
    val clear    = Input(Bool())         // 同期クリア (最優先で 0 にリセット)
    val count    = Output(UInt(width.W)) // 現在のカウント値出力
    val rollover = Output(Bool())        // 周期満了パルス (count == n-1 かつ en 有効時に 1 クロック High)
  })

  val cntReg = RegInit(0.U(width.W))

  // TODO: clear -> en の優先度でカウント動作と rollover 信号を実装してください
  // clear: 次クロックで 0 にリセット (最優先)
  // en: 通常カウント (count == n-1 のとき次クロックで 0 に復帰、それ以外は +1)
  // rollover: cntReg が n-1 かつ en が有効なとき、1 クロックだけ true.B にアサート
  // ※ カウンタ更新論理と出力論理を明確に分離して記述すること

  io.count    := cntReg
  io.rollover := false.B
}

object ModuloNCounterGenerator extends App {
  emitVerilog(new ModuloNCounter(10, 4), Array("--target-dir", "generated"))
}

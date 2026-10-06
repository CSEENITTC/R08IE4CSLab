import chisel3._
import chisel3.util.log2Ceil

// 基本問題1: クロック分周による 1Hz 信号生成・0.5秒LED点滅カウンタ
// デフォルトシステムクロック: 9MHz (9,000,000 Hz)
// 0.5秒 (4,500,000 サイクル) ごとに LED 出力を反転 (トグル) し、1Hzで点滅させる
// 同時に 1秒 (9,000,000 サイクル) に1回、1クロック幅のストローブパルス (oneHzPulse) を出力
class OneHzGenerator(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en          = Input(Bool()) // カウントイネーブル
    val clear       = Input(Bool()) // 同期クリア (最優先)
    val ledBlink    = Output(Bool()) // 0.5秒ごとに反転する点滅信号 (LED駆動用)
    val oneHzPulse  = Output(Bool()) // 1秒に1回、1クロック幅だけ High になる同期パルス
  })

  // 半周期 (0.5秒) のサイクル数: 9,000,000 / 2 = 4,500,000
  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth = log2Ceil(fullPeriod)

  val cntReg = RegInit(0.U(cntWidth.W))
  val ledReg = RegInit(false.B)

  // TODO: clear -> en の優先度でカウント動作を実装してください
  // clear: cntReg を 0、ledReg を false.B に初期化
  // en 有効時:
  //   - cntReg が fullPeriod - 1 に達したら 0 にラップアラウンド
  //   - それ以外は cntReg + 1.U
  //   - cntReg が halfPeriod - 1、および fullPeriod - 1 のタイミングで ledReg を反転 (0.5秒ごと点滅)
  //   - cntReg が fullPeriod - 1 かつ en が有効なとき、oneHzPulse を 1 クロックだけ true.B にアサート

  io.ledBlink   := ledReg
  io.oneHzPulse := false.B
}

object OneHzGeneratorGenerator extends App {
  emitVerilog(new OneHzGenerator(9000000), Array("--target-dir", "generated"))
}

import chisel3._

// 発展問題: プログラマブルPWMパルス発生器
// - periodMax: カウンタのハードウェア最大上限値 (デフォルト 255)
// - io.period: 周期カウンタの上限値 (cntReg は 0 〜 period を巡回するため、1周期は period + 1 クロック)
//              例: period = 4 のとき、cntReg は 0, 1, 2, 3, 4 と変化し 5クロックで1周期となる
// - io.duty  : 1周期のうち High (true.B) を出力するクロック数 (cntReg < duty の期間だけ High)
//              例: period = 4, duty = 2 のとき、cntReg = 0, 1 の 2クロック間 High (デューティ比 2/5 = 40%)
// - io.en    : 動作イネーブル (true.B でカウント＆PWM出力、false.B でカウンタ0リセット＆出力 false.B)
// - io.pwmOut: PWMパルス出力
class PwmGenerator(val periodMax: Int = 255) extends Module {
  val io = IO(new Bundle {
    val period = Input(UInt(8.W)) // カウンタ上限値 (1周期 = period + 1 クロック)
    val duty   = Input(UInt(8.W)) // High出力クロック数 (cntReg < duty の間 High)
    val en     = Input(Bool())    // 動作イネーブル
    val pwmOut = Output(Bool())   // PWM出力
  })

  val cntReg = RegInit(0.U(8.W))

  // TODO: カウンタと比較器を組み合わせ、デューティ比に応じたPWMパルスを生成してください
  // 1. io.en が true.B のとき:
  //    cntReg が io.period 以上 (または periodMax.U 以上) に達したら 0.U に戻し、それ以外は +1.U する
  //    io.en が false.B のときは cntReg を 0.U にリセットする
  // 2. io.pwmOut:
  //    io.en が有効、かつ cntReg < io.duty (かつ io.duty > 0.U) のとき true.B、それ以外は false.B
  io.pwmOut := false.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new PwmGenerator,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

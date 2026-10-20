import chisel3._
import chisel3.util.log2Ceil

// 基本問題2: 1秒ごとにカウントアップする同期クリア付きモジュロNカウンタ
// 9MHzシステムクロックから1秒パルスを生成し、1秒ごとに 0 〜 n-1 をインクリメント
// FPGA実機に合わせて負論理 (0で点灯) で LED に出力
class ModuloNCounter(val n: Int = 10, val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())         // カウントイネーブル
    val clear    = Input(Bool())         // 同期クリア (最優先で 0 にリセット)
    val count    = Output(UInt(width.W)) // カウント出力 (実機LED向け負論理)
    val runLED   = Output(Bool())        // 動作確認用LED (0.5秒ごとに反転、負論理)
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq

  val modReg   = RegInit(0.U(width.W))
  val cntWidth = log2Ceil(clkFreq)
  val cntReg   = RegInit(0.U(cntWidth.W))
  val ledReg   = RegInit(false.B)
  val modEn    = WireDefault(false.B)

  // TODO: clear -> en の優先度でカウント動作とLED制御を実装してください
  // 1. カウンタ更新論理 (cntReg: 0 〜 fullPeriod - 1 を周期カウント)
  // 2. 1秒ストローブパルス生成: modEn := io.en && (cntReg === (fullPeriod - 1).U)
  // 3. モジュロN更新論理 (modReg: clear で 0、modEn 有効時に 0 〜 n-1 をカウントアップ)
  // 4. LED点滅更新論理 (ledReg: clear で false、halfPeriod-1 および fullPeriod-1 で反転)
  // 5. 出力論理 (カウンタ記述と明確に分離し、負論理 ~ledReg, ~modReg を接続)

  io.runLED := ~ledReg
  io.count  := ~modReg
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new ModuloNCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

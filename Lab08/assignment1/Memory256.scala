import chisel3._
import chisel3.util.experimental.loadMemoryFromFileInline

// 課題1: 初期値付き 256バイト 非同期読み出しメモリ (256ワード × 8ビット)
class Memory256(val initHexFile: String = "mem_init.hex") extends Module {
  val io = IO(new Bundle {
    val addr  = Input(UInt(8.W))  // 8ビット アドレス入力 (0〜255)
    val wen   = Input(Bool())     // 書き込みイネーブル
    val wdata = Input(UInt(8.W))  // 8ビット 書き込みデータ
    val rdata = Output(UInt(8.W)) // 8ビット 非同期読み出しデータ
  })

  // TODO:
  // 1. Mem(256, UInt(8.W)) で 256バイトの非同期読み出しメモリ mem を宣言
  // 2. loadMemoryFromFileInline(mem, initHexFile) を用いて初期値ファイルをロード
  // 3. when(io.wen) { mem(io.addr) := io.wdata } でクロック同期書き込み
  // 4. io.rdata := mem(io.addr) で非同期読み出し
  io.rdata := 0.U(8.W)
}

// object VerilogGenerator extends App {
//   _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
//     new Memory256,
//     Array("--target-dir", "generated"),
//     Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
//   )
// }

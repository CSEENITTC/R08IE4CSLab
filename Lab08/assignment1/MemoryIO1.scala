import chisel3._

// トップモジュール: TeCボードのスイッチとLEDを用いて 256バイトメモリ (Memory256) を操作するラッパー回路
class MemoryIO1 extends Module {
  val io = IO(new Bundle {
    val dataSw  = Input(UInt(8.W))  // データスイッチ入力 (DATA_SW[7:0]: P26..P35)
    val setA    = Input(Bool())     // アドレス直接セットボタン (SETA_SW: P21)
    val incA    = Input(Bool())     // アドレス +1 ボタン (INCA_SW: P22)
    val decA    = Input(Bool())     // アドレス -1 ボタン (DECA_SW: P23)
    val writeEn = Input(Bool())     // メモリ書き込みボタン (WRITE_SW: P24)
    val clear   = Input(Bool())     // アドレスリセットスイッチ (RESET_SW: P120)
    val addrLed = Output(UInt(8.W)) // アドレス表示LED (ADDR_LED[7:0]: P94..P83)
    val dataLed = Output(UInt(8.W)) // 読み出しデータ表示LED (DATA_LED[7:0]: P81..P58)
  })

  // 1. 8ビット アドレスレジスタとチャタリング/長押し対策用の1クロック立ち上がりエッジ検出
  val addrReg = RegInit(0.U(8.W))
  val incPrev = RegNext(io.incA, false.B)
  val decPrev = RegNext(io.decA, false.B)
  val incPulse = io.incA && !incPrev
  val decPulse = io.decA && !decPrev

  when(io.clear) {
    addrReg := 0.U
  }.elsewhen(io.setA) {
    addrReg := io.dataSw
  }.elsewhen(incPulse) {
    addrReg := addrReg + 1.U
  }.elsewhen(decPulse) {
    addrReg := addrReg - 1.U
  }

  // TODO:
  // 2. Memory256("mem_init.hex") を Module(new ...) でインスタンス化し、addrReg, io.writeEn, io.dataSw を接続
  // 3. TeCボードのLED負論理に合わせて io.addrLed := ~addrReg, io.dataLed := ~mem256.io.rdata を出力
  io.addrLed := ~addrReg
  io.dataLed := 0.U(8.W)
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new MemoryIO1,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

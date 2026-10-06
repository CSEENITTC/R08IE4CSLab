import chisel3._

// 任意進数カウンタ (ヘルパーモジュール)
class ModuloNCounter(val n: Int, val width: Int) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val load     = Input(Bool())
    val loadData = Input(UInt(width.W))
    val count    = Output(UInt(width.W))
    val rollover = Output(Bool())
  })
  val cntReg = RegInit(0.U(width.W))
  when(io.clear) {
    cntReg := 0.U
  }.elsewhen(io.load) {
    cntReg := Mux(io.loadData < n.U, io.loadData, 0.U)
  }.elsewhen(io.en) {
    when(cntReg === (n - 1).U) { cntReg := 0.U }.otherwise { cntReg := cntReg + 1.U }
  }
  io.count    := cntReg
  io.rollover := io.en && (cntReg === (n - 1).U)
}

// 基本問題: 同期カスケード 60進カウンタ
class Counter60 extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val cout     = Output(Bool())    // 59でアサート
  })

  // TODO: 10進カウンタと6進カウンタを同期カスケード(cout -> en接続)で階層化設計してください
  // ※ リップルクロック（非同期クロック）の使用は厳禁です
  io.secUnits := 0.U
  io.secTens  := 0.U
  io.cout     := false.B
}

object Counter60Generator extends App {
  emitVerilog(new Counter60, Array("--target-dir", "generated"))
}

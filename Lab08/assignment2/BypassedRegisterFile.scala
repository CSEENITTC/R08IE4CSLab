import chisel3._

// 2R1W レジスタファイル (ベース)
class RegisterFile8x8 extends Module {
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(3.W))
    val raddr2 = Input(UInt(3.W))
    val rdata1 = Output(UInt(8.W))
    val rdata2 = Output(UInt(8.W))
    val wen    = Input(Bool())
    val waddr  = Input(UInt(3.W))
    val wdata  = Input(UInt(8.W))
  })
  val mem = Mem(8, UInt(8.W))
  when(io.wen) { mem(io.waddr) := io.wdata }
  io.rdata1 := mem(io.raddr1)
  io.rdata2 := mem(io.raddr2)
}

// 発展問題: RAWバイパス回路付き レジスタファイル
class BypassedRegisterFile extends Module {
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(3.W))
    val raddr2 = Input(UInt(3.W))
    val rdata1 = Output(UInt(8.W))
    val rdata2 = Output(UInt(8.W))
    val wen    = Input(Bool())
    val waddr  = Input(UInt(3.W))
    val wdata  = Input(UInt(8.W))
  })

  // TODO: RegisterFile8x8 をインスタンス化し、同一サイクル書き込み・読み出しのRAW競合時に
  // メモリ配列を通さず wdata を直接フォワーディングするバイパス回路を実装してください
  io.rdata1 := 0.U
  io.rdata2 := 0.U
}

object BypassedRegisterFileGenerator extends App {
  emitVerilog(new BypassedRegisterFile, Array("--target-dir", "generated"))
}

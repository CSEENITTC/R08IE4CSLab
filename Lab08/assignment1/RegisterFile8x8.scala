import chisel3._

// 基本問題: 2R1W 非同期読み出しレジスタファイル (8ワード×8ビット)
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

  // TODO: Mem(8, UInt(8.W)) を用いて同期書き込み・非同期読み出し回路を実装してください
  // 同期書き込み: when(io.wen) { mem(io.waddr) := io.wdata }
  // 非同期読み出し: io.rdata1 := mem(io.raddr1), io.rdata2 := mem(io.raddr2)
  io.rdata1 := 0.U
  io.rdata2 := 0.U
}

object RegisterFile8x8Generator extends App {
  emitVerilog(new RegisterFile8x8, Array("--target-dir", "generated"))
}

import chisel3._
import chisel3.util._

class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd  = Input(UInt(8.W))
    val op  = Input(UInt(2.W))
    val weA = Input(Bool())
    val weB = Input(Bool())
    val res = Output(UInt(8.W))
  })

  val regA = RegInit(0.U(8.W))
  val regB = RegInit(0.U(8.W))

  when(io.weA) {
    regA := io.wd
  }
  when(io.weB) {
    regB := io.wd
  }

  val lu = Module(new LogicUnit())

  lu.io.op := io.op
  lu.io.a  := regA
  lu.io.b  := regB
  io.res   := ~lu.io.res
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipelinedALUWithFlagstb extends AnyFlatSpec with ChiselScalatestTester {
  it should "output results and flags with 1 clock cycle latency" in {
    test(new PipelinedALUWithFlags).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.poke(5.U)
      dut.io.b.poke(5.U)
      dut.io.aluSel.poke("b001".U) // SUB: 5 - 5 = 0
      dut.io.validIn.poke(true.B)

      dut.clock.step(1)
      dut.io.validOut.expect(true.B)
      dut.io.out.expect(0.U)
      dut.io.zero.expect(true.B)
      println(s"1-cycle delayed: out=${dut.io.out.peekInt()}, zero=${dut.io.zero.peekBoolean()}")
    }
  }
}

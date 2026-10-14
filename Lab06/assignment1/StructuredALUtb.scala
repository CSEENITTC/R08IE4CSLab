import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class StructuredALUtb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly perform arithmetic and logic operations" in {
    test(new StructuredALU).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.a.poke(10.U)
      dut.io.b.poke(4.U)

      // ADD (aluSel = 000) -> 14
      dut.io.aluSel.poke("b000".U)
      dut.clock.step(1)
      dut.io.out.expect(14.U)

      // SUB (aluSel = 001) -> 6
      dut.io.aluSel.poke("b001".U)
      dut.clock.step(1)
      dut.io.out.expect(6.U)

      // AND (aluSel = 100) -> 10 & 4 = 0
      dut.io.aluSel.poke("b100".U)
      dut.clock.step(1)
      dut.io.out.expect(0.U)

      // OR (aluSel = 101) -> 10 | 4 = 14
      dut.io.aluSel.poke("b101".U)
      dut.clock.step(1)
      dut.io.out.expect(14.U)

      // XOR (aluSel = 110) -> 10 ^ 4 = 14
      dut.io.aluSel.poke("b110".U)
      dut.clock.step(1)
      dut.io.out.expect(14.U)
    }
  }
}

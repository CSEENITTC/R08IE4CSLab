import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PriorityEncoder4tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly encode highest active bit and valid flag" in {
    test(new PriorityEncoder4).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (inVal <- 0 to 15) {
        dut.io.in.poke(inVal.U(4.W))
        dut.clock.step(1)

        val (expectedPos, expectedValid) = if (inVal == 0) {
          (0, false)
        } else if ((inVal & 8) != 0) {
          (3, true)
        } else if ((inVal & 4) != 0) {
          (2, true)
        } else if ((inVal & 2) != 0) {
          (1, true)
        } else {
          (0, true)
        }

        dut.io.valid.expect(expectedValid.B)
        if (expectedValid) {
          dut.io.pos.expect(expectedPos.U(2.W))
        }
        val inBin = String.format("%4s", inVal.toBinaryString).replace(' ', '0')
        println(f"in=0b$inBin%s (dec=$inVal%2d) => pos=${dut.io.pos.peekInt()}%d, valid=${dut.io.valid.peekBoolean()}%b (expected pos=$expectedPos%d, valid=$expectedValid%b)")
      }
    }
  }
}

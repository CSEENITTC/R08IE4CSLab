import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class JohnsonCountertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "cycle through 2N unique states" in {
    test(new JohnsonCounter(4)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.en.poke(true.B)
      val expected = Seq(
        "b0000", "b0001", "b0011", "b0111",
        "b1111", "b1110", "b1100", "b1000"
      )

      for (i <- 0 until 16) {
        val exp = expected(i % 8)
        println(s"step=$i => out=b${dut.io.out.peekInt().toInt.toBinaryString} (expected: $exp)")
        dut.io.out.expect(exp.U(4.W))
        dut.clock.step(1)
      }
    }
  }
}

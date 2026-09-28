import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HalfAddertb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly implement an And gate" in {
    test(new HalfAdder).withAnnotations(Seq(WriteVcdAnnotation)) {dut => 
      // Test case 1: 0 AND 0 = 0
      println("Start test")
      dut.io.a.poke(0.U(1.W))
      dut.io.b.poke(0.U(1.W))
      step(1)
//      dut.io.out.expect(false.B)
//      dut.io.out.expect(true.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")
      

      // Test case 2: 0 AND 1 = 0
      dut.io.a.poke(false.B)
      dut.io.b.poke(true.B)
      step(1)
    //   dut.io.out.expect(false.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")

      // Test case 2: 0 AND 1 = 0
      dut.io.a.poke(true.B)
      dut.io.b.poke(true.B)
      step(1)
    //   dut.io.out.expect(false.B)
      println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},sum=>${dut.io.sum.peekInt()},cout=>${dut.io.cout.peekInt()}")

    }
  }
}
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class AdderWithParity4tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly implement an And gate" in {
    test(new AdderWithParity4).withAnnotations(Seq(WriteVcdAnnotation)) {dut => 
      // Test case 1: 2 + 3 
        check(dut,2,3,0,5,false,false)
    // Test case 1: 7 + 7 
        check(dut,7,7,0,14,false,true)
    // Test case 1: 8 + 8 
        check(dut,8,8,0,0,true,false)
        
    }
  }

    def check(
        dut: AdderWithParity4,
        a: Int,
        b: Int,
        cin: Int,
        sum: Int,
        cout: Boolean,
        parity: Boolean
    ): Unit = {
    dut.io.a.poke(a)
    dut.io.b.poke(b)
    dut.io.cin.poke(cin)
    dut.io.sum.expect(sum)
    dut.io.cout.expect(cout)
    dut.io.parity.expect(parity)

    dut.clock.step()

    println(s"a=${dut.io.a.peekInt()},b=${dut.io.b.peekInt()},cin=${dut.io.cin.peekInt()}," +
      s"sum=${dut.io.sum.peekInt()}," +
      s"parity=${dut.io.parity.peek()}," +
      s"cout=${dut.io.cout.peek()}"
      )
    }
}
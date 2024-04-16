package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.x64;

public class Ret extends Instruction {
	public Ret() {
		opcodeBytes.write( 0xC3 );
	}
	
	public Ret(short imm16, short mult) {
		opcodeBytes.write( 0xC2 );
		x64.writeShort(immBytes,imm16*mult);
	}
	
	//TODO: why does this pass 8 to Ret() instead of 1??
	public Ret(short imm16) {
		this(imm16,(short)8);
	}
}

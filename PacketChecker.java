import java.net.*;
/**
 * PacketChecker - A class to check the errors of a packet
 * @author H Rose
 * @author Guiseppe Giambanco
 * @author Seth Button-Mosher
 * @version 2205
 */

public class PacketChecker implements TFTPConstants {

   public static String decipher(DatagramPacket pkt) {
      Packet packet = new Packet();
      packet.dissectPacket(pkt);
      String result = "";
      if (packet.getOpcode() > 0 && packet.getOpcode() < 6) {
         result = String.format("Opcode (%d)", packet.getOpcode()); 
      }
      if (packet.getNumber() > -1 && packet.getNumber() < 8) {
         switch (packet.getNumber()) {
            case 1:
            case 2:
               String temp = String.format("\nFilename <%s> Mode <%s> Cannot access server file", packet.getS1(), packet.getS2());
               result = temp + result;
               return result;
            case 4:
               result = result + "Block#: " + packet.getNumber();
         }
      }
      return result;
   }
}